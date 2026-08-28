package unboundtech.client.model;

import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.renderers.models.gear.ModelFortressArmor;

/**
 * Объёмная модель Нано-Таум брони v5.
 *
 * Урок четвёртой примерки (голое лицо и кисти при 100%-но залитой
 * текстуре): фортресс-модель ЧИСТИТ cubeList всех базовых частей бипеда
 * и рендерит только накладки, а её {@code render()} прячет очки, маски,
 * наплечники и орнаменты по NBT фортресс-шлема, которого у нас нет.
 * Отсюда два решения:
 *
 *  - НАНО-ПОДДОСПЕШНИК: свои боксы головы/торса/рук/ног со стандартными
 *    бипед-UV — полнотелая броня (решение владельца), рукава до кистей;
 *    раздутие ног/торса зависит от масштаба модели, чтобы штаны (тонкая)
 *    и ботинок (толстая) не z-файтились: штанина 0.35 → бот 0.55 →
 *    пластины сабатона 0.85 — заодно ступенчатый переход к ботинкам;
 *  - свой {@code render()} БЕЗ фортресс-прятанья: полный фортресс-набор
 *    (свиток, книга, самоцвет, ремешки) виден всегда, ПНВ-маска на боксе
 *    Goggles показана, лицевые маски скрыты, фортресс-наплечники скрыты —
 *    их заменяет наш латунно-таумиевый паулдрон.
 *
 * Поля порта package-private, поэтому накладки различаются по геометрии
 * первого куба в публичном {@code childModels}: маски — z1 = -4.6,
 * фортресс-наплечники — единственные детали рук глубиной 7.
 *
 * UV всех своих боксов — дыры вне бипед-зон и фортресс-занятости;
 * пиксельные патчи рисует {@code tools/gen_nano_thaum.py}.
 */
@SideOnly(Side.CLIENT)
public class ModelNanoThaumArmor extends ModelFortressArmor {

    private static final float PAULDRON_TILT = 0.17F;   // ~10°, п.2 вердикта

    private final ModelRenderer suitHead;
    private final ModelRenderer suitBody;
    private final ModelRenderer suitArmR;
    private final ModelRenderer suitArmL;
    private final ModelRenderer suitLegR;
    private final ModelRenderer suitLegL;
    private final ModelRenderer chestCableR;
    private final ModelRenderer chestCableL;
    private final ModelRenderer pouchR;
    private final ModelRenderer pouchL;
    private final ModelRenderer heartNode;
    private final ModelRenderer pauldronR;
    private final ModelRenderer pauldronL;
    private final ModelRenderer armCableR;
    private final ModelRenderer armCableL;
    private final ModelRenderer lampR;
    private final ModelRenderer lampL;
    private final ModelRenderer sabatonR;
    private final ModelRenderer sabatonL;

    public ModelNanoThaumArmor(float scale) {
        super(scale);

        // --- нано-поддоспешник: боксы бипеда, вычищенные фортрессом ---
        // Голова: под капюшоном (±4.5) и ПНВ (z=-4.25), над кожей (±4.0).
        this.suitHead = new ModelRenderer(this, 0, 0);
        this.suitHead.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, 0.1F);
        this.bipedHead.addChild(this.suitHead);
        // Торс: раздутие от масштаба — нагрудник (0.55) не бьётся с поясом
        // поножей (0.3); пояса Belt* (x=±5) и нагрудник (z=-4) остаются выше.
        this.suitBody = new ModelRenderer(this, 16, 16);
        this.suitBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, scale * 0.5F + 0.05F);
        this.bipedBody.addChild(this.suitBody);
        // Рукава до кистей: под фортресс-наплечником ShoulderR (±2.5).
        this.suitArmR = new ModelRenderer(this, 40, 16);
        this.suitArmR.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, 0.4F);
        this.bipedRightArm.addChild(this.suitArmR);
        this.suitArmL = new ModelRenderer(this, 40, 16);
        this.suitArmL.mirror = true;
        this.suitArmL.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, 0.4F);
        this.bipedLeftArm.addChild(this.suitArmL);
        // Ноги: штанина (тонкая, 0.35) над слоем скина (0.25) и под
        // фортресс-панелями (±2.5); ботинок (толстая, 0.55) поверх штанины.
        float legInf = scale * 0.4F + 0.15F;
        this.suitLegR = new ModelRenderer(this, 0, 16);
        this.suitLegR.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, legInf);
        this.bipedRightLeg.addChild(this.suitLegR);
        this.suitLegL = new ModelRenderer(this, 0, 16);
        this.suitLegL.mirror = true;
        this.suitLegL.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, legInf);
        this.bipedLeftLeg.addChild(this.suitLegL);

        // --- торс: жгуты, подсумки, таум-узел ---
        // Кабели и узел выходят ЗА фортресс-нагрудник (его фронт z=-4),
        // подсумки — за пояс Mbelt (z=-3); утопленная деталь не видна.
        this.chestCableR = new ModelRenderer(this, 120, 0);
        this.chestCableR.addBox(-3.4F, 1.5F, -4.3F, 1, 6, 1);
        this.chestCableL = new ModelRenderer(this, 120, 0);
        this.chestCableL.addBox(2.4F, 1.5F, -4.3F, 1, 6, 1);
        this.pouchR = new ModelRenderer(this, 90, 8);
        this.pouchR.addBox(-3.2F, 8.4F, -3.55F, 2, 3, 1);
        this.pouchL = new ModelRenderer(this, 90, 8);
        this.pouchL.addBox(1.2F, 8.4F, -3.55F, 2, 3, 1);
        this.heartNode = new ModelRenderer(this, 120, 9);
        this.heartNode.addBox(-0.5F, 3.0F, -4.5F, 1, 1, 1);
        this.bipedBody.addChild(this.chestCableR);
        this.bipedBody.addChild(this.chestCableL);
        this.bipedBody.addChild(this.pouchR);
        this.bipedBody.addChild(this.pouchL);
        this.bipedBody.addChild(this.heartNode);

        // --- руки: паулдроны с наклоном, кабели, лампы-терминалы ---
        this.pauldronR = new ModelRenderer(this, 76, 21);
        this.pauldronR.addBox(-4.2F, -2.8F, -2.5F, 6, 4, 5, 0.9F);
        this.pauldronR.rotateAngleZ = PAULDRON_TILT;
        this.pauldronL = new ModelRenderer(this, 76, 21);
        this.pauldronL.mirror = true;
        this.pauldronL.addBox(-1.8F, -2.8F, -2.5F, 6, 4, 5, 0.9F);
        this.pauldronL.rotateAngleZ = -PAULDRON_TILT;
        this.armCableR = new ModelRenderer(this, 84, 8);
        this.armCableR.addBox(-4.55F, 1.0F, -0.5F, 1, 5, 1);
        this.armCableL = new ModelRenderer(this, 84, 8);
        this.armCableL.addBox(3.55F, 1.0F, -0.5F, 1, 5, 1);
        this.lampR = new ModelRenderer(this, 114, 8);
        this.lampR.addBox(-4.7F, 6.4F, -0.5F, 1, 1, 1);
        this.lampL = new ModelRenderer(this, 114, 8);
        this.lampL.addBox(3.7F, 6.4F, -0.5F, 1, 1, 1);
        this.bipedRightArm.addChild(this.pauldronR);
        this.bipedRightArm.addChild(this.armCableR);
        this.bipedRightArm.addChild(this.lampR);
        this.bipedLeftArm.addChild(this.pauldronL);
        this.bipedLeftArm.addChild(this.armCableL);
        this.bipedLeftArm.addChild(this.lampL);

        // --- сабатоны: пластины поверх нано-бота, ступень 0.55 → 0.85 ---
        this.sabatonR = new ModelRenderer(this, 12, 39);
        this.sabatonR.addBox(-2.5F, 8.6F, -2.6F, 5, 3, 5, 0.85F);
        this.sabatonL = new ModelRenderer(this, 12, 39);
        this.sabatonL.mirror = true;
        this.sabatonL.addBox(-2.5F, 8.6F, -2.6F, 5, 3, 5, 0.85F);
        this.bipedRightLeg.addChild(this.sabatonR);
        this.bipedLeftLeg.addChild(this.sabatonL);

        // Фортресс-прятанье отменяем один раз здесь: наш render() его
        // больше не вызывает, флаги никто не перетирает.
        unhideFortress(this.bipedHead, true, false);
        unhideFortress(this.bipedBody, false, false);
        unhideFortress(this.bipedRightArm, false, true);
        unhideFortress(this.bipedLeftArm, false, true);
        unhideFortress(this.bipedRightLeg, false, false);
        unhideFortress(this.bipedLeftLeg, false, false);
    }

    /**
     * Показывает фортресс-накладки, которые {@code render()} порта прячет
     * без NBT фортресс-шлема. Скрытыми остаются лицевые маски (кубы головы
     * с z1=-4.6 — их место занял ПНВ) и фортресс-наплечники (единственные
     * детали рук глубиной 7 — их заменяет наш паулдрон).
     */
    private static void unhideFortress(ModelRenderer part, boolean head, boolean arm) {
        if (part.childModels == null) {
            return;
        }
        for (Object raw : part.childModels) {
            ModelRenderer child = (ModelRenderer) raw;
            if (child.cubeList == null || child.cubeList.isEmpty()) {
                continue;
            }
            ModelBox box = (ModelBox) child.cubeList.get(0);
            boolean mask = head && box.posZ1 < -4.5F && box.posZ1 > -4.7F;
            // строго 7.0: наш паулдрон при раздутии 0.9 имеет глубину 6.8
            boolean plate = arm && box.posZ2 - box.posZ1 > 6.9F;
            child.isHidden = mask || plate;
        }
    }

    /**
     * Рендер без фортресс-прятанья (его логика завязана на NBT чужого
     * предмета). Повторяет геометрию отрисовки порта: голова с масштабом
     * 1.01, детский вариант — с даунскейлом.
     */
    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount,
                       float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scale, entity);
        if (this.isChild) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.75F, 0.75F, 0.75F);
            GlStateManager.translate(0.0F, 16.0F * scale, 0.0F);
            this.bipedHead.render(scale);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
            this.bipedBody.render(scale);
            this.bipedRightArm.render(scale);
            this.bipedLeftArm.render(scale);
            this.bipedRightLeg.render(scale);
            this.bipedLeftLeg.render(scale);
            this.bipedHeadwear.render(scale);
            GlStateManager.popMatrix();
        } else {
            GlStateManager.pushMatrix();
            GlStateManager.scale(1.01F, 1.01F, 1.01F);
            this.bipedHead.render(scale);
            GlStateManager.popMatrix();
            this.bipedBody.render(scale);
            this.bipedRightArm.render(scale);
            this.bipedLeftArm.render(scale);
            this.bipedRightLeg.render(scale);
            this.bipedLeftLeg.render(scale);
            this.bipedHeadwear.render(scale);
        }
    }

    /**
     * Видимость своих деталей по слоту; для FEET дополнительно гасим
     * фортресс-панели ног, оставляя нано-бот и пластины сабатона.
     */
    public void prepareSlot(EntityEquipmentSlot slot) {
        boolean chest = slot == EntityEquipmentSlot.CHEST;
        boolean feet = slot == EntityEquipmentSlot.FEET;

        this.chestCableR.showModel = chest;
        this.chestCableL.showModel = chest;
        this.pouchR.showModel = chest;
        this.pouchL.showModel = chest;
        this.heartNode.showModel = chest;
        this.pauldronR.showModel = chest;
        this.pauldronL.showModel = chest;
        this.armCableR.showModel = chest;
        this.armCableL.showModel = chest;
        this.lampR.showModel = chest;
        this.lampL.showModel = chest;
        this.suitArmR.showModel = chest;
        this.suitArmL.showModel = chest;

        this.sabatonR.showModel = feet;
        this.sabatonL.showModel = feet;
        this.limbChildren(this.bipedRightLeg, this.sabatonR, this.suitLegR, feet);
        this.limbChildren(this.bipedLeftLeg, this.sabatonL, this.suitLegL, feet);
    }

    /** При FEET на ноге видны только нано-бот и сабатон; иначе — всё, кроме сабатона. */
    private void limbChildren(ModelRenderer limb, ModelRenderer plate,
                              ModelRenderer boot, boolean feet) {
        if (limb.childModels == null) {
            return;
        }
        for (Object raw : limb.childModels) {
            ModelRenderer child = (ModelRenderer) raw;
            if (child == plate) {
                continue;
            }
            child.showModel = child == boot || !feet;
        }
    }
}
