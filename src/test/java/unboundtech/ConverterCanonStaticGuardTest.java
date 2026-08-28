package unboundtech;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Канон энергии и балансные константы конвертеров — статический гвард
 * (идиома порта TC4U: пиним значения в исходнике, а не гоняем Minecraft).
 *
 * Курсы EU задаёт ревизия v5 (`ic2_v5_decisions.md` §2), параметры машин —
 * `phase3_converters_spec.md` §1–2. Молча разъехавшиеся числа ломают
 * «второй закон таумодинамики», и заметить это в игре почти невозможно —
 * поэтому они прибиты здесь.
 */
public class ConverterCanonStaticGuardTest {

    @Test
    public void energyCanonKeepsV5Rates() throws IOException {
        String source = read("src/main/java/unboundtech/energy/EnergyCanon.java");

        assertTrue("аура → EU должен стоить 2,000",
                source.contains("EU_PER_NODE_ASPECT_SELL = 2_000;"));
        assertTrue("EU → аура должен стоить 8,000",
                source.contains("EU_PER_NODE_ASPECT_BUY = 8_000;"));
        // Канон §3.1 снизил курс Сингулятора вдвое: было 20 000.
        assertTrue("вис в жезл должен стоить 10,000",
                source.contains("EU_PER_VIS = 10_000;"));
        assertTrue("обратный курс Фокуса Заряда — 1,000",
                source.contains("EU_PER_WAND_VIS_BACK = 1_000;"));
        assertTrue("курс отменённого массфабрикатора должен быть удалён",
                !source.contains("EU_PERMUTATIO_AMPLIFIER"));
        // Второй закон: обратный курс минимум вчетверо дороже прямого.
        assertTrue("проверка второго закона должна остаться в коде",
                source.contains("EU_PER_NODE_ASPECT_BUY < 4 * EU_PER_NODE_ASPECT_SELL"));
    }

    @Test
    public void thaumGeneratorKeepsSpecLimits() throws IOException {
        String source = read(
                "src/main/java/unboundtech/common/tiles/TileThaumGenerator.java");

        assertTrue("буфер 20,000 EU", source.contains("CAPACITY = 20_000.0;"));
        assertTrue("тир LV", source.contains("TIER = 1;"));
        assertTrue("такт добычи — 20 тиков",
                source.contains("DRAIN_INTERVAL = 20;"));
        assertTrue("пол узла — 20% по-аспектной ёмкости",
                source.contains("NODE_FLOOR = 0.2;"));
        assertTrue("пол считается от getNodeVisBase, а не от текущего виса",
                source.contains("Math.ceil(node.getNodeVisBase(aspect) * NODE_FLOOR)"));
        assertTrue("генератор жжёт только Ignis и Potentia",
                source.contains("FUEL_ASPECTS = {Aspect.FIRE, Aspect.ENERGY}"));
        assertTrue("интерференция — 16 блоков",
                source.contains("INTERFERENCE_RADIUS = 16;"));
        assertTrue("вис обращается в EU по канону",
                source.contains("addEnergy(EnergyCanon.EU_PER_NODE_ASPECT_SELL)"));
        assertTrue("после изменения узла обязателен синк",
                source.contains("NodeCache.syncNode(this.world, nodePos)"));
    }

    @Test
    public void aethericEngineKeepsSpecLimits() throws IOException {
        String source = read(
                "src/main/java/unboundtech/common/tiles/TileAethericEngine.java");

        assertTrue("буфер 40,000 EU", source.contains("CAPACITY = 40_000.0;"));
        assertTrue("тир MV", source.contains("TIER = 2;"));
        assertTrue("такт работы — 20 тиков", source.contains("WORK_INTERVAL = 20;"));
        assertTrue("вис покупается по канону",
                source.contains("useEnergy(EnergyCanon.EU_PER_NODE_ASPECT_BUY)"));
        assertTrue("EU списываются только если вис реально влез",
                source.contains("node.addToContainer(aspect, 1) != 0"));
        assertTrue("после изменения узла обязателен синк",
                source.contains("NodeCache.syncNode(this.world, nodePos)"));
        assertTrue("ёмкость узла (aspectsBase) двигатель не трогает",
                !source.contains("setNodeVisBase"));
    }

    @Test
    public void machineStateSwitchDoesNotReAddTileEntity() throws IOException {
        String source = read(
                "src/main/java/unboundtech/common/blocks/BlockMachineBase.java");

        // Пляска ванильной печи (validate + setTileEntity) нужна только когда
        // меняется САМ блок; у нас меняется свойство, и повторный setTileEntity
        // во время тика заставил бы машину тикать дважды.
        assertTrue("смена ACTIVE — один setBlockState",
                source.contains("world.setBlockState(pos, state.withProperty(ACTIVE, active), 3);"));
        assertTrue("тайл не переустанавливается вручную",
                !source.contains("world.setTileEntity(pos, tile)"));
    }

    /**
     * Тир T2. Числа взяты из карточек `05_objects/tempered_thaumium*.md` и
     * `electric_scribing_tools.md`; разъехавшись, они ломают прогрессию тихо.
     */
    @Test
    public void temperedTierKeepsCardNumbers() throws IOException {
        String items = read("src/main/java/unboundtech/common/UTItems.java");

        assertTrue("инструмент — алмаз с прочностью x1.2 (3/1873/8.0/3.0/10)",
                items.contains("\"TEMPERED_THAUMIUM\", 3, 1873, 8.0F, 3.0F, 10"));
        // Массив брони индексируется слотом: ботинки, поножи, нагрудник, шлем.
        // Канон задаёт шлем 4, нагрудник 10, поножи 7, ботинки 3; сумма 24.
        assertTrue("броня 3/7/10/4, прочность 30, зачаровываемость 6, стойкость 1.0",
                items.contains("new int[]{3, 7, 10, 4}, 6, SoundEvents.ITEM_ARMOR_EQUIP_IRON, 1.0F"));
        assertTrue("оредикт материала", items.contains("ORE_INGOT = \"ingotTemperedThaumium\""));

        String material = read("src/main/java/unboundtech/common/UTRecipesT2.java");
        assertTrue("домна: как обычная сталь, 6 000 тиков",
                material.contains("BLAST_DURATION = 6_000;"));
        assertTrue("домна: 1 мБ воздуха ЗА ТИК, как у стали",
                material.contains("BLAST_FLUID = 1;"));
        assertTrue("тигель: Praecantatio 4",
                material.contains("add(Aspect.MAGIC, 4)"));
        assertTrue("тигель: стабилизаторы Permutatio 2 и Vitreus 2",
                material.contains("add(Aspect.EXCHANGE, 2)")
                        && material.contains("add(Aspect.CRYSTAL, 2)"));

        String scribing = read(
                "src/main/java/unboundtech/common/items/ItemElectricScribingTools.java");
        assertTrue("чернильница: 4 000 EU", scribing.contains("CAPACITY = 4_000.0;"));
        assertTrue("чернильница: 50 EU за очко", scribing.contains("EU_PER_POINT = 50.0;"));

        String armor = read(
                "src/main/java/unboundtech/common/items/ItemTemperedArmor.java");
        assertTrue("штраф вис -5% за элемент",
                armor.contains("VIS_PENALTY_PER_PIECE = -5;"));
    }

    /**
     * Тир T3: Флюкс-Конденсатор и Таум-Оверклокер — числа карточек.
     */
    @Test
    public void tier3KeepsCardNumbers() throws IOException {
        String canon = read("src/main/java/unboundtech/energy/EnergyCanon.java");
        assertTrue("курс конденсатора — 2 000 EU за Praecantatio",
                canon.contains("EU_PER_FLUX_ESSENTIA = 2_000;"));

        String condenser = read("src/main/java/unboundtech/common/tiles/TileFluxCondenser.java");
        assertTrue("конденсатор: буфер 10 000 EU", condenser.contains("CAPACITY = 10_000.0;"));
        assertTrue("конденсатор: тир LV", condenser.contains("TIER = 1;"));
        assertTrue("конденсатор: буфер эссенции 8", condenser.contains("ESSENTIA_BUFFER = 8;"));
        assertTrue("сгущение: 4 Praecantatio за Флюкс-Заряд",
                condenser.contains("THICKEN_ESSENTIA = 4;"));
        assertTrue("конденсатор принимает только Praecantatio",
                condenser.contains("return Aspect.MAGIC;"));

        String rules = read("src/main/java/unboundtech/energy/OverclockRules.java");
        assertTrue("оверклокер: время ×0.6", rules.contains("PROCESS_TIME_MULTIPLIER = 0.6D;"));
        assertTrue("оверклокер: потребление ×1.2",
                rules.contains("ENERGY_DEMAND_MULTIPLIER = 1.2D;"));
        assertTrue("оверклокер: пулы по 32", rules.contains("CHARGE_CAPACITY = 32;"));
        assertTrue("оверклокер: 600 тиков на единицу",
                rules.contains("TICKS_PER_ESSENTIA_UNIT = 600;"));
        assertTrue("оверклокер: порог перегрева 6 000",
                rules.contains("HEAT_THRESHOLD = 6_000;"));

        String upgrade = read(
                "src/main/java/unboundtech/common/items/ItemThaumicOverclocker.java");
        assertTrue("стопка запрещена: Math.pow(mult, stackCount) у IC2",
                upgrade.contains("setMaxStackSize(1)"));

        assertTrue("расщепление — 6 000 EU за операцию",
                canon.contains("EU_PER_SPLIT = 6_000;"));
        String splitter = read(
                "src/main/java/unboundtech/common/tiles/TileResonantSplitter.java");
        assertTrue("расщепитель: темп родной центрифуги — 39 тиков",
                splitter.contains("CYCLE_TICKS = 39;"));
        assertTrue("расщепитель: буферы по 8", splitter.contains("BUFFER = 8;"));
        assertTrue("примордиалы не принимаются",
                splitter.contains("aspect.isPrimal()"));

        // Индукционный Тигель (`induction_crucible.md` §5): числа карточки
        // и снятые наказания родного тигля.
        String crucible = read(
                "src/main/java/unboundtech/common/tiles/TileInductionCrucible.java");
        assertTrue("тигель: буфер 10 000 EU", crucible.contains("CAPACITY = 10_000.0;"));
        assertTrue("тигель: 20 EU/t пока горячий", crucible.contains("EU_PER_TICK = 20;"));
        assertTrue("тигель: 100 аспектов, как у родного",
                crucible.contains("MAX_TAGS = 100;"));
        assertTrue("тигель: бак 1 000 мБ", crucible.contains("TANK_CAPACITY = 1000;"));
        assertTrue("тигель: 50 мБ на рецепт", crucible.contains("WATER_PER_CRAFT = 50;"));
        assertTrue("тигель: рецепты — общий реестр ТК, своих нет",
                crucible.contains("findMatchingCrucibleRecipe"));
        // Ищем именно ВЫЗОВ (".spill("), а не слово: комментарии тайла
        // легитимно поминают spill() родного тигля, объясняя отличие.
        assertTrue("главное обещание: никакого spill() — флюкса нет",
                !crucible.contains(".spill("));
    }

    /**
     * Арканные рецепты платят висом из жезла, а жезл хранит только шесть
     * прималов (канон §6.8). Составной аспект в стоимости — ошибка: заплатить
     * за него нечем, и заметить это можно только в игре.
     */
    @Test
    public void arcaneRecipesCostPrimalsOnly() throws IOException {
        String[] sources = {
                "src/main/java/unboundtech/common/UTRecipes.java",
                "src/main/java/unboundtech/common/UTRecipesT2.java",
                "src/main/java/unboundtech/common/UTRecipesT3.java",
        };
        String[] primalsOnly = {"Aspect.AIR", "Aspect.EARTH", "Aspect.FIRE",
                "Aspect.WATER", "Aspect.ORDER", "Aspect.ENTROPY"};
        for (String path : sources) {
            for (String line : read(path).split("\n")) {
                if (!line.contains("addArcaneCraftingRecipe") && !line.contains("new AspectList()")
                        && !line.trim().startsWith(".add(Aspect.")) {
                    continue;
                }
                if (!line.contains(".add(Aspect.")) {
                    continue;
                }
                boolean primal = false;
                for (String allowed : primalsOnly) {
                    if (line.contains(allowed + ",")) {
                        primal = true;
                        break;
                    }
                }
                // Строки тигля (Praecantatio и стабилизаторы) сюда не относятся:
                // тигель платит эссенцией, а не висом из жезла.
                boolean crucible = line.contains("Aspect.MAGIC") || line.contains("Aspect.EXCHANGE")
                        || line.contains("Aspect.CRYSTAL");
                assertTrue("стоимость арканного рецепта обязана быть в прималах: " + line.trim(),
                        primal || crucible);
            }
        }
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
