package unboundtech.common;

import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import unboundtech.UnboundTech;

/**
 * Пометка мобов, рождённых у спаунера (`mechanist_mortar.md` §4.1:
 * «не стреляет по мобам из спаунера — предохранитель против фермы»).
 *
 * В 1.12.2 событие спавна не несёт источник, поэтому честная эвристика:
 * сущность, ВПЕРВЫЕ вошедшая в мир в четырёх блоках от блока-спаунера,
 * получает тег. Ложные срабатывания (моб случайно заспавнился рядом со
 * спаунером) безопасны: мортира просто пропустит одну цель.
 */
@Mod.EventBusSubscriber(modid = UnboundTech.MODID)
public final class UTSpawnerTagger {

    public static final String TAG = "UTSpawnerBorn";
    private static final int RADIUS = 4;

    private UTSpawnerTagger() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote
                || !(event.getEntity() instanceof EntityLiving)
                || event.getEntity().ticksExisted > 0
                || event.getEntity().getEntityData().hasKey(TAG)) {
            return;
        }
        BlockPos at = event.getEntity().getPosition();
        // не трогаем незагруженные чанки — скан не должен запускать
        // каскадную генерацию мира (ревью №18)
        if (!event.getWorld().isAreaLoaded(at.add(-RADIUS, -RADIUS, -RADIUS),
                at.add(RADIUS, RADIUS, RADIUS))) {
            return;
        }
        for (BlockPos pos : BlockPos.getAllInBoxMutable(
                at.add(-RADIUS, -RADIUS, -RADIUS), at.add(RADIUS, RADIUS, RADIUS))) {
            if (event.getWorld().getBlockState(pos).getBlock() == Blocks.MOB_SPAWNER) {
                event.getEntity().getEntityData().setBoolean(TAG, true);
                return;
            }
        }
    }
}
