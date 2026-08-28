package unboundtech.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.UnboundTech;
import unboundtech.client.model.ModelFluxRevolver;
import unboundtech.common.items.ItemFluxRevolver;

/**
 * TEISR револьвера — приём Flan's Mod: предмет рисует Java-модель, а не
 * запечённый json. Барабан провёрнут по числу оставшихся патронов
 * (после каждого выстрела — на одну камору), светятся только заряженные
 * гнёзда. Display-повороты в руках берутся из json builtin/entity.
 */
@SideOnly(Side.CLIENT)
public class RenderFluxRevolver extends TileEntityItemStackRenderer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            UnboundTech.MODID, "textures/models/flux_revolver.png");

    private final ModelFluxRevolver model = new ModelFluxRevolver();

    @Override
    public void renderByItem(ItemStack stack, float partialTicks) {
        int count = ItemFluxRevolver.ammoCount(stack);
        this.model.litChambers = count;
        // каждая израсходованная камора — минус 60° проворота
        this.model.drumAngle = (float) Math.toRadians(
                (ItemFluxRevolver.DRUM_SIZE - count) * 60.0);

        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5F, 0.45F, 0.5F);
        // ModelRenderer растёт вниз по Y — переворот, как у любых моделей
        GlStateManager.scale(1.0F, -1.0F, -1.0F);
        GlStateManager.enableCull();
        // модель построена в удвоенных юнитах — масштаб вдвое мельче
        this.model.renderGun(1.0F / 32.0F);
        GlStateManager.popMatrix();
    }
}
