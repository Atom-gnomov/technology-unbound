package unboundtech.common;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import thaumcraft.common.config.ConfigItems;
import unboundtech.UTLog;
import unboundtech.UnboundTech;
import unboundtech.compat.ic2.IC2Handles;

/**
 * Кузнечные рецепты тира T2 — обычный верстак.
 *
 * Верстак именно обычный, и это принципиально: броня и инструменты из
 * закалённого таумия куются, а не наполняются на алтаре
 * (`tempered_thaumium_armor.md` §6, `tempered_thaumium_tools.md` §6).
 *
 * ⚠️ Регистрация идёт в {@link RegistryEvent.Register}, а не в postInit:
 * в 1.12.2 рецепты — обычный Forge-реестр, и после событий регистрации он
 * заморожен. Арканные рецепты и рецепты тигля этого не касаются — они живут
 * в собственных списках Thaumcraft, поэтому регистрируются позже.
 *
 * ⚠️ Расхождение с каноном, которое нельзя закрыть кодом: карточки называют
 * записи `TEMPERED_ARMOR`/`TEMPERED_TOOLS` «исследованием-владельцем»
 * рецепта, но обычный верстак в ТК исследованием НЕ гейтится — гейт есть
 * только у арканного верстака, тигля и инфузии. Записи показывают рецепты,
 * но не запирают их: скрафтить можно и до изучения.
 *
 * Ссылки на созданные рецепты хранятся полями — страницы Таумономикона
 * принимают сам объект {@link IRecipe}.
 */
@Mod.EventBusSubscriber(modid = UnboundTech.MODID)
public final class UTCrafting {

    public static IRecipe helmet;
    public static IRecipe chestplate;
    public static IRecipe leggings;
    public static IRecipe boots;

    public static IRecipe sword;
    public static IRecipe pickaxe;
    public static IRecipe axe;
    public static IRecipe shovel;
    public static IRecipe hoe;

    public static IRecipe wrench;
    public static IRecipe scribingTools;
    public static IRecipe vaultCasing;
    public static IRecipe casingRecipe;
    public static IRecipe cartridgeIncendiary;
    public static IRecipe cartridgeIlluminating;

    private UTCrafting() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<IRecipe> event) {
        String ingot = UTItems.ORE_INGOT;
        Object stick = Items.STICK;

        // --- Броня: стандартная раскладка, 5 / 8 / 7 / 4 слитка (§6) ---
        helmet = shaped(event, UTItems.TEMPERED_HELMET, UTItems.temperedHelmet,
                "XXX", "X X", 'X', ingot);
        chestplate = shaped(event, UTItems.TEMPERED_CHESTPLATE, UTItems.temperedChestplate,
                "X X", "XXX", "XXX", 'X', ingot);
        leggings = shaped(event, UTItems.TEMPERED_LEGGINGS, UTItems.temperedLeggings,
                "XXX", "X X", "X X", 'X', ingot);
        boots = shaped(event, UTItems.TEMPERED_BOOTS, UTItems.temperedBoots,
                "X X", "X X", 'X', ingot);

        // --- Инструменты: ванильные схемы (§6), состав сверен с §7 ---
        sword = shaped(event, UTItems.TEMPERED_SWORD, UTItems.temperedSword,
                "X", "X", "S", 'X', ingot, 'S', stick);
        pickaxe = shaped(event, UTItems.TEMPERED_PICKAXE, UTItems.temperedPickaxe,
                "XXX", " S ", " S ", 'X', ingot, 'S', stick);
        axe = shaped(event, UTItems.TEMPERED_AXE, UTItems.temperedAxe,
                "XX", "XS", " S", 'X', ingot, 'S', stick);
        shovel = shaped(event, UTItems.TEMPERED_SHOVEL, UTItems.temperedShovel,
                "X", "S", "S", 'X', ingot, 'S', stick);
        hoe = shaped(event, UTItems.TEMPERED_HOE, UTItems.temperedHoe,
                "XX", " S", " S", 'X', ingot, 'S', stick);

        // --- Таумиевый ключ: сетка ключа IC2, но из ОБЫЧНОГО таумия
        // (`thaumium_wrench.md` §2: ключу нужна гибкость живого металла) ---
        wrench = shaped(event, UTItems.THAUMIUM_WRENCH, UTItems.thaumiumWrench,
                "T T", "TTT", " T ", 'T', UTItems.ORE_THAUMIUM);

        // --- Корпус Накопителя (`essentia_vault.md` §6): 4 закалённых
        // таумия + корпус машины IC2 -> 4 шт. Обычный верстак: корпуса —
        // массовый стройматериал, арканный вис тратится на контроллер. ---
        vaultCasing = registerVaultCasing(event);

        // --- Боеприпас T3 (`cartridges.md` §4.1, §6): гильза 8 шт из
        // слитка закалённого таумия; патрон = гильза + сырьё, обычный
        // верстак — «медленно и ровно настолько неудобно, чтобы захотеть
        // автоматизацию». ---
        casingRecipe = shapedStack(event, "casing_x8",
                new ItemStack(UTItems.casing, 8), "T", 'T',
                new ItemStack(UTItems.temperedIngot));
        cartridgeIncendiary = shapelessStack(event, "cartridge_incendiary",
                new ItemStack(UTItems.cartridgeIncendiary),
                new ItemStack(UTItems.casing),
                new ItemStack(thaumcraft.common.config.ConfigItems.itemResource, 1, 0));
        cartridgeIlluminating = shapelessStack(event, "cartridge_illuminating",
                new ItemStack(UTItems.cartridgeIlluminating),
                new ItemStack(UTItems.casing),
                new ItemStack(thaumcraft.common.config.ConfigItems.itemResource, 1, 1));

        // --- Электрочернильница: чернильница ТК + слиток + RE-батарея (§6).
        // Сетка в карточке не задана — значит бесформенный рецепт. ---
        scribingTools = registerScribingTools(event);
    }

    private static IRecipe shapedStack(RegistryEvent.Register<IRecipe> event,
                                       String name, ItemStack result, Object... recipe) {
        ShapedOreRecipe shaped = new ShapedOreRecipe(group(), result, recipe);
        shaped.setRegistryName(unboundtech.UnboundTech.MODID, name);
        event.getRegistry().register(shaped);
        return shaped;
    }

    private static IRecipe shapelessStack(RegistryEvent.Register<IRecipe> event,
                                          String name, ItemStack result, Object... parts) {
        net.minecraftforge.oredict.ShapelessOreRecipe shapeless =
                new net.minecraftforge.oredict.ShapelessOreRecipe(group(), result, parts);
        shapeless.setRegistryName(unboundtech.UnboundTech.MODID, name);
        event.getRegistry().register(shapeless);
        return shapeless;
    }

    private static IRecipe registerVaultCasing(RegistryEvent.Register<IRecipe> event) {
        ItemStack casing = IC2Handles.item("resource", "machine");
        if (casing.isEmpty()) {
            UTLog.warn("Vault casing recipe skipped: IC2 machine casing not found");
            return null;
        }
        ShapedOreRecipe shaped = new ShapedOreRecipe(group(),
                new ItemStack(unboundtech.common.UTBlocks.vaultCasing, 4),
                "T T", " M ", "T T",
                'T', new ItemStack(UTItems.temperedIngot), 'M', casing);
        shaped.setRegistryName(unboundtech.UnboundTech.MODID, "essentia_vault_casing");
        event.getRegistry().register(shaped);
        return shaped;
    }

    private static IRecipe registerScribingTools(RegistryEvent.Register<IRecipe> event) {
        ItemStack battery = IC2Handles.item("re_battery", null);
        if (battery.isEmpty()) {
            UTLog.warn("Electric scribing tools recipe skipped: IC2 RE-battery not found");
            return null;
        }
        // Поле заполняется в ConfigItems.init() из preInit порта, то есть до
        // события рецептов; проверка — страховка на случай смены порядка.
        if (ConfigItems.itemInkwell == null) {
            UTLog.warn("Electric scribing tools recipe skipped: TC inkwell not ready");
            return null;
        }
        ShapelessOreRecipe recipe = new ShapelessOreRecipe(
                group(), new ItemStack(UTItems.electricScribingTools),
                new ItemStack(ConfigItems.itemInkwell),
                UTItems.ORE_INGOT,
                battery);
        recipe.setRegistryName(new ResourceLocation(UnboundTech.MODID,
                UTItems.ELECTRIC_SCRIBING));
        event.getRegistry().register(recipe);
        return recipe;
    }

    private static IRecipe shaped(RegistryEvent.Register<IRecipe> event, String name,
                                  net.minecraft.item.Item result, Object... recipe) {
        ShapedOreRecipe shaped = new ShapedOreRecipe(group(), new ItemStack(result), recipe);
        shaped.setRegistryName(new ResourceLocation(UnboundTech.MODID, name));
        event.getRegistry().register(shaped);
        return shaped;
    }

    private static ResourceLocation group() {
        return new ResourceLocation(UnboundTech.MODID, "tempered");
    }
}
