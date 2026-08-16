package unboundtech.common.tiles;

import ic2.api.energy.prefab.BasicSink;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.config.ConfigItems;
import unboundtech.common.blocks.BlockMachineBase;
import unboundtech.energy.EnergyCanon;

/**
 * Фиал-станция (`05_objects/phial_station.md`): мост между «трубной»
 * эссенцией и «предметной». В ТК они живут в разных мирах, и переливать их
 * руками — самая скучная рутина алхимика.
 *
 * Два режима, переключаются ключом (`machine_feedback.md` §5):
 * розлив (труба → фиал) и слив (фиал → труба). Фильтр аспекта задаётся ПКМ
 * фиалом; пустой фильтр = «любой первый пришедший».
 *
 * Фиал ТК — это {@code ConfigItems.itemEssence}: мета 0 пустой, мета 1
 * полный, аспект лежит в NBT. Ровно восемь единиц, ни каплей больше.
 */
public class TilePhialStation extends TileThaumcraft
        implements ITickable, IMachineStatus, IEssentiaTransport {

    /** §5: буфер 2 000 EU — десять операций. */
    public static final double CAPACITY = 2_000.0;
    private static final int TIER = 1;

    /** §5: ёмкость фиала и внутреннего буфера — ровно один фиал. */
    public static final int PHIAL_AMOUNT = 8;

    /** §5: 1 фиал за 2 секунды. */
    private static final int CYCLE = 40;

    /** Тяга потребителя — выше трубы, иначе труба не отдаст. */
    private static final int SUCTION = 128;

    private static final int ACTIVE_HOLD_TICKS = 100;

    private static final int SLOT_IN = 0;
    private static final int SLOT_OUT = 1;

    /** Мета фиала: 0 — пустой, 1 — полный (см. {@code ItemEssence}). */
    private static final int PHIAL_EMPTY = 0;
    private static final int PHIAL_FULL = 1;

    /** Режим работы; переключается ключом. */
    public enum Mode {
        /** Труба → фиал. */
        FILL,
        /** Фиал → труба. */
        DRAIN
    }

    private final BasicSink sink = new BasicSink(this, CAPACITY, TIER);

    private final ItemStackHandler slots = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            TilePhialStation.this.markDirty();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != SLOT_OUT && stack.getItem() == ConfigItems.itemEssence;
        }
    };

    private Mode mode = Mode.FILL;
    /** Фильтр аспекта; {@code null} — «любой первый пришедший». */
    private Aspect filter;

    private Aspect buffered;
    private int buffer;

    private int counter;
    private boolean active;
    private int activeHold;
    private String idleReason = "";

    @Override
    public boolean shouldRefresh(net.minecraft.world.World world,
            net.minecraft.util.math.BlockPos pos,
            net.minecraft.block.state.IBlockState oldState,
            net.minecraft.block.state.IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void update() {
        this.sink.update();
        if (this.world == null || this.world.isRemote) {
            return;
        }
        this.counter++;
        if (this.counter % CYCLE != 0) {
            return;
        }
        boolean worked = this.mode == Mode.FILL ? this.runFill() : this.runDrain();
        if (worked) {
            this.activeHold = ACTIVE_HOLD_TICKS;
        } else if (this.activeHold > 0) {
            this.activeHold -= CYCLE;
        }
        this.setActive(this.activeHold > 0);
    }

    // ================= режимы =================

    /** Розлив: копим 8 единиц из трубы и наполняем пустой фиал. */
    private boolean runFill() {
        this.pullEssentia();
        if (this.buffered == null || this.buffer < PHIAL_AMOUNT) {
            this.idleReason = "ждёт эссенцию";
            return false;
        }
        ItemStack in = this.slots.getStackInSlot(SLOT_IN);
        if (in.isEmpty() || in.getItem() != ConfigItems.itemEssence
                || in.getItemDamage() != PHIAL_EMPTY) {
            this.idleReason = "нет пустых фиалов";
            return false;
        }
        ItemStack filled = new ItemStack(ConfigItems.itemEssence, 1, PHIAL_FULL);
        ConfigItems.itemEssence.setAspects(filled,
                new AspectList().add(this.buffered, PHIAL_AMOUNT));
        if (!this.pushToOutput(filled, true)) {
            this.idleReason = "выходной слот занят";
            return false;   // §10: работа встаёт, эссенция остаётся в буфере
        }
        if (!this.spendEnergy()) {
            return false;
        }
        this.slots.extractItem(SLOT_IN, 1, false);
        this.pushToOutput(filled, false);
        this.buffer -= PHIAL_AMOUNT;
        if (this.buffer <= 0) {
            this.buffered = null;
        }
        this.markDirty();
        return true;
    }

    /** Слив: берём полный фиал и отдаём восемь единиц наружу. */
    private boolean runDrain() {
        ItemStack in = this.slots.getStackInSlot(SLOT_IN);
        if (in.isEmpty() || in.getItem() != ConfigItems.itemEssence
                || in.getItemDamage() != PHIAL_FULL) {
            this.idleReason = "нет полных фиалов";
            return false;
        }
        AspectList aspects = ConfigItems.itemEssence.getAspects(in);
        if (aspects == null || aspects.size() != 1) {
            this.idleReason = "непонятный фиал";
            return false;
        }
        Aspect aspect = aspects.getAspects()[0];
        ItemStack empty = new ItemStack(ConfigItems.itemEssence, 1, PHIAL_EMPTY);
        if (!this.pushToOutput(empty, true)) {
            this.idleReason = "выходной слот занят";
            return false;
        }
        if (this.pushEssentia(aspect, PHIAL_AMOUNT) < PHIAL_AMOUNT) {
            this.idleReason = "снаружи нет тяги";
            return false;   // §10: отдаём только при наличии тяги
        }
        if (!this.spendEnergy()) {
            return false;
        }
        this.slots.extractItem(SLOT_IN, 1, false);
        this.pushToOutput(empty, false);
        this.markDirty();
        return true;
    }

    private boolean spendEnergy() {
        if (!this.sink.canUseEnergy(EnergyCanon.EU_PER_PHIAL)) {
            this.idleReason = "не хватает энергии";
            return false;
        }
        this.sink.useEnergy(EnergyCanon.EU_PER_PHIAL);
        this.idleReason = "";
        return true;
    }

    private boolean pushToOutput(ItemStack stack, boolean simulate) {
        return this.slots.insertItem(SLOT_OUT, stack.copy(), simulate).isEmpty();
    }

    // ================= обмен с трубами =================

    private void pullEssentia() {
        if (this.mode != Mode.FILL || this.buffer >= PHIAL_AMOUNT) {
            return;
        }
        for (EnumFacing side : EnumFacing.VALUES) {
            IEssentiaTransport transport = this.neighbour(side);
            if (transport == null) {
                continue;
            }
            EnumFacing remote = side.getOpposite();
            if (!transport.canOutputTo(remote) || transport.getEssentiaAmount(remote) <= 0) {
                continue;
            }
            int ours = this.getSuctionAmount(side);
            if (ours <= transport.getSuctionAmount(remote)
                    || ours < transport.getMinimumSuction()) {
                continue;
            }
            Aspect offered = transport.getEssentiaType(remote);
            if (!this.accepts(offered)) {
                continue;
            }
            int taken = transport.takeEssentia(offered, 1, remote);
            if (taken > 0) {
                this.buffered = offered;
                this.buffer += taken;
                this.markDirty();
                return;
            }
        }
    }

    /** Отдаёт эссенцию наружу; возвращает, сколько удалось отдать. */
    private int pushEssentia(Aspect aspect, int amount) {
        int left = amount;
        for (EnumFacing side : EnumFacing.VALUES) {
            IEssentiaTransport transport = this.neighbour(side);
            if (transport == null) {
                continue;
            }
            EnumFacing remote = side.getOpposite();
            if (!transport.canInputFrom(remote)) {
                continue;
            }
            Aspect wanted = transport.getSuctionType(remote);
            if (wanted != null && wanted != aspect) {
                continue;
            }
            if (transport.getSuctionAmount(remote) <= 0) {
                continue;   // тяги нет — насильно не пихаем
            }
            while (left > 0) {
                int accepted = transport.addEssentia(aspect, 1, remote);
                if (accepted <= 0) {
                    break;
                }
                left -= accepted;
            }
            if (left <= 0) {
                break;
            }
        }
        return amount - left;
    }

    private IEssentiaTransport neighbour(EnumFacing side) {
        TileEntity te = ThaumcraftApiHelper.getConnectableTile(this.world,
                this.pos.getX(), this.pos.getY(), this.pos.getZ(), side);
        return te instanceof IEssentiaTransport ? (IEssentiaTransport) te : null;
    }

    private boolean accepts(Aspect aspect) {
        if (aspect == null) {
            return false;
        }
        if (this.filter != null && this.filter != aspect) {
            return false;   // §10: при заданном фильтре чужое не тянем
        }
        return this.buffered == null || this.buffered == aspect;
    }

    // ================= взаимодействие =================

    /** ПКМ ключом: сменить режим (§9). */
    public Mode toggleMode() {
        this.mode = this.mode == Mode.FILL ? Mode.DRAIN : Mode.FILL;
        this.markDirty();
        return this.mode;
    }

    /** ПКМ фиалом: задать фильтр аспекта; пустым фиалом — сбросить (§4). */
    public Aspect applyFilter(ItemStack phial) {
        AspectList aspects = phial.getItem() == ConfigItems.itemEssence
                ? ConfigItems.itemEssence.getAspects(phial) : null;
        this.filter = aspects != null && aspects.size() == 1 ? aspects.getAspects()[0] : null;
        this.markDirty();
        return this.filter;
    }

    public Mode getMode() {
        return this.mode;
    }

    /** Фильтр аспекта; {@code null} — «любой». */
    public Aspect getFilter() {
        return this.filter;
    }

    /** Причина простоя словами — её же показывает GUI. */
    public String getIdleReason() {
        return this.idleReason;
    }

    public ItemStackHandler getSlots() {
        return this.slots;
    }

    private void setActive(boolean value) {
        if (this.active == value) {
            return;
        }
        this.active = value;
        BlockMachineBase.setActive(this.world, this.pos, value);
    }

    // ================= IEssentiaTransport =================

    @Override
    public boolean isConnectable(EnumFacing face) {
        return face != null;
    }

    @Override
    public boolean canInputFrom(EnumFacing face) {
        return face != null && this.mode == Mode.FILL;
    }

    @Override
    public boolean canOutputTo(EnumFacing face) {
        return face != null && this.mode == Mode.DRAIN;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
    }

    @Override
    public Aspect getSuctionType(EnumFacing face) {
        if (this.mode != Mode.FILL) {
            return null;
        }
        return this.buffered != null ? this.buffered : this.filter;
    }

    @Override
    public int getSuctionAmount(EnumFacing face) {
        return this.mode == Mode.FILL && this.buffer < PHIAL_AMOUNT ? SUCTION : 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, EnumFacing face) {
        if (this.mode != Mode.DRAIN || this.buffered != aspect || this.buffer <= 0) {
            return 0;
        }
        this.buffer--;
        if (this.buffer <= 0) {
            this.buffered = null;
        }
        this.markDirty();
        return 1;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, EnumFacing face) {
        if (this.mode != Mode.FILL || !this.accepts(aspect) || amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, PHIAL_AMOUNT - this.buffer);
        if (accepted <= 0) {
            return 0;
        }
        this.buffered = aspect;
        this.buffer += accepted;
        this.markDirty();
        return accepted;
    }

    @Override
    public Aspect getEssentiaType(EnumFacing face) {
        return this.buffered;
    }

    @Override
    public int getEssentiaAmount(EnumFacing face) {
        return this.buffer;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    // ================= IMachineStatus =================

    @Override
    public String getStatusLine() {
        int eu = (int) this.sink.getEnergyStored();
        String head = "Фиал-станция ["
                + (this.mode == Mode.FILL ? "розлив" : "слив") + ", фильтр: "
                + (this.filter == null ? "любой" : this.filter.getName()) + "]";
        String tail = ". Буфер: " + eu + " / " + (int) CAPACITY + " EU, эссенции "
                + this.buffer + " / " + PHIAL_AMOUNT;
        if (this.activeHold > 0) {
            return "§a" + head + ": работает" + tail;
        }
        return "§c" + head + ": " + (this.idleReason.isEmpty() ? "простой" : this.idleReason)
                + tail;
    }

    @Override
    public void writeWrenchNBT(NBTTagCompound tag) {
        this.sink.writeToNBT(tag);
        this.writeState(tag);
    }

    @Override
    public void readWrenchNBT(NBTTagCompound tag) {
        this.sink.readFromNBT(tag);
        this.readState(tag);
    }

    private void writeState(NBTTagCompound tag) {
        tag.setInteger("UTMode", this.mode.ordinal());
        tag.setString("UTFilter", this.filter == null ? "" : this.filter.getTag());
        tag.setString("UTAspect", this.buffered == null ? "" : this.buffered.getTag());
        tag.setInteger("UTAmount", this.buffer);
        tag.setTag("UTSlots", this.slots.serializeNBT());
    }

    private void readState(NBTTagCompound tag) {
        Mode[] modes = Mode.values();
        int ordinal = tag.getInteger("UTMode");
        this.mode = ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : Mode.FILL;
        String filterKey = tag.getString("UTFilter");
        this.filter = filterKey.isEmpty() ? null : Aspect.getAspect(filterKey);
        String key = tag.getString("UTAspect");
        this.buffered = key.isEmpty() ? null : Aspect.getAspect(key);
        this.buffer = tag.getInteger("UTAmount");
        if (this.buffered == null) {
            this.buffer = 0;
        }
        if (tag.hasKey("UTSlots")) {
            this.slots.deserializeNBT(tag.getCompoundTag("UTSlots"));
        }
    }

    public double getEnergyStored() {
        return this.sink.getEnergyStored();
    }

    @Override
    public void readCustomNBT(NBTTagCompound tag) {
        super.readCustomNBT(tag);
        this.sink.readFromNBT(tag);
        this.readState(tag);
        this.active = tag.getBoolean("UTActive");
        this.activeHold = tag.getInteger("UTActiveHold");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        this.sink.writeToNBT(tag);
        this.writeState(tag);
        tag.setBoolean("UTActive", this.active);
        tag.setInteger("UTActiveHold", this.activeHold);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.slots);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.sink.onLoad();
    }

    @Override
    public void invalidate() {
        this.sink.invalidate();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        this.sink.onChunkUnload();
        super.onChunkUnload();
    }
}
