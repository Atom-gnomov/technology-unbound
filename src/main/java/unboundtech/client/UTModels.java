package unboundtech.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import unboundtech.UnboundTech;
import unboundtech.init.UTBlocks;
import unboundtech.init.UTItems;

/** Клиентская регистрация моделей предметов и блок-предметов. */
@Mod.EventBusSubscriber(modid = UnboundTech.MODID, value = Side.CLIENT)
public final class UTModels {

    private UTModels() {
    }

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        registerItemModel(UTItems.thaumSteelIngot);
        registerBlockModel(UTBlocks.thaumGenerator);
        registerBlockModel(UTBlocks.aethericEngine);
    }

    private static void registerItemModel(Item item) {
        if (item == null || item.getRegistryName() == null) {
            return;
        }
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }

    private static void registerBlockModel(Block block) {
        if (block == null || block.getRegistryName() == null) {
            return;
        }
        registerItemModel(Item.getItemFromBlock(block));
    }
}
