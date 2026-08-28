package unboundtech.common.entities;

import java.util.Random;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.common.registry.VillagerRegistry;
import thaumcraft.common.config.ConfigItems;
import unboundtech.UnboundTech;
import unboundtech.common.UTItems;
import unboundtech.compat.ic2.IC2Handles;

/**
 * Житель-Техномаг (`05_objects/technomancer_villager.md`): «из башни его
 * выставили за то, что он мерил вис амперметром; из мастерской — за то,
 * что при нём машина заговорила». Третья профессия ТЕМ ЖЕ механизмом,
 * что Волшебник и Меняла порта, — чужие списки не трогаем (§2).
 *
 * В обычных деревнях НЕ появляется (§3.1: изгнанник) — только в Хижине
 * Изгнанника ({@link unboundtech.common.world.WorldGenExileHut}).
 *
 * Сделки §5; Искры он ПОКУПАЕТ и никогда не продаёт — иначе сломался бы
 * гейт арены T5 (§5: «купить Искры нельзя — только продать»).
 */
public final class UTVillagers {

    public static VillagerRegistry.VillagerProfession technomancer;

    private UTVillagers() {
    }

    public static void register() {
        technomancer = new VillagerRegistry.VillagerProfession(
                UnboundTech.MODID + ":technomancer",
                UnboundTech.MODID + ":textures/entity/technomancer.png",
                "minecraft:textures/entity/zombie_villager/zombie_villager.png");
        net.minecraftforge.fml.common.registry.ForgeRegistries.VILLAGER_PROFESSIONS
                .register(technomancer);
        VillagerRegistry.VillagerCareer career =
                new VillagerRegistry.VillagerCareer(technomancer, "technomancer");

        ItemStack copper = IC2Handles.item("ingot", "copper");
        ItemStack overclocker = IC2Handles.item("upgrade", "overclocker");

        // §5, уровень 1: фиалы за изумруд; медь — за изумруд.
        addTrades(career, 1,
                sell(new ItemStack(ConfigItems.itemEssence, 1, 0), 4, 6, 1),
                copper.isEmpty() ? null : buy(copper, 8, 10, 1));
        // Уровень 2: нитор; покупает Заряженные Искры (слив лишних, §5).
        addTrades(career, 2,
                sell(new ItemStack(ConfigItems.itemResource, 1, 1), 1, 1, 3, 5),
                new Trade(false, new ItemStack(UTItems.chargedSpark), 1, 1, 2, 3));
        // Уровень 3: осколок аспекта; берёт закалённый таумий.
        addTrades(career, 3,
                sell(new ItemStack(ConfigItems.itemShard, 1, 32767), 1, 1, 2, 4),
                buy(new ItemStack(UTItems.temperedIngot), 6, 8, 1));
        // Уровень 4: оверклокер IC2; берёт алюментум.
        addTrades(career, 4,
                overclocker.isEmpty() ? null : sell(overclocker, 1, 1, 8, 12),
                buyForMany(new ItemStack(ConfigItems.itemResource, 1, 0), 1, 2));
        // Уровень 5: вис-кристалл за большие изумруды.
        addTrades(career, 5,
                sell(new ItemStack(ConfigItems.itemShard, 1, 32767), 1, 1, 12, 16));
    }

    /** addTrade без null-дыр: отсутствующая половина сделки просто выпадает. */
    private static void addTrades(VillagerRegistry.VillagerCareer career,
            int level, EntityVillager.ITradeList... trades) {
        for (EntityVillager.ITradeList trade : trades) {
            if (trade != null) {
                career.addTrade(level, trade);
            }
        }
    }

    /** Продажа: N–M предметов за 1 изумруд. */
    private static EntityVillager.ITradeList sell(ItemStack what,
            int countMin, int countMax, int emeralds) {
        return sell(what, countMin, countMax, emeralds, emeralds);
    }

    /** Продажа: N–M предметов за E1–E2 изумрудов. */
    private static EntityVillager.ITradeList sell(ItemStack what,
            int countMin, int countMax, int emMin, int emMax) {
        return new Trade(true, what, countMin, countMax, emMin, emMax);
    }

    /** Покупка: житель берёт N–M предметов, платит 1 изумруд. */
    private static EntityVillager.ITradeList buy(ItemStack what,
            int countMin, int countMax, int emeralds) {
        return new Trade(false, what, countMin, countMax, emeralds, emeralds);
    }

    /** Покупка: житель берёт 1 предмет, платит E1–E2 изумрудов. */
    private static EntityVillager.ITradeList buyForMany(ItemStack what,
            int emMin, int emMax) {
        return new Trade(false, what, 1, 1, emMin, emMax);
    }

    private static final class Trade implements EntityVillager.ITradeList {

        private final boolean selling;
        private final ItemStack what;
        private final int countMin;
        private final int countMax;
        private final int emMin;
        private final int emMax;

        Trade(boolean selling, ItemStack what, int countMin, int countMax,
              int emMin, int emMax) {
            this.selling = selling;
            this.what = what.copy();
            this.countMin = countMin;
            this.countMax = countMax;
            this.emMin = emMin;
            this.emMax = emMax;
        }

        @Override
        public void addMerchantRecipe(IMerchant merchant,
                MerchantRecipeList recipes, Random rand) {
            int count = this.countMin
                    + rand.nextInt(this.countMax - this.countMin + 1);
            int emeralds = this.emMin
                    + rand.nextInt(this.emMax - this.emMin + 1);
            ItemStack goods = this.what.copy();
            if (goods.getItemDamage() == 32767) {
                goods.setItemDamage(rand.nextInt(6));   // случайный кристалл
            }
            goods.setCount(count);
            ItemStack gems = new ItemStack(Items.EMERALD, emeralds);
            if (this.selling) {
                recipes.add(new MerchantRecipe(gems, goods));
            } else {
                recipes.add(new MerchantRecipe(goods, gems));
            }
        }
    }

    /** Хижина зовёт этого жителя в мир. */
    public static EntityVillager spawn(net.minecraft.world.World world,
            net.minecraft.util.math.BlockPos at) {
        EntityVillager villager = new EntityVillager(world);
        villager.setProfession(technomancer);
        villager.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        world.spawnEntity(villager);
        return villager;
    }
}
