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
        EnergyCanon.EU_PER_NODE_ASPECT_SELL = config.get(CAT_ENERGY,
                "eu_per_node_aspect_generated", 2000,
                "EU produced by the Thaumic Alternator per 1 unit of node vis (Ignis/Potentia).")
                .getInt(2000);
        EnergyCanon.EU_PER_NODE_ASPECT_BUY = config.get(CAT_ENERGY,
                "eu_per_node_aspect_restored", 8000,
                "EU consumed by the Aetheric Engine to restore 1 unit of node vis.")
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
                "Thaumic Overclocker (upgrade item, used from phase 3; read now so packs can "
                + "pre-configure). The number of upgrades per machine is NOT configurable: it is "
                + "IC2's own upgrade slot count (4 on standard machines).");
        OverclockRules.PROCESS_TIME_MULTIPLIER = config.get(CAT_OVERCLOCK,
                "process_time_multiplier", 0.6D,
                "Processing time multiplier per upgrade (vanilla IC2 overclocker: 0.7).")
                .getDouble(0.6D);
        OverclockRules.ENERGY_DEMAND_MULTIPLIER = config.get(CAT_OVERCLOCK,
                "energy_demand_multiplier", 1.2D,
                "Energy demand multiplier per upgrade (vanilla IC2 overclocker: 1.6).")
                .getDouble(1.2D);
        OverclockRules.CHARGE_CAPACITY = config.get(CAT_OVERCLOCK, "charge_capacity", 32,
                "Capacity of each essentia pool (Machina and Motus) inside the upgrade.")
                .getInt(32);
        OverclockRules.TICKS_PER_ESSENTIA_UNIT = config.get(CAT_OVERCLOCK,
                "ticks_per_essentia_unit", 600,
                "Ticks of active machine work per 1 unit of each aspect.").getInt(600);
        OverclockRules.HEAT_PER_ACTIVE_TICK = config.get(CAT_OVERCLOCK, "heat_per_active_tick", 1,
                "Heat gained per tick while the machine is processing.").getInt(1);
        OverclockRules.COOLING_PER_IDLE_TICK = config.get(CAT_OVERCLOCK, "cooling_per_idle_tick", 2,
                "Heat lost per tick while the machine is idle.").getInt(2);
        OverclockRules.HEAT_THRESHOLD = config.get(CAT_OVERCLOCK, "heat_threshold", 6000,
                "Heat at which the upgrade goes inert and vents flux.").getInt(6000);
        OverclockRules.INERT_TICKS = config.get(CAT_OVERCLOCK, "inert_ticks", 600,
                "How long the upgrade stays inert after overheating.").getInt(600);
        OverclockRules.FLUX_PER_OVERHEAT = config.get(CAT_OVERCLOCK, "flux_per_overheat", 1,
                "Flux gas blocks released above the machine per overheat event.").getInt(1);

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
