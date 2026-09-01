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
    /**
     * Пустотный Иридий (`void_iridium.md` §6): центр — иридиевая руда
     * IC2, нестабильность 8. ⚠️ Реализационная замена пьедесталов: в
     * ТК4 нет кристаллов Vacuos/Metallum (шарды только прималов) —
     * вместо них шард Perditio (дух Пустоты) и слиток таумия (Metallum);
     * зафиксировано канон-заметкой при коммите.
     */
    public static InfusionRecipe registerVoidIridium() {
        ItemStack ore = IC2Handles.item("misc_resource", "iridium_ore");
        if (ore.isEmpty()) {
            ore = IC2Handles.item("resource", "iridium_ore");
        }
        if (ore.isEmpty()) {
            UTLog.warn("Void Iridium recipe skipped: IC2 iridium ore not found");
            return null;
        }
        return ThaumcraftApi.addInfusionCraftingRecipe(
                UTResearch.VOID_IRIDIUM,
                new ItemStack(UTItems.voidIridium),
                8,
                new AspectList().add(Aspect.VOID, 28).add(Aspect.METAL, 28)
                        .add(Aspect.MAGIC, 18).add(Aspect.EXCHANGE, 12),
                ore,
                new ItemStack[]{
                        new ItemStack(ConfigItems.itemResource, 1, 16),
                        new ItemStack(UTItems.temperedIngot),
                        new ItemStack(ConfigItems.itemShard, 1, 5),
                        new ItemStack(ConfigItems.itemResource, 1, 16),
                        new ItemStack(UTItems.temperedIngot),
                        new ItemStack(ConfigItems.itemResource, 1, 2),
                });
    }

    /**
     * Иридиевый наконечник (`iridium_wand_components.md` §6): x2 за
     * крафт — их всегда нужно два; центр — иридиевая пластина IC2.
     */
    public static InfusionRecipe registerIridiumWandCap() {
        ItemStack plate = IC2Handles.item("crafting", "iridium");
        if (plate.isEmpty()) {
            UTLog.warn("Iridium Wand Cap recipe skipped: IC2 iridium plate not found");
            return null;
        }
        return ThaumcraftApi.addInfusionCraftingRecipe(
                UTResearch.IRIDIUM_WAND_CAP,
                new ItemStack(UTItems.iridiumWandCap, 2),
                5,
                new AspectList().add(Aspect.MAGIC, 24).add(Aspect.ORDER, 16)
                        .add(Aspect.ENTROPY, 16).add(Aspect.METAL, 12),
                IC2Handles.withCount(plate, 2),
                new ItemStack[]{
                        new ItemStack(UTItems.voidIridium),
                        new ItemStack(ConfigItems.itemShard, 1, 4),
                        new ItemStack(ConfigItems.itemShard, 1, 5),
                });
    }

    /**
     * Сингулятор (`singulator.md` §6): центр — МФЭ IC2, нестабильность 6.
     */
    public static InfusionRecipe registerSingulator() {
        ItemStack mfe = IC2Handles.item("te", "mfe");
        if (mfe.isEmpty()) {
            UTLog.warn("Singulator recipe skipped: IC2 MFE not found");
            return null;
        }
        return ThaumcraftApi.addInfusionCraftingRecipe(
                UTResearch.SINGULATOR,
                new ItemStack(unboundtech.common.UTBlocks.singulator),
                6,
                new AspectList().add(Aspect.MAGIC, 32).add(Aspect.ENERGY, 24)
                        .add(Aspect.ORDER, 16).add(Aspect.MECHANISM, 16),
                mfe,
                new ItemStack[]{
                        new ItemStack(UTItems.voidIridium),
                        new ItemStack(UTItems.iridiumWandCap),
                        new ItemStack(ConfigItems.itemShard, 1, 4),
                        new ItemStack(UTItems.voidIridium),
                        new ItemStack(UTItems.temperedIngot),
                        new ItemStack(UTItems.temperedIngot),
                });
    }

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
