package unboundtech.client.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.renderers.models.gear.ModelRobe;
import unboundtech.UnboundTech;

/**
 * Квант-Пустотная броня (путь А) — редизайн по директиве владельца:
 * база — ПУСТОТНАЯ роба ТК (наследуем {@link ModelRobe}: балахон,
 * капюшон, юбка-подол, наплечники — весь силуэт эндгейм-робы), поверх
 * крепятся квантовые части. Задача — МАКСИМАЛЬНО тяжёлый вид:
 * широченные двухъярусные плечи (шире робовских пластин), квант-обруч
 * на капюшоне с самоцветом, нагрудный модуль с лампами, ячейка на
 * спине, утяжелённый НИЗКИЙ подол до щиколоток, кованые сабатоны.
 *
 * Развёртки квант-частей лежат в свободных зонах родной плоскости
 * робы 128x64 (файл 256x128 — текстура генератора поверх копии
 * void_robe_armor.png). Толстая модель (1.0) — грудь и ботинки,
 * тонкая (0.5) — капюшон и ноги, как у ItemVoidRobeArmor порта.
 *
 * Урок фортресса учтён: поля ModelRobe package-private — свои боксы
 * вешаем child-ами на публичные bipedX; для слота FEET родные детали
 * ног робы гасим перебором childModels (иначе двойной рендер с
 * z-файтом при надетых поножах).
 */
@SideOnly(Side.CLIENT)
public class ModelQuantVoidArmour extends ModelRobe {

    private static final ResourceLocation[] GLOW = new ResourceLocation[8];

    static {
        for (int f = 0; f < 8; f++) {
            GLOW[f] = new ResourceLocation(UnboundTech.MODID,
                    "textures/models/armor/quant_void_glow_" + f + ".png");
        }
    }

    private final List<ModelRenderer> headParts = new ArrayList<>();
    private final List<ModelRenderer> chestParts = new ArrayList<>();
    private final List<ModelRenderer> legParts = new ArrayList<>();
    private final List<ModelRenderer> feetParts = new ArrayList<>();
    /** Свои части — чтобы отличать от родных детей робы. */
    private final Set<ModelRenderer> own = new HashSet<>();
    private boolean glowPass;

    public ModelQuantVoidArmour(float f) {
        super(f);

        // ===== капюшон: квант-обруч с самоцветом-третьим глазом =====
        ModelRenderer hoodRing = part(68, 52);
        hoodRing.addBox(-5.0F, -6.5F, -4.8F, 10, 2, 9);
        ModelRenderer gemBrow = part(8, 10);
        gemBrow.addBox(-1.0F, -7.4F, -5.7F, 2, 2, 1);
        group(this.bipedHead, this.headParts, hoodRing, gemBrow);

        // ===== ШИРОЧЕННЫЕ плечи: два яруса поверх робовских пластин ==
        ModelRenderer pauldronR = part(76, 0);
        pauldronR.addBox(-6.5F, -3.5F, -4.5F, 7, 4, 9);
        ModelRenderer pauldronTopR = part(44, 0);
        pauldronTopR.addBox(-7.0F, -5.1F, -3.0F, 9, 2, 6);
        group(this.bipedRightArm, this.chestParts, pauldronR, pauldronTopR);
        ModelRenderer pauldronL = part(76, 0);
        pauldronL.mirror = true;
        pauldronL.addBox(-0.5F, -3.5F, -4.5F, 7, 4, 9);
        ModelRenderer pauldronTopL = part(44, 0);
        pauldronTopL.mirror = true;
        pauldronTopL.addBox(-2.0F, -5.1F, -3.0F, 9, 2, 6);
        group(this.bipedLeftArm, this.chestParts, pauldronL, pauldronTopL);

        // ===== торс: нагрудный квант-модуль, лампы, ячейка спины =====
        ModelRenderer chestCore = part(0, 32);
        chestCore.addBox(-4.0F, 3.0F, -4.5F, 8, 5, 2);
        ModelRenderer lampR = part(16, 10);
        lampR.addBox(-3.0F, 4.2F, -5.0F, 1, 1, 1);
        ModelRenderer lampL = part(16, 10);
        lampL.addBox(2.0F, 4.2F, -5.0F, 1, 1, 1);
        ModelRenderer backCell = part(106, 52);
        backCell.addBox(-3.0F, 3.5F, 3.5F, 6, 6, 1);
        ModelRenderer beltNode = part(8, 0);
        beltNode.addBox(-1.5F, 9.5F, -3.8F, 3, 2, 1);
        group(this.bipedBody, this.chestParts,
                chestCore, lampR, lampL, backCell, beltNode);

        // ===== низкий халат: утяжелённый подол до щиколоток =====
        addSkirt(this.bipedRightLeg, false);
        addSkirt(this.bipedLeftLeg, true);

        // ===== кованые сабатоны =====
        addSabaton(this.bipedRightLeg, false);
        addSabaton(this.bipedLeftLeg, true);
    }

    private void addSkirt(ModelRenderer limb, boolean mirror) {
        float x = mirror ? 0.5F : -3.5F;
        ModelRenderer front = part(44, 8);
        front.mirror = mirror;
        front.addBox(x, 9.0F, -3.5F, 3, 4, 1);
        front.rotateAngleX = -0.12F;
        ModelRenderer back = part(44, 8);
        back.mirror = mirror;
        back.addBox(x, 9.0F, 2.5F, 3, 4, 1);
        back.rotateAngleX = 0.12F;
        ModelRenderer knee = part(36, 28);
        knee.mirror = mirror;
        knee.addBox(-2.5F, 4.5F, -3.3F, 5, 3, 1);
        group(limb, this.legParts, front, back, knee);
    }

    private void addSabaton(ModelRenderer limb, boolean mirror) {
        ModelRenderer cap = part(24, 32);
        cap.mirror = mirror;
        cap.addBox(-3.0F, 9.6F, -3.5F, 6, 3, 4);
        ModelRenderer heel = part(8, 4);
        heel.mirror = mirror;
        heel.addBox(-2.5F, 10.0F, 0.5F, 5, 2, 3);
        group(limb, this.feetParts, cap, heel);
    }

    private ModelRenderer part(int u, int v) {
        ModelRenderer m = new ModelRenderer(this, u, v);
        m.setTextureSize(128, 64);
        return m;
    }

    private void group(ModelRenderer limb, List<ModelRenderer> list,
                       ModelRenderer... parts) {
        for (ModelRenderer m : parts) {
            limb.addChild(m);
            list.add(m);
            this.own.add(m);
        }
    }

    /** Видимость по слоту; родная юбка робы — только для поножей. */
    public void prepareSlot(EntityEquipmentSlot slot) {
        boolean legs = slot == EntityEquipmentSlot.LEGS;
        for (ModelRenderer m : this.headParts) {
            m.showModel = slot == EntityEquipmentSlot.HEAD;
        }
        for (ModelRenderer m : this.chestParts) {
            m.showModel = slot == EntityEquipmentSlot.CHEST;
        }
        for (ModelRenderer m : this.legParts) {
            m.showModel = legs;
        }
        for (ModelRenderer m : this.feetParts) {
            m.showModel = slot == EntityEquipmentSlot.FEET;
        }
        // для FEET ноги робы включены (bipedLeg.showModel), но её родные
        // панели-полы там не нужны — их рисует тонкая модель поножей
        hideForeign(this.bipedRightLeg, legs);
        hideForeign(this.bipedLeftLeg, legs);
    }

    private void hideForeign(ModelRenderer limb, boolean visible) {
        if (limb.childModels == null) {
            return;
        }
        for (Object child : limb.childModels) {
            ModelRenderer m = (ModelRenderer) child;
            if (!this.own.contains(m)) {
                m.showModel = visible;
            }
        }
    }

    /** Рендер робы + эмиссив-проход квант-частей покадровым glow. */
    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount,
                       float ageInTicks, float netHeadYaw, float headPitch,
                       float scale) {
        super.render(entity, limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scale);
        if (this.glowPass) {
            return;
        }
        // глинт LayerArmorBase перерисовывает с depthFunc(EQUAL) — гейт
        if (org.lwjgl.opengl.GL11.glGetInteger(
                org.lwjgl.opengl.GL11.GL_DEPTH_FUNC)
                == org.lwjgl.opengl.GL11.GL_EQUAL) {
            return;
        }
        long time = entity != null && entity.world != null
                ? entity.world.getTotalWorldTime() : 0L;
        Minecraft.getMinecraft().getTextureManager()
                .bindTexture(GLOW[(int) (time / 10 % 8)]);
        float lastX = OpenGlHelper.lastBrightnessX;
        float lastY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                240.0F, 240.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        // роба сама разруливает sneak внутри render() — повторный вызов
        // super.render с glow-текстурой подсветит только узлы (файл
        // прозрачен вне светящихся развёрток), глубина совпадает
        this.glowPass = true;
        super.render(entity, limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scale);
        this.glowPass = false;
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                lastX, lastY);
    }
}
