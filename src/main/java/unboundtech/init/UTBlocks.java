package unboundtech.init;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import unboundtech.UnboundTech;
import unboundtech.block.BlockAethericEngine;
import unboundtech.block.BlockThaumGenerator;
import unboundtech.tile.TileAethericEngine;
import unboundtech.tile.TileThaumGenerator;

/**
 * Блоки мода.
 *
 * Регистрация сознательно БЕЗУСЛОВНА (не гейтится модулями): registry-события
 * Forge приходят раньше preInit, где читается конфиг, а снятие блока с
 * регистрации ломало бы существующие миры. Модули гейтят рецепты и
 * исследования — выключенный модуль означает «контент недоступен», а не
 * «предмет исчез из реестра».
 */
@Mod.EventBusSubscriber(modid = UnboundTech.MODID)
public final class UTBlocks {

    public static Block thaumGenerator;
    public static Block aethericEngine;

    private UTBlocks() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        thaumGenerator = new BlockThaumGenerator()
                .setRegistryName(UnboundTech.MODID, "thaum_generator")
                .setTranslationKey(UnboundTech.MODID + ".thaum_generator");
        aethericEngine = new BlockAethericEngine()
                .setRegistryName(UnboundTech.MODID, "aetheric_engine")
                .setTranslationKey(UnboundTech.MODID + ".aetheric_engine");

        event.getRegistry().registerAll(thaumGenerator, aethericEngine);
    }

    @SubscribeEvent
    public static void registerItemBlocks(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                itemBlock(thaumGenerator),
                itemBlock(aethericEngine));
    }

    private static ItemBlock itemBlock(Block block) {
        ItemBlock item = new ItemBlock(block);
        item.setRegistryName(block.getRegistryName());
        return item;
    }

    /** Вызывается из preInit: тайлы не относятся к Forge-реестрам. */
    public static void registerTileEntities() {
        GameRegistry.registerTileEntity(TileThaumGenerator.class,
                new ResourceLocation(UnboundTech.MODID, "thaum_generator"));
        GameRegistry.registerTileEntity(TileAethericEngine.class,
                new ResourceLocation(UnboundTech.MODID, "aetheric_engine"));
    }
}
