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

        UTLog.info("Research tab {} registered ({} entries)", CATEGORY, 5);
    }
}
