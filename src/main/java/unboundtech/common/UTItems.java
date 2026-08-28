package unboundtech.common;

import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import unboundtech.UnboundTech;
import unboundtech.common.items.ItemCartridge;
import unboundtech.common.items.ItemElectricScribingTools;
import unboundtech.common.items.ItemFluxRevolver;
import unboundtech.common.items.ItemSchemaRing;
import unboundtech.common.items.ItemFluxCharge;
import unboundtech.common.items.ItemNanoThaumArmor;
import unboundtech.common.items.ItemTemperedArmor;
import unboundtech.common.items.ItemThaumicOverclocker;
import unboundtech.common.items.ItemTemperedTools;
import unboundtech.common.items.ItemThaumiumWrench;
import unboundtech.common.items.ItemUTResource;

/**
 * Реестр предметов мода (тир T2, `03_progression.md` §2).
 *
 * Предметы, как и блоки, регистрируются ВСЕГДА — иначе мир с ними ломается
 * при выключенном модуле; гейт модуля решает судьбу рецептов и исследований,
 * а не самого предмета.
 */
@Mod.EventBusSubscriber(modid = UnboundTech.MODID)
public final class UTItems {

    public static final String TEMPERED_INGOT = "tempered_thaumium_ingot";
    public static final String TEMPERED_HELMET = "tempered_helmet";
    public static final String TEMPERED_CHESTPLATE = "tempered_chestplate";
    public static final String TEMPERED_LEGGINGS = "tempered_leggings";
    public static final String TEMPERED_BOOTS = "tempered_boots";
    public static final String TEMPERED_SWORD = "tempered_sword";
    public static final String TEMPERED_PICKAXE = "tempered_pickaxe";
    public static final String TEMPERED_AXE = "tempered_axe";
    public static final String TEMPERED_SHOVEL = "tempered_shovel";
    public static final String TEMPERED_HOE = "tempered_hoe";
    public static final String THAUMIUM_WRENCH = "thaumium_wrench";
    public static final String ELECTRIC_SCRIBING = "electric_scribing_tools";
    /** T3: сгущённый флюкс, сырьё флюкс-патрона (`flux_condenser.md` §4.2). */
    public static final String FLUX_CHARGE = "flux_charge";
    /** T3: апгрейд машин IC2 — скорость за эссенцию (`thaumic_overclocker.md`). */
    public static final String THAUMIC_OVERCLOCKER = "thaumic_overclocker";
    /** ПРОТОТИП T4 (только внешний вид, решение владельца). */
    public static final String CASING = "casing";
    public static final String CARTRIDGE_INCENDIARY = "cartridge_incendiary";
    public static final String CARTRIDGE_ILLUMINATING = "cartridge_illuminating";
    public static final String FLUX_REVOLVER = "flux_revolver";
    public static final String RING_FRAME = "ring_frame";
    public static final String RING_DRIVE = "ring_drive";
    public static final String RING_STRIDE = "ring_stride";
    public static final String RING_BRACE = "ring_brace";

    public static final String NANO_THAUM_HELMET = "nano_thaum_helmet";
    public static final String NANO_THAUM_CHESTPLATE = "nano_thaum_chestplate";
    public static final String NANO_THAUM_LEGGINGS = "nano_thaum_leggings";
    public static final String NANO_THAUM_BOOTS = "nano_thaum_boots";

    /** Оредикт материала (`tempered_thaumium.md`). */
    public static final String ORE_INGOT = "ingotTemperedThaumium";
    /** Оредикт таумия ТК — материал починки ключа. */
    public static final String ORE_THAUMIUM = "ingotThaumium";

    /**
     * `tempered_thaumium_tools.md` §5: алмаз с прочностью ×1.2.
     * Порядок аргументов: уровень добычи, прочность, скорость, урон,
     * зачаровываемость.
     */
    public static final Item.ToolMaterial TOOL_MATERIAL = EnumHelper.addToolMaterial(
            "TEMPERED_THAUMIUM", 3, 1873, 8.0F, 3.0F, 10);

    /**
     * `tempered_thaumium_armor.md` §5. Массив поглощения индексируется
     * СЛОТОМ ({@code EntityEquipmentSlot.getIndex()}): ботинки, поножи,
     * нагрудник, шлем — то есть 3 / 7 / 10 / 4 при канонных
     * «шлем 4, нагрудник 10, поножи 7, ботинки 3». Сумма 24 = алмаз ×1.2.
     */
    public static final ItemArmor.ArmorMaterial ARMOR_MATERIAL = EnumHelper.addArmorMaterial(
            "TEMPERED_THAUMIUM", UnboundTech.MODID + ":tempered", 30,
            new int[]{3, 7, 10, 4}, 6, SoundEvents.ITEM_ARMOR_EQUIP_IRON, 1.0F);

    public static Item temperedIngot;
    public static Item temperedHelmet;
    public static Item temperedChestplate;
    public static Item temperedLeggings;
    public static Item temperedBoots;
    public static Item temperedSword;
    public static Item temperedPickaxe;
    public static Item temperedAxe;
    public static Item temperedShovel;
    public static Item temperedHoe;
    public static Item thaumiumWrench;
    public static Item electricScribingTools;
    public static Item fluxCharge;
    public static Item thaumicOverclocker;
    public static Item nanoThaumHelmet;
    public static Item nanoThaumChestplate;
    public static Item nanoThaumLeggings;
    public static Item nanoThaumBoots;
    public static Item casing;
    public static Item cartridgeIncendiary;
    public static Item cartridgeIlluminating;
    public static Item fluxRevolver;
    public static Item ringFrame;
    public static Item ringDrive;
    public static Item ringStride;
    public static Item ringBrace;

    /**
     * ПРОТОТИП: заглушка уровня примерки. Полоска 4/9/7/4 — из карточки
     * (`getArmorDisplay` при заряде); настоящая защита T4 будет через
     * ISpecialArmor с зарядом, не через этот материал.
     */
    public static final ItemArmor.ArmorMaterial NANO_THAUM_PROTO = EnumHelper.addArmorMaterial(
            "NANO_THAUM_PROTO", UnboundTech.MODID + ":nano_thaum", 40,
            new int[]{4, 7, 9, 4}, 30, SoundEvents.ITEM_ARMOR_EQUIP_IRON, 0.5F);

    private UTItems() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        temperedIngot = make(new ItemUTResource("unboundtech.tooltip.tempered_ingot"),
                TEMPERED_INGOT);

        temperedHelmet = make(new ItemTemperedArmor(ARMOR_MATERIAL, EntityEquipmentSlot.HEAD),
                TEMPERED_HELMET);
        temperedChestplate = make(new ItemTemperedArmor(ARMOR_MATERIAL, EntityEquipmentSlot.CHEST),
                TEMPERED_CHESTPLATE);
        temperedLeggings = make(new ItemTemperedArmor(ARMOR_MATERIAL, EntityEquipmentSlot.LEGS),
                TEMPERED_LEGGINGS);
        temperedBoots = make(new ItemTemperedArmor(ARMOR_MATERIAL, EntityEquipmentSlot.FEET),
                TEMPERED_BOOTS);

        temperedSword = make(new ItemTemperedTools.Sword(TOOL_MATERIAL), TEMPERED_SWORD);
        temperedPickaxe = make(new ItemTemperedTools.Pickaxe(TOOL_MATERIAL), TEMPERED_PICKAXE);
        temperedAxe = make(new ItemTemperedTools.Axe(TOOL_MATERIAL), TEMPERED_AXE);
        temperedShovel = make(new ItemTemperedTools.Spade(TOOL_MATERIAL), TEMPERED_SHOVEL);
        temperedHoe = make(new ItemTemperedTools.Hoe(TOOL_MATERIAL), TEMPERED_HOE);

        thaumiumWrench = make(new ItemThaumiumWrench(), THAUMIUM_WRENCH);
        electricScribingTools = make(new ItemElectricScribingTools(), ELECTRIC_SCRIBING);
        fluxCharge = make(new ItemFluxCharge(), FLUX_CHARGE);
        thaumicOverclocker = make(new ItemThaumicOverclocker(), THAUMIC_OVERCLOCKER);
        nanoThaumHelmet = make(new ItemNanoThaumArmor(NANO_THAUM_PROTO,
                EntityEquipmentSlot.HEAD), NANO_THAUM_HELMET);
        nanoThaumChestplate = make(new ItemNanoThaumArmor(NANO_THAUM_PROTO,
                EntityEquipmentSlot.CHEST), NANO_THAUM_CHESTPLATE);
        nanoThaumLeggings = make(new ItemNanoThaumArmor(NANO_THAUM_PROTO,
                EntityEquipmentSlot.LEGS), NANO_THAUM_LEGGINGS);
        nanoThaumBoots = make(new ItemNanoThaumArmor(NANO_THAUM_PROTO,
                EntityEquipmentSlot.FEET), NANO_THAUM_BOOTS);

        casing = make(new ItemUTResource("unboundtech.tooltip.casing"), CASING);
        cartridgeIncendiary = make(new ItemCartridge(
                unboundtech.common.entities.EntityFluxBullet.TYPE_INCENDIARY,
                "unboundtech.tooltip.cartridge_incendiary"), CARTRIDGE_INCENDIARY);
        cartridgeIlluminating = make(new ItemCartridge(
                unboundtech.common.entities.EntityFluxBullet.TYPE_ILLUMINATING,
                "unboundtech.tooltip.cartridge_illuminating"), CARTRIDGE_ILLUMINATING);
        fluxRevolver = make(new ItemFluxRevolver(), FLUX_REVOLVER);
        ringFrame = make(new ItemSchemaRing(ItemSchemaRing.Variant.FRAME), RING_FRAME);
        ringDrive = make(new ItemSchemaRing(ItemSchemaRing.Variant.DRIVE), RING_DRIVE);
        ringStride = make(new ItemSchemaRing(ItemSchemaRing.Variant.STRIDE), RING_STRIDE);
        ringBrace = make(new ItemSchemaRing(ItemSchemaRing.Variant.BRACE), RING_BRACE);

        event.getRegistry().registerAll(all());
    }

    /**
     * Оредикт и материалы починки. Зовётся из {@code init}: обеим строкам
     * нужен уже созданный предмет, а {@code setRepairItem} принимает
     * готовый {@link ItemStack}.
     */
    public static void init() {
        OreDictionary.registerOre(ORE_INGOT, new ItemStack(temperedIngot));
        ItemStack ingot = new ItemStack(temperedIngot);
        TOOL_MATERIAL.setRepairItem(ingot);
        ARMOR_MATERIAL.setRepairItem(ingot);
        // Регистр апгрейдов IC2 — только подсказки в GUI машин (§12.1
        // карточки: побочной валидации у него нет, безопасно).
        ic2.api.upgrade.UpgradeRegistry.register(new ItemStack(thaumicOverclocker));
        // Профиль аспектов револьвера — ручной (`flux_revolver.md` §7):
        // Telum движок сам не припишет, наши стволы не наследуют
        // ItemSword/ItemBow, а компоненты рецепта его не содержат.
        thaumcraft.api.ThaumcraftApi.registerObjectTag(new ItemStack(fluxRevolver),
                new thaumcraft.api.aspects.AspectList()
                        .add(thaumcraft.api.aspects.Aspect.METAL, 8)
                        .add(thaumcraft.api.aspects.Aspect.WEAPON, 6)
                        .add(thaumcraft.api.aspects.Aspect.TOOL, 4)
                        .add(thaumcraft.api.aspects.Aspect.MAGIC, 3));
    }

    private static Item make(Item item, String name) {
        return item
                .setRegistryName(UnboundTech.MODID, name)
                .setTranslationKey(UnboundTech.MODID + "." + name)
                .setCreativeTab(UTBlocks.TAB);
    }

    /** Все предметы мода — для регистрации и для моделей на клиенте. */
    public static Item[] all() {
        return new Item[]{
                temperedIngot,
                temperedHelmet, temperedChestplate, temperedLeggings, temperedBoots,
                temperedSword, temperedPickaxe, temperedAxe, temperedShovel, temperedHoe,
                thaumiumWrench, electricScribingTools, fluxCharge, thaumicOverclocker,
                nanoThaumHelmet, nanoThaumChestplate, nanoThaumLeggings, nanoThaumBoots,
                casing, cartridgeIncendiary, cartridgeIlluminating, fluxRevolver,
                ringFrame, ringDrive, ringStride, ringBrace,
        };
    }
}
