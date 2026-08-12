package unboundtech.common.tiles;

import ic2.api.energy.prefab.BasicSource;
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
 * Эссентиальная горелка: эссенция → EU (`05_objects/essentia_burner.md`).
 *
 * Второй вход энергии для мага и первая причина для техника построить
 * алхимическую печь. В отличие от Таум-Генератора не знает ни узлов, ни
 * интерференции — но требует непрерывной подачи сырья.
 *
 * Курсы живут в {@link EnergyCanon} (канон §3.1) и предварительны до замера
 * реального темпа добычи эссенции. Аспекты вне таблицы горелка не принимает
 * вовсе: {@code essentiaValue} возвращает по ним ноль.
 *
 * <h3>Как она получает эссенцию</h3>
 * Труба ТК — «пылесос»: тянет тот, у кого тяга БОЛЬШЕ. Потребитель обязан
 * тянуть сам, поэтому здесь повторён приём {@code fillReservoir} из
 * {@code TileEssentiaCrystalizer} порта. Аргумент {@code EnumFacing} в
 * {@link IEssentiaTransport} — это грань ЭТОГО тайла, поэтому соседу всегда
 * передаётся {@code dir.getOpposite()}. Метод может прийти и с {@code null}
 * (труба зовёт {@code getSuctionAmount(null)}), поэтому аргумент нигде не
 * разыменовывается.
 */
public class TileEssentiaBurner extends TileThaumcraft
        implements ITickable, IMachineStatus, IEssentiaTransport {

    /** §5: буфер 10 000 EU, выход до 32 EU/t (тир LV). */
    public static final double CAPACITY = 10_000.0;
    private static final int TIER = 1;

    /** §5: цикл — 1 единица эссенции за 20 тиков. */
    private static final int CYCLE = 20;
    /** §5: внутренний буфер эссенции — 8 единиц. */
    public static final int ESSENTIA_BUFFER = 8;

    /** Тяга потребителя: выше трубы, иначе труба не отдаст. */
    private static final int SUCTION = 128;

    /** MF §2: состояние держится не меньше 100 тиков — полоска не мигает. */
    private static final int ACTIVE_HOLD_TICKS = 100;

    private final BasicSource source = new BasicSource(this, CAPACITY, TIER);

    private Aspect stored;
    private int amount;

    private int counter;
    private boolean active;
    private int activeHold;

    /** Причина простоя — см. {@link #getStatusLine()}. */
    private boolean euFull;

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
        boolean burned = this.burnOne();
        if (burned) {
            this.activeHold = ACTIVE_HOLD_TICKS;
        } else if (this.activeHold > 0) {
            this.activeHold -= CYCLE;
        }
        this.setActive(this.activeHold > 0);
    }

    /** Сжигает одну единицу, если есть что и куда. */
    private boolean burnOne() {
        if (this.stored == null || this.amount <= 0) {
            return false;
        }
        int value = EnergyCanon.essentiaValue(this.stored);
        if (value <= 0) {
            return false;   // аспект вне таблицы сюда попасть не должен
        }
        if (this.source.getEnergyStored() > CAPACITY - value) {
            this.euFull = true;   // §10: буфер полон — эссенция НЕ тратится
            return false;
        }
        this.euFull = false;
        this.amount--;
        if (this.amount <= 0) {
            this.stored = null;
        }
        this.source.addEnergy(value);
        this.markDirty();
        return true;
    }

    /** Тянет по единице из подключённых труб — тем же приёмом, что порт. */
    private void pullEssentia() {
        if (!this.wantsEssentia()) {
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
            int ours = this.getSuctionAmount(side);
            if (ours <= transport.getSuctionAmount(remote)
                    || ours < transport.getMinimumSuction()) {
                continue;
            }
            Aspect offered = transport.getEssentiaType(remote);
            if (!this.accepts(offered)) {
                continue;   // §10: аспект не из таблицы — не принимаем
            }
            int taken = transport.takeEssentia(offered, 1, remote);
            if (taken > 0) {
                this.stored = offered;
                this.amount += taken;
                this.markDirty();
                return;   // не больше единицы за цикл
            }
        }
    }

    /** §10: досжигает текущий аспект, только потом берёт другой. */
    private boolean accepts(Aspect aspect) {
        if (EnergyCanon.essentiaValue(aspect) <= 0) {
            return false;
        }
        return this.stored == null || this.stored == aspect;
    }

    private boolean wantsEssentia() {
        return this.amount < ESSENTIA_BUFFER && !this.euBufferFull();
    }

    /** Полным считаем, когда не влезет и самая дешёвая порция. */
    private boolean euBufferFull() {
        return this.source.getEnergyStored() > CAPACITY - EnergyCanon.EU_ESSENTIA_PLANT;
    }

    private void setActive(boolean value) {
        if (this.active == value) {
            return;
        }
        this.active = value;
        BlockMachineBase.setActive(this.world, this.pos, value);
    }

    /** Для клиента: цвет пламени равен цвету сжигаемого аспекта (§8). */
    public Aspect getBurningAspect() {
        return this.stored;
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
        return false;   // чистый потребитель
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        // Тягу задаём мы сами, снаружи её не навязать.
    }

    @Override
    public Aspect getSuctionType(EnumFacing face) {
        return this.stored;   // null = «любой», пока буфер пуст
    }

    @Override
    public int getSuctionAmount(EnumFacing face) {
        return this.wantsEssentia() ? SUCTION : 0;   // §10: буфер полон — тяги нет
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, EnumFacing face) {
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, EnumFacing face) {
        if (face != null && !this.canInputFrom(face)) {
            return 0;
        }
        if (!this.accepts(aspect) || amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, ESSENTIA_BUFFER - this.amount);
        if (accepted <= 0) {
            return 0;
        }
        this.stored = aspect;
        this.amount += accepted;
        this.markDirty();
        return accepted;   // контракт: сколько ПРИНЯЛИ
    }

    @Override
    public Aspect getEssentiaType(EnumFacing face) {
        return this.stored;
    }

    @Override
    public int getEssentiaAmount(EnumFacing face) {
        return this.amount;
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
        int eu = (int) this.source.getEnergyStored();
        String buffer = ". Буфер: " + eu + " / " + (int) CAPACITY + " EU";
        if (this.euFull || this.euBufferFull()) {
            return "§bЭссент. Горелка: буфер полон" + buffer;
        }
        if (this.stored == null || this.amount <= 0) {
            return "§cЭссент. Горелка: нет эссенции" + buffer;
        }
        return "§aЭссент. Горелка: горит "
                + this.stored.getName() + " по "
                + EnergyCanon.essentiaValue(this.stored) + " EU, осталось "
                + this.amount + " / " + ESSENTIA_BUFFER + buffer;
    }

    @Override
    public void writeWrenchNBT(NBTTagCompound tag) {
        this.source.writeToNBT(tag);
        this.writeEssentia(tag);
    }

    @Override
    public void readWrenchNBT(NBTTagCompound tag) {
        this.source.readFromNBT(tag);
        this.readEssentia(tag);
    }

    private void writeEssentia(NBTTagCompound tag) {
        tag.setString("UTAspect", this.stored == null ? "" : this.stored.getTag());
        tag.setInteger("UTAmount", this.amount);
    }

    private void readEssentia(NBTTagCompound tag) {
        String key = tag.getString("UTAspect");
        this.stored = key.isEmpty() ? null : Aspect.getAspect(key);
        this.amount = tag.getInteger("UTAmount");
        if (this.stored == null) {
            this.amount = 0;
        }
    }

    public double getEnergyStored() {
        return this.source.getEnergyStored();
    }

    @Override
    public void readCustomNBT(NBTTagCompound tag) {
        super.readCustomNBT(tag);
        this.source.readFromNBT(tag);
        this.readEssentia(tag);
        this.active = tag.getBoolean("UTActive");
        this.activeHold = tag.getInteger("UTActiveHold");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        this.source.writeToNBT(tag);
        this.writeEssentia(tag);
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
