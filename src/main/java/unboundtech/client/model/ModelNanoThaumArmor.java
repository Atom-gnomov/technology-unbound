package unboundtech.client.model;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.renderers.models.gear.ModelFortressArmor;

/**
 * Объёмная модель Нано-Таум брони v3 — по вердикту спора трёх арт-критиков
 * (workflow nano-thaum-style-debate):
 *
 *  - очки: НЕ свои боксы (первые линзы оказались пятнами на лице — их UV
 *    попадали в бипед-зону головы), а маска ПНВ IC2, натянутая текстурой на
 *    фортрессовский бокс Goggles — он уже есть в основе и сидит по месту;
 *  - паулдроны 6x4x5 с наклоном ±10° и латунной окантовкой верхней грани
 *    (п.2 вердикта: «читается свес, а не объём»; 7 в ширину не влезло в
 *    свободные UV-дыры развёртки);
 *  - лампы-терминалы на внешней стороне предплечий у запястья — зелёные,
 *    на концах жил (п.5); голубой на костюме — вето (п.0: только два
 *    эмиссивных цвета, зелёный IC2 и фиолетовый таум);
 *  - нагрудный таум-узел 1x1 — единственный фиолетовый эмиссив-акцент;
 *  - жгуты на груди и кабели рук — зелёная жила в тёмном кожухе;
 *  - сабатоны (у фортресс-брони ботинок нет вовсе) — нано-подложка с
 *    кованой пластиной; фортресс-панели ног при FEET гасятся через
 *    публичный {@code childModels};
 *  - лезвия предплечий отложены в v2 консенсусом критиков 3:0.
 *
 * ⚠️ UV нормализованы: доли ЕДИНОЙ плоскости 128x64, размер файла и
 * плоскости не трогаем (ломает ВСЕ старые боксы; выучено на белых руках
 * первой примерки). UV всех новых боксов — дыры вне бипед-зон И вне
 * фортресс-занятости, найдены двойным сканом альфы. Пиксельные патчи
 * рисует {@code tools/gen_nano_thaum.py} — координаты согласованы с U/V.
 */
@SideOnly(Side.CLIENT)
public class ModelNanoThaumArmor extends ModelFortressArmor {

    private static final float PAULDRON_TILT = 0.17F;   // ~10°, п.2 вердикта

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

        // --- торс: жгуты, подсумки, таум-узел ---
        // ⚠️ Урок третьей примерки: боксы бипеда раздуты параметром scale
        // (торс при 1.0 — до z=-3), а addBox без scale НЕ раздувает — деталь
        // с z=-2.8 оказывается ВНУТРИ брони и не видна («пробрешины»).
        // Все навесные детали посажены явно за раздутую поверхность.
        this.chestCableR = new ModelRenderer(this, 120, 0);
        this.chestCableR.addBox(-3.4F, 1.5F, -3.55F, 1, 6, 1);
        this.chestCableL = new ModelRenderer(this, 120, 0);
        this.chestCableL.addBox(2.4F, 1.5F, -3.55F, 1, 6, 1);
        this.pouchR = new ModelRenderer(this, 90, 8);
        this.pouchR.addBox(-3.2F, 8.4F, -3.55F, 2, 3, 1);
        this.pouchL = new ModelRenderer(this, 90, 8);
        this.pouchL.addBox(1.2F, 8.4F, -3.55F, 2, 3, 1);
        this.heartNode = new ModelRenderer(this, 120, 9);
        this.heartNode.addBox(-0.5F, 3.0F, -3.7F, 1, 1, 1);
        this.bipedBody.addChild(this.chestCableR);
        this.bipedBody.addChild(this.chestCableL);
        this.bipedBody.addChild(this.pouchR);
        this.bipedBody.addChild(this.pouchL);
        this.bipedBody.addChild(this.heartNode);

        // --- руки: паулдроны с наклоном, кабели, лампы-терминалы ---
        // паулдрон раздут scale-параметром: должен нависать над раздутым
        // плечом со всех сторон, как у космодесанта
        this.pauldronR = new ModelRenderer(this, 76, 21);
        this.pauldronR.addBox(-4.2F, -2.8F, -2.5F, 6, 4, 5, 0.9F);
        this.pauldronR.rotateAngleZ = PAULDRON_TILT;
        this.pauldronL = new ModelRenderer(this, 76, 21);
        this.pauldronL.mirror = true;
        this.pauldronL.addBox(-1.8F, -2.8F, -2.5F, 6, 4, 5, 0.9F);
        this.pauldronL.rotateAngleZ = -PAULDRON_TILT;
        // кабели по ВНЕШНЕЙ стороне: поверхность руки при 1.0 — x=±4
        this.armCableR = new ModelRenderer(this, 84, 8);
        this.armCableR.addBox(-4.55F, 1.0F, -0.5F, 1, 5, 1);
        this.armCableL = new ModelRenderer(this, 84, 8);
        this.armCableL.addBox(3.55F, 1.0F, -0.5F, 1, 5, 1);
        // лампы на КОНЦАХ кабелей, у запястья (п.5: терминал, не наклейка)
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

        // --- сабатоны: пластины ПОВЕРХ нано-бота (нога при 1.0 — до ±3,
        // без раздутия пластина тонула внутри и «ботинок не было») ---
        this.sabatonR = new ModelRenderer(this, 12, 39);
        this.sabatonR.addBox(-2.5F, 8.6F, -2.6F, 5, 3, 5, 1.15F);
        this.sabatonL = new ModelRenderer(this, 12, 39);
        this.sabatonL.mirror = true;
        this.sabatonL.addBox(-2.5F, 8.6F, -2.6F, 5, 3, 5, 1.15F);
        this.bipedRightLeg.addChild(this.sabatonR);
        this.bipedLeftLeg.addChild(this.sabatonL);
    }

    /**
     * Видимость своих деталей по слоту; для FEET дополнительно гасим
     * фортресс-панели ног, оставляя только сабатоны. Очки отдельной
     * видимости не требуют — фортресс-Goggles живут на bipedHead и гаснут
     * вместе с ним.
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

        this.sabatonR.showModel = feet;
        this.sabatonL.showModel = feet;
        this.limbChildren(this.bipedRightLeg, this.sabatonR, feet);
        this.limbChildren(this.bipedLeftLeg, this.sabatonL, feet);
    }

    /** При FEET на ноге виден только сабатон; иначе — всё, кроме него. */
    private void limbChildren(ModelRenderer limb, ModelRenderer ours, boolean feet) {
        if (limb.childModels == null) {
            return;
        }
        for (Object raw : limb.childModels) {
            ModelRenderer child = (ModelRenderer) raw;
            if (child == ours) {
                continue;
            }
            child.showModel = !feet;
        }
    }
}
