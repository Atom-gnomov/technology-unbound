package unboundtech.common;

import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigItems;
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
    public static ShapedArcaneRecipe thaumicOverclocker;
    public static ShapedArcaneRecipe resonantSplitter;

    private UTRecipesT3() {
    }

    public static void register() {
        fluxCondenser = registerFluxCondenser();
        thaumicOverclocker = registerThaumicOverclocker();
        resonantSplitter = registerResonantSplitter();
    }

    /**
     * Расщепитель (`resonant_splitter.md` §6): четыре слитка вокруг
     * центрифуги ТК и корпуса машины IC2. §12.2 карточки закрыт: центрифуга
     * порта — это `blockTube` мета 2 (сверено по ConfigRecipesArcaneSlice),
     * а не blockMetalDevice. Аспекты — Ordo 10, Perditio 8, Aer 4 после
     * калибровки (было 55 вис при вилке T3 20–35).
     */
    private static ShapedArcaneRecipe registerResonantSplitter() {
        ItemStack casing = IC2Handles.item("resource", "machine");
        if (casing.isEmpty()) {
            UTLog.warn("Resonant Splitter recipe skipped: IC2 machine casing not found");
            return null;
        }
        return ThaumcraftApi.addArcaneCraftingRecipe(
                UTResearch.RESONANT_SPLITTER,
                new ItemStack(UTBlocks.resonantSplitter),
                new AspectList().add(Aspect.ORDER, 10).add(Aspect.ENTROPY, 8)
                        .add(Aspect.AIR, 4),
                "TCT",
                "TMT",
                'T', new ItemStack(UTItems.temperedIngot),
                'C', new ItemStack(ConfigBlocks.blockTube, 1, 2),
                'M', casing);
    }

    /**
     * Оверклокер (`thaumic_overclocker.md` §6): пустой фиал над родным
     * оверклокером IC2 в оправе из трёх слитков. Аспекты — Ordo 8, Aer 8,
     * Ignis 4 после калибровки (было 50 вис при вилке T3 20–35).
     */
    private static ShapedArcaneRecipe registerThaumicOverclocker() {
        ItemStack ic2Overclocker = IC2Handles.item("upgrade", "overclocker");
        if (ic2Overclocker.isEmpty()) {
            UTLog.warn("Thaumic Overclocker recipe skipped: IC2 overclocker not found");
            return null;
        }
        return ThaumcraftApi.addArcaneCraftingRecipe(
                UTResearch.THAUMIC_OVERCLOCKER,
                new ItemStack(UTItems.thaumicOverclocker),
                new AspectList().add(Aspect.ORDER, 8).add(Aspect.AIR, 8)
                        .add(Aspect.FIRE, 4),
                " P ",
                "TOT",
                " T ",
                'P', new ItemStack(ConfigItems.itemEssence, 1, 0),
                'O', ic2Overclocker,
                'T', new ItemStack(UTItems.temperedIngot));
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
