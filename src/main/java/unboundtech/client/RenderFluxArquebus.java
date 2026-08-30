package unboundtech.client;

import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.UnboundTech;
import unboundtech.client.model.ModelFluxArquebus;
import unboundtech.common.entities.EntityFluxBullet;
import unboundtech.common.items.ItemFluxArquebus;

/**
 * TEISR Флюкс-Аркебузы — по ТЗ `docs/concepts/flux_arquebus_model.md`:
 *
 *  - отдача и падение серпентина едут от кулдаун-кривой предмета
 *    (getCooldown с partialTicks) — ни байта NBT (ТЗ §2);
 *  - окно перезарядки — клиентский WeakHashMap (ТЗ §3), шомпол ходит
 *    синусоидой 8 юнитов, серпентин взводится в конце окна;
 *  - анимации только в руках (контекст пишет перспектив-обёртка из
 *    UTModels), fullbright-проходы — везде (ТЗ §1);
 *  - уголёк мерцает с null-check мира (ТЗ §6); после каждого
 *    fullbright-прохода — сброс color и lightmap (ТЗ §5);
 *  - штык: рендер ВЛОЖЕННОГО меча из NBT UTBayonet у хомута.
 */
@SideOnly(Side.CLIENT)
public class RenderFluxArquebus extends TileEntityItemStackRenderer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            UnboundTech.MODID, "textures/models/flux_arquebus.png");

    /** Контекст рендера; пишется PerspectiveCapture до вызова TEISR. */
    public static ItemCameraTransforms.TransformType context =
            ItemCameraTransforms.TransformType.NONE;

    /** ТЗ §3: старт окна перезарядки, только клиент. */
    private static final WeakHashMap<EntityPlayer, Long> RELOAD_START =
            new WeakHashMap<>();

    private static final int RELOAD_WINDOW = ItemFluxArquebus.RELOAD_TICKS;
    private static final float SERPENTINE_COCKED = -0.9F;

    private final ModelFluxArquebus model = new ModelFluxArquebus();

    public static void noteReload(EntityPlayer player) {
        if (player.world != null) {
            RELOAD_START.put(player, player.world.getTotalWorldTime());
        }
    }

    @Override
    public void renderByItem(ItemStack stack, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        boolean inHand = context == ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND
                || context == ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND
                || context == ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND
                || context == ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND;

        boolean loaded = ItemFluxArquebus.isLoaded(stack);
        float time = mc.world != null
                ? mc.world.getTotalWorldTime() + partialTicks : 0.0F;

        // --- состояние анимаций ---
        float recoil = 0.0F;
        float serpentineAngle = loaded ? SERPENTINE_COCKED : 0.0F;
        float ramrodOffset = 0.0F;
        if (inHand && player != null && mc.world != null) {
            Long start = RELOAD_START.get(player);
            long now = mc.world.getTotalWorldTime();
            boolean reloading = start != null && now - start < RELOAD_WINDOW;
            if (reloading) {
                float p = (now - start + partialTicks) / RELOAD_WINDOW;
                // шомпол: ход 8 юнитов при p 0.2..0.8 (ТЗ, вариант А)
                if (p > 0.2F && p < 0.8F) {
                    ramrodOffset = -8.0F / 32.0F
                            * (float) Math.sin(Math.PI * (p - 0.2F) / 0.6F);
                }
                // серпентин: последние 8 тиков — плавный взвод
                float cockStart = 1.0F - 8.0F / RELOAD_WINDOW;
                serpentineAngle = p < cockStart ? 0.0F
                        : SERPENTINE_COCKED * (p - cockStart) / (1.0F - cockStart);
            } else {
                float cd = player.getCooldownTracker()
                        .getCooldown(stack.getItem(), partialTicks);
                if (cd > 0.0F) {
                    recoil = cd * cd;   // §Анимации: кривая 1→0 за 12 тиков
                    // серпентин падает на полку за первые 3 тика
                    float s = Math.min(1.0F, (1.0F - cd) * 4.0F);
                    serpentineAngle = loaded ? SERPENTINE_COCKED
                            : SERPENTINE_COCKED * (1.0F - s) * 0.0F;
                }
            }
        }
        this.model.serpentine.rotateAngleZ = serpentineAngle;
        this.model.ramrod.offsetX = ramrodOffset;
        this.model.glowTop.showModel = loaded;
        this.model.glowSide.showModel = loaded;

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5F, 0.45F, 0.5F);
        if (recoil > 0.0F) {
            // отдача: назад по стволу + задирание до 12°
            GlStateManager.translate(0.12F * recoil, 0.0F, 0.0F);
            GlStateManager.rotate(-12.0F * recoil, 0.0F, 0.0F, 1.0F);
        }
        mc.getTextureManager().bindTexture(TEXTURE);
        GlStateManager.enableCull();
        this.model.renderBody(1.0F / 32.0F);

        // --- fullbright-проходы (ТЗ §5: сбрасывать color и lightmap) ---
        float lastX = OpenGlHelper.lastBrightnessX;
        float lastY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                240.0F, 240.0F);
        // уголёк: мерцание того же оранжевого (ТЗ §6: null-check времени)
        float flicker = 0.6F + 0.15F * (float) Math.sin(time * 0.3F);
        GlStateManager.color(1.0F, flicker, 0.2F, 1.0F);
        this.model.renderMatchTip(1.0F / 32.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (loaded) {
            int type = ItemFluxArquebus.ammoType(stack);
            float[] tint = glowTint(type);
            GlStateManager.color(tint[0], tint[1], tint[2], 1.0F);
            this.model.renderChamberGlow(1.0F / 32.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                lastX, lastY);

        // --- штык: вложенный меч из NBT у хомута (канон §8 штыка) ---
        ItemStack bayonet = ItemFluxArquebus.bayonet(stack);
        if (!bayonet.isEmpty()) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(-44.0F / 32.0F, 0.0F, 3.0F / 32.0F);
            GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-45.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.scale(0.6F, 0.6F, 0.6F);
            mc.getRenderItem().renderItem(bayonet,
                    ItemCameraTransforms.TransformType.FIXED);
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();
        context = ItemCameraTransforms.TransformType.NONE;
    }

    /** Цвета щелей по типу патрона (ТЗ §Состояния). */
    private static float[] glowTint(int type) {
        switch (type) {
            case EntityFluxBullet.TYPE_ILLUMINATING:
                return new float[]{1.0F, 1.0F, 0.92F};
            case EntityFluxBullet.TYPE_VIS:
                return new float[]{0.77F, 0.57F, 0.91F};
            case EntityFluxBullet.TYPE_FLUX:
                return new float[]{0.42F, 0.48F, 0.18F};
            default:
                return new float[]{1.0F, 0.55F, 0.18F};
        }
    }
}
