package unboundtech.aspects;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.ItemResource;
import unboundtech.UTLog;
import unboundtech.common.UTBlocks;
import unboundtech.common.UTItems;
import unboundtech.common.UTRecipes;
import unboundtech.compat.ic2.IC2Handles;

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
        registered += registerTemperedTier();
        registered += fromArcane(UTBlocks.thaumGenerator, UTRecipes.thaumGenerator,
                AspectFormula.Process.MACHINE_ASSEMBLY, "thaum_generator");
        registered += fromArcane(UTBlocks.aethericEngine, UTRecipes.aethericEngine,
                AspectFormula.Process.MACHINE_ASSEMBLY, "aetheric_engine");
        UTLog.info("Object aspects derived by formula: {}", registered);
    }

    /**
     * Тир T2. Состав берётся из тех же рецептов, что зарегистрированы в игре
     * ({@code tempered_thaumium_tools.md} §7, {@code ..._armor.md} §7).
     *
     * Эталонный путь материала — А, доменная печь: предмет один, значит и
     * список аспектов у него один (`tempered_thaumium.md` §7).
     *
     * У брони и инструментов подписи нет — верстак обычный. Аспекты
     * назначения (Tutamen у брони, Perfodio у кирки, Telum у меча) ТК
     * дописывает САМ по классу предмета и его характеристикам; руками их
     * писать нельзя, вышло бы вдвое (§2.6).
     */
    private static int registerTemperedTier() {
        ItemStack ingot = new ItemStack(UTItems.temperedIngot);
        ItemStack thaumium = new ItemStack(ConfigItems.itemResource, 1,
                ItemResource.META_THAUMIUM_INGOT);
        int count = 0;

        count += fromComponents(UTItems.temperedIngot, AspectFormula.Process.SMELTING,
                "tempered_thaumium_ingot", thaumium);

        count += fromComponents(UTItems.temperedHelmet, "tempered_helmet", stack(ingot, 5));
        count += fromComponents(UTItems.temperedChestplate, "tempered_chestplate",
                stack(ingot, 8));
        count += fromComponents(UTItems.temperedLeggings, "tempered_leggings", stack(ingot, 7));
        count += fromComponents(UTItems.temperedBoots, "tempered_boots", stack(ingot, 4));

        count += fromComponents(UTItems.temperedPickaxe, "tempered_pickaxe",
                stack(ingot, 3), sticks(2));
        count += fromComponents(UTItems.temperedAxe, "tempered_axe",
                stack(ingot, 3), sticks(2));
        count += fromComponents(UTItems.temperedSword, "tempered_sword",
                stack(ingot, 2), sticks(1));
        count += fromComponents(UTItems.temperedShovel, "tempered_shovel",
                stack(ingot, 1), sticks(2));
        count += fromComponents(UTItems.temperedHoe, "tempered_hoe",
                stack(ingot, 2), sticks(2));

        // Ключ — из ОБЫЧНОГО таумия (§2 карточки: ему нужна гибкость живого
        // металла). Шесть слитков, сетка ключа IC2.
        count += fromComponents(UTItems.thaumiumWrench, "thaumium_wrench",
                stack(thaumium, 6));

        // Чернильница: аспекты компонентов чужие (ТК и IC2), точный итог
        // печатается в лог — карточка держит лишь ожидаемый профиль.
        ItemStack battery = IC2Handles.item("re_battery", null);
        if (!battery.isEmpty() && ConfigItems.itemInkwell != null) {
            count += fromComponents(UTItems.electricScribingTools, "electric_scribing_tools",
                    new ItemStack(ConfigItems.itemInkwell), ingot.copy(), battery);
        }
        return count;
    }

    private static ItemStack stack(ItemStack base, int size) {
        ItemStack copy = base.copy();
        copy.setCount(size);
        return copy;
    }

    private static ItemStack sticks(int size) {
        return new ItemStack(net.minecraft.init.Items.STICK, size);
    }

    /** Обычный верстак — подписи процесса нет. */
    private static int fromComponents(Item item, String label, ItemStack... components) {
        return fromComponents(item, AspectFormula.Process.PLAIN_BENCH, label, components);
    }

    private static int fromComponents(Item item, AspectFormula.Process process,
                                      String label, ItemStack... components) {
        if (item == null) {
            UTLog.warn("Aspects for {} skipped: item is not registered", label);
            return 0;
        }
        AspectList aspects = AspectFormula.derive(
                java.util.Arrays.asList(components), process, label);
        if (aspects == null || aspects.size() == 0) {
            return 0;
        }
        ThaumcraftApi.registerObjectTag(new ItemStack(item), aspects);
        return 1;
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
