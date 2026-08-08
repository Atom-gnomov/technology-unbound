package unboundtech.energy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import unboundtech.UnboundTech;

/**
 * Реестр установленных Таум-Генераторов по измерениям.
 *
 * Нужен для правила «нельзя две штуки ближе 16 блоков» (лор LORE_RESONANCE_LIMITS):
 * сканировать куб ±16 обращениями к getTileEntity — 35 937 вызовов, поэтому
 * позиции держим в памяти и сверяем расстояние по ним.
 *
 * Только серверная сторона; чистится при выгрузке мира.
 */
@Mod.EventBusSubscriber(modid = UnboundTech.MODID)
public final class GeneratorRegistry {

    /**
     * Квадрат дистанции интерференции: евклидовы 16 блоков ВКЛЮЧИТЕЛЬНО
     * (ровно 16 блоков по оси — уже глушат друг друга).
     */
    public static final double MIN_DISTANCE_SQ = 16.0D * 16.0D;

    private static final Map<Integer, Set<BlockPos>> BY_DIMENSION =
            new HashMap<Integer, Set<BlockPos>>();

    private GeneratorRegistry() {
    }

    public static synchronized void add(int dimension, BlockPos pos) {
        Set<BlockPos> set = BY_DIMENSION.get(dimension);
        if (set == null) {
            set = new HashSet<BlockPos>();
            BY_DIMENSION.put(dimension, set);
        }
        set.add(pos.toImmutable());
    }

    public static synchronized void remove(int dimension, BlockPos pos) {
        Set<BlockPos> set = BY_DIMENSION.get(dimension);
        if (set != null) {
            set.remove(pos);
            if (set.isEmpty()) {
                BY_DIMENSION.remove(dimension);
            }
        }
    }

    /** @return true, если рядом (&lt;16 блоков) стоит другой генератор. */
    public static synchronized boolean hasNeighbourNear(int dimension, BlockPos pos) {
        Set<BlockPos> set = BY_DIMENSION.get(dimension);
        if (set == null) {
            return false;
        }
        for (BlockPos other : set) {
            if (!other.equals(pos) && other.distanceSq(pos) <= MIN_DISTANCE_SQ) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() == null || event.getWorld().isRemote) {
            return;
        }
        int dimension = event.getWorld().provider.getDimension();
        synchronized (GeneratorRegistry.class) {
            BY_DIMENSION.remove(dimension);
        }
    }
}
