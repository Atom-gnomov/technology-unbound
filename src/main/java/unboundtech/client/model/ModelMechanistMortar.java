package unboundtech.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Мортира Механистов — TESR-модель по ТЗ адверсарного воркфлоу
 * (`docs/concepts/mechanist_mortar_model.md`): тренога-краб, тумба-крест,
 * поворотный стол с цапфами и маховиком (yaw), короткий толстый ствол
 * вдоль +Z с бандажами и вентщелями (pitch поднимает к небу), откат по
 * оси ствола при выстреле.
 *
 * Удвоенные юниты (32 = 1 блок), рендер scale 1/32; высоты «от пола
 * вверх» по ТЗ пересчитаны в модельные Y с TESR-флипом scale(1,-1,-1):
 * модельный Y = 24 - высота_от_пола... нет: TESR транслирует на +1.5 и
 * флипует — здесь координаты заданы «пол = y 48, вверх = меньше», как
 * у ванильных моделей. Иерархия ног ОБЯЗАТЕЛЬНА (ТЗ): legN держит
 * rotateAngleY, child thighN — свой наклон.
 */
@SideOnly(Side.CLIENT)
public class ModelMechanistMortar extends ModelBase {

    private final ModelRenderer base;
    public final ModelRenderer yawGroup;
    public final ModelRenderer pitchGroup;
    /** Откат ствола: offsetZ по оси трубы (ставит TESR). */
    public final ModelRenderer barrelRecoil;
    public final ModelRenderer ventGlow;
    public final ModelRenderer autoLamp;

    public ModelMechanistMortar() {
        this.textureWidth = 128;
        this.textureHeight = 64;

        // ===== статичная станина (пол = y 48) =====
        this.base = new ModelRenderer(this, 0, 0);
        // тумба-крест
        this.base.setTextureOffset(0, 0).addBox(-7.0F, 32.0F, -5.0F, 14, 12, 10);
        this.base.setTextureOffset(0, 22).addBox(-5.0F, 32.0F, -7.0F, 10, 12, 14);
        // погон — латунное кольцо на талии
        this.base.setTextureOffset(48, 0).addBox(-8.0F, 30.0F, -8.0F, 16, 2, 16);
        // патронный короб с крышкой
        this.base.setTextureOffset(48, 18).addBox(7.0F, 36.0F, -2.5F, 8, 7, 5);
        this.base.setTextureOffset(48, 30).addBox(6.5F, 35.0F, -3.0F, 9, 1, 6);
        // кабель-ввод MV сзади
        this.base.setTextureOffset(48, 37).addBox(-2.0F, 40.0F, 7.0F, 4, 3, 3);
        // ноги-крабы: иерархия legN (yaw) -> thighN (наклон) + башмак
        for (int i = 0; i < 3; i++) {
            ModelRenderer leg = new ModelRenderer(this, 0, 0);
            leg.setRotationPoint(0.0F, 0.0F, 0.0F);
            leg.rotateAngleY = (float) (Math.PI * 2.0 / 3.0 * i);
            ModelRenderer thigh = new ModelRenderer(this, 96, 0);
            thigh.setRotationPoint(0.0F, 38.0F, 5.0F);
            thigh.rotateAngleX = 0.44F;   // 25° наружу (знак — ревью №7)
            thigh.addBox(-2.0F, 0.0F, -2.5F, 4, 12, 5);
            leg.addChild(thigh);
            ModelRenderer boot = new ModelRenderer(this, 96, 18);
            boot.addBox(-3.5F, 45.0F, 9.0F, 7, 3, 9);
            leg.addChild(boot);
            this.base.addChild(leg);
        }
        // зелёная лампа авто-режима над коробом
        // UV-переезды ревью №11: (120,0), (120,8), (0,48) вылезали за 128x64
        this.autoLamp = new ModelRenderer(this, 0, 58);
        this.autoLamp.addBox(9.0F, 34.0F, -2.0F, 4, 1, 4);

        // ===== yaw: поворотный стол, цапфы, маховик (пивот центр, y 30) ==
        this.yawGroup = new ModelRenderer(this, 0, 0);
        this.yawGroup.setRotationPoint(0.0F, 30.0F, 0.0F);
        this.yawGroup.setTextureOffset(64, 18).addBox(-8.0F, -3.5F, -7.0F, 16, 3, 14);
        // цапфы-щёки: внутренние грани на ±6 — труба 10 проходит
        this.yawGroup.setTextureOffset(60, 46).addBox(6.0F, -12.0F, -4.0F, 3, 9, 8);
        this.yawGroup.setTextureOffset(60, 46).addBox(-9.0F, -12.0F, -4.0F, 3, 9, 8);
        // латунный маховик наведения на правой щеке
        this.yawGroup.setTextureOffset(104, 46).addBox(9.0F, -9.5F, -0.5F, 1, 5, 5);
        this.yawGroup.setTextureOffset(104, 46).addBox(10.0F, -7.5F, 0.5F, 2, 1, 1);

        // ===== pitch: ствол вдоль +Z (пивот на оси цапф, y -8 отн. yaw) =
        this.pitchGroup = new ModelRenderer(this, 0, 0);
        this.pitchGroup.setRotationPoint(0.0F, -8.0F, 0.0F);
        this.yawGroup.addChild(this.pitchGroup);
        this.barrelRecoil = new ModelRenderer(this, 0, 0);
        // труба 10x10, короткая и толстая
        this.barrelRecoil.setTextureOffset(0, 0).addBox(-5.0F, -5.0F, -6.0F, 10, 10, 18);
        // казённик чуть шире сзади
        this.barrelRecoil.setTextureOffset(56, 0).addBox(-5.5F, -5.5F, -9.0F, 11, 11, 3);
        // бандажи-кольца
        this.barrelRecoil.setTextureOffset(48, 0).addBox(-5.5F, -5.5F, 2.0F, 11, 11, 2);
        this.barrelRecoil.setTextureOffset(48, 0).addBox(-5.5F, -5.5F, 8.0F, 11, 11, 2);
        // дульный срез — латунная кромка жерла
        this.barrelRecoil.setTextureOffset(48, 14).addBox(-5.5F, -5.5F, 11.0F, 11, 11, 1);
        this.pitchGroup.addChild(this.barrelRecoil);
        // вентщели у казённой части — флюкс-эмиссив, виден СБОКУ (ТЗ)
        this.ventGlow = new ModelRenderer(this, 96, 36);
        this.ventGlow.addBox(-5.5F, -3.0F, -5.0F, 11, 6, 4);
        // не child ствола: рендерится вторым проходом с тем же трансформом
    }

    /** База + подвижные группы; углы уже выставлены TESR-ом. */
    public void renderBody(float scale) {
        this.base.render(scale);
        this.yawGroup.render(scale);
    }

    /** Лампа авто-режима (эмиссив, showModel ставит TESR). */
    public void renderLamp(float scale) {
        this.autoLamp.render(scale);
    }

    /**
     * Вентщели: повторяем трансформ yaw→pitch→recoil вручную — бокс не
     * child, чтобы база не рисовала его тёмным (щель только светится).
     */
    public void renderVents(float scale) {
        this.ventGlow.rotationPointX = this.yawGroup.rotationPointX;
        this.ventGlow.rotationPointY = this.yawGroup.rotationPointY;
        this.ventGlow.rotationPointZ = this.yawGroup.rotationPointZ;
        this.ventGlow.rotateAngleY = this.yawGroup.rotateAngleY;
        // компоновка углов: вент сидит на стволе → складываем pitch
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        this.yawGroup.postRender(scale);
        this.pitchGroup.postRender(scale);
        net.minecraft.client.renderer.GlStateManager.translate(
                this.barrelRecoil.offsetX, this.barrelRecoil.offsetY,
                this.barrelRecoil.offsetZ);
        ModelRenderer local = this.ventGlow;
        float rx = local.rotationPointX;
        float ry = local.rotationPointY;
        float rz = local.rotationPointZ;
        float ay = local.rotateAngleY;
        local.setRotationPoint(0.0F, 0.0F, 0.0F);
        local.rotateAngleY = 0.0F;
        local.render(scale);
        local.setRotationPoint(rx, ry, rz);
        local.rotateAngleY = ay;
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }
}
