package unboundtech.tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.nodes.INode;
import thaumcraft.common.tiles.TileJarNode;
import unboundtech.block.BlockUTMachine;

/**
 * База для машин, работающих с узлами ауры.
 *
 * Паттерн скопирован с TileWandPedestal порта: кэш позиций узлов в кубе ±8,
 * пересканирование раз в 100 тиков и только если попытка работы реально
 * провалилась (17³ = 4913 обращений к getTileEntity — этого нельзя делать
 * ни каждый тик, ни «на всякий случай», когда работать было нечем).
 *
 * Узлы-в-банке (TileJarNode) исключаются, как и в самом порте.
 */
public abstract class TileNodeWorker extends TileThaumcraft implements ITickable {

    protected static final int RADIUS = 8;
    protected static final int WORK_INTERVAL = 20;
    protected static final int RESCAN_INTERVAL = 100;

    /**
     * Сколько тиков машина считается «работающей» после удачного цикла.
     * Без этого запаса ACTIVE мигал бы каждые 20–40 тиков (буфер то полон,
     * то нет), дёргая соседей и пересчёт освещения.
     */
    private static final int ACTIVE_LINGER = 100;

    private List<BlockPos> nodes;
    private boolean needsRescan = true;
    private int activeLinger;
    protected int counter;
    protected boolean active;

    @Override
    public void onLoad() {
        super.onLoad();
        // Развод фаз: иначе все машины базы, загруженные одним тиком,
        // навсегда попадают в один и тот же тик пересканирования.
        this.counter = Math.abs(this.pos.hashCode()) % RESCAN_INTERVAL;
    }

    @Override
    public void update() {
        if (this.world == null || this.world.isRemote) {
            return;
        }
        this.counter++;
        this.tickEnergy();

        if (this.nodes == null
                || (this.counter % RESCAN_INTERVAL == 0 && (this.needsRescan || this.nodes.isEmpty()))) {
            this.scanNodes();
            this.needsRescan = false;
        }

        if (this.counter % WORK_INTERVAL == 0) {
            boolean attempted = this.canWork();
            if (attempted) {
                if (this.work()) {
                    this.activeLinger = ACTIVE_LINGER;
                } else {
                    // Работать было чем, но ни один узел не подошёл — список устарел.
                    this.needsRescan = true;
                }
            }
        }

        if (this.activeLinger > 0) {
            this.activeLinger--;
        }
        boolean shouldBeActive = this.activeLinger > 0;
        if (shouldBeActive != this.active) {
            this.active = shouldBeActive;
            BlockUTMachine.setActiveState(this.world, this.pos, shouldBeActive);
            this.markDirty();
        }
    }

    /**
     * Тайл обязан пережить смену собственного состояния блока: у модовых
     * тайлов Forge по умолчанию пересоздаёт TileEntity при ЛЮБОЙ смене
     * состояния (проверка «ванильности» по имени пакета), что обнуляло бы
     * буфер EU и выкидывало машину из энергосети несколько раз в секунду.
     */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    /** Вызывается каждый тик — для энергетических операций подкласса. */
    protected void tickEnergy() {
    }

    /** Дешёвая проверка перед дорогим обходом узлов (место в буфере / наличие EU). */
    protected boolean canWork() {
        return true;
    }

    /** @return true, если работа выполнена в этом цикле. */
    protected abstract boolean work();

    /** Строка статуса для ПКМ по блоку (буфер, причина простоя). */
    public abstract ITextComponent getStatusMessage();

    protected ITextComponent status(String key, Object... args) {
        return new TextComponentTranslation(key, args);
    }

    protected List<BlockPos> nodes() {
        return this.nodes == null ? Collections.<BlockPos>emptyList() : this.nodes;
    }

    /**
     * @return узел в позиции или null, если чанк выгружен либо там уже не узел.
     * Проверка isBlockLoaded обязательна: список позиций живёт до 100 тиков и
     * без неё getTileEntity синхронно подгружал бы (и генерировал) чанки.
     */
    protected INode nodeAt(BlockPos nodePos) {
        if (!this.world.isBlockLoaded(nodePos)) {
            return null;
        }
        TileEntity te = this.world.getTileEntity(nodePos);
        if (te instanceof INode && !(te instanceof TileJarNode)) {
            return (INode) te;
        }
        return null;
    }

    /**
     * Синхронизация узла с клиентом: takeFromContainer/addToContainer в порте
     * зовут только markDirty(), без этого таумометр показывает устаревшие числа.
     */
    protected void syncNode(BlockPos nodePos) {
        IBlockState state = this.world.getBlockState(nodePos);
        this.world.notifyBlockUpdate(nodePos, state, state, 3);
    }

    private void scanNodes() {
        List<BlockPos> found = new ArrayList<BlockPos>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    BlockPos check = this.pos.add(dx, dy, dz);
                    if (!this.world.isBlockLoaded(check)) {
                        continue;
                    }
                    TileEntity te = this.world.getTileEntity(check);
                    if (te instanceof INode && !(te instanceof TileJarNode)) {
                        found.add(check);
                    }
                }
            }
        }
        this.nodes = found;
    }

    @Override
    public void readCustomNBT(NBTTagCompound tag) {
        this.active = tag.getBoolean("active");
        this.activeLinger = tag.getInteger("linger");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound tag) {
        tag.setBoolean("active", this.active);
        tag.setInteger("linger", this.activeLinger);
    }
}
