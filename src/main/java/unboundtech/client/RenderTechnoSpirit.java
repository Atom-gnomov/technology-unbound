package unboundtech.client;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.UnboundTech;
import unboundtech.client.model.ModelTechnoSpirit;
import unboundtech.common.entities.EntityTechnoSpirit;

/**
 * Рендер техно-духа: уменьшенный (§4.2: 0.5 от оригинала), полупрозрачный
 * (alpha ≈ 0.6) и с фиолетовым отливом — «узнаваемый силуэт, который
 * движется неправильно и просвечивает».
 */
@SideOnly(Side.CLIENT)
public class RenderTechnoSpirit extends RenderLiving<EntityTechnoSpirit> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            UnboundTech.MODID, "textures/entity/techno_spirit.png");

    public RenderTechnoSpirit(RenderManager manager) {
        super(manager, new ModelTechnoSpirit(), 0.2f);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityTechnoSpirit entity) {
        return TEXTURE;
    }

    @Override
    protected void preRenderCallback(EntityTechnoSpirit entity, float partialTicks) {
        GlStateManager.scale(0.5f, 0.5f, 0.5f);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(0.85f, 0.7f, 1.0f, 0.6f);
    }
}
