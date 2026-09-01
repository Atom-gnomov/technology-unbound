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
    /** Тир T2: материал моста и всё, что из него (`03_progression.md` §3). */
    public static final String TEMPERED_THAUMIUM = "TEMPERED_THAUMIUM";
    public static final String TEMPERED_ARMOR = "TEMPERED_ARMOR";
    public static final String TEMPERED_TOOLS = "TEMPERED_TOOLS";
    public static final String ELECTRIC_SCRIBING = "ELECTRIC_SCRIBING";
    /** Фаза 3а: конвертеры (спека phase3_converters_spec.md §3). */
    public static final String VIS_TO_EU_GENERATOR = "VIS_TO_EU_GENERATOR";
    public static final String EU_TO_VIS_ENGINE = "EU_TO_VIS_ENGINE";
    public static final String LORE_RESONANCE_LIMITS = "LORE_RESONANCE_LIMITS";
    /** Сводка курсов и темпов — справочник игрока, все числа в одном месте. */
    public static final String THAUMODYNAMIC_TABLES = "THAUMODYNAMIC_TABLES";
    /** Тир T3: конец флюксовой цепочки (`flux_condenser.md`). */
    public static final String FLUX_CONDENSER = "FLUX_CONDENSER";
    /** Тир T3: скорость за эссенцию (`thaumic_overclocker.md`). */
    public static final String THAUMIC_OVERCLOCKER = "THAUMIC_OVERCLOCKER";
    /** Тир T3: расщепление без потерь (`resonant_splitter.md`). */
    public static final String RESONANT_SPLITTER = "RESONANT_SPLITTER";

    /** Индукционный Тигель — T3, алхимическая ветка. */
    public static final String INDUCTION_CRUCIBLE = "INDUCTION_CRUCIBLE";

    /** Флюкс-Револьвер: вход в оружейную ветку, с гильзой и патронами T3. */
    public static final String FLUX_REVOLVER = "FLUX_REVOLVER";

    /** Флюкс-Аркебуза: T4-ствол, открывает вис- и флюкс-патроны. */
    public static final String FLUX_ARQUEBUS = "FLUX_ARQUEBUS";

    /** Мортира Механистов: вершина оружейной ветки (T5).
     *  ⚠️ Родитель по канону — FLUX_MACHINEGUN, которого ещё нет;
     *  временно висит на аркебузе (приём А-7), перевесить с пулемётом. */
    public static final String MECHANIST_MORTAR = "MECHANIST_MORTAR";

    /** Техномаг: лорная запись об изгнаннике двух школ (§3.1). */
    public static final String TECHNOMANCER = "TECHNOMANCER";

    /** Техно-духи: лорная запись о реакции мира (T3, вилка T2 — §2.2). */
    public static final String TECHNO_SPIRITS = "TECHNO_SPIRITS";

    /** Фокус Заряда: жезл как переносная розетка. */
    public static final String FOCUS_CHARGE = "FOCUS_CHARGE";

    /** Пустотный Иридий: гейт вкладки «Синтез» (T4). */
    public static final String VOID_IRIDIUM = "VOID_IRIDIUM";

    /** Иридиевый наконечник жезла (T4; стержень — T5, отдельно). */
    public static final String IRIDIUM_WAND_CAP = "CAP_iridium";

    /** Сингулятор: жезл от розетки (T4). */
    public static final String SINGULATOR = "SINGULATOR";

    /** Патронная Линия: боеприпас пачками (T4). */
    public static final String CARTRIDGE_PRESS = "CARTRIDGE_PRESS";

    /** Вис-Кромка: отложенный урон на чужих энергоклинках (T4). */
    public static final String VIS_EDGE = "VIS_EDGE";

    /** Кольца Схемы: атрибуты игрока за 2 EU/t на кольцо. */
    public static final String SCHEMA_RINGS = "SCHEMA_RINGS";

    /** Шина эссенции: узел и кабели одной записью (кабель — расходник). */
    public static final String ESSENTIA_BUS = "ESSENTIA_BUS";

    /** Эссентиальный Накопитель — вершина логистической ветки T3. */
    public static final String ESSENTIA_VAULT = "ESSENTIA_VAULT";
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
                .setPages(
                        new ResearchPage("unboundtech.research_page.ALUMENTUM_FUEL.1"),
                        new ResearchPage("unboundtech.research_page.ALUMENTUM_FUEL.2"))
                .registerResearchItem();

        // Поведение живёт в порте ≥1.2.8.1 (TileNitor = IHeatSource, 20 HU/t);
        // запись документирует его игроку. Ветка продолжится нитор-термальным
        // генератором в фазе 3+ (rt_1_ic2.md).
        new ResearchItem(
                NITOR_HEAT, CATEGORY,
                // 18 единиц: T2 по research_design.md §2.2 (3 аспекта, 15–25).
                // Было 12 — ниже нижней границы тира.
                new AspectList().add(Aspect.FIRE, 7).add(Aspect.LIGHT, 6)
                        .add(Aspect.ENERGY, 5),
                4, 0, 1,
                new ItemStack(thaumcraft.common.config.ConfigItems.itemResource, 1,
                        thaumcraft.common.items.ItemResource.META_NITOR))
                .setParents(INTRO)
                .setPages(
                        new ResearchPage("unboundtech.research_page.NITOR_HEAT.1"),
                        new ResearchPage("unboundtech.research_page.NITOR_HEAT.2"))
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
                // complexity 3: §2.4 канона отводит тройку конвертерам
                // («всё, что меняет правила игры»); было 2.
                // 28 единиц: T3 по §2.2 (25–35); было 24 — ниже границы.
                new AspectList().add(Aspect.ENERGY, 10).add(Aspect.MECHANISM, 8)
                        .add(Aspect.MAGIC, 6).add(Aspect.FIRE, 4),
                4, -2, 3,
                new ItemStack(unboundtech.common.UTBlocks.thaumGenerator))
                // Родитель по канону (03_progression §3): корпус конвертера —
                // закалённый таумий, значит гейт T2 стоит перед гейтом T3.
                // Заглушка ORE_MACERATION (T1_T4_audit А-7) снята.
                .setParents(TEMPERED_THAUMIUM)
                .setPages(pagesWithRecipe(
                        unboundtech.common.UTRecipes.thaumGenerator,
                        new ResearchPage("unboundtech.research_page.VIS_TO_EU_GENERATOR.1"),
                        new ResearchPage("unboundtech.research_page.VIS_TO_EU_GENERATOR.2"),
                        new ResearchPage("unboundtech.research_page.VIS_TO_EU_GENERATOR.3")));
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
                // complexity 3 — как у генератора, оба конвертера (§2.4).
                new AspectList().add(Aspect.ENERGY, 10).add(Aspect.AURA, 8)
                        .add(Aspect.EXCHANGE, 6).add(Aspect.ORDER, 4),
                6, -1, 3,
                new ItemStack(unboundtech.common.UTBlocks.aethericEngine))
                .setParents(VIS_TO_EU_GENERATOR)
                .setPages(pagesWithRecipe(
                        unboundtech.common.UTRecipes.aethericEngine,
                        new ResearchPage("unboundtech.research_page.EU_TO_VIS_ENGINE.1"),
                        new ResearchPage("unboundtech.research_page.EU_TO_VIS_ENGINE.2"),
                        new ResearchPage("unboundtech.research_page.EU_TO_VIS_ENGINE.3")))
                .registerResearchItem();

        // Справочник: все курсы, темпы и сравнение с обычным топливом IC2
        // в одном месте. Числа выведены из кода (формула генератора IC2:
        // burnTime/4 тиков по 10 EU/t; Стирлинг: 0.5 EU за HU; регенерация
        // узла: 600 тиков, BRIGHT 400, PALE 900, FADING — никогда).
        new ResearchItem(
                THAUMODYNAMIC_TABLES, CATEGORY,
                new AspectList().add(Aspect.ORDER, 5).add(Aspect.MIND, 5)
                        .add(Aspect.ENERGY, 3),
                8, -1, 1,
                new ItemStack(net.minecraft.init.Items.PAPER))
                .setRound()
                .setSecondary()
                .setParents(EU_TO_VIS_ENGINE)
                .setPages(
                        new ResearchPage("unboundtech.research_page.THAUMODYNAMIC_TABLES.1"),
                        new ResearchPage("unboundtech.research_page.THAUMODYNAMIC_TABLES.2"),
                        new ResearchPage("unboundtech.research_page.THAUMODYNAMIC_TABLES.3"),
                        // Дописка T3: курс конденсатора и цена Флюкс-Заряда.
                        new ResearchPage("unboundtech.research_page.THAUMODYNAMIC_TABLES.4"))
                .registerResearchItem();

        UTLog.info("Converter research registered (phase 3a)");
    }

    /**
     * Собирает страницы записи, добавляя страницу рецепта последней и только
     * если он зарегистрирован (без IC2-предмета рецепта нет — пустую страницу
     * показывать нельзя).
     */
    private static ResearchPage[] pagesWithRecipe(
            thaumcraft.api.crafting.ShapedArcaneRecipe recipe, ResearchPage... pages) {
        if (recipe == null) {
            return pages;
        }
        ResearchPage[] all = java.util.Arrays.copyOf(pages, pages.length + 1);
        all[pages.length] = new ResearchPage(recipe);
        return all;
    }

    /**
     * Тир T2 — материал моста и всё, что из него куётся.
     * Вызывается ПОСЛЕ {@link unboundtech.common.UTRecipesT2#register()}:
     * страницы держат сами объекты рецептов.
     *
     * Стоимости — по лестнице `research_design.md` §2.2 (T2: 3 аспекта,
     * 15–25 единиц); complexity 2 — «материалы и простые машины» (§2.4).
     */
    public static void registerTemperedTier() {
        ItemStack ingot = new ItemStack(unboundtech.common.UTItems.temperedIngot);

        // --- Материал: два пути (tempered_thaumium.md §6) ---
        // 24 единицы: Metallum 10 + Ignis 8 + Praecantatio 6.
        ResearchItem material = new ResearchItem(
                TEMPERED_THAUMIUM, CATEGORY,
                new AspectList().add(Aspect.METAL, 10).add(Aspect.FIRE, 8)
                        .add(Aspect.MAGIC, 6),
                2, -3, 2,
                ingot)
                .setParents(ORE_MACERATION)
                .setPages(temperedMaterialPages());
        material.registerResearchItem();

        // --- Броня: 25 единиц, верхняя граница T2 ---
        new ResearchItem(
                TEMPERED_ARMOR, CATEGORY,
                new AspectList().add(Aspect.METAL, 10).add(Aspect.ARMOR, 9)
                        .add(Aspect.MAGIC, 6),
                0, -4, 2,
                new ItemStack(unboundtech.common.UTItems.temperedChestplate))
                .setParents(TEMPERED_THAUMIUM)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.TEMPERED_ARMOR.1"),
                        new ResearchPage("unboundtech.research_page.TEMPERED_ARMOR.2"),
                        recipePage(unboundtech.common.UTCrafting.helmet),
                        recipePage(unboundtech.common.UTCrafting.chestplate),
                        recipePage(unboundtech.common.UTCrafting.leggings),
                        recipePage(unboundtech.common.UTCrafting.boots)))
                .registerResearchItem();

        // --- Инструменты: 18 единиц (Instrumentum 8, Metallum 6, Ordo 4) ---
        new ResearchItem(
                TEMPERED_TOOLS, CATEGORY,
                new AspectList().add(Aspect.TOOL, 8).add(Aspect.METAL, 6)
                        .add(Aspect.ORDER, 4),
                2, -5, 2,
                new ItemStack(unboundtech.common.UTItems.temperedPickaxe))
                .setParents(TEMPERED_THAUMIUM)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.TEMPERED_TOOLS.1"),
                        new ResearchPage("unboundtech.research_page.TEMPERED_TOOLS.2"),
                        new ResearchPage("unboundtech.research_page.TEMPERED_TOOLS.3"),
                        recipePage(unboundtech.common.UTCrafting.pickaxe),
                        recipePage(unboundtech.common.UTCrafting.axe),
                        recipePage(unboundtech.common.UTCrafting.shovel),
                        recipePage(unboundtech.common.UTCrafting.sword),
                        recipePage(unboundtech.common.UTCrafting.hoe)))
                .registerResearchItem();

        // --- Электрочернильница: 24 единицы (Cognitio 10, Potentia 8, Instrumentum 6) ---
        new ResearchItem(
                ELECTRIC_SCRIBING, CATEGORY,
                new AspectList().add(Aspect.MIND, 10).add(Aspect.ENERGY, 8)
                        .add(Aspect.TOOL, 6),
                4, -4, 2,
                new ItemStack(unboundtech.common.UTItems.electricScribingTools))
                .setParents(TEMPERED_THAUMIUM)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.ELECTRIC_SCRIBING.1"),
                        new ResearchPage("unboundtech.research_page.ELECTRIC_SCRIBING.2"),
                        recipePage(unboundtech.common.UTCrafting.scribingTools)))
                .registerResearchItem();

        UTLog.info("Tier T2 research registered (tempered thaumium and kin)");
    }

    /**
     * Тир T3. Вызывается ПОСЛЕ {@link unboundtech.common.UTRecipesT3#register()}
     * — страницы держат сами объекты рецептов.
     *
     * Дерево — по канону (`03_progression.md` §3): TEMPERED_THAUMIUM →
     * THAUMIC_OVERCLOCKER → FLUX_CONDENSER (перегрев рождает флюкс, его
     * надо куда-то девать). Стоимости — §6 карточек; complexity 3 у обоих —
     * «всё, что меняет правила игры» (§2.4).
     */
    public static void registerT3() {
        // Оверклокер: Machina 12, Motus 10, Potentia 8, Praecantatio 4 = 34.
        new ResearchItem(
                THAUMIC_OVERCLOCKER, CATEGORY,
                new AspectList().add(Aspect.MECHANISM, 12).add(Aspect.MOTION, 10)
                        .add(Aspect.ENERGY, 8).add(Aspect.MAGIC, 4),
                4, -6, 3,
                new ItemStack(unboundtech.common.UTItems.thaumicOverclocker))
                .setParents(TEMPERED_THAUMIUM)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.THAUMIC_OVERCLOCKER.1"),
                        new ResearchPage("unboundtech.research_page.THAUMIC_OVERCLOCKER.2"),
                        new ResearchPage("unboundtech.research_page.THAUMIC_OVERCLOCKER.3"),
                        unboundtech.common.UTRecipesT3.thaumicOverclocker == null ? null
                                : new ResearchPage(
                                        unboundtech.common.UTRecipesT3.thaumicOverclocker)))
                .registerResearchItem();

        new ResearchItem(
                FLUX_CONDENSER, CATEGORY,
                new AspectList().add(Aspect.MAGIC, 12).add(Aspect.ENERGY, 10)
                        .add(Aspect.ENTROPY, 8).add(Aspect.MECHANISM, 4),
                6, -5, 3,
                new ItemStack(unboundtech.common.UTBlocks.fluxCondenser))
                .setParents(THAUMIC_OVERCLOCKER)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.FLUX_CONDENSER.1"),
                        new ResearchPage("unboundtech.research_page.FLUX_CONDENSER.2"),
                        new ResearchPage("unboundtech.research_page.FLUX_CONDENSER.3"),
                        // Дописка про второй источник мути — перегрев оверклокера.
                        new ResearchPage("unboundtech.research_page.FLUX_CONDENSER.4"),
                        unboundtech.common.UTRecipesT3.fluxCondenser == null ? null
                                : new ResearchPage(unboundtech.common.UTRecipesT3.fluxCondenser)))
                .registerResearchItem();

        // Тигель: Ignis 12, Machina 10, Praecantatio 8, Aqua 4 = 34 (§6).
        new ResearchItem(
                INDUCTION_CRUCIBLE, CATEGORY,
                new AspectList().add(Aspect.FIRE, 12).add(Aspect.MECHANISM, 10)
                        .add(Aspect.MAGIC, 8).add(Aspect.WATER, 4),
                -2, -4, 3,
                new ItemStack(unboundtech.common.UTBlocks.inductionCrucible))
                .setParents(TEMPERED_THAUMIUM)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.INDUCTION_CRUCIBLE.1"),
                        new ResearchPage("unboundtech.research_page.INDUCTION_CRUCIBLE.2"),
                        new ResearchPage("unboundtech.research_page.INDUCTION_CRUCIBLE.3"),
                        unboundtech.common.UTRecipesT3.inductionCrucible == null ? null
                                : new ResearchPage(
                                        unboundtech.common.UTRecipesT3.inductionCrucible)))
                .registerResearchItem();

        // Расщепитель: Permutatio 12, Ordo 10, Machina 8, Praecantatio 4 = 34.
        // Родитель — Индукционный Тигель, как велит канон (§2 карточки:
        // «расщепитель забирает у него составные аспекты»); TODO А-7 закрыт.
        new ResearchItem(
                RESONANT_SPLITTER, CATEGORY,
                new AspectList().add(Aspect.EXCHANGE, 12).add(Aspect.ORDER, 10)
                        .add(Aspect.MECHANISM, 8).add(Aspect.MAGIC, 4),
                0, -6, 3,
                new ItemStack(unboundtech.common.UTBlocks.resonantSplitter))
                .setParents(INDUCTION_CRUCIBLE)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.RESONANT_SPLITTER.1"),
                        new ResearchPage("unboundtech.research_page.RESONANT_SPLITTER.2"),
                        new ResearchPage("unboundtech.research_page.RESONANT_SPLITTER.3"),
                        unboundtech.common.UTRecipesT3.resonantSplitter == null ? null
                                : new ResearchPage(
                                        unboundtech.common.UTRecipesT3.resonantSplitter)))
                .registerResearchItem();

        // Револьвер: Telum 12, Ignis 10, Machina 8 = 30; complexity 2 (§6).
        // Эта же запись выдаёт гильзу и оба патрона T3.
        new ResearchItem(
                FLUX_REVOLVER, CATEGORY,
                new AspectList().add(Aspect.WEAPON, 12).add(Aspect.FIRE, 10)
                        .add(Aspect.MECHANISM, 8),
                8, -6, 2,
                new ItemStack(unboundtech.common.UTItems.fluxRevolver))
                .setParents(FLUX_CONDENSER)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.FLUX_REVOLVER.1"),
                        new ResearchPage("unboundtech.research_page.FLUX_REVOLVER.2"),
                        new ResearchPage("unboundtech.research_page.FLUX_REVOLVER.3"),
                        unboundtech.common.UTRecipesT3.fluxRevolver == null ? null
                                : new ResearchPage(
                                        unboundtech.common.UTRecipesT3.fluxRevolver),
                        recipePage(unboundtech.common.UTCrafting.casingRecipe),
                        recipePage(unboundtech.common.UTCrafting.cartridgeBall),
                        recipePage(unboundtech.common.UTCrafting.cartridgeIncendiary),
                        recipePage(unboundtech.common.UTCrafting.cartridgeIlluminating)))
                .registerResearchItem();

        // Аркебуза: Telum 16, Ignis 12, Machina 10, Praecantatio 8 = 46;
        // complexity 3, родитель — револьвер. Выдаёт вис- и флюкс-патроны.
        new ResearchItem(
                FLUX_ARQUEBUS, CATEGORY,
                new AspectList().add(Aspect.WEAPON, 16).add(Aspect.FIRE, 12)
                        .add(Aspect.MECHANISM, 10).add(Aspect.MAGIC, 8),
                10, -7, 3,
                new ItemStack(unboundtech.common.UTItems.fluxArquebus))
                .setParents(FLUX_REVOLVER)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.FLUX_ARQUEBUS.1"),
                        new ResearchPage("unboundtech.research_page.FLUX_ARQUEBUS.2"),
                        new ResearchPage("unboundtech.research_page.FLUX_ARQUEBUS.3"),
                        unboundtech.common.UTRecipesT3.fluxArquebus == null ? null
                                : new ResearchPage(
                                        unboundtech.common.UTRecipesT3.fluxArquebus),
                        recipePage(unboundtech.common.UTCrafting.cartridgeVis),
                        recipePage(unboundtech.common.UTCrafting.cartridgeFlux)))
                .registerResearchItem();

        // Мортира: Telum 18, Machina 14, Terra 12, Praecantatio 10 = 54.
        new ResearchItem(
                MECHANIST_MORTAR, CATEGORY,
                new AspectList().add(Aspect.WEAPON, 18).add(Aspect.MECHANISM, 14)
                        .add(Aspect.EARTH, 12).add(Aspect.MAGIC, 10),
                12, -6, 3,
                new ItemStack(unboundtech.common.UTBlocks.mechanistMortar))
                .setParents(FLUX_ARQUEBUS)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.MECHANIST_MORTAR.1"),
                        new ResearchPage("unboundtech.research_page.MECHANIST_MORTAR.2"),
                        new ResearchPage("unboundtech.research_page.MECHANIST_MORTAR.3"),
                        unboundtech.common.UTRecipesT3.mechanistMortar == null ? null
                                : new ResearchPage(
                                        unboundtech.common.UTRecipesT3.mechanistMortar)))
                .registerResearchItem();

        // Патронная Линия: Machina 14, Telum 12, Praecantatio 12,
        // Fabrico 10 = 48 (T4), родитель — аркебуза.
        new ResearchItem(
                CARTRIDGE_PRESS, CATEGORY,
                new AspectList().add(Aspect.MECHANISM, 14).add(Aspect.WEAPON, 12)
                        .add(Aspect.MAGIC, 12).add(Aspect.CRAFT, 10),
                12, -8, 3,
                new ItemStack(unboundtech.common.UTBlocks.cartridgeLine))
                .setParents(FLUX_ARQUEBUS)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.CARTRIDGE_PRESS.1"),
                        new ResearchPage("unboundtech.research_page.CARTRIDGE_PRESS.2"),
                        unboundtech.common.UTRecipesT3.cartridgeLine == null
                                ? null : new ResearchPage(
                                        unboundtech.common.UTRecipesT3.cartridgeLine)))
                .registerResearchItem();

        // Вис-Кромка: Telum 14, Perditio 12, Praecantatio 12, Ignis 8
        // = 46 (T4), родитель — аркебуза.
        new ResearchItem(
                VIS_EDGE, CATEGORY,
                new AspectList().add(Aspect.WEAPON, 14).add(Aspect.ENTROPY, 12)
                        .add(Aspect.MAGIC, 12).add(Aspect.FIRE, 8),
                14, -7, 3,
                new ItemStack(unboundtech.common.UTItems.visEdge))
                .setParents(FLUX_ARQUEBUS)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.VIS_EDGE.1"),
                        new ResearchPage("unboundtech.research_page.VIS_EDGE.2"),
                        unboundtech.common.UTRecipesT3.visEdge == null ? null
                                : new ResearchPage(
                                        unboundtech.common.UTRecipesT3.visEdge)))
                .registerResearchItem();

        // ===== T4: порядок В-10 — иридий -> наконечник -> Сингулятор =====

        // Пустотный Иридий: Vacuos 14, Metallum 12, Praecantatio 10,
        // Permutatio 8 = 44 (T4); родители — оба конвертера-материала.
        new ResearchItem(
                VOID_IRIDIUM, CATEGORY,
                new AspectList().add(Aspect.VOID, 14).add(Aspect.METAL, 12)
                        .add(Aspect.MAGIC, 10).add(Aspect.EXCHANGE, 8),
                10, -1, 2,
                new ItemStack(unboundtech.common.UTItems.voidIridium))
                .setParents(EU_TO_VIS_ENGINE, TEMPERED_THAUMIUM)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.VOID_IRIDIUM.1"),
                        new ResearchPage("unboundtech.research_page.VOID_IRIDIUM.2"),
                        unboundtech.common.UTRecipesT3.voidIridium == null ? null
                                : new ResearchPage(
                                        unboundtech.common.UTRecipesT3.voidIridium)))
                .registerResearchItem();

        // Иридиевый наконечник: Praecantatio 16, Ordo 12, Machina 12,
        // Perditio 10 = 50 (T4).
        new ResearchItem(
                IRIDIUM_WAND_CAP, CATEGORY,
                new AspectList().add(Aspect.MAGIC, 16).add(Aspect.ORDER, 12)
                        .add(Aspect.MECHANISM, 12).add(Aspect.ENTROPY, 10),
                12, -2, 2,
                new ItemStack(unboundtech.common.UTItems.iridiumWandCap))
                .setParents(VOID_IRIDIUM)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.IRIDIUM_WAND_CAP.1"),
                        new ResearchPage("unboundtech.research_page.IRIDIUM_WAND_CAP.2"),
                        unboundtech.common.UTRecipesT3.iridiumWandCap == null ? null
                                : new ResearchPage(
                                        unboundtech.common.UTRecipesT3.iridiumWandCap)))
                .registerResearchItem();

        // Сингулятор: Praecantatio 14, Potentia 12, Machina 10, Ordo 10
        // = 46 (T4), complexity 3.
        new ResearchItem(
                SINGULATOR, CATEGORY,
                new AspectList().add(Aspect.MAGIC, 14).add(Aspect.ENERGY, 12)
                        .add(Aspect.MECHANISM, 10).add(Aspect.ORDER, 10),
                12, -4, 3,
                new ItemStack(unboundtech.common.UTBlocks.singulator))
                .setParents(IRIDIUM_WAND_CAP)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.SINGULATOR.1"),
                        new ResearchPage("unboundtech.research_page.SINGULATOR.2"),
                        new ResearchPage("unboundtech.research_page.SINGULATOR.3"),
                        unboundtech.common.UTRecipesT3.singulator == null ? null
                                : new ResearchPage(
                                        unboundtech.common.UTRecipesT3.singulator)))
                .registerResearchItem();

        // Техномаг: лорная запись, объясняющая, почему мод существует
        // (§3.1: если бы школы разговаривали, мост построили бы без игрока).
        new ResearchItem(
                TECHNOMANCER, CATEGORY,
                new AspectList().add(Aspect.MAN, 8).add(Aspect.EXCHANGE, 8)
                        .add(Aspect.MIND, 6),
                8, -2, 1,
                new ItemStack(net.minecraft.init.Items.EMERALD))
                .setParents(INTRO)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.TECHNOMANCER.1"),
                        new ResearchPage("unboundtech.research_page.TECHNOMANCER.2")))
                .registerResearchItem();

        // Техно-духи: Bestia 8, Potentia 8, Praecantatio 6 = 22; лорная
        // запись complexity 1 — ниже вилки T3 намеренно (§6: исключение
        // §2.2, запись без предмета меряется на тир ниже).
        new ResearchItem(
                TECHNO_SPIRITS, CATEGORY,
                new AspectList().add(Aspect.BEAST, 8).add(Aspect.ENERGY, 8)
                        .add(Aspect.MAGIC, 6),
                4, -8, 1,
                new ItemStack(unboundtech.common.UTItems.chargedSpark))
                .setParents(VIS_TO_EU_GENERATOR)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.TECHNO_SPIRITS.1"),
                        new ResearchPage("unboundtech.research_page.TECHNO_SPIRITS.2"),
                        new ResearchPage("unboundtech.research_page.TECHNO_SPIRITS.3")))
                .registerResearchItem();

        // Фокус Заряда: Praecantatio 12, Potentia 12, Machina 8 = 32 (§6).
        new ResearchItem(
                FOCUS_CHARGE, CATEGORY,
                new AspectList().add(Aspect.MAGIC, 12).add(Aspect.ENERGY, 12)
                        .add(Aspect.MECHANISM, 8),
                8, -4, 2,
                new ItemStack(unboundtech.common.UTItems.focusCharge))
                .setParents(VIS_TO_EU_GENERATOR)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.FOCUS_CHARGE.1"),
                        new ResearchPage("unboundtech.research_page.FOCUS_CHARGE.2"),
                        unboundtech.common.UTRecipesT3.focusCharge == null ? null
                                : new ResearchPage(
                                        unboundtech.common.UTRecipesT3.focusCharge)))
                .registerResearchItem();

        // Кольца: Corpus 12, Machina 12, Praecantatio 10 = 34; complexity 2.
        // Одна запись выдаёт все четыре кольца (§6).
        new ResearchItem(
                SCHEMA_RINGS, CATEGORY,
                new AspectList().add(Aspect.FLESH, 12).add(Aspect.MECHANISM, 12)
                        .add(Aspect.MAGIC, 10),
                6, -7, 2,
                new ItemStack(unboundtech.common.UTItems.ringFrame))
                .setParents(EU_TO_VIS_ENGINE)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.SCHEMA_RINGS.1"),
                        new ResearchPage("unboundtech.research_page.SCHEMA_RINGS.2"),
                        new ResearchPage("unboundtech.research_page.SCHEMA_RINGS.3"),
                        unboundtech.common.UTRecipesT3.ringFrame == null ? null
                                : new ResearchPage(unboundtech.common.UTRecipesT3.ringFrame),
                        unboundtech.common.UTRecipesT3.ringDrive == null ? null
                                : new ResearchPage(unboundtech.common.UTRecipesT3.ringDrive),
                        unboundtech.common.UTRecipesT3.ringStride == null ? null
                                : new ResearchPage(unboundtech.common.UTRecipesT3.ringStride),
                        unboundtech.common.UTRecipesT3.ringBrace == null ? null
                                : new ResearchPage(unboundtech.common.UTRecipesT3.ringBrace)))
                .registerResearchItem();

        // Шина: Permutatio 10, Machina 10, Ordo 8, Praecantatio 4 = 32 (§6).
        new ResearchItem(
                ESSENTIA_BUS, CATEGORY,
                new AspectList().add(Aspect.EXCHANGE, 10).add(Aspect.MECHANISM, 10)
                        .add(Aspect.ORDER, 8).add(Aspect.MAGIC, 4),
                -2, -7, 3,
                new ItemStack(unboundtech.common.UTBlocks.busNode))
                .setParents(TEMPERED_THAUMIUM)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.ESSENTIA_BUS.1"),
                        new ResearchPage("unboundtech.research_page.ESSENTIA_BUS.2"),
                        new ResearchPage("unboundtech.research_page.ESSENTIA_BUS.3"),
                        unboundtech.common.UTRecipesT3.busNode == null ? null
                                : new ResearchPage(unboundtech.common.UTRecipesT3.busNode),
                        unboundtech.common.UTRecipesT3.conduitI == null ? null
                                : new ResearchPage(unboundtech.common.UTRecipesT3.conduitI)))
                .registerResearchItem();

        // Накопитель: Vacuos 12, Ordo 10, Machina 8, Praecantatio 4 = 34 (§6).
        new ResearchItem(
                ESSENTIA_VAULT, CATEGORY,
                new AspectList().add(Aspect.VOID, 12).add(Aspect.ORDER, 10)
                        .add(Aspect.MECHANISM, 8).add(Aspect.MAGIC, 4),
                -4, -6, 3,
                new ItemStack(unboundtech.common.UTBlocks.vaultController))
                .setParents(ESSENTIA_BUS)
                .setPages(pages(
                        new ResearchPage("unboundtech.research_page.ESSENTIA_VAULT.1"),
                        new ResearchPage("unboundtech.research_page.ESSENTIA_VAULT.2"),
                        new ResearchPage("unboundtech.research_page.ESSENTIA_VAULT.3"),
                        unboundtech.common.UTRecipesT3.vaultController == null ? null
                                : new ResearchPage(
                                        unboundtech.common.UTRecipesT3.vaultController),
                        unboundtech.common.UTRecipesT3.vaultGolemPort == null ? null
                                : new ResearchPage(
                                        unboundtech.common.UTRecipesT3.vaultGolemPort)))
                .registerResearchItem();

        UTLog.info("Tier T3 research registered (overclocker, condenser, splitter,"
                + " crucible, bus, vault)");
    }


    /**
     * Страницы записи о материале: лор, затем ОБА пути (§6 карточки — игрок
     * сразу видит, что путей два), затем ключ, который идёт в комплекте.
     *
     * ⚠️ Путь А показан текстом, а не страницей рецепта: рецепт доменной печи
     * живёт в реестре IC2, а {@code ResearchPage} умеет только рецепты ТК и
     * ванильный {@code IRecipe}. Страницы «рецепт машины IC2» в API нет.
     */
    private static ResearchPage[] temperedMaterialPages() {
        return pages(
                new ResearchPage("unboundtech.research_page.TEMPERED_THAUMIUM.1"),
                new ResearchPage("unboundtech.research_page.TEMPERED_THAUMIUM.2"),
                new ResearchPage("unboundtech.research_page.TEMPERED_THAUMIUM.3"),
                unboundtech.common.UTRecipesT2.temperedCrucible == null
                        ? null
                        : new ResearchPage(unboundtech.common.UTRecipesT2.temperedCrucible),
                recipePage(unboundtech.common.UTCrafting.wrench));
    }

    private static ResearchPage recipePage(net.minecraft.item.crafting.IRecipe recipe) {
        return recipe == null ? null : new ResearchPage(recipe);
    }

    /** Выбрасывает страницы, которых нет: пустую страницу показывать нельзя. */
    private static ResearchPage[] pages(ResearchPage... candidates) {
        java.util.List<ResearchPage> kept = new java.util.ArrayList<>();
        for (ResearchPage page : candidates) {
            if (page != null) {
                kept.add(page);
            }
        }
        return kept.toArray(new ResearchPage[0]);
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
