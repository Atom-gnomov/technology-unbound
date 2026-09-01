package unboundtech.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.common.tiles.TileSingulator;

/**
 * Сингулятор (`singulator.md` §8): единственный блок мода, в котором
 * ВИДНО вставленный предмет — жезл стоит вертикально в вилке-держателе
 * и медленно проворачивается, пока идёт зарядка.
 */
@SideOnly(Side.CLIENT)
public class TESRSingulator extends TileEntitySpecialRenderer<TileSingulator> {

    public static void register() {
        net.minecraftforge.fml.client.registry.ClientRegistry
                .bindTileEntitySpecialRenderer(TileSingulator.class,
                        new TESRSingulator());
    }

    @Override
    public void render(TileSingulator tile, double x, double y, double z,
                       float partialTicks, int destroyStage, float alpha) {
        ItemStack wand = tile.getWand();
        if (wand.isEmpty()) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 1.05, z + 0.5);
        long time = tile.getWorld() != null
                ? tile.getWorld().getTotalWorldTime() : 0L;
        // проворот только пока идёт зарядка (§8) — по ACTIVE блокстейта
        net.minecraft.block.state.IBlockState state = tile.getWorld() == null
                ? null : tile.getWorld().getBlockState(tile.getPos());
        boolean charging = state != null && state.getProperties()
                .containsKey(unboundtech.common.blocks.BlockMachineBase.ACTIVE)
                && state.getValue(unboundtech.common.blocks.BlockMachineBase.ACTIVE);
        float angle = charging ? (time + partialTicks) * 0.8F
                : time % 450 * 0.8F;
        GlStateManager.rotate(angle, 0.0F, 1.0F, 0.0F);
        // жезл в предметной иконке нарисован по диагонали — доворот 45°
        // вокруг Z ставит его вертикально в вилке
        GlStateManager.rotate(45.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.scale(0.7F, 0.7F, 0.7F);
        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItem(wand,
                ItemCameraTransforms.TransformType.FIXED);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }
}
