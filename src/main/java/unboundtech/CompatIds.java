package unboundtech;

/**
 * Все modid внешних модов — в одном месте.
 *
 * modid аддонов сверены с mcmod.info реальных jar-ов 2026-08-06:
 * MoreElectricTools.v1.662.jar → «mets», Advanced Solar Panels-4.3.0.jar →
 * «advanced_solar_panels» (см. docs/IC2_API_ASSUMPTIONS.md, раздел «modid»).
 */
public final class CompatIds {

    public static final String THAUMCRAFT = "thaumcraft";
    public static final String IC2 = "ic2";
    public static final String METS = "mets";
    public static final String ASP = "advanced_solar_panels";

    private CompatIds() {
    }
}
