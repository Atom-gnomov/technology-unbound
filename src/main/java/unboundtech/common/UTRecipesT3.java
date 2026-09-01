package unboundtech.common;

import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigItems;
import unboundtech.UTLog;
import unboundtech.compat.ic2.IC2Handles;
import unboundtech.research.UTResearch;

/**
 * Арканные рецепты тира T3.
 *
 * Пока здесь один Флюкс-Конденсатор (`flux_condenser.md` §6). «Сердцем»
 * рецепта выбрана катушка IC2 (`crafting:coil`) — открытый вопрос §12.1
 * закрыт лором самой карточки: «подставить ей медные катушки вместо
 * пустого воздуха». Наличие константы в {@code CraftingItemType} сверено
 * по jar-у IC2 2.8.222.
 */
public final class UTRecipesT3 {

    public static ShapedArcaneRecipe fluxCondenser;
    public static ShapedArcaneRecipe thaumicOverclocker;
    public static ShapedArcaneRecipe resonantSplitter;
    public static ShapedArcaneRecipe inductionCrucible;
    public static ShapedArcaneRecipe busNode;
    public static ShapedArcaneRecipe conduitI;
    public static thaumcraft.api.crafting.ShapelessArcaneRecipe conduitII;
    public static thaumcraft.api.crafting.ShapelessArcaneRecipe conduitIII;
    public static ShapedArcaneRecipe vaultController;
    public static ShapedArcaneRecipe vaultGolemPort;
    public static ShapedArcaneRecipe fluxRevolver;
    public static thaumcraft.api.crafting.InfusionRecipe focusCharge;
    public static thaumcraft.api.crafting.InfusionRecipe fluxArquebus;
    public static thaumcraft.api.crafting.InfusionRecipe mechanistMortar;
    public static ShapedArcaneRecipe ringFrame;
    public static ShapedArcaneRecipe ringDrive;
    public static ShapedArcaneRecipe ringStride;
    public static ShapedArcaneRecipe ringBrace;

    private UTRecipesT3() {
    }

    public static thaumcraft.api.crafting.ShapedArcaneRecipe cartridgeLine;
    public static thaumcraft.api.crafting.InfusionRecipe visEdge;
    public static thaumcraft.api.crafting.InfusionRecipe voidIridium;
    public static thaumcraft.api.crafting.InfusionRecipe iridiumWandCap;
    public static thaumcraft.api.crafting.InfusionRecipe singulator;

    public static void register() {
        fluxCondenser = registerFluxCondenser();
        thaumicOverclocker = registerThaumicOverclocker();
        resonantSplitter = registerResonantSplitter();
        inductionCrucible = registerInductionCrucible();
        registerBus();
        fluxRevolver = registerFluxRevolver();
        registerRings();
        focusCharge = UTRecipesInfusion.registerFocusCharge();
        fluxArquebus = UTRecipesInfusion.registerFluxArquebus();
        mechanistMortar = UTRecipesInfusion.registerMechanistMortar();
        // T4 (#23): порядок В-10 — иридий -> наконечник -> Сингулятор
        voidIridium = UTRecipesInfusion.registerVoidIridium();
        iridiumWandCap = UTRecipesInfusion.registerIridiumWandCap();
        singulator = UTRecipesInfusion.registerSingulator();
        // T4 (#24): оружейная ветка — Линия и Вис-Кромка
        cartridgeLine = registerCartridgeLine();
        visEdge = UTRecipesInfusion.registerVisEdge();
    }

    /**
     * Патронная Линия (`cartridge_line.md` §6): арканный верстак —
     * симметрия закона 1 живёт в материалах, не в розетке. Только
     * примордиалы (§6.8 канона).
     */
    private static thaumcraft.api.crafting.ShapedArcaneRecipe registerCartridgeLine() {
        net.minecraft.item.ItemStack casing = unboundtech.compat.ic2.IC2Handles
                .item("resource", "machine");
        net.minecraft.item.ItemStack coil = unboundtech.compat.ic2.IC2Handles
                .item("crafting", "coil");
        if (casing.isEmpty() || coil.isEmpty()) {
            unboundtech.UTLog.warn(
                    "Cartridge Line recipe skipped: IC2 parts not found");
            return null;
        }
        return thaumcraft.api.ThaumcraftApi.addArcaneCraftingRecipe(
                unboundtech.research.UTResearch.CARTRIDGE_PRESS,
                new net.minecraft.item.ItemStack(
                        unboundtech.common.UTBlocks.cartridgeLine),
                new thaumcraft.api.aspects.AspectList()
                        .add(thaumcraft.api.aspects.Aspect.ORDER, 16)
                        .add(thaumcraft.api.aspects.Aspect.FIRE, 14)
                        .add(thaumcraft.api.aspects.Aspect.EARTH, 8),
                "TMT",
                "CFC",
                "TCT",
                'T', new net.minecraft.item.ItemStack(UTItems.temperedIngot),
                'M', new net.minecraft.item.ItemStack(
                        net.minecraft.init.Blocks.PISTON),
                'C', coil,
                'F', casing);
    }

    /**
     * Кольца Схемы (`schema_rings.md` §6): закалённый таумий сверху и
     * снизу, по бокам углеволокно IC2, в центре вис-кристалл СВОЕГО
     * примала: Остова — Terra, Привода — Ignis, Хода — Aer, Упора —
     * Ordo. Аспекты — Ordo 9, Terra 7, Aer 4 (пересчитаны §4).
     */
    private static void registerRings() {
        ItemStack fibre = IC2Handles.item("crafting", "carbon_fibre");
        if (fibre.isEmpty()) {
            UTLog.warn("Schema rings recipes skipped: IC2 carbon fibre not found");
            return;
        }
        ringFrame = ring(UTItems.ringFrame, fibre, 3);
        ringDrive = ring(UTItems.ringDrive, fibre, 1);
        ringStride = ring(UTItems.ringStride, fibre, 0);
        ringBrace = ring(UTItems.ringBrace, fibre, 4);
    }

    private static ShapedArcaneRecipe ring(net.minecraft.item.Item result,
                                           ItemStack fibre, int shardMeta) {
        return ThaumcraftApi.addArcaneCraftingRecipe(
                UTResearch.SCHEMA_RINGS,
                new ItemStack(result),
                new AspectList().add(Aspect.ORDER, 9).add(Aspect.EARTH, 7)
                        .add(Aspect.AIR, 4),
                " T ",
                "CVC",
                " T ",
                'T', new ItemStack(UTItems.temperedIngot),
                'C', fibre,
                'V', new ItemStack(ConfigItems.itemShard, 1, shardMeta));
    }

    /**
     * Флюкс-Револьвер (`flux_revolver.md` §6): слиток над бронзовой
     * оправой на деревянной рукояти. Аспекты — Ignis 10, Ordo 7,
     * Perditio 5 (пересчитаны по recipe_calibration.md §4). Ручной
     * профиль аспектов ствола живёт в UTItems.init (§7) — этот файл
     * сторожится тестом «в арканных стоимостях только прималы».
     */
    private static ShapedArcaneRecipe registerFluxRevolver() {
        ItemStack bronze = IC2Handles.item("ingot", "bronze");
        if (bronze.isEmpty()) {
            UTLog.warn("Flux Revolver recipe skipped: IC2 bronze not found");
            return null;
        }
        return ThaumcraftApi.addArcaneCraftingRecipe(
                UTResearch.FLUX_REVOLVER,
                new ItemStack(UTItems.fluxRevolver),
                new AspectList().add(Aspect.FIRE, 10).add(Aspect.ORDER, 7)
                        .add(Aspect.ENTROPY, 5),
                " T ",
                "BTB",
                " W ",
                'T', new ItemStack(UTItems.temperedIngot),
                'B', bronze,
                'W', new ItemStack(net.minecraft.init.Items.STICK));
    }

    /**
     * Шина эссенции: узел (`bus_node.md` §6), кабели трёх тиров
     * (`essentia_conduit.md` §6: I — 4 сегмента за 4 вис, «1 вис за
     * сегмент — ровно банка»; II = I + вис-кристалл, III = II +
     * углеволокно IC2), контроллер и голем-порт накопителя
     * (`essentia_vault.md` §6, нижняя граница T3 — 27 блоков корпуса
     * уже цена). Корпус накопителя — ванильный верстак, см. UTCrafting.
     */
    private static void registerBus() {
        busNode = ThaumcraftApi.addArcaneCraftingRecipe(
                UTResearch.ESSENTIA_BUS,
                new ItemStack(UTBlocks.busNode),
                new AspectList().add(Aspect.ORDER, 9).add(Aspect.AIR, 7)
                        .add(Aspect.WATER, 4),
                " T ",
                "TUT",
                " G ",
                'T', new ItemStack(UTItems.temperedIngot),
                'U', new ItemStack(ConfigBlocks.blockTube, 1, 0),
                'G', new ItemStack(net.minecraft.init.Blocks.GLASS));
        conduitI = ThaumcraftApi.addArcaneCraftingRecipe(
                UTResearch.ESSENTIA_BUS,
                new ItemStack(UTBlocks.conduitI, 4),
                new AspectList().add(Aspect.ORDER, 2).add(Aspect.AIR, 2),
                "GTG",
                'G', new ItemStack(net.minecraft.init.Blocks.GLASS),
                'T', new ItemStack(UTItems.temperedIngot));
        conduitII = ThaumcraftApi.addShapelessArcaneCraftingRecipe(
                UTResearch.ESSENTIA_BUS,
                new ItemStack(UTBlocks.conduitII),
                new AspectList().add(Aspect.ORDER, 4).add(Aspect.AIR, 4),
                new ItemStack(UTBlocks.conduitI),
                new ItemStack(ConfigItems.itemShard, 1, 32767));
        ItemStack fibre = IC2Handles.item("crafting", "carbon_fibre");
        if (fibre.isEmpty()) {
            UTLog.warn("Conduit III recipe skipped: IC2 carbon fibre not found");
        } else {
            conduitIII = ThaumcraftApi.addShapelessArcaneCraftingRecipe(
                    UTResearch.ESSENTIA_BUS,
                    new ItemStack(UTBlocks.conduitIII),
                    new AspectList().add(Aspect.ORDER, 6).add(Aspect.AIR, 5)
                            .add(Aspect.ENTROPY, 3),
                    new ItemStack(UTBlocks.conduitII),
                    fibre);
        }
        ItemStack circuit = IC2Handles.item("crafting", "advanced_circuit");
        if (circuit.isEmpty()) {
            UTLog.warn("Vault controller recipe skipped: IC2 circuit not found");
        } else {
            vaultController = ThaumcraftApi.addArcaneCraftingRecipe(
                    UTResearch.ESSENTIA_VAULT,
                    new ItemStack(UTBlocks.vaultController),
                    new AspectList().add(Aspect.ORDER, 9).add(Aspect.WATER, 7)
                            .add(Aspect.EARTH, 4),
                    " S ",
                    " C ",
                    " M ",
                    'S', new ItemStack(ConfigItems.itemShard, 1, 32767),
                    'C', circuit,
                    'M', new ItemStack(UTBlocks.vaultCasing));
        }
        vaultGolemPort = ThaumcraftApi.addArcaneCraftingRecipe(
                UTResearch.ESSENTIA_VAULT,
                new ItemStack(UTBlocks.vaultGolemPort),
                new AspectList().add(Aspect.ORDER, 7).add(Aspect.AIR, 5),
                " U ",
                " M ",
                'U', new ItemStack(ConfigBlocks.blockTube, 1, 0),
                'M', new ItemStack(UTBlocks.vaultCasing));
    }

    /**
     * Индукционный Тигель (`induction_crucible.md` §6): тигель ТК между
     * двух нагревательных спиралей IC2, снизу корпус машины в оправе
     * закалённого таумия. Аспекты — Ignis 12, Ordo 8, Aqua 4 (пересчитаны
     * карточкой по recipe_calibration.md §4). Тигель порта — blockMetalDevice
     * мета 0 (сверено по BlockMetalDevice: TYPE=0 — crucible).
     */
    private static ShapedArcaneRecipe registerInductionCrucible() {
        ItemStack casing = IC2Handles.item("resource", "machine");
        ItemStack coil = IC2Handles.item("crafting", "coil");
        if (casing.isEmpty() || coil.isEmpty()) {
            UTLog.warn("Induction Crucible recipe skipped: IC2 parts not found");
            return null;
        }
        return ThaumcraftApi.addArcaneCraftingRecipe(
                UTResearch.INDUCTION_CRUCIBLE,
                new ItemStack(UTBlocks.inductionCrucible),
                new AspectList().add(Aspect.FIRE, 12).add(Aspect.ORDER, 8)
                        .add(Aspect.WATER, 4),
                "KCK",
                "TMT",
                'K', coil,
                'C', new ItemStack(ConfigBlocks.blockMetalDevice, 1, 0),
                'T', new ItemStack(UTItems.temperedIngot),
                'M', casing);
    }

    /**
     * Расщепитель (`resonant_splitter.md` §6): четыре слитка вокруг
     * центрифуги ТК и корпуса машины IC2. §12.2 карточки закрыт: центрифуга
     * порта — это `blockTube` мета 2 (сверено по ConfigRecipesArcaneSlice),
     * а не blockMetalDevice. Аспекты — Ordo 10, Perditio 8, Aer 4 после
     * калибровки (было 55 вис при вилке T3 20–35).
     */
    private static ShapedArcaneRecipe registerResonantSplitter() {
        ItemStack casing = IC2Handles.item("resource", "machine");
        if (casing.isEmpty()) {
            UTLog.warn("Resonant Splitter recipe skipped: IC2 machine casing not found");
            return null;
        }
        return ThaumcraftApi.addArcaneCraftingRecipe(
                UTResearch.RESONANT_SPLITTER,
                new ItemStack(UTBlocks.resonantSplitter),
                new AspectList().add(Aspect.ORDER, 10).add(Aspect.ENTROPY, 8)
                        .add(Aspect.AIR, 4),
                "TCT",
                "TMT",
                'T', new ItemStack(UTItems.temperedIngot),
                'C', new ItemStack(ConfigBlocks.blockTube, 1, 2),
                'M', casing);
    }

    /**
     * Оверклокер (`thaumic_overclocker.md` §6): пустой фиал над родным
     * оверклокером IC2 в оправе из трёх слитков. Аспекты — Ordo 8, Aer 8,
     * Ignis 4 после калибровки (было 50 вис при вилке T3 20–35).
     */
    private static ShapedArcaneRecipe registerThaumicOverclocker() {
        ItemStack ic2Overclocker = IC2Handles.item("upgrade", "overclocker");
        if (ic2Overclocker.isEmpty()) {
            UTLog.warn("Thaumic Overclocker recipe skipped: IC2 overclocker not found");
            return null;
        }
        return ThaumcraftApi.addArcaneCraftingRecipe(
                UTResearch.THAUMIC_OVERCLOCKER,
                new ItemStack(UTItems.thaumicOverclocker),
                new AspectList().add(Aspect.ORDER, 8).add(Aspect.AIR, 8)
                        .add(Aspect.FIRE, 4),
                " P ",
                "TOT",
                " T ",
                'P', new ItemStack(ConfigItems.itemEssence, 1, 0),
                'O', ic2Overclocker,
                'T', new ItemStack(UTItems.temperedIngot));
    }

    /**
     * Сетка §6: крест из трёх слитков закалённого таумия, катушки IC2 и
     * банки эссенции ТК. Аспекты — Ordo 10, Perditio 7, Ignis 5: только
     * примордиалы (канон §6.8), числа после калибровки (§4: было 45 вис
     * при вилке T3 в 20–35).
     */
    private static ShapedArcaneRecipe registerFluxCondenser() {
        ItemStack coil = IC2Handles.item("crafting", "coil");
        if (coil.isEmpty()) {
            UTLog.warn("Flux Condenser recipe skipped: IC2 coil not found");
            return null;
        }
        return ThaumcraftApi.addArcaneCraftingRecipe(
                UTResearch.FLUX_CONDENSER,
                new ItemStack(UTBlocks.fluxCondenser),
                new AspectList().add(Aspect.ORDER, 10).add(Aspect.ENTROPY, 7)
                        .add(Aspect.FIRE, 5),
                " T ",
                "TCT",
                " J ",
                'T', new ItemStack(UTItems.temperedIngot),
                'C', coil,
                'J', new ItemStack(ConfigBlocks.blockJar));
    }
}
