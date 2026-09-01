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
import unboundtech.common.items.ItemVoidIridium;
import unboundtech.common.items.ItemElectricScribingTools;
import unboundtech.common.items.ItemFluxArquebus;
import unboundtech.common.items.ItemFluxRevolver;
import unboundtech.common.items.ItemChargedSpark;
import unboundtech.common.items.ItemFocusCharge;
import unboundtech.common.items.ItemSchemaRing;
import unboundtech.common.items.ItemFluxCharge;
import unboundtech.common.items.ItemNanoThaumArmor;
import unboundtech.common.items.ItemQuantumHybridArmor;
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
    public static final String FLUX_ARQUEBUS = "flux_arquebus";
    public static final String CARTRIDGE_VIS = "cartridge_vis";
    public static final String CARTRIDGE_FLUX = "cartridge_flux";
    public static final String CARTRIDGE_BALL = "cartridge_ball";
    public static final String VOID_IRIDIUM = "void_iridium";
    public static final String IRIDIUM_WAND_CAP = "iridium_wand_cap";
    public static final String FOCUS_CHARGE = "focus_charge";
    public static final String CHARGED_SPARK = "charged_spark";
    public static final String RING_FRAME = "ring_frame";
    public static final String RING_DRIVE = "ring_drive";
    public static final String RING_STRIDE = "ring_stride";
    public static final String RING_BRACE = "ring_brace";

    public static final String QUANT_VOID_HELMET = "quant_void_helmet";
    public static final String QUANT_VOID_CHESTPLATE = "quant_void_chestplate";
    public static final String QUANT_VOID_LEGGINGS = "quant_void_leggings";
    public static final String QUANT_VOID_BOOTS = "quant_void_boots";
    public static final String QUANT_ICHOR_HELMET = "quant_ichor_helmet";
    public static final String QUANT_ICHOR_CHESTPLATE = "quant_ichor_chestplate";
    public static final String QUANT_ICHOR_LEGGINGS = "quant_ichor_leggings";
    public static final String QUANT_ICHOR_BOOTS = "quant_ichor_boots";

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
    public static Item quantVoidHelmet;
    public static Item quantVoidChestplate;
    public static Item quantVoidLeggings;
    public static Item quantVoidBoots;
    public static Item quantIchorHelmet;
    public static Item quantIchorChestplate;
    public static Item quantIchorLeggings;
    public static Item quantIchorBoots;
    public static Item casing;
    public static Item cartridgeIncendiary;
    public static Item cartridgeIlluminating;
    public static Item fluxRevolver;
    public static Item fluxArquebus;
    public static Item cartridgeVis;
    public static Item cartridgeFlux;
    public static Item cartridgeBall;
    public static Item voidIridium;
    public static Item iridiumWandCap;
    public static Item focusCharge;
    public static Item chargedSpark;
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
        quantVoidHelmet = make(new ItemQuantumHybridArmor(NANO_THAUM_PROTO,
                EntityEquipmentSlot.HEAD, 0), QUANT_VOID_HELMET);
        quantVoidChestplate = make(new ItemQuantumHybridArmor(NANO_THAUM_PROTO,
                EntityEquipmentSlot.CHEST, 0), QUANT_VOID_CHESTPLATE);
        quantVoidLeggings = make(new ItemQuantumHybridArmor(NANO_THAUM_PROTO,
                EntityEquipmentSlot.LEGS, 0), QUANT_VOID_LEGGINGS);
        quantVoidBoots = make(new ItemQuantumHybridArmor(NANO_THAUM_PROTO,
                EntityEquipmentSlot.FEET, 0), QUANT_VOID_BOOTS);
        quantIchorHelmet = make(new ItemQuantumHybridArmor(NANO_THAUM_PROTO,
                EntityEquipmentSlot.HEAD, 1), QUANT_ICHOR_HELMET);
        quantIchorChestplate = make(new ItemQuantumHybridArmor(NANO_THAUM_PROTO,
                EntityEquipmentSlot.CHEST, 1), QUANT_ICHOR_CHESTPLATE);
        quantIchorLeggings = make(new ItemQuantumHybridArmor(NANO_THAUM_PROTO,
                EntityEquipmentSlot.LEGS, 1), QUANT_ICHOR_LEGGINGS);
        quantIchorBoots = make(new ItemQuantumHybridArmor(NANO_THAUM_PROTO,
                EntityEquipmentSlot.FEET, 1), QUANT_ICHOR_BOOTS);
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
        cartridgeBall = make(new ItemCartridge(
                unboundtech.common.entities.EntityFluxBullet.TYPE_BALL,
                "unboundtech.tooltip.cartridge_ball"), CARTRIDGE_BALL);
        // T4 (`void_iridium.md`, `iridium_wand_components.md`)
        voidIridium = make(new ItemVoidIridium(), VOID_IRIDIUM);
        iridiumWandCap = make(new Item().setMaxStackSize(16), IRIDIUM_WAND_CAP);
        fluxRevolver = make(new ItemFluxRevolver(), FLUX_REVOLVER);
        fluxArquebus = make(new ItemFluxArquebus(), FLUX_ARQUEBUS);
        cartridgeVis = make(new ItemCartridge(
                unboundtech.common.entities.EntityFluxBullet.TYPE_VIS,
                "unboundtech.tooltip.cartridge_vis"), CARTRIDGE_VIS);
        cartridgeFlux = make(new ItemCartridge(
                unboundtech.common.entities.EntityFluxBullet.TYPE_FLUX,
                "unboundtech.tooltip.cartridge_flux"), CARTRIDGE_FLUX);
        focusCharge = make(new ItemFocusCharge(), FOCUS_CHARGE);
        chargedSpark = make(new ItemChargedSpark(), CHARGED_SPARK);
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
        // Иридиевый наконечник жезла (`iridium_wand_components.md` §4.1):
        // база 0.75 — лучше пустотного и ихорного (0.8), особая скидка
        // 0.65 на Ordo и Perditio; craftCost 12. WandCap кладёт себя в
        // реестр сам, хук порту не нужен (§2.1 карточки).
        new thaumcraft.api.wands.WandCap("iridium", 0.75f,
                java.util.Arrays.asList(
                        thaumcraft.api.aspects.Aspect.ORDER,
                        thaumcraft.api.aspects.Aspect.ENTROPY),
                0.65f, new ItemStack(iridiumWandCap), 12)
                .setTexture(new net.minecraft.util.ResourceLocation(
                        unboundtech.UnboundTech.MODID,
                        "textures/models/wand_cap_iridium.png"));
        ItemStack ingot = new ItemStack(temperedIngot);
        TOOL_MATERIAL.setRepairItem(ingot);
        ARMOR_MATERIAL.setRepairItem(ingot);
        // Регистр апгрейдов IC2 — только подсказки в GUI машин (§12.1
        // карточки: побочной валидации у него нет, безопасно).
        ic2.api.upgrade.UpgradeRegistry.register(new ItemStack(thaumicOverclocker));
        // Профиль аспектов револьвера — ручной (`flux_revolver.md` §7):
        // Telum движок сам не припишет, наши стволы не наследуют
        // ItemSword/ItemBow, а компоненты рецепта его не содержат.
        // Техно-дух сканируется таумометром (`techno_spirit.md` §7):
        // у сущности нет рецепта, аспекты назначаются вручную.
        thaumcraft.api.ThaumcraftApi.registerEntityTag(
                "unboundtech.techno_spirit",
                new thaumcraft.api.aspects.AspectList()
                        .add(thaumcraft.api.aspects.Aspect.BEAST, 5)
                        .add(thaumcraft.api.aspects.Aspect.ENERGY, 4)
                        .add(thaumcraft.api.aspects.Aspect.MECHANISM, 3)
                        .add(thaumcraft.api.aspects.Aspect.MAGIC, 2));
        thaumcraft.api.ThaumcraftApi.registerObjectTag(new ItemStack(fluxArquebus),
                new thaumcraft.api.aspects.AspectList()
                        .add(thaumcraft.api.aspects.Aspect.WEAPON, 12)
                        .add(thaumcraft.api.aspects.Aspect.METAL, 10)
                        .add(thaumcraft.api.aspects.Aspect.MECHANISM, 6)
                        .add(thaumcraft.api.aspects.Aspect.MAGIC, 5));
        thaumcraft.api.ThaumcraftApi.registerObjectTag(new ItemStack(fluxRevolver),
                new thaumcraft.api.aspects.AspectList()
                        .add(thaumcraft.api.aspects.Aspect.METAL, 8)
                        .add(thaumcraft.api.aspects.Aspect.WEAPON, 6)
                        .add(thaumcraft.api.aspects.Aspect.TOOL, 4)
                        .add(thaumcraft.api.aspects.Aspect.MAGIC, 3));
    }

    /** Предмет-патрон по типу пули — для разрядки стволов. */
    public static Item cartridgeFor(int bulletType) {
        switch (bulletType) {
            case 1: return cartridgeIlluminating;
            case 2: return cartridgeVis;
            case 3: return cartridgeFlux;
            case 4: return cartridgeBall;
            default: return cartridgeIncendiary;
        }
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
                quantVoidHelmet, quantVoidChestplate, quantVoidLeggings, quantVoidBoots,
                quantIchorHelmet, quantIchorChestplate, quantIchorLeggings, quantIchorBoots,
                casing, cartridgeIncendiary, cartridgeIlluminating, fluxRevolver,
                fluxArquebus, cartridgeVis, cartridgeFlux, cartridgeBall,
                voidIridium, iridiumWandCap,
                focusCharge, chargedSpark, ringFrame, ringDrive, ringStride, ringBrace,
        };
    }
}
