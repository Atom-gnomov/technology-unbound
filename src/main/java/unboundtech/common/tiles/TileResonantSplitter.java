package unboundtech.common.tiles;

import ic2.api.energy.prefab.BasicSink;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import unboundtech.common.blocks.BlockMachineBase;
import unboundtech.energy.EnergyCanon;

/**
 * Резонансный Расщепитель (`05_objects/resonant_splitter.md`): составной
 * аспект → ОБА компонента, без потерь и детерминированно. Родная центрифуга
 * ТК из единицы выдаёт один случайный компонент (второй гибнет); мы продаём
 * честность за 6 000 EU и тот же темп в 39 тиков.
 *
 * Входной буфер — один составной аспект, до 8 единиц. Выходной — два слота
 * компонентов, до 8 каждый. Операция не начинается, если ЛЮБОЙ из
 * компонентов некуда класть (§10: иначе второй пришлось бы уничтожить —
 * ровно то, чего этот блок существует не делать).
 *
 * Примордиалы не принимаются вовсе ({@code addEssentia} = 0, своя тяга их
 * не тянет): эквивалент карточного «отдаём обратно», но без цикла
 * принял-вернул. Редстоун глушит, как родную центрифугу.
 */
public class TileResonantSplitter extends TileThaumcraft
        implements ITickable, IMachineStatus, IEssentiaTransport, IAspectContainer {

    /** §5: буфер 20 000 EU, вход LV. */
    public static final double CAPACITY = 20_000.0;
    private static final int TIER = 1;

    /** §5: темп родной центрифуги. */
    public static final int CYCLE_TICKS = 39;
    /** §5: буферы по 8 единиц. */
    public static final int BUFFER = 8;

    private static final int SUCTION = 128;
    private static final int ACTIVE_HOLD_TICKS = 100;

    private final BasicSink sink = new BasicSink(this, CAPACITY, TIER);

    private Aspect input;
    private int inputAmount;
    private Aspect outA;
    private int outAmountA;
    private Aspect outB;
    private int outAmountB;

    private int progress;
    private boolean active;
    private int activeHold;
    private int counter;

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
        if (this.counter % 20 == 0) {
            this.pullEssentia();
        }

        boolean worked = false;
        if (!this.world.isBlockPowered(this.pos) && this.canOperate()) {
            // Прогресс идёт только при живом питании: кончилось посреди
            // цикла — замирает, входная единица не теряется (§10).
            if (this.sink.canUseEnergy(EnergyCanon.EU_PER_SPLIT)) {
                this.progress++;
                worked = true;
                if (this.progress >= CYCLE_TICKS) {
                    this.progress = 0;
                    this.split();
                }
            }
        } else {
            this.progress = 0;
        }

        if (worked) {
            this.activeHold = ACTIVE_HOLD_TICKS;
        } else if (this.activeHold > 0) {
            this.activeHold--;
        }
        this.setActive(this.activeHold > 0);
    }

    /** Есть что расщеплять и куда класть оба компонента. */
    private boolean canOperate() {
        if (this.input == null || this.inputAmount < 1) {
            return false;
        }
        Aspect[] parts = this.input.getComponents();
        if (parts == null || parts.length != 2) {
            return false;   // §10: чужой мод может завести странный аспект
        }
        return this.roomFor(parts[0]) && this.roomFor(parts[1]);
    }

    private boolean roomFor(Aspect part) {
        if (this.outA == null || this.outA == part) {
            if (this.outA == null || this.outAmountA < BUFFER) {
                return true;
            }
        }
        if (this.outB == null || this.outB == part) {
            return this.outB == null || this.outAmountB < BUFFER;
        }
        return false;
    }

    /** Одна операция: 1 составной → по единице ОБОИХ компонентов. */
    private void split() {
        Aspect[] parts = this.input.getComponents();
        this.sink.useEnergy(EnergyCanon.EU_PER_SPLIT);
        this.inputAmount--;
        Aspect done = this.input;
        if (this.inputAmount <= 0) {
            this.input = null;
        }
        for (Aspect part : parts) {
            this.stash(part);
        }
        // Два расходящихся потока цвета компонентов (§8): по событию на
        // сторону, параметр несёт цвет и знак стороны.
        this.world.addBlockEvent(this.pos, this.getBlockType(), EVENT_SPLIT,
                packSplit(parts[0].getColor(), false));
        this.world.addBlockEvent(this.pos, this.getBlockType(), EVENT_SPLIT,
                packSplit(parts[1].getColor(), true));
        this.markDirty();
        // done намеренно не используется дальше — имя оставлено для ясности
        // диффа с карточкой (§4.1: вход всегда списывается целиком).
    }

    private void stash(Aspect part) {
        if (this.outA == part || (this.outA == null && this.outB != part)) {
            this.outA = part;
            this.outAmountA++;
        } else {
            this.outB = part;
            this.outAmountB++;
        }
    }

    /** Тянет составной аспект из труб — тем же приёмом, что порт. */
    private void pullEssentia() {
        if (this.inputAmount >= BUFFER) {
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
            Aspect offered = transport.getEssentiaType(remote);
            if (!this.acceptsInput(offered)) {
                continue;
            }
            int ours = this.getSuctionAmount(side);
            if (ours <= transport.getSuctionAmount(remote)
                    || ours < transport.getMinimumSuction()) {
                continue;
            }
            int taken = transport.takeEssentia(offered, 1, remote);
            if (taken > 0) {
                this.input = offered;
                this.inputAmount += taken;
                this.markDirty();
                return;
            }
        }
    }

    /** Только составной с ровно двумя компонентами, один вид за раз. */
    private boolean acceptsInput(Aspect aspect) {
        if (aspect == null || aspect.isPrimal()) {
            return false;
        }
        Aspect[] parts = aspect.getComponents();
        if (parts == null || parts.length != 2) {
            return false;
        }
        return this.input == null || this.input == aspect;
    }

    /** ПКМ фиалом (§9): отдаёт 8 единиц компонента, которого больше. */
    public Aspect richestOutput() {
        if (this.outAmountA >= this.outAmountB && this.outA != null) {
            return this.outA;
        }
        return this.outB;
    }

    private void setActive(boolean value) {
        if (this.active == value) {
            return;
        }
        this.active = value;
        BlockMachineBase.setActive(this.world, this.pos, value);
    }

    // ================= частицы =================

    protected static final int EVENT_SPLIT = 3;

    private static int packSplit(int colour, boolean rightSide) {
        return (colour & 0xFFFFFF) | (rightSide ? 1 << 24 : 0);
    }

    @Override
    public boolean receiveClientEvent(int id, int param) {
        if (this.world == null || !this.world.isRemote) {
            return id == EVENT_SPLIT;
        }
        if (id == EVENT_SPLIT) {
            boolean right = (param >> 24 & 1) != 0;
            int colour = param & 0xFFFFFF;
            float dx = right ? 0.9F : -0.9F;
            for (int i = 0; i < 2; i++) {
                thaumcraft.common.Thaumcraft.proxy.sparkle(
                        this.pos.getX() + 0.5F + dx * (0.4F + 0.3F * i),
                        this.pos.getY() + 1.05F + 0.1F * i,
                        this.pos.getZ() + 0.5F,
                        colour);
            }
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
        return face != null;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
    }

    @Override
    public Aspect getSuctionType(EnumFacing face) {
        return this.input;   // null = «любой» (фильтрует acceptsInput/addEssentia)
    }

    @Override
    public int getSuctionAmount(EnumFacing face) {
        return this.inputAmount < BUFFER ? SUCTION : 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, EnumFacing face) {
        if (aspect == this.outA && this.outAmountA > 0) {
            this.outAmountA--;
            if (this.outAmountA <= 0) {
                this.outA = null;
            }
            this.markDirty();
            return 1;
        }
        if (aspect == this.outB && this.outAmountB > 0) {
            this.outAmountB--;
            if (this.outAmountB <= 0) {
                this.outB = null;
            }
            this.markDirty();
            return 1;
        }
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, EnumFacing face) {
        if (!this.acceptsInput(aspect) || amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, BUFFER - this.inputAmount);
        if (accepted <= 0) {
            return 0;
        }
        this.input = aspect;
        this.inputAmount += accepted;
        this.markDirty();
        return accepted;
    }

    /** Наружу показываем выход: трубы забирают компоненты, не сырьё. */
    @Override
    public Aspect getEssentiaType(EnumFacing face) {
        return this.richestOutput();
    }

    @Override
    public int getEssentiaAmount(EnumFacing face) {
        return Math.max(this.outAmountA, this.outAmountB);
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    // ================= IAspectContainer (голем у выхода, §10) =================

    @Override
    public AspectList getAspects() {
        AspectList list = new AspectList();
        if (this.outA != null && this.outAmountA > 0) {
            list.add(this.outA, this.outAmountA);
        }
        if (this.outB != null && this.outAmountB > 0) {
            list.add(this.outB, this.outAmountB);
        }
        return list;
    }

    @Override
    public void setAspects(AspectList aspects) {
        // Контейнер выходной, извне не заполняется.
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        return this.acceptsInput(aspect);
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        int accepted = this.addEssentia(aspect, amount, null);
        return amount - accepted;   // контракт контейнера: возвращает ОСТАТОК
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        int taken = 0;
        while (taken < amount && this.takeEssentia(aspect, 1, null) > 0) {
            taken++;
        }
        return taken >= amount;
    }

    @Override
    public boolean takeFromContainer(AspectList list) {
        if (!this.doesContainerContain(list)) {
            return false;
        }
        for (Aspect aspect : list.getAspects()) {
            if (aspect != null) {
                this.takeFromContainer(aspect, list.getAmount(aspect));
            }
        }
        return true;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        return this.containerContains(aspect) >= amount;
    }

    @Override
    public boolean doesContainerContain(AspectList list) {
        for (Aspect aspect : list.getAspects()) {
            if (aspect != null && !this.doesContainerContainAmount(aspect, list.getAmount(aspect))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int containerContains(Aspect aspect) {
        if (aspect == this.outA) {
            return this.outAmountA;
        }
        if (aspect == this.outB) {
            return this.outAmountB;
        }
        return 0;
    }

    // ================= IMachineStatus =================

    @Override
    public String getStatusLine() {
        int eu = (int) this.sink.getEnergyStored();
        String tail = ". Буфер " + eu + " / " + (int) CAPACITY + " EU";
        if (this.world.isBlockPowered(this.pos)) {
            return "§cРасщепитель: заглушен редстоуном" + tail;
        }
        if (this.input == null) {
            return "§bРасщепитель: ждёт составной аспект (примордиал не расщепляется)" + tail;
        }
        Aspect[] parts = this.input.getComponents();
        String what = this.input.getName() + " → " + parts[0].getName()
                + " + " + parts[1].getName();
        if (!this.canOperate()) {
            return "§cРасщепитель: слейте выход — некуда класть компоненты (" + what + ")" + tail;
        }
        if (!this.sink.canUseEnergy(EnergyCanon.EU_PER_SPLIT)) {
            return "§eРасщепитель: копит " + EnergyCanon.EU_PER_SPLIT + " EU на операцию ("
                    + what + ")" + tail;
        }
        return "§aРасщепитель: " + what + ", вход " + this.inputAmount
                + ", выход " + this.outAmountA + "+" + this.outAmountB + tail;
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
        tag.setString("UTIn", this.input == null ? "" : this.input.getTag());
        tag.setInteger("UTInAmt", this.inputAmount);
        tag.setString("UTOutA", this.outA == null ? "" : this.outA.getTag());
        tag.setInteger("UTOutAmtA", this.outAmountA);
        tag.setString("UTOutB", this.outB == null ? "" : this.outB.getTag());
        tag.setInteger("UTOutAmtB", this.outAmountB);
        tag.setInteger("UTProgress", this.progress);
    }

    private void readState(NBTTagCompound tag) {
        this.input = aspectOf(tag.getString("UTIn"));
        this.inputAmount = this.input == null ? 0 : tag.getInteger("UTInAmt");
        this.outA = aspectOf(tag.getString("UTOutA"));
        this.outAmountA = this.outA == null ? 0 : tag.getInteger("UTOutAmtA");
        this.outB = aspectOf(tag.getString("UTOutB"));
        this.outAmountB = this.outB == null ? 0 : tag.getInteger("UTOutAmtB");
        this.progress = tag.getInteger("UTProgress");
    }

    private static Aspect aspectOf(String key) {
        return key.isEmpty() ? null : Aspect.getAspect(key);
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
