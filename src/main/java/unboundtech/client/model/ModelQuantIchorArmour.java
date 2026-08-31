package unboundtech.client.model;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.UnboundTech;

/**
 * Квант-Ихорная броня (путь Б) — редизайн по директиве владельца:
 * база — КВАНТ (подкладка из настоящих текстур IC2, основу видно),
 * поверх — 3D-воксели ткани Тинкерера фортресс-приёмом: полы робы,
 * задняя мантия, пояс-кушак, ШАРОВАРЫ на ногах, шапка-таблетка с
 * КОРОНОЙ-АНТЕННОЙ (шпиль + 4 зубца + самоцвет), панельки и самоцветы
 * на груди/поясе. По кванту ТЕЧЁТ ихор — бегущие дэши glow-кадров.
 *
 * Силуэт: стройный маг в шароварах и короне — антипод тяжёлого
 * балахона Квант-Пустоты ({@code ModelQuantVoidArmour}); разница
 * читается издалека (директива: «даже по силуэту»).
 *
 * Плоскость 128x64: зона 0..64x0..32 — стандартная развёртка брони
 * (туда генератор кладёт перекрашенный квант), правая половина и низ —
 * развёртки роб. Эмиссив — второй проход внутри render() покадровыми
 * glow-PNG (приём нано-таума: GL_EQUAL-гейт от глинта, lightmap
 * 240/240 с восстановлением, sneak-сдвиг 0.2).
 */
@SideOnly(Side.CLIENT)
public class ModelQuantIchorArmour extends ModelBiped {

    private static final ResourceLocation[] GLOW = new ResourceLocation[8];

    static {
        for (int f = 0; f < 8; f++) {
            GLOW[f] = new ResourceLocation(UnboundTech.MODID,
                    "textures/models/armor/quant_ichor_glow_" + f + ".png");
        }
    }

    // квант-подкладка
    private final ModelRenderer suitBody;
    private final ModelRenderer suitArmL;
    private final ModelRenderer suitArmR;
    private final ModelRenderer suitLegL;
    private final ModelRenderer suitLegR;
    // качающиеся полы робы
    private final ModelRenderer frontRobeR;
    private final ModelRenderer frontRobeL;
    private final ModelRenderer backRobe;
    // группы по слотам
    private final List<ModelRenderer> headParts = new ArrayList<>();
    private final List<ModelRenderer> chestParts = new ArrayList<>();
    private final List<ModelRenderer> legParts = new ArrayList<>();
    private final List<ModelRenderer> feetParts = new ArrayList<>();

    public ModelQuantIchorArmour(float scale) {
        super(scale, 0.0F, 128, 64);
        // родные кубы бипеда (infl = scale) хоронили подкладку и
        // детали (скептик №3) — поверхность задают ТОЛЬКО свои боксы
        this.bipedHead.cubeList.clear();
        this.bipedHeadwear.cubeList.clear();
        this.bipedBody.cubeList.clear();
        this.bipedRightArm.cubeList.clear();
        this.bipedLeftArm.cubeList.clear();
        this.bipedRightLeg.cubeList.clear();
        this.bipedLeftLeg.cubeList.clear();

        // ===== квант-подкладка: основу ВИДНО (директива) =====
        ModelRenderer suitHead = part(0, 0);
        suitHead.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, 1.0F);
        this.bipedHead.addChild(suitHead);
        this.headParts.add(suitHead);
        this.suitBody = part(16, 16);
        this.suitBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4,
                scale >= 1.0F ? 1.05F : 0.3F);
        this.bipedBody.addChild(this.suitBody);
        this.suitArmR = part(40, 16);
        this.suitArmR.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, 0.4F);
        this.bipedRightArm.addChild(this.suitArmR);
        this.suitArmL = part(40, 16);
        this.suitArmL.mirror = true;
        this.suitArmL.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, 0.4F);
        this.bipedLeftArm.addChild(this.suitArmL);
        float legInf = scale >= 1.0F ? 0.55F : 0.35F;
        this.suitLegR = part(0, 16);
        this.suitLegR.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, legInf);
        this.bipedRightLeg.addChild(this.suitLegR);
        this.suitLegL = part(0, 16);
        this.suitLegL.mirror = true;
        this.suitLegL.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, legInf);
        this.bipedLeftLeg.addChild(this.suitLegL);

        // ===== шапка-таблетка + корона-антенна =====
        ModelRenderer hat = part(64, 0);
        hat.addBox(-5.0F, -11.0F, -5.0F, 10, 2, 10);
        ModelRenderer spire = part(104, 0);
        spire.addBox(-0.5F, -15.0F, -0.5F, 1, 4, 1);
        ModelRenderer spireGem = part(110, 0);
        spireGem.addBox(-0.5F, -16.0F, -0.5F, 1, 1, 1);
        head(hat, spire, spireGem);
        for (int i = 0; i < 4; i++) {
            ModelRenderer prong = part(116, 0);
            prong.addBox((i & 1) == 0 ? -4.6F : 3.6F, -13.0F,
                    (i & 2) == 0 ? -4.6F : 3.6F, 1, 2, 1);
            head(prong);
        }

        // ===== робы на торсе: полы, мантия, кушак, панельки, самоцветы =
        this.frontRobeR = part(64, 14);
        this.frontRobeR.setRotationPoint(0.0F, 11.5F, -4.0F);
        this.frontRobeR.addBox(-3.8F, 0.0F, 0.0F, 3, 9, 1);
        this.frontRobeL = part(64, 14);
        this.frontRobeL.mirror = true;
        this.frontRobeL.setRotationPoint(0.0F, 11.5F, -4.0F);
        this.frontRobeL.addBox(0.8F, 0.0F, 0.0F, 3, 9, 1);
        this.backRobe = part(74, 14);
        this.backRobe.setRotationPoint(0.0F, 11.5F, 3.0F);
        this.backRobe.addBox(-4.0F, 0.0F, 0.0F, 8, 9, 1);
        ModelRenderer beltSash = part(64, 26);
        beltSash.addBox(-4.0F, 8.6F, -2.0F, 8, 2, 4, 1.3F);
        ModelRenderer chestPanelR = part(94, 14);
        chestPanelR.addBox(-3.6F, 2.6F, -3.4F, 3, 2, 1);
        ModelRenderer chestPanelL = part(94, 14);
        chestPanelL.mirror = true;
        chestPanelL.addBox(0.6F, 2.6F, -3.4F, 3, 2, 1);
        ModelRenderer gemChest = part(104, 14);
        gemChest.addBox(-1.0F, 5.2F, -3.5F, 2, 2, 1);
        ModelRenderer gemBelt = part(112, 14);
        gemBelt.addBox(-1.0F, 8.6F, -3.7F, 2, 2, 1);
        ModelRenderer backPanel = part(48, 44);
        backPanel.addBox(-1.0F, 9.4F, 2.7F, 2, 2, 1);
        body(this.frontRobeR, this.frontRobeL, this.backRobe, beltSash,
                chestPanelR, chestPanelL, gemChest, gemBelt, backPanel);

        // рукава: плечевые валики + манжеты
        ModelRenderer rollR = part(64, 34);
        rollR.addBox(-3.5F, -3.0F, -2.5F, 5, 2, 5, 0.8F);
        ModelRenderer cuffR = part(88, 26);
        cuffR.addBox(-3.5F, 7.6F, -2.5F, 5, 3, 5, 0.7F);
        armR(rollR, cuffR);
        ModelRenderer rollL = part(64, 34);
        rollL.mirror = true;
        rollL.addBox(-1.5F, -3.0F, -2.5F, 5, 2, 5, 0.8F);
        ModelRenderer cuffL = part(88, 26);
        cuffL.mirror = true;
        cuffL.addBox(-1.5F, 7.6F, -2.5F, 5, 3, 5, 0.7F);
        armL(rollL, cuffL);

        // ===== ШАРОВАРЫ: пышные штанины + манжета у колена =====
        ModelRenderer bloomR = part(0, 34);
        bloomR.addBox(-3.0F, 1.0F, -3.0F, 6, 6, 6, 0.75F);
        ModelRenderer kneeCuffR = part(26, 34);
        kneeCuffR.addBox(-2.5F, 7.2F, -2.5F, 5, 2, 5, 0.6F);
        leg(this.bipedRightLeg, bloomR, kneeCuffR);
        ModelRenderer bloomL = part(0, 34);
        bloomL.mirror = true;
        bloomL.addBox(-3.0F, 1.0F, -3.0F, 6, 6, 6, 0.75F);
        ModelRenderer kneeCuffL = part(26, 34);
        kneeCuffL.mirror = true;
        kneeCuffL.addBox(-2.5F, 7.2F, -2.5F, 5, 2, 5, 0.6F);
        leg(this.bipedLeftLeg, bloomL, kneeCuffL);

        // ботинки: квантовый носок
        ModelRenderer toeR = part(26, 44);
        toeR.addBox(-2.5F, 9.8F, -3.0F, 5, 2, 5, 0.9F);
        foot(this.bipedRightLeg, toeR);
        ModelRenderer toeL = part(26, 44);
        toeL.mirror = true;
        toeL.addBox(-2.5F, 9.8F, -3.0F, 5, 2, 5, 0.9F);
        foot(this.bipedLeftLeg, toeL);
    }

    private ModelRenderer part(int u, int v) {
        return new ModelRenderer(this, u, v);
    }

    private void head(ModelRenderer... parts) {
        for (ModelRenderer m : parts) {
            this.bipedHead.addChild(m);
            this.headParts.add(m);
        }
    }

    private void body(ModelRenderer... parts) {
        for (ModelRenderer m : parts) {
            this.bipedBody.addChild(m);
            this.chestParts.add(m);
        }
    }

    private void armR(ModelRenderer... parts) {
        for (ModelRenderer m : parts) {
            this.bipedRightArm.addChild(m);
            this.chestParts.add(m);
        }
    }

    private void armL(ModelRenderer... parts) {
        for (ModelRenderer m : parts) {
            this.bipedLeftArm.addChild(m);
            this.chestParts.add(m);
        }
    }

    private void leg(ModelRenderer limb, ModelRenderer... parts) {
        for (ModelRenderer m : parts) {
            limb.addChild(m);
            this.legParts.add(m);
        }
    }

    private void foot(ModelRenderer limb, ModelRenderer... parts) {
        for (ModelRenderer m : parts) {
            limb.addChild(m);
            this.feetParts.add(m);
        }
    }

    /** Видимость по слоту (все 4 комбинации, приём нано-таума). */
    public void prepareSlot(EntityEquipmentSlot slot) {
        boolean chest = slot == EntityEquipmentSlot.CHEST;
        boolean legs = slot == EntityEquipmentSlot.LEGS;
        boolean feet = slot == EntityEquipmentSlot.FEET;
        for (ModelRenderer m : this.headParts) {
            m.showModel = slot == EntityEquipmentSlot.HEAD;
        }
        for (ModelRenderer m : this.chestParts) {
            m.showModel = chest;
        }
        for (ModelRenderer m : this.legParts) {
            m.showModel = legs;
        }
        for (ModelRenderer m : this.feetParts) {
            m.showModel = feet;
        }
        this.suitArmL.showModel = chest;
        this.suitArmR.showModel = chest;
        this.suitBody.showModel = chest;
        this.suitLegL.showModel = legs || feet;
        this.suitLegR.showModel = legs || feet;
    }

    /** Полы робы качаются как у ModelRobe порта: min двух фаз шага. */
    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount,
                                  float ageInTicks, float netHeadYaw,
                                  float headPitch, float scaleFactor, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scaleFactor, entity);
        float a = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        float b = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI)
                * 1.4F * limbSwingAmount;
        float c = Math.min(a, b);
        this.frontRobeR.rotateAngleX = c - 0.1F;
        this.frontRobeL.rotateAngleX = c - 0.1F;
        this.backRobe.rotateAngleX = -c + 0.1F;
    }

    /** Рендер: база + «текущий ихор» — покадровый эмиссив-проход. */
    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount,
                       float ageInTicks, float netHeadYaw, float headPitch,
                       float scale) {
        super.render(entity, limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scale);
        this.renderGlow(entity, scale);
    }

    private void renderGlow(Entity entity, float scale) {
        // глинт-проход LayerArmorBase зовёт render() с depthFunc(EQUAL)
        // и своей текстурой — туда не соваться (урок ревью нано-таума)
        if (org.lwjgl.opengl.GL11.glGetInteger(
                org.lwjgl.opengl.GL11.GL_DEPTH_FUNC)
                == org.lwjgl.opengl.GL11.GL_EQUAL) {
            return;
        }
        long time = entity != null && entity.world != null
                ? entity.world.getTotalWorldTime() : 0L;
        Minecraft.getMinecraft().getTextureManager()
                .bindTexture(GLOW[(int) (time / 3 % 8)]);
        float lastX = OpenGlHelper.lastBrightnessX;
        float lastY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                240.0F, 240.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.pushMatrix();
        if (entity != null && entity.isSneaking()) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
        }
        // glow-файл прозрачен вне светящихся развёрток: перерисовка тех
        // же частей подсвечивает только дэши/самоцветы (приём нано)
        this.bipedHead.render(scale);
        this.bipedBody.render(scale);
        this.bipedRightArm.render(scale);
        this.bipedLeftArm.render(scale);
        this.bipedRightLeg.render(scale);
        this.bipedLeftLeg.render(scale);
        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                lastX, lastY);
    }
}
