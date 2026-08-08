package unboundtech.recipe;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.ItemResource;
import unboundtech.UTLog;
import unboundtech.compat.ic2.IC2Handles;
import unboundtech.init.UTBlocks;
import unboundtech.init.UTItems;
import unboundtech.research.UTResearch;

/**
 * Рецепты Таумкрафта для контента мода (тигель + арканный верстак).
 * Объекты рецептов сохраняются, чтобы вставить их страницами в исследования.
 *
 * Вызывается в postInit — после того, как ТК и IC2 закончили свою регистрацию.
 */
public final class UTRecipes {

    public static CrucibleRecipe thaumSteel;
    public static ShapedArcaneRecipe thaumGenerator;
    public static ShapedArcaneRecipe aethericEngine;

    private UTRecipes() {
    }

    public static void register() {
        registerThaumSteel();
        registerConverters();
    }

    /**
     * Таум-Сталь: закалённое железо IC2, пропитанное магией.
     * Катализатор — предмет IC2; если имя не разрешилось, откатываемся
     * на ванильный слиток железа (рецепт остаётся играбельным).
     */
    private static void registerThaumSteel() {
        // В IC2 Experimental «закалённое железо» = ingot/steel
        // (refined_iron существует только в IC2 Classic).
        ItemStack steel = IC2Handles.item("ingot", "steel");
        ItemStack catalyst = steel.isEmpty()
                ? new ItemStack(Items.IRON_INGOT)
                : steel;
        if (steel.isEmpty()) {
            UTLog.warn("IC2 steel ingot not found — Thaum Steel falls back to vanilla iron ingot");
        }
        thaumSteel = ThaumcraftApi.addCrucibleRecipe(
                UTResearch.THAUM_STEEL,
                new ItemStack(UTItems.thaumSteelIngot),
                catalyst,
                new AspectList().add(Aspect.METAL, 4).add(Aspect.MAGIC, 2));
    }

    private static void registerConverters() {
        ItemStack generator = IC2Handles.item("te", "generator");
        ItemStack circuit = IC2Handles.item("crafting", "circuit");
        ItemStack advCircuit = IC2Handles.item("crafting", "advanced_circuit");
        ItemStack steel = new ItemStack(UTItems.thaumSteelIngot);
        ItemStack quicksilver = new ItemStack(ConfigItems.itemResource, 1,
                ItemResource.META_QUICKSILVER);

        if (generator.isEmpty() || circuit.isEmpty()) {
            UTLog.warn("IC2 generator/circuit not found — converter recipes skipped");
            return;
        }

        // Таум-Генератор: генератор IC2 в оправе из таум-стали, ртуть как проводник ауры.
        thaumGenerator = ThaumcraftApi.addArcaneCraftingRecipe(
                UTResearch.VIS_TO_EU_GENERATOR,
                new ItemStack(UTBlocks.thaumGenerator),
                new AspectList().add(Aspect.ORDER, 25).add(Aspect.FIRE, 25).add(Aspect.AIR, 15),
                "SQS",
                "CGC",
                "SQS",
                'S', steel,
                'Q', quicksilver,
                'C', circuit,
                'G', generator);

        // Эфирный Двигатель: обратное преобразование — продвинутая схема + осколок порядка.
        ItemStack orderShard = new ItemStack(ConfigItems.itemShard, 1, 4);
        aethericEngine = ThaumcraftApi.addArcaneCraftingRecipe(
                UTResearch.EU_TO_VIS_ENGINE,
                new ItemStack(UTBlocks.aethericEngine),
                new AspectList().add(Aspect.ORDER, 25).add(Aspect.ENTROPY, 15).add(Aspect.AIR, 25),
                "SOS",
                "QAQ",
                "SBS",
                'S', steel,
                'O', orderShard,
                'Q', quicksilver,
                'A', advCircuit.isEmpty() ? circuit : advCircuit,
                'B', new ItemStack(Blocks.GLASS));
    }
}
