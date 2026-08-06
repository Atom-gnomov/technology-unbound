package unboundtech;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import unboundtech.config.UTConfig;
import unboundtech.module.ModuleManager;

/**
 * Unbound Technology — мост между TC4 Unbound и IndustrialCraft 2.
 *
 * Жизненный цикл (см. docs/integration/phase1_core_spec.md в репо порта):
 *  - preInit: конфиг, решение о включённых модулях;
 *  - init:    контент, не зависящий от чужого postInit;
 *  - postInit: аспекты, исследования и рецепты — строго ПОСЛЕ Thaumcraft
 *    (after:thaumcraft гарантирует порядок postInit между модами) и IC2.
 */
@Mod(
    modid = UnboundTech.MODID,
    name = UnboundTech.NAME,
    version = UnboundTech.VERSION,
    dependencies = "required-after:forge@[14.23.5.2847,);"
                 + "required-after:thaumcraft;"
                 + "required-after:ic2;"
                 + "after:mets;"
                 + "after:advanced_solar_panels",
    acceptedMinecraftVersions = "[1.12.2]"
)
public class UnboundTech {

    public static final String MODID = "unboundtech";
    public static final String NAME = "Unbound Technology";
    public static final String VERSION = "0.1.2";

    @Mod.Instance(MODID)
    public static UnboundTech instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        UTLog.info("{} {} pre-init", NAME, VERSION);
        UTConfig.load(event.getSuggestedConfigurationFile());
        ModuleManager.preInit();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModuleManager.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        ModuleManager.postInit();
        UTLog.info("{} loaded. Modules: {}", NAME, ModuleManager.enabledSummary());
    }
}
