package unboundtech.tile;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.MinecraftForge;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.INode;
import unboundtech.energy.EnergyCanon;

/**
 * Эфирный Двигатель: тратит EU и пополняет недостающие аспекты узлов в 8 блоках.
 * Спека: docs/integration/phase3_converters_spec.md §2 (репо порта).
 *
 * ВАЖНО (факт из кода порта): ауры чанков в TC4-порте не существует —
 * TC6-фасад AuraHelper/AuraChunk мёртв. Поэтому «восстановление ауры» = зарядка
 * узлов: +1 недостающего аспекта за EnergyCanon.EU_PER_AURA_BUY.
 *
 * Ёмкость узла (aspectsBase) сознательно не трогаем — лечение угасающих узлов
 * это отдельное исследование поздних фаз.
 */
public class TileAethericEngine extends TileNodeWorker implements IEnergySink {

    public static final double CAPACITY = 40_000.0D;
    public static final int TIER = 2;

    private double buffer;
    private boolean inEnergyNet;

    // ---- жизненный цикл ----

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.world != null && !this.world.isRemote && !this.inEnergyNet) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
            this.inEnergyNet = true;
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
        if (this.world != null && !this.world.isRemote && this.inEnergyNet) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            this.inEnergyNet = false;
        }
    }

    // ---- работа ----

    @Override
    protected boolean canWork() {
        return this.buffer >= EnergyCanon.EU_PER_AURA_BUY;
    }

    @Override
    protected boolean work() {
        for (BlockPos nodePos : this.nodes()) {
            INode node = this.nodeAt(nodePos);
            if (node == null) {
                continue;
            }
            AspectList current = node.getAspects();
            AspectList base = node.getAspectsBase();
            if (current == null || base == null) {
                continue;
            }
            List<Aspect> missing = new ArrayList<Aspect>();
            for (Aspect aspect : base.getAspects()) {
                if (aspect != null && current.getAmount(aspect) < base.getAmount(aspect)) {
                    missing.add(aspect);
                }
            }
            if (missing.isEmpty()) {
                continue;
            }
            Aspect target = missing.get(this.world.rand.nextInt(missing.size()));
            int overflow = node.addToContainer(target, 1);
            if (overflow > 0) {
                continue;
            }
            this.buffer -= EnergyCanon.EU_PER_AURA_BUY;
            this.syncNode(nodePos);
            this.markDirty();
            return true;
        }
        return false;
    }

    public double getBuffer() {
        return this.buffer;
    }

    @Override
    public ITextComponent getStatusMessage() {
        return this.status("unboundtech.status.buffer", (int) this.buffer, (int) CAPACITY);
    }

    // ---- IC2: потребитель энергии ----

    @Override
    public double getDemandedEnergy() {
        return Math.max(0.0D, CAPACITY - this.buffer);
    }

    @Override
    public int getSinkTier() {
        return TIER;
    }

    @Override
    public double injectEnergy(EnumFacing directionFrom, double amount, double voltage) {
        // Без markDirty (см. TileThaumGenerator.drawEnergy): вызывается каждый тик.
        this.buffer += amount;
        double overflow = 0.0D;
        if (this.buffer > CAPACITY) {
            overflow = this.buffer - CAPACITY;
            this.buffer = CAPACITY;
        }
        return overflow;
    }

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, EnumFacing side) {
        return true;
    }

    // ---- NBT ----

    @Override
    public void readCustomNBT(NBTTagCompound tag) {
        super.readCustomNBT(tag);
        this.buffer = tag.getDouble("buffer");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        tag.setDouble("buffer", this.buffer);
    }
}
