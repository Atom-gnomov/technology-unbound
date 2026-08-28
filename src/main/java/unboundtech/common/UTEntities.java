package unboundtech.common;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import unboundtech.UnboundTech;
import unboundtech.common.entities.EntityFluxBullet;
import unboundtech.common.entities.EntityTechnoSpirit;

/** Сущности мода: снаряд Флюкс-Револьвера и техно-дух. */
public final class UTEntities {

    private UTEntities() {
    }

    public static void register() {
        EntityRegistry.registerModEntity(
                new ResourceLocation(UnboundTech.MODID, "flux_bullet"),
                EntityFluxBullet.class, "flux_bullet", 0, UnboundTech.instance,
                64, 1, true);
        EntityRegistry.registerModEntity(
                new ResourceLocation(UnboundTech.MODID, "techno_spirit"),
                EntityTechnoSpirit.class, "techno_spirit", 1, UnboundTech.instance,
                64, 3, true, 0x6E419A, 0xF7B03C);
    }
}
