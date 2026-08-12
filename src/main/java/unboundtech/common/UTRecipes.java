package unboundtech.common;

import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.ItemResource;
import unboundtech.UTLog;
import unboundtech.compat.ic2.IC2Handles;
import unboundtech.research.UTResearch;

/**
 * Рецепты арканного верстака для машин мода.
 *
 * Корпус — ЗАКАЛЁННЫЙ ТАУМИЙ (`05_objects/tempered_thaumium.md` §2): именно
 * он в каноне корпусной материал моста. Временный обычный таумий с
 * пометкой «фаза 3в» убран вместе с самим названием «таум-сталь», которое
 * канон отменил (карточка §0: рядом с «Таум-слитком» порта оно путалось).
 *
 * Рецепты хранятся в полях — страницы исследований показывают сам объект
 * рецепта, поэтому карта ConfigResearch.recipes не нужна.
 */
public final class UTRecipes {

    /** Меты осколков: 1 = Ignis (огонь), 4 = Ordo (порядок). */
    private static final int SHARD_FIRE = 1;
    private static final int SHARD_ORDER = 4;

    public static ShapedArcaneRecipe thaumGenerator;
    public static ShapedArcaneRecipe aethericEngine;

    private UTRecipes() {
    }

    public static void register() {
        thaumGenerator = registerThaumGenerator();
        aethericEngine = registerAethericEngine();
    }

    private static ShapedArcaneRecipe registerThaumGenerator() {
        ItemStack generator = IC2Handles.item("te", "generator");
        if (generator.isEmpty()) {
            UTLog.warn("Thaum Generator recipe skipped: IC2 generator not found");
            return null;
        }
        return ThaumcraftApi.addArcaneCraftingRecipe(
                UTResearch.VIS_TO_EU_GENERATOR,
                new ItemStack(UTBlocks.thaumGenerator),
                // Ordo 8, Ignis 8, Aqua 8 — пересчёт по recipe_calibration.md §4
                // (было 25/25/25 = 75 вис, в 3.1 раза выше эталона ТК;
                // вилка T3 — 20–35 вис). Пропорции и главный примал те же.
                new AspectList().add(Aspect.ORDER, 8).add(Aspect.FIRE, 8)
                        .add(Aspect.WATER, 8),
                "TST",
                "TGT",
                " S ",
                'T', temperedThaumium(),
                'S', new ItemStack(ConfigItems.itemShard, 1, SHARD_FIRE),
                'G', generator);
    }

    private static ShapedArcaneRecipe registerAethericEngine() {
        ItemStack circuit = IC2Handles.item("crafting", "advanced_circuit");
        if (circuit.isEmpty()) {
            UTLog.warn("Aetheric Engine recipe skipped: IC2 advanced circuit not found");
            return null;
        }
        return ThaumcraftApi.addArcaneCraftingRecipe(
                UTResearch.EU_TO_VIS_ENGINE,
                new ItemStack(UTBlocks.aethericEngine),
                // Ordo 8, Perditio 8, Aer 8 — тот же пересчёт (§4).
                new AspectList().add(Aspect.ORDER, 8).add(Aspect.ENTROPY, 8)
                        .add(Aspect.AIR, 8),
                "TST",
                "TCT",
                " Q ",
                'T', temperedThaumium(),
                'S', new ItemStack(ConfigItems.itemShard, 1, SHARD_ORDER),
                'C', circuit,
                'Q', new ItemStack(ConfigItems.itemResource, 1,
                        ItemResource.META_QUICKSILVER));
    }

    /** Корпусной материал моста — закалённый таумий (T2). */
    private static ItemStack temperedThaumium() {
        return new ItemStack(UTItems.temperedIngot);
    }
}
