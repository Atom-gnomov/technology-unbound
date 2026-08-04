package unboundtech.config;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import net.minecraftforge.common.config.Configuration;
import unboundtech.UTLog;
import unboundtech.energy.EnergyCanon;
import unboundtech.energy.OverclockRules;
import unboundtech.module.UTModule;

/**
 * Конфиг мода: config/unboundtech.cfg.
 * Категории: [modules] — выключатели модулей, [energy] — курсы EnergyCanon.
 */
public final class UTConfig {

    private static final String CAT_MODULES = "modules";
    private static final String CAT_ENERGY = "energy";
    private static final String CAT_OVERCLOCK = "overclock";

    private static Configuration config;
    private static final Map<UTModule, Boolean> MODULE_FLAGS = new EnumMap<>(UTModule.class);

    private UTConfig() {
    }

    public static void load(File file) {
        config = new Configuration(file);
        config.load();

        config.setCategoryComment(CAT_MODULES,
                "Content modules. A module also requires its companion mod to be present "
                + "(mets/asp); setting false here force-disables it regardless.");
        for (UTModule module : UTModule.values()) {
            boolean value = config.get(CAT_MODULES, module.configKey(), true).getBoolean(true);
            MODULE_FLAGS.put(module, value);
        }

        config.setCategoryComment(CAT_ENERGY,
                "Energy exchange rates (v5 canon). Round-trip Vis->EU->Vis must stay <= 25% "
                + "('second law of thaumodynamics'); a WARN is logged if this config breaks it.");
        EnergyCanon.EU_PER_AURA_SELL = config.get(CAT_ENERGY, "eu_per_aura_unit_generated", 2000,
                "EU produced by the Thaumic Alternator per 1 unit of node aura (Ignis/Potentia).")
                .getInt(2000);
        EnergyCanon.EU_PER_AURA_BUY = config.get(CAT_ENERGY, "eu_per_aura_unit_restored", 8000,
                "EU consumed by the Aetheric Engine to restore 1 unit of chunk aura.")
                .getInt(8000);
        EnergyCanon.EU_PER_VIS = config.get(CAT_ENERGY, "eu_per_wand_vis", 20000,
                "EU consumed to charge 1 vis directly into a wand (Singulator, iridium rod).")
                .getInt(20000);
        EnergyCanon.EU_ESSENTIA_HOT = config.get(CAT_ENERGY, "eu_per_essentia_ignis_potentia", 2000,
                "Essentia Burner (phase 4): EU per 1 Ignis/Potentia essentia.")
                .getInt(2000);
        EnergyCanon.EU_ESSENTIA_PERDITIO = config.get(CAT_ENERGY, "eu_per_essentia_perditio", 1250,
                "Essentia Burner (phase 4): EU per 1 Perditio essentia.")
                .getInt(1250);
        EnergyCanon.EU_ESSENTIA_PLANT = config.get(CAT_ENERGY, "eu_per_essentia_arbor_herba", 500,
                "Essentia Burner (phase 4): EU per 1 Arbor/Herba essentia.")
                .getInt(500);
        EnergyCanon.EU_PERMUTATIO_AMPLIFIER = config.get(CAT_ENERGY, "eu_permutatio_amplifier", 5000,
                "Mass Fabricator (phase 10): amplifier EU value of 1 Permutatio essentia.")
                .getInt(5000);

        config.setCategoryComment(CAT_OVERCLOCK,
                "Thaumic Overclocker limits (used from phase 3; read now so packs can pre-configure).");
        OverclockRules.CHUNK_LIMIT = config.get(CAT_OVERCLOCK, "chunk_limit", 4,
                "Base overclocker load points per chunk before flux events.").getInt(4);
        OverclockRules.PER_MACHINE_LIMIT = config.get(CAT_OVERCLOCK, "per_machine_limit", 2,
                "Base overclockers per machine.").getInt(2);

        if (config.hasChanged()) {
            config.save();
        }

        EnergyCanon.validateSecondLaw();
    }

    public static boolean moduleEnabled(UTModule module) {
        Boolean flag = MODULE_FLAGS.get(module);
        if (flag == null) {
            UTLog.warn("Module {} queried before config load; treating as disabled", module);
            return false;
        }
        return flag;
    }
}
