package unboundtech.compat.asp;

import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import unboundtech.CompatIds;
import unboundtech.UTLog;
import unboundtech.compat.ModItems;

/**
 * Аспекты предметов Advanced Solar Panels (модуль asp, правило v4 submod §4:
 * контент ASP не дублируем — навешиваем аспекты и вплетаем в прогрессию).
 *
 * Ключевое следствие аспектов: САННАРИУМ ПЛАВИТСЯ В ТИГЛЕ — тигель ТК
 * разбирает предмет на его аспект-лист, так что Lux/Potentia на саннариуме
 * автоматически делают его источником эссенции света и силы
 * (обещание диздока «Sunnarium в тигле», ic2_v4_submod.md §4).
 *
 * Реестровые имена и меты сверены декомпиляцией ASP 4.3.0 (Chocohead):
 *  - advanced_solar_panels:machines (ItemBlock, меты = TEs.getId 0..5);
 *  - advanced_solar_panels:crafting (ItemMulti, меты = CraftingTypes 0..13);
 *  - шлемы — отдельные предметы (модели/registry: *_solar_helmet).
 *
 * Ориентиры величин — канон аспектов машин IC2 (ic2_v3_machines.md §1):
 * solar_generator = Machina 4/Lux 4/Potentia 2, lapotron = Potentia 12/
 * Vitreus 4/Auram 2, iridium = Metallum 8/Alienis 4; тиры ASP идут выше.
 */
public final class ASPAspects {

    private static int registered;
    private static int missing;

    private ASPAspects() {
    }

    public static void register() {
        registered = 0;
        missing = 0;

        // --- Машины (advanced_solar_panels:machines, меты TEs 0..5) ---
        machine(0, list().add(Aspect.MECHANISM, 10).add(Aspect.EXCHANGE, 12)
                .add(Aspect.ENERGY, 8));                       // Molecular Transformer
        machine(1, list().add(Aspect.MECHANISM, 8).add(Aspect.ENERGY, 12)
                .add(Aspect.ELDRITCH, 4));                     // Quantum Generator
        machine(2, list().add(Aspect.MECHANISM, 6).add(Aspect.LIGHT, 6)
                .add(Aspect.ENERGY, 4));                       // Advanced Solar Panel
        machine(3, list().add(Aspect.MECHANISM, 8).add(Aspect.LIGHT, 8)
                .add(Aspect.ENERGY, 6));                       // Hybrid Solar Panel
        machine(4, list().add(Aspect.MECHANISM, 10).add(Aspect.LIGHT, 10)
                .add(Aspect.ENERGY, 8));                       // Ultimate Hybrid Solar Panel
        machine(5, list().add(Aspect.MECHANISM, 12).add(Aspect.LIGHT, 12)
                .add(Aspect.ENERGY, 10).add(Aspect.ELDRITCH, 4)); // Quantum Solar Panel

        // --- Материалы (advanced_solar_panels:crafting, меты CraftingTypes) ---
        crafting(0, list().add(Aspect.LIGHT, 8).add(Aspect.ENERGY, 4)
                .add(Aspect.FIRE, 2));                         // Sunnarium → тигель: Lux/Potentia
        crafting(1, list().add(Aspect.LIGHT, 2).add(Aspect.ENERGY, 1)); // Sunnarium Part
        crafting(2, list().add(Aspect.LIGHT, 8).add(Aspect.METAL, 4)
                .add(Aspect.ENERGY, 4));                       // Sunnarium Alloy
        crafting(3, list().add(Aspect.ENERGY, 8).add(Aspect.POISON, 4)
                .add(Aspect.LIGHT, 4));                        // Irradiant Uranium
        crafting(4, list().add(Aspect.LIGHT, 12).add(Aspect.ENERGY, 6)
                .add(Aspect.FIRE, 3));                         // Enriched Sunnarium
        crafting(5, list().add(Aspect.LIGHT, 12).add(Aspect.METAL, 6)
                .add(Aspect.ENERGY, 6));                       // Enriched Sunnarium Alloy
        crafting(6, list().add(Aspect.CRYSTAL, 4).add(Aspect.LIGHT, 6)); // Irradiant Glass Pane
        crafting(7, list().add(Aspect.METAL, 8).add(Aspect.ELDRITCH, 2)
                .add(Aspect.ARMOR, 2));                        // Iridium-Iron Plate
        crafting(8, list().add(Aspect.METAL, 10).add(Aspect.ARMOR, 6)
                .add(Aspect.ELDRITCH, 2));                     // Reinforced Iridium Iron Plate
        crafting(9, list().add(Aspect.METAL, 10).add(Aspect.ARMOR, 6)
                .add(Aspect.LIGHT, 6));                        // Irradiant Reinforced Plate
        crafting(10, list().add(Aspect.METAL, 8).add(Aspect.ELDRITCH, 4)); // Iridium Ingot (канон IC2)
        crafting(11, list().add(Aspect.METAL, 3).add(Aspect.ENERGY, 2)
                .add(Aspect.POISON, 2));                       // Uranium Ingot (канон uranium_238)
        crafting(12, list().add(Aspect.MECHANISM, 8).add(Aspect.EXCHANGE, 8)
                .add(Aspect.ENERGY, 6));                       // MT Core
        crafting(13, list().add(Aspect.MECHANISM, 8).add(Aspect.ELDRITCH, 8)
                .add(Aspect.ENERGY, 8));                       // Quantum Core

        // --- Солнечные шлемы ---
        tag("advanced_solar_helmet", 0, list().add(Aspect.ARMOR, 4)
                .add(Aspect.LIGHT, 6).add(Aspect.MECHANISM, 4).add(Aspect.ENERGY, 2));
        tag("hybrid_solar_helmet", 0, list().add(Aspect.ARMOR, 6)
                .add(Aspect.LIGHT, 8).add(Aspect.MECHANISM, 6).add(Aspect.ENERGY, 4));
        tag("ultimate_solar_helmet", 0, list().add(Aspect.ARMOR, 8)
                .add(Aspect.LIGHT, 10).add(Aspect.MECHANISM, 8).add(Aspect.ENERGY, 6));

        UTLog.info("ASP aspects: {} registered, {} missing", registered, missing);
    }

    private static void machine(int meta, AspectList aspects) {
        tag("machines", meta, aspects);
    }

    private static void crafting(int meta, AspectList aspects) {
        tag("crafting", meta, aspects);
    }

    private static void tag(String path, int meta, AspectList aspects) {
        ItemStack stack = ModItems.item(CompatIds.ASP, path, meta);
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
