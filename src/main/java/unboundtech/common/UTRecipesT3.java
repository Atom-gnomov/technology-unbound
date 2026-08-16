package unboundtech.common;

import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.common.config.ConfigBlocks;
import unboundtech.UTLog;
import unboundtech.compat.ic2.IC2Handles;
import unboundtech.research.UTResearch;

/**
 * Арканные рецепты тира T3.
 *
 * Пока здесь один Флюкс-Конденсатор (`flux_condenser.md` §6). «Сердцем»
 * рецепта выбрана катушка IC2 (`crafting:coil`) — открытый вопрос §12.1
 * закрыт лором самой карточки: «подставить ей медные катушки вместо
 * пустого воздуха». Наличие константы в {@code CraftingItemType} сверено
 * по jar-у IC2 2.8.222.
 */
public final class UTRecipesT3 {

    public static ShapedArcaneRecipe fluxCondenser;

    private UTRecipesT3() {
    }

    public static void register() {
        fluxCondenser = registerFluxCondenser();
    }

    /**
     * Сетка §6: крест из трёх слитков закалённого таумия, катушки IC2 и
     * банки эссенции ТК. Аспекты — Ordo 10, Perditio 7, Ignis 5: только
     * примордиалы (канон §6.8), числа после калибровки (§4: было 45 вис
     * при вилке T3 в 20–35).
     */
    private static ShapedArcaneRecipe registerFluxCondenser() {
        ItemStack coil = IC2Handles.item("crafting", "coil");
        if (coil.isEmpty()) {
            UTLog.warn("Flux Condenser recipe skipped: IC2 coil not found");
            return null;
        }
        return ThaumcraftApi.addArcaneCraftingRecipe(
                UTResearch.FLUX_CONDENSER,
                new ItemStack(UTBlocks.fluxCondenser),
                new AspectList().add(Aspect.ORDER, 10).add(Aspect.ENTROPY, 7)
                        .add(Aspect.FIRE, 5),
                " T ",
                "TCT",
                " J ",
                'T', new ItemStack(UTItems.temperedIngot),
                'C', coil,
                'J', new ItemStack(ConfigBlocks.blockJar));
    }
}
