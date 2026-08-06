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
                source.contains("EU_PER_AURA_SELL = 2_000;"));
        assertTrue("EU → аура должен стоить 8,000",
                source.contains("EU_PER_AURA_BUY = 8_000;"));
        assertTrue("вис в жезл должен стоить 20,000",
                source.contains("EU_PER_VIS = 20_000;"));
        // Второй закон: обратный курс минимум вчетверо дороже прямого.
        assertTrue("проверка второго закона должна остаться в коде",
                source.contains("EU_PER_AURA_BUY < 4 * EU_PER_AURA_SELL"));
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
                source.contains("addEnergy(EnergyCanon.EU_PER_AURA_SELL)"));
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
                source.contains("useEnergy(EnergyCanon.EU_PER_AURA_BUY)"));
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

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
