package unboundtech.compat.mets;

import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import unboundtech.CompatIds;
import unboundtech.UTLog;
import unboundtech.compat.ModItems;

/**
 * Аспекты предметов More Electric Tools (модуль mets, правило v4 submod §4:
 * электроинструменты — ниша METS, мы их не дублируем; наша часть — вплести
 * материалы METS в таум-прогрессию аспектами; выполняет и пометку диздока
 * «дополнить таблицу аспектов предметами METS на этапе реализации»).
 *
 * Реестровые имена = имена файлов моделей METS 1.662 (обычные предметы,
 * по одному registry name на предмет, мета 0; блоки — через их ItemBlock).
 * Список курирован: сигнатурные материалы, накопители, оружие, машины —
 * не весь каталог METS (~77 позиций), чтобы не раздувать сканирование.
 *
 * Ориентиры величин — канон IC2 (ic2_v3_machines.md §1): advanced_circuit =
 * Machina 6/Cognitio 4/Lux 2, lapotron = Potentia 12/Vitreus 4/Auram 2,
 * re_battery = Potentia 4/Metallum 2; «супер»-тиры METS идут выше.
 */
public final class METSAspects {

    private static int registered;
    private static int missing;

    private METSAspects() {
    }

    public static void register() {
        registered = 0;
        missing = 0;

        // --- Руды и базовые материалы ---
        tag("niobium_ore", list().add(Aspect.METAL, 3).add(Aspect.EARTH, 1)
                .add(Aspect.ENERGY, 1));
        tag("titanium_ore", list().add(Aspect.METAL, 3).add(Aspect.EARTH, 1)
                .add(Aspect.ORDER, 1));
        tag("titanium_dust", list().add(Aspect.METAL, 2).add(Aspect.ENTROPY, 1));
        tag("niobium_dust", list().add(Aspect.METAL, 3).add(Aspect.ENERGY, 1));
        tag("thorium_dust", list().add(Aspect.METAL, 2).add(Aspect.ENERGY, 3)
                .add(Aspect.POISON, 2));
        tag("titanium_ingot", list().add(Aspect.METAL, 4).add(Aspect.ORDER, 2));
        tag("titanium_block", list().add(Aspect.METAL, 12).add(Aspect.ORDER, 6));
        tag("niobium_titanium_ingot", list().add(Aspect.METAL, 6).add(Aspect.ENERGY, 3)
                .add(Aspect.ORDER, 2));
        tag("super_iridium_alloy", list().add(Aspect.METAL, 10).add(Aspect.ELDRITCH, 4)
                .add(Aspect.ORDER, 2));
        tag("neutron_plate", list().add(Aspect.METAL, 12).add(Aspect.VOID, 8)
                .add(Aspect.ELDRITCH, 4));

        // --- Электроника и накопители ---
        tag("superconducting_cable", list().add(Aspect.METAL, 4).add(Aspect.ENERGY, 6)
                .add(Aspect.ORDER, 3));
        tag("super_circuit", list().add(Aspect.MECHANISM, 8).add(Aspect.MIND, 6)
                .add(Aspect.ENERGY, 4));
        tag("lithium_battery", list().add(Aspect.ENERGY, 6).add(Aspect.METAL, 2));
        tag("advanced_lithium_battery", list().add(Aspect.ENERGY, 8).add(Aspect.METAL, 3));
        tag("thorium_battery", list().add(Aspect.ENERGY, 10).add(Aspect.POISON, 3)
                .add(Aspect.METAL, 3));
        tag("super_lapotron_crystal", list().add(Aspect.ENERGY, 16).add(Aspect.CRYSTAL, 4)
                .add(Aspect.AURA, 4));

        // --- Живой металл (мостик к голем-ремеслу, см. исследование) ---
        tag("nano_living_metal", list().add(Aspect.METAL, 6).add(Aspect.LIFE, 6)
                .add(Aspect.MECHANISM, 4));
        tag("living_circuit", list().add(Aspect.MECHANISM, 6).add(Aspect.MIND, 6)
                .add(Aspect.LIFE, 6));
        tag("field_generator", list().add(Aspect.ARMOR, 8).add(Aspect.MECHANISM, 6)
                .add(Aspect.ENERGY, 6));

        // --- Оружие и инструменты (канон: telum у сабель, machina у машин) ---
        tag("advanced_iridium_sword", list().add(Aspect.WEAPON, 8).add(Aspect.METAL, 6)
                .add(Aspect.ENERGY, 4));
        tag("electric_submachine_gun", list().add(Aspect.WEAPON, 6).add(Aspect.MECHANISM, 4)
                .add(Aspect.ENERGY, 3));
        tag("tactical_laser_submachine_gun", list().add(Aspect.WEAPON, 8).add(Aspect.LIGHT, 4)
                .add(Aspect.MECHANISM, 5).add(Aspect.ENERGY, 4));
        tag("electric_plasma_gun", list().add(Aspect.WEAPON, 10).add(Aspect.FIRE, 6)
                .add(Aspect.ENERGY, 6));
        tag("nano_bow", list().add(Aspect.WEAPON, 6).add(Aspect.MECHANISM, 4)
                .add(Aspect.FLIGHT, 2));
        tag("geomagnetic_detector", list().add(Aspect.SENSES, 4).add(Aspect.MECHANISM, 3)
                .add(Aspect.EARTH, 2));

        // --- Геомагнитный генератор (блоки) ---
        tag("geomagnetic_pedestal", list().add(Aspect.MECHANISM, 8).add(Aspect.ENERGY, 6)
                .add(Aspect.EARTH, 4));
        tag("geomagnetic_antenna", list().add(Aspect.MECHANISM, 6).add(Aspect.ENERGY, 4)
                .add(Aspect.AIR, 4));

        UTLog.info("METS aspects: {} registered, {} missing", registered, missing);
    }

    private static void tag(String path, AspectList aspects) {
        ItemStack stack = ModItems.item(CompatIds.METS, path);
        if (stack.isEmpty()) {
            missing++;
            return;
        }
        ThaumcraftApi.registerObjectTag(stack, aspects);
        registered++;
    }

    private static AspectList list() {
        return new AspectList();
    }
}
