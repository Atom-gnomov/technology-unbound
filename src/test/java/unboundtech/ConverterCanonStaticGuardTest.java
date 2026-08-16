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
     * Арканные рецепты платят висом из жезла, а жезл хранит только шесть
     * прималов (канон §6.8). Составной аспект в стоимости — ошибка: заплатить
     * за него нечем, и заметить это можно только в игре.
     */
    @Test
    public void arcaneRecipesCostPrimalsOnly() throws IOException {
        String[] sources = {
                "src/main/java/unboundtech/common/UTRecipes.java",
                "src/main/java/unboundtech/common/UTRecipesT2.java",
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
