package unboundtech.common;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import unboundtech.UnboundTech;
import unboundtech.common.blocks.BlockAethericEngine;
import unboundtech.common.blocks.BlockFluxCondenser;
import unboundtech.common.blocks.BlockBusNode;
import unboundtech.common.blocks.BlockEssentiaConduit;
import unboundtech.common.blocks.BlockInductionCrucible;
import unboundtech.common.blocks.BlockPhotonLight;
import unboundtech.common.blocks.BlockVaultCasing;
import unboundtech.common.blocks.BlockVaultController;
import unboundtech.common.blocks.BlockVaultGolemPort;
import unboundtech.common.blocks.BlockResonantSplitter;
import unboundtech.common.blocks.BlockThaumGenerator;
import unboundtech.common.tiles.TileAethericEngine;
import unboundtech.common.tiles.TileFluxCondenser;
import unboundtech.common.tiles.TileBusNode;
import unboundtech.common.tiles.TileEssentiaVaultController;
import unboundtech.common.tiles.TileInductionCrucible;
import unboundtech.common.tiles.TileVaultGolemPort;
import unboundtech.common.tiles.TileResonantSplitter;
import unboundtech.common.tiles.TileThaumGenerator;

/**
 * Реестр блоков мода и их ItemBlock-ов.
 *
 * Блоки регистрируются всегда (иначе мир с сохранёнными блоками ломается
 * при выключенном модуле); гейт модуля решает только судьбу рецептов и
 * исследований — выключенный CORE делает машины недоступными в игре,
 * но уже поставленные блоки не превращаются в дыры.
 */
@Mod.EventBusSubscriber(modid = UnboundTech.MODID)
public final class UTBlocks {

    public static final String THAUM_GENERATOR = "thaum_generator";
    public static final String AETHERIC_ENGINE = "aetheric_engine";
    public static final String FLUX_CONDENSER = "flux_condenser";
    public static final String RESONANT_SPLITTER = "resonant_splitter";
    public static final String INDUCTION_CRUCIBLE = "induction_crucible";
    public static final String BUS_NODE = "bus_node";
    public static final String CONDUIT_I = "essentia_conduit_i";
    public static final String CONDUIT_II = "essentia_conduit_ii";
    public static final String CONDUIT_III = "essentia_conduit_iii";
    public static final String VAULT_CASING = "essentia_vault_casing";
    public static final String VAULT_CONTROLLER = "essentia_vault_controller";
    public static final String VAULT_GOLEM_PORT = "essentia_vault_golem_port";

    public static Block thaumGenerator;
    public static Block aethericEngine;
    public static Block fluxCondenser;
    public static Block resonantSplitter;
    public static Block inductionCrucible;
    public static Block busNode;
    public static Block conduitI;
    public static Block conduitII;
    public static Block conduitIII;
    public static Block vaultCasing;
    public static Block vaultController;
    public static Block vaultGolemPort;
    /** Свет осветительного патрона: без предмета, ставится только снарядом. */
    public static Block photonLight;

    /** Вкладка креатива мода; иконка — Таум-Генератор. */
    public static final CreativeTabs TAB = new CreativeTabs(UnboundTech.MODID) {
        @Override
        public ItemStack createIcon() {
            return thaumGenerator == null
                    ? new ItemStack(net.minecraft.init.Items.REDSTONE)
                    : new ItemStack(thaumGenerator);
        }
    };

    private UTBlocks() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        thaumGenerator = make(new BlockThaumGenerator(), THAUM_GENERATOR);
        aethericEngine = make(new BlockAethericEngine(), AETHERIC_ENGINE);
        fluxCondenser = make(new BlockFluxCondenser(), FLUX_CONDENSER);
        resonantSplitter = make(new BlockResonantSplitter(), RESONANT_SPLITTER);
        inductionCrucible = make(new BlockInductionCrucible(), INDUCTION_CRUCIBLE);
        busNode = make(new BlockBusNode(), BUS_NODE);
        conduitI = make(new BlockEssentiaConduit(2), CONDUIT_I);
        conduitII = make(new BlockEssentiaConduit(4), CONDUIT_II);
        conduitIII = make(new BlockEssentiaConduit(8), CONDUIT_III);
        vaultCasing = make(new BlockVaultCasing(), VAULT_CASING);
        vaultController = make(new BlockVaultController(), VAULT_CONTROLLER);
        vaultGolemPort = make(new BlockVaultGolemPort(), VAULT_GOLEM_PORT);
        photonLight = new BlockPhotonLight()
                .setRegistryName(UnboundTech.MODID, "photon_light")
                .setTranslationKey(UnboundTech.MODID + ".photon_light");
        event.getRegistry().register(photonLight);
        event.getRegistry().registerAll(all());

        GameRegistry.registerTileEntity(TileThaumGenerator.class,
                new ResourceLocation(UnboundTech.MODID, THAUM_GENERATOR));
        GameRegistry.registerTileEntity(TileAethericEngine.class,
                new ResourceLocation(UnboundTech.MODID, AETHERIC_ENGINE));
        GameRegistry.registerTileEntity(TileFluxCondenser.class,
                new ResourceLocation(UnboundTech.MODID, FLUX_CONDENSER));
        GameRegistry.registerTileEntity(TileResonantSplitter.class,
                new ResourceLocation(UnboundTech.MODID, RESONANT_SPLITTER));
        GameRegistry.registerTileEntity(TileInductionCrucible.class,
                new ResourceLocation(UnboundTech.MODID, INDUCTION_CRUCIBLE));
        GameRegistry.registerTileEntity(TileBusNode.class,
                new ResourceLocation(UnboundTech.MODID, BUS_NODE));
        GameRegistry.registerTileEntity(TileEssentiaVaultController.class,
                new ResourceLocation(UnboundTech.MODID, VAULT_CONTROLLER));
        GameRegistry.registerTileEntity(TileVaultGolemPort.class,
                new ResourceLocation(UnboundTech.MODID, VAULT_GOLEM_PORT));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (Block block : all()) {
            event.getRegistry().register(itemBlock(block));
        }
    }

    private static Block make(Block block, String name) {
        return block
                .setRegistryName(UnboundTech.MODID, name)
                .setTranslationKey(UnboundTech.MODID + "." + name)
                .setCreativeTab(TAB);
    }

    private static ItemBlock itemBlock(Block block) {
        ItemBlock item = new ItemBlock(block);
        item.setRegistryName(block.getRegistryName());
        return item;
    }

    /** Все блоки мода — для регистрации и для моделей на клиенте. */
    public static Block[] all() {
        return new Block[]{thaumGenerator, aethericEngine, fluxCondenser,
                resonantSplitter, inductionCrucible, busNode, conduitI, conduitII,
                conduitIII, vaultCasing, vaultController, vaultGolemPort};
    }
}
