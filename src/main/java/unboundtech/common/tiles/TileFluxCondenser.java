package unboundtech.common.tiles;

import ic2.api.energy.prefab.BasicSource;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import unboundtech.common.blocks.BlockMachineBase;
import unboundtech.energy.EnergyCanon;

/**
 * Флюкс-Конденсатор (`05_objects/flux_condenser.md`): стоит в конце чужой
 * цепочки уборки флюкса и превращает `Praecantatio` с родного Флюкс-Скруббера
 * ТК в электричество — уборка из повинности становится выгодой.
 *
 * Принимает ТОЛЬКО {@link Aspect#MAGIC}. Это принципиально (§4): после
 * вычеркивания Эссент. Горелки конденсатор — единственный сток эссенции в
 * моде, и снять ограничение — значит воссоздать удалённую горелку.
 *
 * Два режима, переключаются ключом (§4.2):
 *  - КОНДЕНСАЦИЯ: 1 `Praecantatio` → 2 000 EU;
 *  - СГУЩЕНИЕ: 4 `Praecantatio` + 2 000 EU ИЗ СОБСТВЕННОГО БУФЕРА →
 *    1 Флюкс-Заряд (выплёвывается предметом над блоком, как тигель ТК).
 *    Режим платит, а не зарабатывает: Заряд стоит 10 000 EU эквивалента,
 *    чтобы уборка флюкса оставалась выгоднее торговли им.
 *
 * Эссенцию тянет сам, приёмом {@code fillReservoir} порта: труба отдаёт
 * тому, у кого тяга больше; аргумент {@code EnumFacing} — грань ЭТОГО
 * тайла, соседу передаётся {@code side.getOpposite()}; аргумент бывает
 * {@code null} и не разыменовывается.
 */
public class TileFluxCondenser extends TileThaumcraft
        implements ITickable, IMachineStatus, IEssentiaTransport,
        unboundtech.common.gui.ISyncedMachine, unboundtech.common.gui.IEnergyGauge {

    /** Клиентские копии полей GUI (ХФ-7). */
    private int guiEnergy;
    private int guiEssentia;
    private int guiMode;
    private int guiState;

    private int stateCode() {
        if (this.mode == Mode.CONDENSE
                && this.source.getEnergyStored()
                        > CAPACITY - EnergyCanon.EU_PER_FLUX_ESSENTIA) {
            return 1;
        }
        if (this.essentia < (this.mode == Mode.THICKEN ? THICKEN_ESSENTIA : 1)) {
            return 2;
        }
        if (this.mode == Mode.THICKEN
                && !this.source.canUseEnergy(EnergyCanon.EU_PER_FLUX_ESSENTIA)) {
            return 3;
        }
        return 0;
    }

    @Override
    public int[] syncFields() {
        return new int[]{(int) this.source.getEnergyStored(), this.essentia,
                this.mode.ordinal(), this.stateCode()};
    }

    @Override
    public void applySyncField(int index, int value) {
        switch (index) {
            case 0: this.guiEnergy = value; break;
            case 1: this.guiEssentia = value; break;
            case 2: this.guiMode = value; break;
            case 3: this.guiState = value; break;
            default: break;
        }
    }

    @Override
    public double gaugeEnergy() {
        return this.world != null && this.world.isRemote
                ? this.guiEnergy : this.source.getEnergyStored();
    }

    @Override
    public double gaugeCapacity() {
        return CAPACITY;
    }

    public int guiEssentia() {
        return this.guiEssentia;
    }

    private String clientStatusLine() {
        String head = "Флюкс-Конденсатор ["
                + (this.guiMode == Mode.CONDENSE.ordinal()
                        ? "конденсация" : "сгущение") + "]";
        String tail = ". Мути: " + this.guiEssentia + " / " + ESSENTIA_BUFFER
                + ", буфер " + this.guiEnergy + " / " + (int) CAPACITY + " EU";
        switch (this.guiState) {
            case 1: return "§b" + head + ": буфер полон" + tail;
            case 2: return "§b" + head + ": ждёт эссенцию со скруббера" + tail;
            case 3: return "§e" + head + ": не хватает EU на сгущение" + tail;
            default: return "§a" + head + ": работает" + tail;
        }
    }

    /** §5: буфер 10 000 EU, выход LV (до 32 EU/t). */
    public static final double CAPACITY = 10_000.0;
    private static final int TIER = 1;

    /** §4: попытка обработки — раз в 20 тиков. */
    private static final int CYCLE = 20;
    /** §5: буфер эссенции — 8 единиц, размер фиала. */
    public static final int ESSENTIA_BUFFER = 8;

    /** §4.2: вход сгущения — 4 `Praecantatio` и одна порция EU. */
    private static final int THICKEN_ESSENTIA = 4;

    /** Тяга потребителя: выше трубы, иначе труба не отдаст. */
    private static final int SUCTION = 128;

    /** MF §2: состояние не мигает — держится не меньше 100 тиков. */
    private static final int ACTIVE_HOLD_TICKS = 100;

    /** Режим работы; переключается ключом (§4.2). */
    public enum Mode {
        /** 1 `Praecantatio` → EU. */
        CONDENSE,
        /** 4 `Praecantatio` + EU → Флюкс-Заряд. */
        THICKEN
    }

    private final BasicSource source = new BasicSource(this, CAPACITY, TIER);

    private int essentia;
    private Mode mode = Mode.CONDENSE;

    private int counter;
    private boolean active;
    private int activeHold;

    @Override
    public boolean shouldRefresh(net.minecraft.world.World world,
            net.minecraft.util.math.BlockPos pos,
            net.minecraft.block.state.IBlockState oldState,
            net.minecraft.block.state.IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void update() {
        this.source.update();
        if (this.world == null || this.world.isRemote) {
            return;
        }
        this.counter++;
        if (this.counter % CYCLE != 0) {
            return;
        }
        this.pullEssentia();
        boolean worked = this.mode == Mode.CONDENSE ? this.condenseOne() : this.thickenOnce();
        if (worked) {
            this.activeHold = ACTIVE_HOLD_TICKS;
            // Частицы: муть втягивается в колбу (§8). Смещения нет — нулевой
            // параметр рисует искры у самого блока.
            this.world.addBlockEvent(this.pos, this.getBlockType(),
                    TileThaumGenerator.EVENT_VIS_FLOW, TileThaumGenerator.packOffset(
                            this.pos, this.pos.up()));
        } else if (this.activeHold > 0) {
            this.activeHold -= CYCLE;
        }
        this.setActive(this.activeHold > 0);
    }

    /** Конденсация: единица мути — порция тока. */
    private boolean condenseOne() {
        if (this.essentia < 1) {
            return false;
        }
        int rate = EnergyCanon.EU_PER_FLUX_ESSENTIA;
        if (this.source.getEnergyStored() > CAPACITY - rate) {
            return false;   // §10: буфер полон — эссенция не тратится
        }
        this.essentia--;
        this.source.addEnergy(rate);
        this.markDirty();
        return true;
    }

    /** Сгущение: четыре единицы мути и порция тока — один Флюкс-Заряд. */
    private boolean thickenOnce() {
        int fee = EnergyCanon.EU_PER_FLUX_ESSENTIA;
        if (this.essentia < THICKEN_ESSENTIA || !this.source.canUseEnergy(fee)) {
            return false;
        }
        this.essentia -= THICKEN_ESSENTIA;
        this.source.useEnergy(fee);
        ItemStack charge = new ItemStack(unboundtech.common.UTItems.fluxCharge);
        EntityItem drop = new EntityItem(this.world,
                this.pos.getX() + 0.5, this.pos.getY() + 1.2, this.pos.getZ() + 0.5, charge);
        drop.motionX = 0.0;
        drop.motionY = 0.15;
        drop.motionZ = 0.0;
        this.world.spawnEntity(drop);
        this.markDirty();
        return true;
    }

    /** Тянет по единице `Praecantatio` из подключённых труб. */
    private void pullEssentia() {
        if (this.essentia >= ESSENTIA_BUFFER || !this.wantsEssentia()) {
            return;
        }
        for (EnumFacing side : EnumFacing.VALUES) {
            TileEntity te = ThaumcraftApiHelper.getConnectableTile(this.world,
                    this.pos.getX(), this.pos.getY(), this.pos.getZ(), side);
            if (!(te instanceof IEssentiaTransport)) {
                continue;
            }
            IEssentiaTransport transport = (IEssentiaTransport) te;
            EnumFacing remote = side.getOpposite();
            if (!transport.canOutputTo(remote) || transport.getEssentiaAmount(remote) <= 0) {
                continue;
            }
            if (transport.getEssentiaType(remote) != Aspect.MAGIC) {
                continue;   // §10: чужой аспект не движется
            }
            int ours = this.getSuctionAmount(side);
            if (ours <= transport.getSuctionAmount(remote)
                    || ours < transport.getMinimumSuction()) {
                continue;
            }
            int taken = transport.takeEssentia(Aspect.MAGIC, 1, remote);
            if (taken > 0) {
                this.essentia += taken;
                this.markDirty();
                return;
            }
        }
    }

    /** Тяга снимается, когда некуда работать (§10: ничего не теряется). */
    private boolean wantsEssentia() {
        if (this.mode == Mode.THICKEN) {
            return true;   // сгущению эссенции всегда мало
        }
        return this.source.getEnergyStored() <= CAPACITY - EnergyCanon.EU_PER_FLUX_ESSENTIA;
    }

    /** ПКМ ключом (§9): конденсация ↔ сгущение. */
    public Mode toggleMode() {
        this.mode = this.mode == Mode.CONDENSE ? Mode.THICKEN : Mode.CONDENSE;
        this.markDirty();
        return this.mode;
    }

    public Mode getMode() {
        return this.mode;
    }

    private void setActive(boolean value) {
        if (this.active == value) {
            return;
        }
        this.active = value;
        BlockMachineBase.setActive(this.world, this.pos, value);
    }

    /** Частицы мути у колбы — общий механизм конвертеров. */
    @Override
    public boolean receiveClientEvent(int id, int param) {
        if (this.world == null || !this.world.isRemote) {
            return id == TileThaumGenerator.EVENT_VIS_FLOW;
        }
        if (id == TileThaumGenerator.EVENT_VIS_FLOW) {
            TileThaumGenerator.spawnVisThread(this.world, this.pos, param, true);
            return true;
        }
        return super.receiveClientEvent(id, param);
    }

    // ================= IEssentiaTransport =================

    @Override
    public boolean isConnectable(EnumFacing face) {
        return face != null;
    }

    @Override
    public boolean canInputFrom(EnumFacing face) {
        return face != null;
    }

    @Override
    public boolean canOutputTo(EnumFacing face) {
        return false;   // чистый потребитель: эссенция отсюда не забирается (§9)
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
    }

    @Override
    public Aspect getSuctionType(EnumFacing face) {
        return Aspect.MAGIC;   // тяга объявлена только на Praecantatio
    }

    @Override
    public int getSuctionAmount(EnumFacing face) {
        return this.essentia < ESSENTIA_BUFFER && this.wantsEssentia() ? SUCTION : 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, EnumFacing face) {
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, EnumFacing face) {
        if (aspect != Aspect.MAGIC || amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, ESSENTIA_BUFFER - this.essentia);
        if (accepted <= 0) {
            return 0;
        }
        this.essentia += accepted;
        this.markDirty();
        return accepted;   // контракт: сколько ПРИНЯЛИ
    }

    @Override
    public Aspect getEssentiaType(EnumFacing face) {
        return this.essentia > 0 ? Aspect.MAGIC : null;
    }

    @Override
    public int getEssentiaAmount(EnumFacing face) {
        return this.essentia;
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
        if (this.world != null && this.world.isRemote) {
            return this.clientStatusLine();
        }
        int eu = (int) this.source.getEnergyStored();
        String head = "Флюкс-Конденсатор ["
                + (this.mode == Mode.CONDENSE ? "конденсация" : "сгущение") + "]";
        String tail = ". Мути: " + this.essentia + " / " + ESSENTIA_BUFFER
                + ", буфер " + eu + " / " + (int) CAPACITY + " EU";
        if (this.mode == Mode.CONDENSE
                && this.source.getEnergyStored() > CAPACITY - EnergyCanon.EU_PER_FLUX_ESSENTIA) {
            return "§b" + head + ": буфер полон" + tail;
        }
        if (this.essentia < (this.mode == Mode.THICKEN ? THICKEN_ESSENTIA : 1)) {
            return "§b" + head + ": ждёт эссенцию со скруббера" + tail;
        }
        if (this.mode == Mode.THICKEN
                && !this.source.canUseEnergy(EnergyCanon.EU_PER_FLUX_ESSENTIA)) {
            return "§e" + head + ": не хватает EU на сгущение" + tail;
        }
        return "§a" + head + ": работает" + tail;
    }

    @Override
    public void writeWrenchNBT(NBTTagCompound tag) {
        this.source.writeToNBT(tag);
        this.writeState(tag);
    }

    @Override
    public void readWrenchNBT(NBTTagCompound tag) {
        this.source.readFromNBT(tag);
        this.readState(tag);
    }

    private void writeState(NBTTagCompound tag) {
        tag.setInteger("UTEssentia", this.essentia);
        tag.setInteger("UTMode", this.mode.ordinal());
    }

    private void readState(NBTTagCompound tag) {
        this.essentia = tag.getInteger("UTEssentia");
        Mode[] modes = Mode.values();
        int ordinal = tag.getInteger("UTMode");
        this.mode = ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : Mode.CONDENSE;
    }

    public double getEnergyStored() {
        return this.source.getEnergyStored();
    }

    @Override
    public void readCustomNBT(NBTTagCompound tag) {
        super.readCustomNBT(tag);
        this.source.readFromNBT(tag);
        this.readState(tag);
        this.active = tag.getBoolean("UTActive");
        this.activeHold = tag.getInteger("UTActiveHold");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        this.source.writeToNBT(tag);
        this.writeState(tag);
        tag.setBoolean("UTActive", this.active);
        tag.setInteger("UTActiveHold", this.activeHold);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.source.onLoad();
    }

    @Override
    public void invalidate() {
        this.source.invalidate();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        this.source.onChunkUnload();
        super.onChunkUnload();
    }
}
