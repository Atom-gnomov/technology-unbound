package unboundtech.common;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import unboundtech.UnboundTech;
import unboundtech.common.entities.EntityFluxBullet;

/** Сущности мода. Пока одна — снаряд Флюкс-Револьвера. */
public final class UTEntities {

    private UTEntities() {
    }

    public static void register() {
        EntityRegistry.registerModEntity(
                new ResourceLocation(UnboundTech.MODID, "flux_bullet"),
                EntityFluxBullet.class, "flux_bullet", 0, UnboundTech.instance,
                64, 1, true);
    }
}
