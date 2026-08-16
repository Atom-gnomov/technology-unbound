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
import unboundtech.common.blocks.BlockThaumGenerator;
import unboundtech.common.tiles.TileAethericEngine;
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

    public static Block thaumGenerator;
    public static Block aethericEngine;

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
        event.getRegistry().registerAll(all());

        GameRegistry.registerTileEntity(TileThaumGenerator.class,
                new ResourceLocation(UnboundTech.MODID, THAUM_GENERATOR));
        GameRegistry.registerTileEntity(TileAethericEngine.class,
                new ResourceLocation(UnboundTech.MODID, AETHERIC_ENGINE));
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
        return new Block[]{thaumGenerator, aethericEngine};
    }
}
