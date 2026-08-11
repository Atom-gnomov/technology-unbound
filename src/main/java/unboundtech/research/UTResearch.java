package unboundtech.research;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;
import unboundtech.UTLog;
import unboundtech.UnboundTech;
import unboundtech.compat.ic2.IC2Handles;

/**
 * Вкладка «Индустриальная Магия» (категория UNBOUNDTECH) и исследования фазы 1.
 * Дерево: docs/integration/rt_1_ic2.md + правки v3-аудита (репо порта).
 *
 * Механика открытия: THAUM_IC2_INTRO — скрытое исследование с item-триггерами
 * на машины IC2. Успешный скан машины выдаёт «зацепку» @THAUM_IC2_INTRO —
 * запись проявляется в Таумономиконе, дальше игрок исследует её обычным
 * способом. Нюансы движка порта (проверено по ResearchManager/ScanManager):
 *  - скан вообще не проходит, пока игрок не открыл родительские аспекты
 *    составных аспектов предмета (validScan) — свежий игрок сначала
 *    посканирует простые вещи, это ожидаемая прогрессия ТК;
 *  - за один скан выдаётся ОДНА случайная зацепка из всех подходящих
 *    скрытых исследований, а цель скана расходуется — если генератор
 *    (у него есть Ignis) отдал зацепку чужому исследованию, вкладку
 *    откроет скан любой другой машины (сравнение item-триггеров идёт
 *    с ignoreDamage, так что подходит любой блок-механизм te);
 *  - аспект-триггер Machina НЕ ставим по умолчанию: он срабатывал бы от
 *    ванильных поршней/воронок. Он включается только как аварийный фолбэк,
 *    если ни один item-триггер не разрешился (сменились имена в IC2).
 *
 * Ланг: имена/описания — жёсткие префиксы порта tc.research_name.KEY /
 * tc.research_text.KEY / tc.research_category.KEY; тексты страниц — наши
 * ключи unboundtech.research_page.* (страница переводит ключ как есть).
 */
public final class UTResearch {

    public static final String CATEGORY = "UNBOUNDTECH";

    public static final String INTRO = "THAUM_IC2_INTRO";
    public static final String LORE_EU_AND_VIS = "LORE_EU_AND_VIS";
    public static final String LORE_ELAN_VITAL = "LORE_ELAN_VITAL";
    public static final String ORE_MACERATION = "THAUM_ORE_MACERATION";
    public static final String ALUMENTUM_FUEL = "ALUMENTUM_FUEL";
    /** Нитор-тепло: порт ≥1.2.8.1 отдаёт 20 HU/t машинам IC2 (rt_1: NITOR_HEAT). */
    public static final String NITOR_HEAT = "NITOR_HEAT";
    /** Фаза 3а: конвертеры (спека phase3_converters_spec.md §3). */
    public static final String VIS_TO_EU_GENERATOR = "VIS_TO_EU_GENERATOR";
    public static final String EU_TO_VIS_ENGINE = "EU_TO_VIS_ENGINE";
    public static final String LORE_RESONANCE_LIMITS = "LORE_RESONANCE_LIMITS";
    /** Модуль asp: солнечная алхимия (саннариум в тигле). */
    public static final String SOLAR_SUNNARIUM = "SOLAR_SUNNARIUM";
    /** Модуль mets: техно-материалы под таумометром. */
    public static final String TECHNO_MATERIALS = "TECHNO_MATERIALS";

    private UTResearch() {
    }

    public static void register() {
        ResearchCategories.registerCategory(
                CATEGORY,
                new ResourceLocation(UnboundTech.MODID, "textures/misc/tab_unboundtech.png"),
                new ResourceLocation("thaumcraft", "textures/gui/gui_researchback.png"));

        ItemStack generator = IC2Handles.item("te", "generator");
        ItemStack macerator = IC2Handles.item("te", "macerator");

        java.util.List<ItemStack> triggers = new java.util.ArrayList<>();
        for (ItemStack machine : new ItemStack[]{generator, macerator,
                IC2Handles.item("te", "electric_furnace"),
                IC2Handles.item("te", "solar_generator")}) {
            if (!machine.isEmpty()) {
                triggers.add(machine);
            }
        }

        ResearchItem intro = new ResearchItem(
                INTRO, CATEGORY,
                new AspectList().add(Aspect.MECHANISM, 6).add(Aspect.ENERGY, 6)
                        .add(Aspect.MAGIC, 3),
                0, 0, 1,
                generator.isEmpty()
                        ? new ItemStack(net.minecraft.init.Items.REDSTONE)
                        : generator)
                .setSpecial()
                .setHidden()
                .setPages(
                        new ResearchPage("unboundtech.research_page.THAUM_IC2_INTRO.1"),
                        new ResearchPage("unboundtech.research_page.THAUM_IC2_INTRO.2"));
        if (!triggers.isEmpty()) {
            intro.setItemTriggers(triggers.toArray(new ItemStack[0]));
        } else {
            UTLog.warn("No IC2 machine items resolved for THAUM_IC2_INTRO triggers; "
                    + "falling back to the broad Machina aspect trigger");
            intro.setAspectTriggers(Aspect.MECHANISM);
        }
        intro.registerResearchItem();

        new ResearchItem(
                LORE_EU_AND_VIS, CATEGORY,
                new AspectList().add(Aspect.ENERGY, 4).add(Aspect.MAGIC, 4),
                -2, -1, 1,
                new ItemStack(net.minecraft.init.Items.BOOK))
                .setRound()
                .setSecondary()
                .setParents(INTRO)
                .setPages(new ResearchPage("unboundtech.research_page.LORE_EU_AND_VIS.1"))
                .registerResearchItem();

        new ResearchItem(
                LORE_ELAN_VITAL, CATEGORY,
                new AspectList().add(Aspect.AURA, 4).add(Aspect.MECHANISM, 4),
                -2, 1, 1,
                new ItemStack(net.minecraft.init.Items.BOOK))
                .setRound()
                .setSecondary()
                .setParents(INTRO)
                .setPages(new ResearchPage("unboundtech.research_page.LORE_ELAN_VITAL.1"))
                .registerResearchItem();

        ResearchItem maceration = new ResearchItem(
                ORE_MACERATION, CATEGORY,
                new AspectList().add(Aspect.ENTROPY, 5).add(Aspect.METAL, 5)
                        .add(Aspect.MECHANISM, 3),
                2, -1, 1,
                macerator.isEmpty()
                        ? new ItemStack(net.minecraft.init.Blocks.IRON_ORE)
                        : macerator)
                .setParents(INTRO)
                .setPages(
                        new ResearchPage("unboundtech.research_page.THAUM_ORE_MACERATION.1"),
                        new ResearchPage("unboundtech.research_page.THAUM_ORE_MACERATION.2"));
        maceration.registerResearchItem();

        new ResearchItem(
                ALUMENTUM_FUEL, CATEGORY,
                new AspectList().add(Aspect.FIRE, 5).add(Aspect.ENERGY, 5),
                2, 1, 1,
                new ItemStack(thaumcraft.common.config.ConfigItems.itemResource, 1,
                        thaumcraft.common.items.ItemResource.META_ALUMENTUM))
                .setParents(INTRO)
                .setPages(new ResearchPage("unboundtech.research_page.ALUMENTUM_FUEL.1"))
                .registerResearchItem();

        // Поведение живёт в порте ≥1.2.8.1 (TileNitor = IHeatSource, 20 HU/t);
        // запись документирует его игроку. Ветка продолжится нитор-термальным
        // генератором в фазе 3+ (rt_1_ic2.md).
        new ResearchItem(
                NITOR_HEAT, CATEGORY,
                new AspectList().add(Aspect.FIRE, 4).add(Aspect.LIGHT, 4)
                        .add(Aspect.ENERGY, 4),
                4, 0, 1,
                new ItemStack(thaumcraft.common.config.ConfigItems.itemResource, 1,
                        thaumcraft.common.items.ItemResource.META_NITOR))
                .setParents(INTRO)
                .setPages(new ResearchPage("unboundtech.research_page.NITOR_HEAT.1"))
                .registerResearchItem();

        UTLog.info("Research tab {} registered ({} entries)", CATEGORY, 6);
    }

    /**
     * Исследования фазы 3а (конвертеры). Вызывается ПОСЛЕ {@link #register()}
     * и после регистрации рецептов: страницы показывают объекты рецептов
     * напрямую, поэтому карта ConfigResearch.recipes не нужна.
     *
     * Родитель двигателя — сам генератор, а не INTRO: генератор и так растёт
     * из INTRO, а прямая линия через пол-вкладки только запутывала бы схему
     * (спека §3 требует лишь «требует VIS_TO_EU_GENERATOR»).
     */
    public static void registerConverters() {
        ResearchItem generator = new ResearchItem(
                VIS_TO_EU_GENERATOR, CATEGORY,
                new AspectList().add(Aspect.ENERGY, 8).add(Aspect.MECHANISM, 6)
                        .add(Aspect.MAGIC, 6).add(Aspect.FIRE, 4),
                4, -2, 2,
                new ItemStack(unboundtech.common.UTBlocks.thaumGenerator))
                // TODO (T1_T4_audit А-7): родитель-ЗАГЛУШКА в обход гейта T2.
                // Канон требует TEMPERED_THAUMIUM (03_progression §3), но его
                // исследования ещё нет. Перевесить при реализации материала.
                .setParents(ORE_MACERATION)
                .setPages(pagesWithRecipe(
                        new ResearchPage("unboundtech.research_page.VIS_TO_EU_GENERATOR.1"),
                        new ResearchPage("unboundtech.research_page.VIS_TO_EU_GENERATOR.2"),
                        unboundtech.common.UTRecipes.thaumGenerator));
        generator.registerResearchItem();

        new ResearchItem(
                LORE_RESONANCE_LIMITS, CATEGORY,
                new AspectList().add(Aspect.AURA, 6).add(Aspect.MECHANISM, 4),
                6, -3, 1,
                new ItemStack(net.minecraft.init.Items.BOOK))
                .setRound()
                .setSecondary()
                .setParents(VIS_TO_EU_GENERATOR)
                .setPages(new ResearchPage(
                        "unboundtech.research_page.LORE_RESONANCE_LIMITS.1"))
                .registerResearchItem();

        new ResearchItem(
                EU_TO_VIS_ENGINE, CATEGORY,
                new AspectList().add(Aspect.ENERGY, 10).add(Aspect.AURA, 8)
                        .add(Aspect.EXCHANGE, 6).add(Aspect.ORDER, 4),
                6, -1, 2,
                new ItemStack(unboundtech.common.UTBlocks.aethericEngine))
                .setParents(VIS_TO_EU_GENERATOR)
                .setPages(pagesWithRecipe(
                        new ResearchPage("unboundtech.research_page.EU_TO_VIS_ENGINE.1"),
                        new ResearchPage("unboundtech.research_page.EU_TO_VIS_ENGINE.2"),
                        unboundtech.common.UTRecipes.aethericEngine))
                .registerResearchItem();

        UTLog.info("Converter research registered (phase 3a)");
    }

    /**
     * Собирает страницы записи, добавляя страницу рецепта только если он
     * зарегистрирован (без IC2-предмета рецепта нет — пустую страницу
     * показывать нельзя).
     */
    private static ResearchPage[] pagesWithRecipe(ResearchPage first, ResearchPage second,
            thaumcraft.api.crafting.ShapedArcaneRecipe recipe) {
        if (recipe == null) {
            return new ResearchPage[]{first, second};
        }
        return new ResearchPage[]{first, second, new ResearchPage(recipe)};
    }

    /**
     * Запись модуля asp. Вызывается ПОСЛЕ {@link #register()} (категория уже
     * существует) и только при активном модуле — у игрока без ASP записи нет.
     */
    public static void registerAspLore() {
        ItemStack sunnarium = unboundtech.compat.ModItems.item(
                unboundtech.CompatIds.ASP, "crafting", 0);
        new ResearchItem(
                SOLAR_SUNNARIUM, CATEGORY,
                new AspectList().add(Aspect.LIGHT, 4).add(Aspect.ENERGY, 4)
                        .add(Aspect.FIRE, 2),
                0, -2, 1,
                sunnarium.isEmpty()
                        ? new ItemStack(net.minecraft.init.Items.GLOWSTONE_DUST)
                        : sunnarium)
                .setRound()
                .setSecondary()
                .setParents(INTRO)
                .setPages(
                        new ResearchPage("unboundtech.research_page.SOLAR_SUNNARIUM.1"),
                        new ResearchPage("unboundtech.research_page.SOLAR_SUNNARIUM.2"))
                .registerResearchItem();
        UTLog.info("Research {} registered (asp module)", SOLAR_SUNNARIUM);
    }

    /** Запись модуля mets. Контракт тот же, что у {@link #registerAspLore()}. */
    public static void registerMetsLore() {
        ItemStack circuit = unboundtech.compat.ModItems.item(
                unboundtech.CompatIds.METS, "super_circuit", 0);
        new ResearchItem(
                TECHNO_MATERIALS, CATEGORY,
                new AspectList().add(Aspect.METAL, 4).add(Aspect.MECHANISM, 4)
                        .add(Aspect.ORDER, 2),
                0, 2, 1,
                circuit.isEmpty()
                        ? new ItemStack(net.minecraft.init.Items.IRON_INGOT)
                        : circuit)
                .setRound()
                .setSecondary()
                .setParents(INTRO)
                .setPages(
                        new ResearchPage("unboundtech.research_page.TECHNO_MATERIALS.1"),
                        new ResearchPage("unboundtech.research_page.TECHNO_MATERIALS.2"))
                .registerResearchItem();
        UTLog.info("Research {} registered (mets module)", TECHNO_MATERIALS);
    }
}
