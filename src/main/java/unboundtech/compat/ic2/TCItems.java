package unboundtech.compat.ic2;

import net.minecraft.item.ItemStack;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.ItemResource;

/**
 * Стеки предметов TC4 Unbound, нужные рецептам IC2-машин.
 *
 * Используем внутренние классы порта (не api) сознательно: сабмод собирается
 * против dev-jar порта и версионируется вместе с ним. Меты — только через
 * константы ItemResource, чтобы переезд мет в порте ломал компиляцию,
 * а не тихо портил рецепты.
 */
final class TCItems {

    private TCItems() {
    }

    static ItemStack quicksilver(int count) {
        return new ItemStack(ConfigItems.itemResource, count, ItemResource.META_QUICKSILVER);
    }

    static ItemStack amber(int count) {
        return new ItemStack(ConfigItems.itemResource, count, ItemResource.META_AMBER);
    }

    /** @param shardMeta 0..5 = воздух, огонь, вода, земля, порядок, энтропия. */
    static ItemStack shard(int shardMeta, int count) {
        return new ItemStack(ConfigItems.itemShard, count, shardMeta);
    }

    static ItemStack taintSlime() {
        return new ItemStack(ConfigItems.itemResource, 1, ItemResource.META_TAINT_SLIME);
    }

    static ItemStack taintTendril() {
        return new ItemStack(ConfigItems.itemResource, 1, ItemResource.META_TAINT_TENDRIL);
    }

    /**
     * Кристалл-кластер аспекта (blockCrystal): меты 0..5 совпадают с метами
     * осколков (см. рецепт "clusters" в ConfigRecipesSpecialSlice порта:
     * 6 осколков меты a → кластер меты a; мета 6 — сбалансированный).
     */
    static ItemStack crystalCluster(int aspectMeta, int count) {
        return new ItemStack(ConfigBlocks.blockCrystal, count, aspectMeta);
    }

    /** Янтарный блок = blockCosmeticOpaque:0 (в крафте порта — 4 янтаря 2×2). */
    static ItemStack amberBlock(int count) {
        return new ItemStack(ConfigBlocks.blockCosmeticOpaque, count, 0);
    }

    /** Серебролист = blockCustomPlant:2 (plantTypes: shimmerleaf). */
    static ItemStack shimmerleaf() {
        return new ItemStack(ConfigBlocks.blockCustomPlant, 1, 2);
    }
}
