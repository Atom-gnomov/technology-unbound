package unboundtech.aspects;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import unboundtech.UTLog;
import unboundtech.common.UTBlocks;
import unboundtech.common.UTRecipes;

/**
 * Регистрация аспектов НАШИХ объектов.
 *
 * Канон {@code docs/design/04_systems/aspect_economy.md} §5 требует применять
 * формулу к нашим предметам «обязательно, все без исключения». Без явной
 * регистрации Таумкрафт вывел бы аспекты сам
 * ({@code ThaumcraftCraftingManager.generateTags} разбирает арканные,
 * тигельные и инфузионные рецепты) — но по СВОИМ правилам, а не по нашей
 * формуле, и числа карточек оказались бы фикцией.
 *
 * Вызывается из postInit ПОСЛЕ {@link UTRecipes} (нужны сами рецепты) и
 * ПОСЛЕ аспектов IC2 (нужны аспекты компонентов).
 */
public final class UTAspects {

    private UTAspects() {
    }

    public static void register() {
        int registered = 0;
        registered += fromArcane(UTBlocks.thaumGenerator, UTRecipes.thaumGenerator,
                AspectFormula.Process.MACHINE_ASSEMBLY, "thaum_generator");
        registered += fromArcane(UTBlocks.aethericEngine, UTRecipes.aethericEngine,
                AspectFormula.Process.MACHINE_ASSEMBLY, "aetheric_engine");
        UTLog.info("Object aspects derived by formula: {}", registered);
    }

    /**
     * Считает аспекты по арканному рецепту объекта и регистрирует их.
     *
     * @return 1, если аспекты зарегистрированы, иначе 0
     */
    private static int fromArcane(Block block, ShapedArcaneRecipe recipe,
                                  AspectFormula.Process process, String label) {
        if (block == null) {
            UTLog.warn("Aspects for {} skipped: block is not registered", label);
            return 0;
        }
        AspectList aspects = AspectFormula.deriveFromArcane(recipe, process, label);
        if (aspects == null || aspects.size() == 0) {
            // Рецепт не встал (нет предмета чужого мода) — блок всё равно
            // недостижим, назначать ему аспекты не за что.
            return 0;
        }
        ThaumcraftApi.registerObjectTag(new ItemStack(block), aspects);
        return 1;
    }
}
