package unboundtech;

/**
 * Все modid внешних модов — в одном месте.
 *
 * Все строки подтверждены по @Mod-аннотациям и mcmod.info самих модов
 * (см. docs/IC2_API_ASSUMPTIONS.md).
 */
public final class CompatIds {

    public static final String THAUMCRAFT = "thaumcraft";
    public static final String IC2 = "ic2";
    /** More Electric Tools (LT_lrsoft), @Mod(modid="mets"), конфиг config/mets.cfg. */
    public static final String METS = "mets";
    /**
     * Advanced Solar Panels 1.12.2 (Chocohead), @Mod(modid="advanced_solar_panels").
     * Осторожно: у версии 1.7.10 был другой id (AdvancedSolarPanel), а у
     * «Advanced Solars Classic Edition» под IC2 Classic — id advancedsolars.
     */
    public static final String ASP = "advanced_solar_panels";

    private CompatIds() {
    }
}
