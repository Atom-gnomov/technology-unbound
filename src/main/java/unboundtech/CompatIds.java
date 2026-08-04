package unboundtech;

/**
 * Все modid внешних модов — в одном месте.
 *
 * ВНИМАНИЕ: id аддонов IC2 (METS, Advanced Solar Panels) записаны по данным
 * из сети и подлежат сверке с реальными jar-ами при первом запуске сборки
 * с этими модами (см. docs/IC2_API_ASSUMPTIONS.md, раздел «modid»).
 */
public final class CompatIds {

    public static final String THAUMCRAFT = "thaumcraft";
    public static final String IC2 = "ic2";
    /** TODO(verify): сверить по jar METS. */
    public static final String METS = "mets";
    /** TODO(verify): сверить по jar Advanced Solar Panels. */
    public static final String ASP = "advanced_solar_panels";

    private CompatIds() {
    }
}
