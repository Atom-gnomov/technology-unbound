package unboundtech.client.model;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.renderers.models.gear.ModelFortressArmor;

/**
 * Объёмная модель Нано-Таум брони: фортресс-основа порта плюс наши
 * нано-детали (запрос владельца, ориентир — силовая броня космодесанта):
 *
 *  - очки ПНВ: две линзы с перемычкой на лбу, зелёное свечение;
 *  - жгуты-провода на груди и кабели по внешней стороне рук (голубое
 *    свечение — «электрическая» половина пары цветов §8.2);
 *  - паулдроны: массивные накладки поверх фортресс-наплечников;
 *  - подсумки на поясе;
 *  - сабатоны: ботинки из нано-подложки, обложенные таум-пластинами —
 *    у самой фортресс-брони ботинок НЕТ (шлем/торс/поножи), поэтому
 *    прежний прототип на ноги ничего не рисовал.
 *
 * Все новые боксы — children стандартных частей {@code ModelBiped}, их UV
 * лежат в НИЖНЕЙ половине текстуры: после {@code super()} расширяем
 * {@code textureHeight} 64 → 128, старые боксы фортресс уже посчитаны по
 * 128×64 и не сдвигаются, новые считаются по 128×128. Пиксельные зоны
 * патчей рисует {@code tools/gen_nano_thaum.py} — координаты согласованы
 * с константами U/V здесь.
 *
 * Фортресс-панели ног (package-private поля порта) при слоте FEET гасятся
 * через публичный {@code childModels} — иначе ботинки рендерились бы с
 * чужими наколенниками.
 */
@SideOnly(Side.CLIENT)
public class ModelNanoThaumArmor extends ModelFortressArmor {

    private final ModelRenderer goggleR;
    private final ModelRenderer goggleL;
    private final ModelRenderer goggleBridge;
    private final ModelRenderer chestCableR;
    private final ModelRenderer chestCableL;
    private final ModelRenderer pouchR;
    private final ModelRenderer pouchL;
    private final ModelRenderer pauldronR;
    private final ModelRenderer pauldronL;
    private final ModelRenderer armCableR;
    private final ModelRenderer armCableL;
    private final ModelRenderer sabatonR;
    private final ModelRenderer sabatonL;

    public ModelNanoThaumArmor(float scale) {
        super(scale);
        // Нижняя половина UV-плоскости — наша; фортресс уже разложен по 64.
        this.textureHeight = 128;

        // --- очки ПНВ (UV согласованы с gen_nano_thaum.py) ---
        this.goggleR = new ModelRenderer(this, 0, 64);
        this.goggleR.addBox(-3.5F, -5.5F, -4.9F, 2, 2, 1);
        this.goggleL = new ModelRenderer(this, 8, 64);
        this.goggleL.addBox(1.5F, -5.5F, -4.9F, 2, 2, 1);
        this.goggleBridge = new ModelRenderer(this, 16, 64);
        this.goggleBridge.addBox(-1.5F, -5.2F, -4.8F, 3, 1, 1);
        this.bipedHead.addChild(this.goggleR);
        this.bipedHead.addChild(this.goggleL);
        this.bipedHead.addChild(this.goggleBridge);

        // --- жгуты на груди и подсумки пояса ---
        this.chestCableR = new ModelRenderer(this, 0, 70);
        this.chestCableR.addBox(-3.4F, 1.5F, -2.8F, 1, 5, 1);
        this.chestCableL = new ModelRenderer(this, 0, 70);
        this.chestCableL.addBox(2.4F, 1.5F, -2.8F, 1, 5, 1);
        this.pouchR = new ModelRenderer(this, 12, 70);
        this.pouchR.addBox(-3.2F, 8.6F, -2.8F, 2, 2, 1);
        this.pouchL = new ModelRenderer(this, 12, 70);
        this.pouchL.addBox(1.2F, 8.6F, -2.8F, 2, 2, 1);
        this.bipedBody.addChild(this.chestCableR);
        this.bipedBody.addChild(this.chestCableL);
        this.bipedBody.addChild(this.pouchR);
        this.bipedBody.addChild(this.pouchL);

        // --- паулдроны и кабели рук ---
        this.pauldronR = new ModelRenderer(this, 32, 64);
        this.pauldronR.addBox(-3.6F, -2.6F, -2.5F, 5, 3, 5);
        this.pauldronL = new ModelRenderer(this, 32, 64);
        this.pauldronL.mirror = true;
        this.pauldronL.addBox(-1.4F, -2.6F, -2.5F, 5, 3, 5);
        this.armCableR = new ModelRenderer(this, 6, 70);
        this.armCableR.addBox(-3.7F, 1.0F, -0.5F, 1, 4, 1);
        this.armCableL = new ModelRenderer(this, 6, 70);
        this.armCableL.addBox(2.7F, 1.0F, -0.5F, 1, 4, 1);
        this.bipedRightArm.addChild(this.pauldronR);
        this.bipedRightArm.addChild(this.armCableR);
        this.bipedLeftArm.addChild(this.pauldronL);
        this.bipedLeftArm.addChild(this.armCableL);

        // --- сабатоны ---
        this.sabatonR = new ModelRenderer(this, 56, 64);
        this.sabatonR.addBox(-2.5F, 8.6F, -2.6F, 5, 3, 5);
        this.sabatonL = new ModelRenderer(this, 56, 64);
        this.sabatonL.mirror = true;
        this.sabatonL.addBox(-2.5F, 8.6F, -2.6F, 5, 3, 5);
        this.bipedRightLeg.addChild(this.sabatonR);
        this.bipedLeftLeg.addChild(this.sabatonL);
    }

    /**
     * Видимость своих деталей по слоту; для FEET дополнительно гасим
     * фортресс-панели ног, оставляя только сабатоны.
     */
    public void prepareSlot(EntityEquipmentSlot slot) {
        boolean head = slot == EntityEquipmentSlot.HEAD;
        boolean chest = slot == EntityEquipmentSlot.CHEST;
        boolean feet = slot == EntityEquipmentSlot.FEET;

        this.goggleR.showModel = head;
        this.goggleL.showModel = head;
        this.goggleBridge.showModel = head;

        this.chestCableR.showModel = chest;
        this.chestCableL.showModel = chest;
        this.pouchR.showModel = chest;
        this.pouchL.showModel = chest;
        this.pauldronR.showModel = chest;
        this.pauldronL.showModel = chest;
        this.armCableR.showModel = chest;
        this.armCableL.showModel = chest;

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
