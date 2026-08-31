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
 * ПРОТОТИП Квант-Гибридной брони (`quantum_hybrid_armour.md`) — внешний
 * вид двух эндгейм-путей для примерки, тот же приём, что у Нано-Таума.
 *
 * Редизайн по директиве владельца (ХФ-8): пути РАЗОШЛИСЬ базой и
 * силуэтом. Путь А (Пустотный) — база ModelRobe (балахон, капюшон,
 * юбка) + квантовые части сверху, максимально тяжёлый вид
 * ({@code ModelQuantVoidArmour}). Путь Б (Ихорный) — квант-подкладка
 * + 3D робы/шаровары/шапка с короной-антенной, ихор течёт по кванту
 * ({@code ModelQuantIchorArmour}). Разница читается по силуэту.
 *
 * ⚠️ Механики T4 здесь НЕТ: ни ISpecialArmor 92 %, ни заряда, ни
 * полёта пути Б — придут с настоящей реализацией. Характеристики —
 * заглушки уровня примерки.
 */
public class ItemQuantumHybridArmor extends ItemArmor {

    /** 0 — Пустотный (А), 1 — Ихорный (Б). */
    public final int path;

    private ModelBiped modelThick;
    private ModelBiped modelThin;

    public ItemQuantumHybridArmor(ArmorMaterial material,
                                  EntityEquipmentSlot slot, int path) {
        super(material, 0, slot);
        this.path = path;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity,
                                  EntityEquipmentSlot slot, String type) {
        return this.path == 0
                ? "unboundtech:textures/models/armor/quant_void.png"
                : "unboundtech:textures/models/armor/quant_ichor.png";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModelBiped getArmorModel(EntityLivingBase entityLiving, ItemStack itemStack,
                                    EntityEquipmentSlot armorSlot, ModelBiped fallback) {
        boolean isVoid = this.path == 0;
        if (this.modelThick == null) {
            this.modelThick = isVoid
                    ? new unboundtech.client.model.ModelQuantVoidArmour(1.0f)
                    : new unboundtech.client.model.ModelQuantIchorArmour(1.0f);
        }
        if (this.modelThin == null) {
            this.modelThin = isVoid
                    ? new unboundtech.client.model.ModelQuantVoidArmour(0.5f)
                    : new unboundtech.client.model.ModelQuantIchorArmour(0.5f);
        }
        // Пустота — конвенция ItemVoidRobeArmor: толстая для груди и
        // ботинок, тонкая для капюшона и ног (юбка живёт в тонкой);
        // Ихор — как нано: тонкая только для поножей
        boolean thin = isVoid
                ? armorSlot == EntityEquipmentSlot.HEAD
                        || armorSlot == EntityEquipmentSlot.LEGS
                : armorSlot == EntityEquipmentSlot.LEGS;
        ModelBiped model = thin ? this.modelThin : this.modelThick;
        model.bipedHead.showModel = armorSlot == EntityEquipmentSlot.HEAD;
        model.bipedHeadwear.showModel = false;
        // у Пустоты юбка робы висит на body — телу быть и для поножей
        model.bipedBody.showModel = armorSlot == EntityEquipmentSlot.CHEST
                || (isVoid && armorSlot == EntityEquipmentSlot.LEGS);
        model.bipedRightArm.showModel = armorSlot == EntityEquipmentSlot.CHEST;
        model.bipedLeftArm.showModel = armorSlot == EntityEquipmentSlot.CHEST;
        model.bipedRightLeg.showModel = armorSlot == EntityEquipmentSlot.LEGS
                || armorSlot == EntityEquipmentSlot.FEET;
        model.bipedLeftLeg.showModel = armorSlot == EntityEquipmentSlot.LEGS
                || armorSlot == EntityEquipmentSlot.FEET;
        model.isSneak = entityLiving.isSneaking();
        model.isRiding = entityLiving.isRiding();
        model.isChild = entityLiving.isChild();
        if (isVoid) {
            ((unboundtech.client.model.ModelQuantVoidArmour) model)
                    .prepareSlot(armorSlot);
        } else {
            ((unboundtech.client.model.ModelQuantIchorArmour) model)
                    .prepareSlot(armorSlot);
        }
        return model;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> lines, ITooltipFlag flag) {
        lines.add((this.path == 0 ? "§5" : "§6") + I18n.translateToLocal(
                "unboundtech.tooltip.quant_" + (this.path == 0 ? "void" : "ichor")));
        lines.add("§8" + I18n.translateToLocal("unboundtech.tooltip.nano_thaum_proto"));
    }
}
