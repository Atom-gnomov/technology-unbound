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
public class TileAethericEngine extends TileThaumcraft implements ITickable,
        IMachineStatus, unboundtech.common.gui.ISyncedMachine,
        unboundtech.common.gui.IEnergyGauge {

    /** Клиентские копии полей GUI (уроки ревью каркаса №1-№3). */
    private int guiEnergy;
    private boolean guiActive;
    private boolean guiNodeFound;

    @Override
    public int[] syncFields() {
        return new int[]{
                (int) this.getEnergyStored(),
                this.active ? 1 : 0,
                this.nodeCache.nodes(this.world, this.pos, this.counter)
                        .isEmpty() ? 0 : 1,
        };
    }

    @Override
    public void applySyncField(int index, int value) {
        switch (index) {
            case 0: this.guiEnergy = value; break;
            case 1: this.guiActive = value != 0; break;
            case 2: this.guiNodeFound = value != 0; break;
            default: break;
        }
    }

    @Override
    public double gaugeEnergy() {
        return this.world != null && this.world.isRemote
                ? this.guiEnergy : this.getEnergyStored();
    }

    @Override
    public double gaugeCapacity() {
        return CAPACITY;
    }

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
            // Нить вис от машины к узлу (обратное направление, §3).
            this.world.addBlockEvent(this.pos, this.getBlockType(),
                    TileThaumGenerator.EVENT_VIS_FLOW,
                    TileThaumGenerator.packOffset(this.pos, nodePos));
            return true;
        }
        // Все узлы полны — это норма, а не устаревший кэш (см. NodeCache).
        this.nodeCache.markStaleIfNodesGone(this.world);
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

    /** Приём блок-события частиц; сама нить — общий код генератора. */
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

    // ================= IMachineStatus =================

    @Override
    public String getStatusLine() {
        if (this.world != null && this.world.isRemote) {
            return this.clientStatusLine();
        }
        int eu = (int) this.sink.getEnergyStored();
        String tail = ". Буфер " + eu + " / " + (int) CAPACITY + " EU";
        if (eu < EnergyCanon.EU_PER_NODE_ASPECT_BUY) {
            return "§eДвигатель: накапливает " + eu + " из "
                    + EnergyCanon.EU_PER_NODE_ASPECT_BUY + " EU на аспект" + tail;
        }
        if (this.nodeCache.nodes(this.world, this.pos, this.counter).isEmpty()) {
            return "§cДвигатель: нет узла в радиусе 8 блоков" + tail;
        }
        return (this.active ? "§aДвигатель: заряжает узел"
                : "§bДвигатель: все узлы полны") + tail;
    }

    /** Клиентская строка — только из синкнутых контейнером полей. */
    private String clientStatusLine() {
        int eu = this.guiEnergy;
        String tail = ". Буфер " + eu + " / " + (int) CAPACITY + " EU";
        if (eu < EnergyCanon.EU_PER_NODE_ASPECT_BUY) {
            return "§eДвигатель: накапливает " + eu + " из "
                    + EnergyCanon.EU_PER_NODE_ASPECT_BUY + " EU на аспект" + tail;
        }
        if (!this.guiNodeFound) {
            return "§cДвигатель: нет узла в радиусе 8 блоков" + tail;
        }
        return (this.guiActive ? "§aДвигатель: заряжает узел"
                : "§bДвигатель: все узлы полны") + tail;
    }

    @Override
    public void writeWrenchNBT(NBTTagCompound tag) {
        this.sink.writeToNBT(tag);
    }

    @Override
    public void readWrenchNBT(NBTTagCompound tag) {
        this.sink.readFromNBT(tag);
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
