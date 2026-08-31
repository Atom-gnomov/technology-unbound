package unboundtech.client;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.UnboundTech;
import unboundtech.client.model.ModelMechanistMortar;
import unboundtech.common.tiles.TileMechanistMortar;

/**
 * TESR Мортиры (`docs/concepts/mechanist_mortar_model.md`): стандартный
 * флип translate(+0.5,+1.5,+0.5) + scale(1,-1,-1); клиент плавно доводит
 * yaw/pitch к серверным целям (лерп в тайле), откат ствола — кривая от
 * времени последнего выстрела, вентщели — флюкс-эмиссив fullbright.
 */
@SideOnly(Side.CLIENT)
public class RenderMechanistMortar extends TileEntitySpecialRenderer<TileMechanistMortar> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            UnboundTech.MODID, "textures/models/mechanist_mortar.png");

    private final ModelMechanistMortar model = new ModelMechanistMortar();

    @Override
    public void render(TileMechanistMortar tile, double x, double y, double z,
                       float partialTicks, int destroyStage, float alpha) {
        // плавный довод к серверным углам (~15%/кадр)
        tile.clientYaw += (wrapDelta(tile.getAimYaw() - tile.clientYaw)) * 0.15F;
        tile.clientPitch += (tile.getAimPitch() - tile.clientPitch) * 0.15F;

        // знаки выверены ревью №5/№6: флип scale(1,-1,-1) зеркалит Z и Y,
        // поэтому yaw получает +180°, а подъём ствола — ПОЛОЖИТЕЛЬНЫЙ угол
        this.model.yawGroup.rotateAngleY =
                (tile.clientYaw + 180.0F) * 0.017453292F;
        this.model.pitchGroup.rotateAngleX = tile.clientPitch * 0.017453292F;

        // откат: 12 тиков после выстрела, назад по оси трубы
        float recoil = 0.0F;
        if (tile.getWorld() != null) {
            long dt = tile.getWorld().getTotalWorldTime() - tile.lastShotTime;
            if (dt >= 0 && dt < 12) {
                float k = 1.0F - (dt + partialTicks) / 12.0F;
                recoil = k * k;
            }
        }
        this.model.barrelRecoil.offsetZ = -4.0F / 16.0F * recoil;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 1.5, z + 0.5);
        GlStateManager.scale(1.0F, -1.0F, -1.0F);
        this.bindTexture(TEXTURE);
        GlStateManager.enableCull();
        this.model.renderBody(1.0F / 32.0F);

        // эмиссивы: лампа авто-режима + вентщели (fullbright, ТЗ)
        float lastX = OpenGlHelper.lastBrightnessX;
        float lastY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                240.0F, 240.0F);
        GlStateManager.disableLighting();   // честный fullbright (ревью №12)
        if (!tile.isManual()) {
            this.model.renderLamp(1.0F / 32.0F);
        }
        float pulse = 0.75F + 0.25F * MathHelper.sin(
                (tile.getWorld() == null ? 0
                        : tile.getWorld().getTotalWorldTime() + partialTicks) * 0.15F);
        if (tile.ventsLit()) {   // §Состояния: пусто/нет EU — щели тёмные
            GlStateManager.color(0.64F * pulse, 0.43F * pulse,
                    0.91F * pulse, 1.0F);
            this.model.renderVents(1.0F / 32.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
        GlStateManager.enableLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                lastX, lastY);
        GlStateManager.popMatrix();
    }

    private static float wrapDelta(float degrees) {
        while (degrees > 180.0F) {
            degrees -= 360.0F;
        }
        while (degrees < -180.0F) {
            degrees += 360.0F;
        }
        return degrees;
    }

    public static void register() {
        net.minecraftforge.fml.client.registry.ClientRegistry
                .bindTileEntitySpecialRenderer(TileMechanistMortar.class,
                        new RenderMechanistMortar());
    }
}
