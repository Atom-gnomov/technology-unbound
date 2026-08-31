package unboundtech.common.tiles;

import ic2.api.energy.prefab.BasicSource;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.INode;
import unboundtech.common.blocks.BlockMachineBase;
import unboundtech.energy.EnergyCanon;

/**
 * Таум-Генератор: вис узла → EU (спека фазы 3а §1).
 *
 * Тянет по 1 вис Ignis/Potentia из ближайшего узла раз в 20 тиков и кладёт
 * в буфер {@link EnergyCanon#EU_PER_NODE_ASPECT_SELL} EU за единицу. Узел никогда
 * не выдаивается досуха: работает пол в 20% ПО-АСПЕКТНОЙ ёмкости
 * ({@code getNodeVisBase}), так что узел всегда остаётся живым и
 * восстанавливается сам. Долговременный потолок задаёт не машина, а реген
 * узла — «пробуждённых» узлов и прочих множителей нет намеренно.
 *
 * Интерференция: два генератора в 16 блоках глушат друг друга (лорное
 * обоснование — LORE_RESONANCE_LIMITS), иначе узел обставлялся бы кольцом
 * машин и превращался в ферму.
 */
public class TileThaumGenerator extends TileThaumcraft implements ITickable,
        IMachineStatus, unboundtech.common.gui.ISyncedMachine,
        unboundtech.common.gui.IEnergyGauge {

    /** Клиентские копии полей GUI — живут от контейнера (ХФ-7).
     *  Ревью каркаса №2/№3: статус-строка обязана строиться ТОЛЬКО из
     *  них — прочие поля клиентского тайла обновляются редко, а
     *  NodeCache на клиенте заморожен и трогать его нельзя. */
    private int guiEnergy;
    private boolean guiInterfered;
    private boolean guiWorking;
    private boolean guiNodeFound;

    @Override
    public int[] syncFields() {
        return new int[]{
                (int) this.getEnergyStored(),
                this.interfered ? 1 : 0,
                this.activeHold > 0 ? 1 : 0,
                this.nodeCache.nodes(this.world, this.pos, this.counter)
                        .isEmpty() ? 0 : 1,
        };
    }

    @Override
    public void applySyncField(int index, int value) {
        switch (index) {
            case 0: this.guiEnergy = value; break;
            case 1: this.guiInterfered = value != 0; break;
            case 2: this.guiWorking = value != 0; break;
            case 3: this.guiNodeFound = value != 0; break;
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

    /** Буфер: 10 «порций» ауры, tier 1 (LV, 32 EU/t на выход). */
    public static final double CAPACITY = 20_000.0;
    private static final int TIER = 1;

    private static final int DRAIN_INTERVAL = 20;
    private static final int INTERFERENCE_INTERVAL = 100;
    private static final int INTERFERENCE_RADIUS = 16;

    /** Пол: аспект не опускается ниже 20% своей ёмкости в узле. */
    private static final double NODE_FLOOR = 0.2;

    /** Аспекты, которые генератор умеет сжигать (Ignis / Potentia). */
    private static final Aspect[] FUEL_ASPECTS = {Aspect.FIRE, Aspect.ENERGY};

    /**
     * Блок-события для частиц (`machine_feedback.md` §3). Доставка ванильная:
     * {@code World.addBlockEvent} с сервера → {@code receiveClientEvent} на
     * клиенте, никакого своего сетевого канала. Параметр — смещение узла от
     * машины, упакованное по 5 бит на ось (радиус 8 влезает с запасом).
     */
    protected static final int EVENT_VIS_FLOW = 1;
    /** Красная искра отказа — не чаще раза в 100 тиков (§3). */
    protected static final int EVENT_FAULT = 2;

    protected static int packOffset(BlockPos from, BlockPos to) {
        return (to.getX() - from.getX() + 8)
                | (to.getY() - from.getY() + 8) << 5
                | (to.getZ() - from.getZ() + 8) << 10;
    }

    /**
     * Эффект обмена вис между машиной и узлом — ровно тот, каким ТК рисует
     * зарядку жезла: фиолетовый разряд {@code nodeBolt}
     * ({@code FXLightningBolt}, как в инфузионной матрице порта) плюс пара
     * родных искр у принимающей стороны. {@code reverse} — направление
     * (у двигателя разряд бьёт от машины к узлу). Клиентский код: и bolt,
     * и sparkle сами молчат на сервере.
     */
    protected static void spawnVisThread(net.minecraft.world.World world, BlockPos machine,
                                         int packedOffset, boolean reverse) {
        int dx = (packedOffset & 31) - 8;
        int dy = (packedOffset >> 5 & 31) - 8;
        int dz = (packedOffset >> 10 & 31) - 8;
        float mx = machine.getX() + 0.5F;
        float my = machine.getY() + 0.5F;
        float mz = machine.getZ() + 0.5F;
        float nx = mx + dx;
        float ny = my + dy;
        float nz = mz + dz;
        // Разряд: у генератора — от узла к машине, у двигателя — наоборот.
        if (reverse) {
            thaumcraft.common.Thaumcraft.proxy.nodeBolt(world, mx, my, mz, nx, ny, nz);
        } else {
            thaumcraft.common.Thaumcraft.proxy.nodeBolt(world, nx, ny, nz, mx, my, mz);
        }
        // Пара искр у принимающей стороны — послесвечение разряда.
        float tx = reverse ? nx : mx;
        float ty = reverse ? ny : my;
        float tz = reverse ? nz : mz;
        for (int i = 0; i < 2; i++) {
            thaumcraft.common.Thaumcraft.proxy.sparkle(
                    tx + (world.rand.nextFloat() - 0.5F) * 0.6F,
                    ty + 0.3F + world.rand.nextFloat() * 0.4F,
                    tz + (world.rand.nextFloat() - 0.5F) * 0.6F,
                    1.2F, 0, -0.02F);   // type 0 — фиолетовый вис
        }
    }

    @Override
    public boolean receiveClientEvent(int id, int param) {
        if (this.world == null || !this.world.isRemote) {
            // Сервер получает то же событие — подтверждаем, чтобы ванила не
            // перепроверяла блок, но не делаем ничего.
            return id == EVENT_VIS_FLOW || id == EVENT_FAULT;
        }
        if (id == EVENT_VIS_FLOW) {
            spawnVisThread(this.world, this.pos, param, false);
            return true;
        }
        if (id == EVENT_FAULT) {
            thaumcraft.common.Thaumcraft.proxy.sparkle(
                    this.pos.getX() + 0.5F, this.pos.getY() + 1.05F, this.pos.getZ() + 0.5F,
                    1.0F, 4, -0.01F);   // type 4 — красная искра у полоски
            return true;
        }
        return super.receiveClientEvent(id, param);
    }

    /**
     * Сколько тиков «морда» остаётся горящей после удачного забора вис.
     * Чуть больше интервала регенерации узла (600), чтобы у работающей на
     * пределе машины состояние не мигало.
     */
    private static final int ACTIVE_HOLD_TICKS = 640;

    private final BasicSource source = new BasicSource(this, CAPACITY, TIER);

    /** Остаток «залипания» состояния ACTIVE в тиках (см. ACTIVE_HOLD_TICKS). */
    private int activeHold = 0;
    private final NodeCache nodeCache = new NodeCache();

    private int counter;
    private boolean active;
    private boolean interfered;

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
        this.source.update();
        if (this.world == null || this.world.isRemote) {
            return;
        }
        this.counter++;

        if (this.counter % INTERFERENCE_INTERVAL == 0) {
            this.interfered = this.hasNeighbouringGenerator();
            if (this.interfered) {
                // Одиночная красная искра у полоски, не чаще раза в 100 тиков.
                this.world.addBlockEvent(this.pos, this.getBlockType(), EVENT_FAULT, 0);
            }
        }

        if (this.counter % DRAIN_INTERVAL != 0) {
            return;
        }

        boolean working = false;
        // Тянем, только когда в буфере есть место под целую порцию вис.
        if (!this.interfered
                && this.source.getEnergyStored()
                        <= CAPACITY - EnergyCanon.EU_PER_NODE_ASPECT_SELL) {
            working = this.drainOneVis();
        }
        // ACTIVE — «машина в работе», а не «забрала вис прямо в этом такте».
        // Узел регенерирует 1 вис за 600 тиков, машина пробует раз в 20 — без
        // залипания морда горела бы 20 тиков из 600 и каждое переключение
        // тянуло бы пересчёт освещения и пакет всем клиентам рядом.
        if (working) {
            this.activeHold = ACTIVE_HOLD_TICKS;
        } else if (this.activeHold > 0) {
            this.activeHold -= DRAIN_INTERVAL;
        }
        this.setActive(this.activeHold > 0);
    }

    /**
     * Забирает 1 вис из первого подходящего узла.
     *
     * @return true, если вис получен и обращён в EU
     */
    private boolean drainOneVis() {
        List<BlockPos> nodes = this.nodeCache.nodes(this.world, this.pos, this.counter);
        for (BlockPos nodePos : nodes) {
            INode node = NodeCache.nodeAt(this.world, nodePos);
            if (node == null) {
                continue;
            }
            AspectList aspects = node.getAspects();
            if (aspects == null || aspects.size() <= 0) {
                continue;
            }
            for (Aspect aspect : FUEL_ASPECTS) {
                int available = aspects.getAmount(aspect);
                // Минимум 1: у стороннего узла ёмкость аспекта может быть 0,
                // и тогда пол выродился бы в ноль, а аспект исчез бы навсегда
                // (реген узла обходит только aspectsBase).
                int floor = Math.max(1,
                        (int) Math.ceil(node.getNodeVisBase(aspect) * NODE_FLOOR));
                if (available - 1 < floor) {
                    continue;
                }
                if (!node.takeFromContainer(aspect, 1)) {
                    continue;
                }
                this.source.addEnergy(EnergyCanon.EU_PER_NODE_ASPECT_SELL);
                NodeCache.syncNode(this.world, nodePos);
                // Нить вис от узла к машине — раз в успешный цикл (§3).
                this.world.addBlockEvent(this.pos, this.getBlockType(),
                        EVENT_VIS_FLOW, packOffset(this.pos, nodePos));
                return true;
            }
        }
        // Ни один узел не отдал вис. Это штатное состояние (реген узла — 1 вис
        // за 600 тиков), поэтому пересканируем мир только если узлы исчезли.
        this.nodeCache.markStaleIfNodesGone(this.world);
        return false;
    }

    /** @return true, если рядом есть ещё один генератор (обе машины глохнут). */
    private boolean hasNeighbouringGenerator() {
        int minChunkX = (this.pos.getX() - INTERFERENCE_RADIUS) >> 4;
        int maxChunkX = (this.pos.getX() + INTERFERENCE_RADIUS) >> 4;
        int minChunkZ = (this.pos.getZ() - INTERFERENCE_RADIUS) >> 4;
        int maxChunkZ = (this.pos.getZ() + INTERFERENCE_RADIUS) >> 4;
        int radiusSq = INTERFERENCE_RADIUS * INTERFERENCE_RADIUS;

        // Идём по тайлам загруженных чанков, а не по 33³ позициям мира:
        // тот же ответ, но на три порядка меньше обращений.
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                Chunk chunk = this.world.getChunkProvider().getLoadedChunk(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (Map.Entry<BlockPos, TileEntity> entry
                        : chunk.getTileEntityMap().entrySet()) {
                    TileEntity tile = entry.getValue();
                    if (tile == this || !(tile instanceof TileThaumGenerator)
                            || tile.isInvalid()) {
                        continue;
                    }
                    if (entry.getKey().distanceSq(this.pos) <= radiusSq) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void setActive(boolean value) {
        if (this.active == value) {
            return;
        }
        this.active = value;
        BlockMachineBase.setActive(this.world, this.pos, value);
    }

    /** Для тултипа/гогглов: машина заглушена соседним генератором. */
    public boolean isInterfered() {
        return this.interfered;
    }

    // ================= IMachineStatus =================

    @Override
    public String getStatusLine() {
        if (this.world != null && this.world.isRemote) {
            return this.clientStatusLine();
        }
        int eu = (int) this.source.getEnergyStored();
        if (this.interfered) {
            return "\u00a7cРезонансная интерференция: рядом второй генератор";
        }
        if (eu >= CAPACITY - EnergyCanon.EU_PER_NODE_ASPECT_SELL) {
            return "\u00a7bБуфер полон: " + eu + " / " + (int) CAPACITY + " EU";
        }
        if (this.nodeCache.nodes(this.world, this.pos, this.counter).isEmpty()) {
            return "\u00a7cНет узла в радиусе 8 блоков";
        }
        return (this.activeHold > 0 ? "\u00a7aРаботает: " : "\u00a7eЖдёт регенерации узла: ")
                + eu + " / " + (int) CAPACITY + " EU";
    }

    /** Клиентская строка — только из полей, синкнутых контейнером. */
    private String clientStatusLine() {
        int eu = this.guiEnergy;
        if (this.guiInterfered) {
            return "§cРезонансная интерференция: рядом второй генератор";
        }
        if (eu >= CAPACITY - EnergyCanon.EU_PER_NODE_ASPECT_SELL) {
            return "§bБуфер полон: " + eu + " / " + (int) CAPACITY + " EU";
        }
        if (!this.guiNodeFound) {
            return "§cНет узла в радиусе 8 блоков";
        }
        return (this.guiWorking ? "§aРаботает: " : "§eЖдёт регенерации узла: ")
                + eu + " / " + (int) CAPACITY + " EU";
    }

    @Override
    public void writeWrenchNBT(NBTTagCompound tag) {
        this.source.writeToNBT(tag);
        tag.setBoolean("UTInterfered", this.interfered);
    }

    @Override
    public void readWrenchNBT(NBTTagCompound tag) {
        this.source.readFromNBT(tag);
        this.interfered = tag.getBoolean("UTInterfered");
    }

    public double getEnergyStored() {
        return this.source.getEnergyStored();
    }

    @Override
    public void readCustomNBT(NBTTagCompound tag) {
        super.readCustomNBT(tag);
        this.source.readFromNBT(tag);
        this.active = tag.getBoolean("UTActive");
        this.interfered = tag.getBoolean("UTInterfered");
        this.activeHold = tag.getInteger("UTActiveHold");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        this.source.writeToNBT(tag);
        tag.setBoolean("UTActive", this.active);
        tag.setBoolean("UTInterfered", this.interfered);
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
