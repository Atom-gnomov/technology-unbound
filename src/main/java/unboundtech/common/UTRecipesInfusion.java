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
     * Флюкс-Аркебуза (`flux_arquebus.md` §6): первый ствол, требующий
     * алтаря. Центр — Флюкс-Револьвер (расходуется), нестабильность 5
     * (нижняя граница вилки T4 после калибровки).
     */
    public static InfusionRecipe registerFluxArquebus() {
        ItemStack fibre = IC2Handles.item("crafting", "carbon_fibre");
        if (fibre.isEmpty()) {
            UTLog.warn("Flux Arquebus recipe skipped: IC2 carbon fibre not found");
            return null;
        }
        return ThaumcraftApi.addInfusionCraftingRecipe(
                UTResearch.FLUX_ARQUEBUS,
                new ItemStack(UTItems.fluxArquebus),
                5,
                new AspectList().add(Aspect.WEAPON, 32).add(Aspect.FIRE, 24)
                        .add(Aspect.MECHANISM, 16).add(Aspect.MAGIC, 16),
                new ItemStack(UTItems.fluxRevolver),
                new ItemStack[]{
                        new ItemStack(UTItems.temperedIngot),
                        new ItemStack(ConfigItems.itemShard, 1, 1),
                        new ItemStack(UTItems.temperedIngot),
                        new ItemStack(ConfigItems.itemShard, 1, 4),
                        fibre,
                });
    }

    /**
     * Мортира Механистов (`mechanist_mortar.md` §6): нестабильность 9 —
     * нижняя граница T5, чуть тяжелее крафта Ихора, как положено
     * вершине оружейной ветки. Центр — продвинутый корпус машины IC2,
     * аркебуза расходуется.
     */
    public static InfusionRecipe registerMechanistMortar() {
        ItemStack casing = IC2Handles.item("resource", "advanced_machine");
        ItemStack iridium = IC2Handles.item("crafting", "iridium");
        if (casing.isEmpty() || iridium.isEmpty()) {
            UTLog.warn("Mechanist Mortar recipe skipped: IC2 parts not found");
            return null;
        }
        return ThaumcraftApi.addInfusionCraftingRecipe(
                UTResearch.MECHANIST_MORTAR,
                new ItemStack(unboundtech.common.UTBlocks.mechanistMortar),
                9,
                new AspectList().add(Aspect.WEAPON, 56).add(Aspect.MECHANISM, 40)
                        .add(Aspect.EARTH, 32).add(Aspect.MAGIC, 24),
                casing,
                new ItemStack[]{
                        new ItemStack(UTItems.temperedIngot),
                        new ItemStack(UTItems.fluxArquebus),
                        new ItemStack(UTItems.temperedIngot),
                        iridium,
                        new ItemStack(ConfigItems.itemShard, 1, 3),
                });
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
