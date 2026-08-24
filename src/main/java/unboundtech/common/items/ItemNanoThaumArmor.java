package unboundtech.common.items;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * ПРОТОТИП Нано-Таум брони (`05_objects/nano_thaum_armor.md`) — только
 * внешний вид, для примерки решением владельца.
 *
 * Объёмная модель — та самая {@code ModelFortressArmor} порта (~40 деталей:
 * наплечники, наглазники, свиток, самоцвет), которую карточка §8 называет
 * эталоном; текстура — наш композит: развёртка фортресс-брони, перекрашенная
 * в нано-карбон и закалённый таумий (`tools/gen_nano_thaum.py`, генерируется
 * из установленных jar-ов — мод приватный, решение владельца).
 *
 * Класс модели упоминается ТОЛЬКО внутри {@link #getArmorModel} — тот же
 * приём ленивой клиентской загрузки, что у {@code ItemFortressArmor} порта:
 * поля типа {@link ModelBiped}, и выделенный сервер класс не грузит.
 *
 * ⚠️ Механики T4 здесь НЕТ: ни {@code ISpecialArmor} (92 % поглощения),
 * ни заряда 2 000 000 EU, ни джетпака — они придут с настоящей реализацией
 * T4. Характеристики ниже — заглушки уровня прототипа.
 */
public class ItemNanoThaumArmor extends ItemArmor {

    private ModelBiped modelThick;
    private ModelBiped modelThin;

    public ItemNanoThaumArmor(ArmorMaterial material, EntityEquipmentSlot slot) {
        super(material, 0, slot);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity,
                                  EntityEquipmentSlot slot, String type) {
        // Ванильная логика слоёв: штаны бипеда живут в отдельном файле
        // (как layer_2 у ванилы и nano_2 у IC2) — иначе бёдра голые.
        return slot == EntityEquipmentSlot.LEGS
                ? "unboundtech:textures/models/armor/nano_thaum_armor_2.png"
                : "unboundtech:textures/models/armor/nano_thaum_armor_1.png";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModelBiped getArmorModel(EntityLivingBase entityLiving, ItemStack itemStack,
                                    EntityEquipmentSlot armorSlot, ModelBiped fallback) {
        if (this.modelThick == null) {
            this.modelThick = new unboundtech.client.model.ModelNanoThaumArmor(1.0f);
        }
        if (this.modelThin == null) {
            this.modelThin = new unboundtech.client.model.ModelNanoThaumArmor(0.5f);
        }
        // Ванильная логика: тонкая (0.5) — ТОЛЬКО поножи. Шлем обязан быть
        // толстым: слой волос скина раздут ровно на 0.5, и тонкий шлем
        // z-файтился с ним — волосы пробивались сквозь капюшон на затылке.
        ModelBiped model = this.armorType == EntityEquipmentSlot.LEGS
                ? this.modelThin : this.modelThick;
        model.bipedHead.showModel = armorSlot == EntityEquipmentSlot.HEAD;
        model.bipedHeadwear.showModel = armorSlot == EntityEquipmentSlot.HEAD;
        model.bipedBody.showModel = armorSlot == EntityEquipmentSlot.CHEST
                || armorSlot == EntityEquipmentSlot.LEGS;
        model.bipedRightArm.showModel = armorSlot == EntityEquipmentSlot.CHEST;
        model.bipedLeftArm.showModel = armorSlot == EntityEquipmentSlot.CHEST;
        model.bipedRightLeg.showModel = armorSlot == EntityEquipmentSlot.LEGS
                || armorSlot == EntityEquipmentSlot.FEET;
        model.bipedLeftLeg.showModel = armorSlot == EntityEquipmentSlot.LEGS
                || armorSlot == EntityEquipmentSlot.FEET;
        model.isSneak = entityLiving.isSneaking();
        model.isRiding = entityLiving.isRiding();
        model.isChild = entityLiving.isChild();
        ((unboundtech.client.model.ModelNanoThaumArmor) model).prepareSlot(armorSlot);
        return model;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> lines, ITooltipFlag flag) {
        lines.add("§d" + I18n.translateToLocal("unboundtech.tooltip.nano_thaum_proto"));
    }
}
