package unboundtech.tile;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySource;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.MinecraftForge;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.INode;
import unboundtech.energy.EnergyCanon;
import unboundtech.energy.GeneratorRegistry;

/**
 * Таум-Генератор: вытягивает Ignis/Potentia из узлов в 8 блоках и отдаёт EU.
 * Спека: docs/integration/phase3_converters_spec.md §1 (репо порта).
 *
 * Баланс держится на трёх ограничителях:
 *  - пол 20% от по-аспектной ёмкости узла (узлы не убиваются);
 *  - тир LV (32 EU/t на выходе) — краткосрочный потолок;
 *  - естественная регенерация узла (+1 вис / ~600 тиков) — долгосрочный;
 *  - интерференция: другой генератор ближе 16 блоков глушит оба.
 */
public class TileThaumGenerator extends TileNodeWorker implements IEnergySource {

    /** Аспекты, которые генератор умеет сжигать (Ignis, Potentia). */
    private static final Aspect[] FUEL_ASPECTS = {Aspect.FIRE, Aspect.ENERGY};

    public static final double CAPACITY = 20_000.0D;
    public static final int TIER = 1;
    public static final double MAX_OUTPUT = 32.0D;

    /** Доля ёмкости узла, ниже которой не опускаемся. */
    private static final float PRESERVE_FRACTION = 0.2F;

    private double buffer;
    private boolean inEnergyNet;
    private boolean interfering;

    // ---- жизненный цикл ----

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.world != null && !this.world.isRemote) {
            GeneratorRegistry.add(this.world.provider.getDimension(), this.pos);
            if (!this.inEnergyNet) {
                MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
                this.inEnergyNet = true;
            }
        }
    }

    @Override
    public void invalidate() {
        this.detach();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        this.detach();
        super.onChunkUnload();
    }

    private void detach() {
        if (this.world != null && !this.world.isRemote) {
            GeneratorRegistry.remove(this.world.provider.getDimension(), this.pos);
            if (this.inEnergyNet) {
                MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
                this.inEnergyNet = false;
            }
        }
    }

    // ---- работа ----

    @Override
    protected boolean canWork() {
        // Проверка дешёвая (обход множества генераторов, а не тайлов мира),
        // поэтому делаем её каждый рабочий цикл — состояние всегда актуально.
        this.interfering = GeneratorRegistry.hasNeighbourNear(
                this.world.provider.getDimension(), this.pos);
        return !this.interfering && this.buffer + EnergyCanon.EU_PER_AURA_SELL <= CAPACITY;
    }

    @Override
    protected boolean work() {
        for (BlockPos nodePos : this.nodes()) {
            INode node = this.nodeAt(nodePos);
            if (node == null || node.getAspects() == null) {
                continue;
            }
            for (Aspect aspect : FUEL_ASPECTS) {
                int have = node.getAspects().getAmount(aspect);
                if (have <= 0) {
                    continue;
                }
                // Пол — 20% ёмкости, но НИКОГДА не ноль: у стороннего узла
                // ёмкость аспекта может быть 0, и тогда аспект исчез бы
                // навсегда (регенерация узла обходит только aspectsBase).
                int floor = Math.max(1,
                        MathHelper.ceil(node.getNodeVisBase(aspect) * PRESERVE_FRACTION));
                if (have - 1 < floor) {
                    continue;
                }
                if (!node.takeFromContainer(aspect, 1)) {
                    continue;
                }
                this.buffer = Math.min(CAPACITY, this.buffer + EnergyCanon.EU_PER_AURA_SELL);
                this.syncNode(nodePos);
                this.markDirty();
                return true;
            }
        }
        return false;
    }

    public boolean isInterfering() {
        return this.interfering;
    }

    public double getBuffer() {
        return this.buffer;
    }

    @Override
    public ITextComponent getStatusMessage() {
        if (this.interfering) {
            return this.status("unboundtech.status.interference");
        }
        return this.status("unboundtech.status.buffer",
                (int) this.buffer, (int) CAPACITY);
    }

    // ---- IC2: источник энергии ----

    @Override
    public double getOfferedEnergy() {
        return Math.min(this.buffer, MAX_OUTPUT);
    }

    @Override
    public void drawEnergy(double amount) {
        // Без markDirty: IC2 зовёт это каждый тик, а markDirty тянет за собой
        // updateComparatorOutputLevel (до 8 getBlockState). Буфер сохраняется
        // в рабочем цикле, потеря максимум одной порции при жёстком краше.
        this.buffer = Math.max(0.0D, this.buffer - amount);
    }

    @Override
    public int getSourceTier() {
        return TIER;
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor receiver, EnumFacing side) {
        return true;
    }

    // ---- NBT ----

    @Override
    public void readCustomNBT(NBTTagCompound tag) {
        super.readCustomNBT(tag);
        this.buffer = tag.getDouble("buffer");
        this.interfering = tag.getBoolean("interfering");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        tag.setDouble("buffer", this.buffer);
        tag.setBoolean("interfering", this.interfering);
    }
}
