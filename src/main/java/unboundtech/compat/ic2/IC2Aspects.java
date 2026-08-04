package unboundtech.compat.ic2;

import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import unboundtech.UTLog;

/**
 * Аспекты предметов IC2 для сканирования таумометром.
 * Канон таблицы: docs/integration/ic2_v3_machines.md §1 (репо порта).
 *
 * Маппинг латинских имён на константы порта (thaumcraft.api.aspects.Aspect):
 * machina=MECHANISM, potentia=ENERGY, cognitio=MIND, lux=LIGHT,
 * metallum=METAL, ordo=ORDER, pannus=CLOTH, venenum=POISON,
 * perditio=ENTROPY, alienis=ELDRITCH, permutatio=EXCHANGE, vacuos=VOID,
 * iter=TRAVEL, volatus=FLIGHT, perfodio=MINE, telum=WEAPON,
 * vitreus=CRYSTAL, auram=AURA, motus=MOTION, tutamen=ARMOR.
 *
 * Вызывается в postInit ПОСЛЕ Thaumcraft (registerObjectTag — просто map put,
 * перезаписи со стороны ТК для чужих предметов не бывает).
 */
public final class IC2Aspects {

    private static int registered;
    private static int missing;

    private IC2Aspects() {
    }

    public static void register() {
        registered = 0;
        missing = 0;

        // --- Провода и материалы ---
        tag("cable", "type:copper,insulation:0",
                list().add(Aspect.METAL, 2).add(Aspect.ENERGY, 1));
        tag("crafting", "rubber",
                list().add(Aspect.MOTION, 1).add(Aspect.WATER, 1));
        tag("crafting", "circuit",
                list().add(Aspect.MECHANISM, 4).add(Aspect.MIND, 2).add(Aspect.ENERGY, 2));
        tag("crafting", "advanced_circuit",
                list().add(Aspect.MECHANISM, 6).add(Aspect.MIND, 4).add(Aspect.LIGHT, 2));
        tag("ingot", "refined_iron",
                list().add(Aspect.METAL, 4).add(Aspect.ORDER, 2));
        tag("crafting", "carbon_fibre",
                list().add(Aspect.CLOTH, 2).add(Aspect.ORDER, 2).add(Aspect.FIRE, 1));
        tag("crafting", "scrap",
                list().add(Aspect.ENTROPY, 2).add(Aspect.EXCHANGE, 1));
        tag("crafting", "iridium",
                list().add(Aspect.METAL, 8).add(Aspect.ELDRITCH, 4));
        tag("misc_resource", "matter",
                list().add(Aspect.ELDRITCH, 8).add(Aspect.EXCHANGE, 8).add(Aspect.VOID, 4));

        // --- Энергохранилища ---
        tag("re_battery", null,
                list().add(Aspect.ENERGY, 4).add(Aspect.METAL, 2));
        tag("energy_crystal", null,
                list().add(Aspect.ENERGY, 8).add(Aspect.CRYSTAL, 4));
        tag("lapotron_crystal", null,
                list().add(Aspect.ENERGY, 12).add(Aspect.CRYSTAL, 4).add(Aspect.AURA, 2));

        // --- Ядерное ---
        tag("nuclear", "uranium_238",
                list().add(Aspect.METAL, 3).add(Aspect.ENERGY, 2).add(Aspect.POISON, 2));

        // --- Машины (блоки te; сканирование любой открывает вкладку) ---
        tag("te", "generator",
                list().add(Aspect.MECHANISM, 5).add(Aspect.FIRE, 3).add(Aspect.ENERGY, 3));
        tag("te", "solar_generator",
                list().add(Aspect.MECHANISM, 4).add(Aspect.LIGHT, 4).add(Aspect.ENERGY, 2));
        tag("te", "macerator",
                list().add(Aspect.MECHANISM, 5).add(Aspect.ENTROPY, 3).add(Aspect.ENERGY, 2));
        tag("te", "iron_furnace",
                list().add(Aspect.MECHANISM, 3).add(Aspect.FIRE, 3).add(Aspect.METAL, 2));
        tag("te", "electric_furnace",
                list().add(Aspect.MECHANISM, 4).add(Aspect.FIRE, 3).add(Aspect.ENERGY, 2));
        tag("te", "nuclear_reactor",
                list().add(Aspect.MECHANISM, 8).add(Aspect.ENERGY, 8)
                        .add(Aspect.FIRE, 4).add(Aspect.POISON, 4));
        tag("te", "teleporter",
                list().add(Aspect.TRAVEL, 8).add(Aspect.ELDRITCH, 6).add(Aspect.MECHANISM, 4));

        // --- Снаряжение ---
        tag("jetpack", null,
                list().add(Aspect.FLIGHT, 6).add(Aspect.MECHANISM, 4).add(Aspect.ENERGY, 3));
        tag("drill", null,
                list().add(Aspect.MINE, 4).add(Aspect.MECHANISM, 3).add(Aspect.ENERGY, 2));
        tag("chainsaw", null,
                list().add(Aspect.WEAPON, 4).add(Aspect.MECHANISM, 3).add(Aspect.ENERGY, 2));
        tag("nano_saber", null,
                list().add(Aspect.WEAPON, 6).add(Aspect.ENERGY, 4).add(Aspect.LIGHT, 2));
        // Хазмат (ботинки хазмата в IC2 — резиновые сапоги)
        for (String piece : new String[]{"hazmat_helmet", "hazmat_chestplate",
                "hazmat_leggings", "rubber_boots"}) {
            tag(piece, null, list().add(Aspect.ARMOR, 3).add(Aspect.CLOTH, 2));
        }
        // Нано-броня (значений в док-таблице не было — экстраполяция от квантовой,
        // строка дописана в ic2_v3_machines.md §1 при реализации)
        for (String piece : new String[]{"nano_helmet", "nano_chestplate",
                "nano_leggings", "nano_boots"}) {
            tag(piece, null, list().add(Aspect.ARMOR, 6)
                    .add(Aspect.MECHANISM, 4).add(Aspect.ENERGY, 4));
        }
        // Квантовая броня
        for (String piece : new String[]{"quantum_helmet", "quantum_chestplate",
                "quantum_leggings", "quantum_boots"}) {
            tag(piece, null, list().add(Aspect.ARMOR, 8).add(Aspect.MECHANISM, 6)
                    .add(Aspect.ELDRITCH, 4).add(Aspect.ENERGY, 4));
        }

        UTLog.info("IC2 aspects: {} items tagged, {} not found in this IC2 build",
                registered, missing);
    }

    private static AspectList list() {
        return new AspectList();
    }

    private static void tag(String name, String variant, AspectList aspects) {
        ItemStack stack = variant == null ? IC2Handles.item(name) : IC2Handles.item(name, variant);
        if (stack.isEmpty()) {
            missing++;
            return;
        }
        ThaumcraftApi.registerObjectTag(stack, aspects);
        registered++;
    }
}
