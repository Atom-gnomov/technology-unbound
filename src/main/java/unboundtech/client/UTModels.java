package unboundtech.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.UnboundTech;
import unboundtech.common.UTBlocks;
import unboundtech.common.UTItems;

/**
 * Модели предметов-блоков. Отдельный клиентский подписчик вместо
 * {@code @SidedProxy}: серверной половины у мода пока нет, а
 * {@link ModelRegistryEvent} и так приходит только на клиенте.
 */
@Mod.EventBusSubscriber(modid = UnboundTech.MODID, value = Side.CLIENT)
public final class UTModels {

    private UTModels() {
    }

    /**
     * Аркебуза: обёртка модели пишет TransformType до TEISR-рендера —
     * анимации отдачи/шомпола положены только в руках (ТЗ п.1).
     */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onModelBake(net.minecraftforge.client.event.ModelBakeEvent event) {
        ModelResourceLocation key = new ModelResourceLocation(
                UTItems.fluxArquebus.getRegistryName(), "inventory");
        net.minecraft.client.renderer.block.model.IBakedModel base =
                event.getModelRegistry().getObject(key);
        if (base != null) {
            event.getModelRegistry().putObject(key, new PerspectiveCapture(base));
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        // Снаряд револьвера: рендер вращающейся иконки патрона.
        net.minecraftforge.fml.client.registry.RenderingRegistry
                .registerEntityRenderingHandler(
                        unboundtech.common.entities.EntityFluxBullet.class,
                        manager -> new net.minecraft.client.renderer.entity.RenderSnowball<>(
                                manager, UTItems.cartridgeIncendiary,
                                net.minecraft.client.Minecraft.getMinecraft()
                                        .getRenderItem()));
        net.minecraftforge.fml.client.registry.RenderingRegistry
                .registerEntityRenderingHandler(
                        unboundtech.common.entities.EntityTechnoSpirit.class,
                        RenderTechnoSpirit::new);
        net.minecraftforge.fml.client.registry.RenderingRegistry
                .registerEntityRenderingHandler(
                        unboundtech.common.entities.EntityMortarShell.class,
                        manager -> new net.minecraft.client.renderer.entity.RenderSnowball<>(
                                manager, net.minecraft.init.Items.FIRE_CHARGE,
                                net.minecraft.client.Minecraft.getMinecraft()
                                        .getRenderItem()));
        RenderMechanistMortar.register();
        TESRSingulator.register();
        // Револьвер: Java-модель через TEISR (школа Flan's Mod) — json
        // builtin/entity даёт только display-повороты.
        UTItems.fluxRevolver.setTileEntityItemStackRenderer(
                new RenderFluxRevolver());
        UTItems.fluxArquebus.setTileEntityItemStackRenderer(
                new RenderFluxArquebus());
        for (Block block : UTBlocks.all()) {
            Item item = Item.getItemFromBlock(block);
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(block.getRegistryName(), "inventory"));
        }
        for (Item item : UTItems.all()) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }
}
