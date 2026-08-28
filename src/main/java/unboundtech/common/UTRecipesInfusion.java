package unboundtech.common;

import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.config.ConfigItems;
import unboundtech.UTLog;
import unboundtech.compat.ic2.IC2Handles;
import unboundtech.research.UTResearch;

/**
 * ИНФУЗИОННЫЕ рецепты мода — отдельно от арканных намеренно: матрица
 * платит эссенцией из банок, и составные аспекты здесь ЗАКОННЫ, а
 * гвард-тест «в арканных стоимостях только прималы» сторожит файлы
 * UTRecipes*T*.java построчно и не отличил бы одно от другого.
 */
public final class UTRecipesInfusion {

    private UTRecipesInfusion() {
    }

    /**
     * Фокус Заряда (`techno_foci.md` §6): матрица инфузии, нестабильность
     * 3. Центр по карточке — «пустой фокус ТК», которого в TC4 не
     * существует (второй такой случай после «флюкса конденсатора») —
     * адаптация: центр — медная катушка IC2, сердце любой машины;
     * пьедесталы карточки сохранены.
     */
    public static InfusionRecipe registerFocusCharge() {
        ItemStack coil = IC2Handles.item("crafting", "coil");
        if (coil.isEmpty()) {
            UTLog.warn("Focus Charge recipe skipped: IC2 coil not found");
            return null;
        }
        return ThaumcraftApi.addInfusionCraftingRecipe(
                UTResearch.FOCUS_CHARGE,
                new ItemStack(UTItems.focusCharge),
                3,
                new AspectList().add(Aspect.MAGIC, 24).add(Aspect.ENERGY, 20)
                        .add(Aspect.MECHANISM, 16),
                coil,
                new ItemStack[]{
                        new ItemStack(ConfigItems.itemShard, 1, 4),
                        new ItemStack(UTItems.temperedIngot),
                        new ItemStack(ConfigItems.itemShard, 1, 1),
                        new ItemStack(UTItems.temperedIngot),
                });
    }
}
