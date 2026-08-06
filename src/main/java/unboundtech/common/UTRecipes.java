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
 * Рецепты арканного верстака для машин мода (спека фазы 3а §3).
 *
 * Корпус собирается из таумия: таум-сталь — материал фазы 3в, до неё
 * временно стоит слиток таумия (TODO фазы 3в: заменить обе строки).
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
                new AspectList().add(Aspect.ORDER, 25).add(Aspect.FIRE, 25)
                        .add(Aspect.WATER, 25),
                "TST",
                "TGT",
                " S ",
                'T', thaumium(),
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
                new AspectList().add(Aspect.ORDER, 25).add(Aspect.ENTROPY, 25)
                        .add(Aspect.AIR, 25),
                "TST",
                "TCT",
                " Q ",
                'T', thaumium(),
                'S', new ItemStack(ConfigItems.itemShard, 1, SHARD_ORDER),
                'C', circuit,
                'Q', new ItemStack(ConfigItems.itemResource, 1,
                        ItemResource.META_QUICKSILVER));
    }

    /** TODO(фаза 3в): заменить на таум-сталь, когда она появится. */
    private static ItemStack thaumium() {
        return new ItemStack(ConfigItems.itemResource, 1,
                ItemResource.META_THAUMIUM_INGOT);
    }
}
