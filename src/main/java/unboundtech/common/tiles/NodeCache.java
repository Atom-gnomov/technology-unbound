package unboundtech.common.tiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import thaumcraft.api.nodes.INode;
import thaumcraft.common.tiles.TileJarNode;

/**
 * Кэш соседних узлов для машин, работающих с висом (спека фазы 3а §0.4,
 * паттерн подсмотрен у {@code TileWandPedestal.findNodes}).
 *
 * Политика: куб ±8 вокруг машины, полное пересканирование не чаще раза
 * в 100 тиков и ТОЛЬКО если прошлая попытка работы была неудачной —
 * пока узел кормит машину, мир не перебирается вовсе.
 *
 * Узлы в банке ({@link TileJarNode}) исключаются намеренно: банка — уже
 * «карманный» узел игрока, машина не должна её доить (то же исключение
 * стоит в пьедестале жезлов).
 */
public final class NodeCache {

    public static final int RADIUS = 8;
    private static final int RESCAN_INTERVAL = 100;

    private List<BlockPos> nodes = Collections.emptyList();
    private boolean stale = true;
    private boolean scannedOnce;

    /**
     * @param counter тик-счётчик тайла (пересканирование привязано к нему,
     *                чтобы машины не сканировали мир синхронно каждый тик)
     * @return позиции известных узлов; список может быть пустым
     */
    public List<BlockPos> nodes(World world, BlockPos center, int counter) {
        // Первый скан — сразу: иначе только что поставленная машина стояла бы
        // мёртвой до ближайшей сотни тиков.
        if (this.stale && (!this.scannedOnce || counter % RESCAN_INTERVAL == 0)) {
            this.rescan(world, center);
        }
        return this.nodes;
    }

    /** Помечает кэш устаревшим: следующая сотня тиков пересканирует мир. */
    public void markStale() {
        this.stale = true;
    }

    private void rescan(World world, BlockPos center) {
        List<BlockPos> found = new ArrayList<>();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (nodeAt(world, pos) != null) {
                        found.add(pos);
                    }
                }
            }
        }
        this.nodes = found;
        this.stale = false;
        this.scannedOnce = true;
    }

    /** @return узел в этой позиции или null (банки-узлы не считаются). */
    public static INode nodeAt(World world, BlockPos pos) {
        // Позиции живут в кэше до 100 тиков: без этой проверки getTileEntity
        // синхронно подгрузил бы (а то и сгенерировал) выгруженный чанк
        // прямо из тика машины.
        if (!world.isBlockLoaded(pos)) {
            return null;
        }
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof INode && !(te instanceof TileJarNode)) {
            return (INode) te;
        }
        return null;
    }

    /**
     * Рассылает клиентам содержимое узла: мутаторы {@code takeFromContainer}/
     * {@code addToContainer} синк не шлют, без этого таумометр показывает
     * застывшие цифры (спека §0.3).
     */
    public static void syncNode(World world, BlockPos pos) {
        net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
    }
}
