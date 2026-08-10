package unboundtech.common.tiles;

import ic2.api.energy.prefab.BasicSink;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.INode;
import unboundtech.common.blocks.BlockMachineBase;
import unboundtech.energy.EnergyCanon;

/**
 * Эфирный Двигатель: EU → вис узла (спека фазы 3а §2).
 *
 * Раз в 20 тиков доливает 1 недостающий вис в ближайший неполный узел за
 * {@link EnergyCanon#EU_PER_NODE_ASPECT_BUY} EU — вчетверо дороже, чем Таум-Генератор
 * платит за тот же вис. Это и есть «второй закон таумодинамики»: связка
 * генератор+двигатель на одном узле всегда убыточна, вечного двигателя нет.
 *
 * Ёмкость узла ({@code aspectsBase}) не трогается сознательно: лечение
 * угасающих узлов — отдельное исследование поздних фаз, здесь узел лишь
 * заполняется до своего собственного потолка.
 */
public class TileAethericEngine extends TileThaumcraft implements ITickable {

    /** Буфер: 5 «порций» ауры, tier 2 (MV, приём до 128 EU/t). */
    public static final double CAPACITY = 40_000.0;
    private static final int TIER = 2;

    private static final int WORK_INTERVAL = 20;

    private final BasicSink sink = new BasicSink(this, CAPACITY, TIER);
    private final NodeCache nodeCache = new NodeCache();

    private int counter;
    private boolean active;

    /**
     * КРИТИЧНО: у модовых тайлов Forge по умолчанию пересоздаёт TileEntity при
     * ЛЮБОЙ смене состояния блока — {@code TileEntity.shouldRefresh} возвращает
     * {@code !isVanilla || блок изменился}, а {@code isVanilla} проверяется по
     * имени пакета ({@code net.minecraft.*}). Без этого переопределения каждое
     * переключение ACTIVE обнуляло бы буфер EU и выкидывало машину из
     * энергосети IC2. Порт по той же причине переопределяет метод в восьми
     * своих тайлах (TileJar, TilePedestal, TileCamo…).
     */
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
        if (this.counter % WORK_INTERVAL != 0) {
            return;
        }

        boolean working = false;
        if (this.sink.canUseEnergy(EnergyCanon.EU_PER_NODE_ASPECT_BUY)) {
            working = this.rechargeOneAspect();
        }
        this.setActive(working);
    }

    /**
     * Доливает единицу случайного недостающего аспекта в первый неполный узел
     * (тот же выбор аспекта, что у естественной регенерации узла).
     *
     * @return true, если вис долит и EU списаны
     */
    private boolean rechargeOneAspect() {
        List<BlockPos> nodes = this.nodeCache.nodes(this.world, this.pos, this.counter);
        for (BlockPos nodePos : nodes) {
            INode node = NodeCache.nodeAt(this.world, nodePos);
            if (node == null) {
                continue;
            }
            Aspect aspect = this.pickMissingAspect(node);
            if (aspect == null) {
                continue;
            }
            // Излишек > 0 означает, что узел всё-таки полон по этому аспекту:
            // ничего не списываем, вис возвращать некуда — он не создавался.
            if (node.addToContainer(aspect, 1) != 0) {
                continue;
            }
            this.sink.useEnergy(EnergyCanon.EU_PER_NODE_ASPECT_BUY);
            NodeCache.syncNode(this.world, nodePos);
            return true;
        }
        this.nodeCache.markStale();
        return false;
    }

    /** @return случайный аспект, которого узлу недостаёт до его ёмкости. */
    private Aspect pickMissingAspect(INode node) {
        AspectList base = node.getAspectsBase();
        if (base == null || base.size() <= 0) {
            return null;
        }
        AspectList current = node.getAspects();
        List<Aspect> missing = new ArrayList<>();
        for (Aspect aspect : base.getAspects()) {
            if (aspect == null) {
                continue;
            }
            int have = current == null ? 0 : current.getAmount(aspect);
            if (have < node.getNodeVisBase(aspect)) {
                missing.add(aspect);
            }
        }
        if (missing.isEmpty()) {
            return null;
        }
        return missing.get(this.world.rand.nextInt(missing.size()));
    }

    private void setActive(boolean value) {
        if (this.active == value) {
            return;
        }
        this.active = value;
        BlockMachineBase.setActive(this.world, this.pos, value);
    }

    public double getEnergyStored() {
        return this.sink.getEnergyStored();
    }

    @Override
    public void readCustomNBT(NBTTagCompound tag) {
        super.readCustomNBT(tag);
        this.sink.readFromNBT(tag);
        this.active = tag.getBoolean("UTActive");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        this.sink.writeToNBT(tag);
        tag.setBoolean("UTActive", this.active);
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
