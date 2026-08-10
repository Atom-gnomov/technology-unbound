package unboundtech.compat.ic2;

import ic2.api.recipe.Recipes;
import net.minecraft.item.ItemStack;
import unboundtech.UTLog;

/**
 * Рецепты машин IC2 для материалов ТК (фаза 1, канон:
 * docs/integration/ic2_v3_machines.md §2 в репо порта).
 *
 * Дробитель:
 *  - киноварь → 2× ртуть (как плавка, но с удвоением);
 *  - янтарная руда → 2× янтарь;
 *  - зачарованные руды (6 стихий) → 2× осколка соответствующего аспекта;
 *  - кристалл-кластер аспекта → 4 осколка (переработка с потерей: крафт
 *    кластера в порте стоит 6 осколков).
 * Компрессор:
 *  - 6× осколков аспекта → кристалл-кластер (курс крафта порта, автоматизация);
 *  - 4× янтаря → янтарный блок (курс крафта порта 2×2).
 * Экстрактор:
 *  - таинт-слизь / таинт-тендрил → резина («полимеризация скверны»);
 *  - серебролист → 2× живичной смолы.
 *
 * Серебряной руды здесь НЕТ намеренно: IC2 обрабатывает её сам своим же
 * конфигом (assets/ic2/config/macerator.ini: `OreDict:oreSilver =
 * ic2:crushed#silver*2`), и у него есть вся цепочка — crushed → purified →
 * dust → ingot, вплоть до термоцентрифуги. Свой рецепт `oreSilver → dust`
 * либо не встал бы (addRecipe с overwrite=false), либо увёл бы руду мимо
 * промывателя, оставив серебро единственной рудой пака без обогащения.
 * Подробности: docs/design/04_systems/T13_crosscheck.md П-1 в репо порта.
 *
 * Входы задаём через оредикт, где он есть (oreCinnabar/oreAmber,
 * oreInfused*) — зарегистрированы портом в ConfigItems.registerOreDictionary.
 * Выходы — предметы порта, берём через оредикт-имена нельзя (нужны точные
 * меты), поэтому используем стеки из чужого реестра через GameRegistry.
 *
 * Алюментум-топливо отдельного кода не требует: порт 1.2.8.0 вернул ему
 * furnace burn time 6400 тиков, генератор IC2 принимает его сам (~16k EU).
 */
public final class IC2Recipes {

    private IC2Recipes() {
    }

    public static void register() {
        int added = 0;

        // --- Дробитель (macerator) ---
        added += macerate("oreCinnabar", TCItems.quicksilver(2));
        added += macerate("oreAmber", TCItems.amber(2));
        String[] infusedOres = {"oreInfusedAir", "oreInfusedFire", "oreInfusedWater",
                "oreInfusedEarth", "oreInfusedOrder", "oreInfusedEntropy"};
        for (int i = 0; i < infusedOres.length; i++) {
            added += macerate(infusedOres[i], TCItems.shard(i, 2));
        }
        for (int i = 0; i < 6; i++) {
            added += macerateStack(TCItems.crystalCluster(i, 1), TCItems.shard(i, 4));
        }

        // --- Компрессор (compressor) ---
        for (int i = 0; i < 6; i++) {
            added += compress(TCItems.shard(i, 6), TCItems.crystalCluster(i, 1));
        }
        added += compress(TCItems.amber(4), TCItems.amberBlock(1));

        // --- Экстрактор (extractor) ---
        ItemStack rubber = IC2Handles.item("crafting", "rubber");
        added += extract(TCItems.taintSlime(), IC2Handles.withCount(rubber, 1));
        added += extract(TCItems.taintTendril(), IC2Handles.withCount(rubber, 1));
        added += extract(TCItems.shimmerleaf(), IC2Handles.withCount(
                IC2Handles.item("misc_resource", "resin"), 2));

        UTLog.info("IC2 machine recipes registered: {}", added);
    }

    private static int macerate(String oreDictInput, ItemStack output) {
        if (output.isEmpty()) {
            UTLog.warn("Macerator recipe for {} skipped: empty output", oreDictInput);
            return 0;
        }
        Recipes.macerator.addRecipe(
                Recipes.inputFactory.forOreDict(oreDictInput), null, false, output);
        return 1;
    }

    /** Дробитель со стек-входом (размер входного стека = требуемое количество). */
    private static int macerateStack(ItemStack input, ItemStack output) {
        if (input.isEmpty() || output.isEmpty()) {
            UTLog.warn("Macerator (stack) recipe skipped: empty input or output");
            return 0;
        }
        Recipes.macerator.addRecipe(
                Recipes.inputFactory.forStack(input), null, false, output);
        return 1;
    }

    private static int compress(ItemStack input, ItemStack output) {
        if (input.isEmpty() || output.isEmpty()) {
            UTLog.warn("Compressor recipe skipped: empty input or output");
            return 0;
        }
        Recipes.compressor.addRecipe(
                Recipes.inputFactory.forStack(input), null, false, output);
        return 1;
    }

    private static int extract(ItemStack input, ItemStack output) {
        if (input.isEmpty() || output.isEmpty()) {
            UTLog.warn("Extractor recipe skipped: empty input or output");
            return 0;
        }
        Recipes.extractor.addRecipe(
                Recipes.inputFactory.forStack(input), null, false, output);
        return 1;
    }
}
