package unboundtech.common;

import ic2.api.recipe.Recipes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.ItemResource;
import unboundtech.UTLog;
import unboundtech.research.UTResearch;

/**
 * Два пути закалённого таумия (`05_objects/tempered_thaumium.md` §4).
 * Оба дают один и тот же предмет — выбор за игроком, с какой стороны он
 * пришёл, и материал физически нельзя получить, не тронув оба мода.
 *
 * <b>Путь А (магический):</b> таумий в доменной печи IC2. Ровно как обычная
 * сталь — {@code @fluid:1 @duration:6000}; базовый рецепт снят с
 * {@code assets/ic2/config/blast_furnace.ini} самого IC2 2.8.222:
 * {@code minecraft:iron_ingot = ic2:ingot#steel ic2:misc_resource#slag
 * @fluid:1 @duration:6000}.
 *
 * ⚠️ {@code fluid} тратится ЗА ТИК, а не за операцию: печь каждый тик
 * увеличивает прогресс и сливает {@code fluid} мБ. Суммарно 6 000 мБ
 * воздуха на слиток, как у стали. Прежняя редакция карточки просила
 * {@code duration 12 000, fluid 2} — это дало бы 24 000 мБ, вчетверо против
 * стали и три полных бака на слиток.
 *
 * <b>Путь Б (технический):</b> сталь IC2 в тигле + {@code Praecantatio 4}
 * и стабилизаторы {@code Permutatio 2}, {@code Vitreus 2}. Сталь уже
 * «мертва», магию в неё не вливают, а вплавляют.
 */
public final class UTRecipesT2 {

    /** Путь А: тики работы домны — как у стали. */
    private static final int BLAST_DURATION = 6_000;
    /** Путь А: сжатый воздух, мБ ЗА ТИК — как у стали. */
    private static final int BLAST_FLUID = 1;

    /** Рецепт тигля — страница Таумономикона показывает сам объект. */
    public static CrucibleRecipe temperedCrucible;

    private UTRecipesT2() {
    }

    public static void register() {
        registerBlastFurnace();
        temperedCrucible = registerCrucible();
    }


    /** Путь А: таумий → закалённый таумий в доменной печи IC2. */
    private static void registerBlastFurnace() {
        ItemStack thaumium = new ItemStack(ConfigItems.itemResource, 1,
                ItemResource.META_THAUMIUM_INGOT);
        NBTTagCompound meta = new NBTTagCompound();
        meta.setInteger("fluid", BLAST_FLUID);
        meta.setInteger("duration", BLAST_DURATION);
        // Шлака не даём: карточка задаёт единственный выход. У стали шлак
        // свой, наш передел канон побочным продуктом не наделяет.
        boolean added = Recipes.blastfurnace.addRecipe(
                Recipes.inputFactory.forStack(thaumium), meta, false,
                new ItemStack(UTItems.temperedIngot));
        if (added) {
            UTLog.info("Blast furnace recipe registered: thaumium -> tempered thaumium"
                    + " (fluid {}, duration {})", BLAST_FLUID, BLAST_DURATION);
        } else {
            UTLog.warn("Blast furnace recipe for tempered thaumium was rejected");
        }
    }

    /** Путь Б: сталь IC2 в тигле со стабилизаторами. */
    private static CrucibleRecipe registerCrucible() {
        // Катализатор строкой: CrucibleRecipe сам разворачивает её в оредикт
        // на конструкторе, поэтому регистрация обязана идти после оредикта IC2.
        return ThaumcraftApi.addCrucibleRecipe(
                UTResearch.TEMPERED_THAUMIUM,
                new ItemStack(UTItems.temperedIngot),
                "ingotSteel",
                new AspectList()
                        .add(Aspect.MAGIC, 4)       // Praecantatio
                        .add(Aspect.EXCHANGE, 2)    // Permutatio — перестраивает решётку
                        .add(Aspect.CRYSTAL, 2));   // Vitreus — держит форму при остывании
    }
}
