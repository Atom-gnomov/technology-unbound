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
 * Путь А (Пустотный): тьма, поглощающая свет, холодный лиловый на оси,
 * рваный плащ. Путь Б (Ихорный): живое золото, пульс, который никогда
 * не гаснет. Геометрия одна ({@code ModelQuantumHybridArmour}), путь
 * ставится модели КАЖДЫЙ кадр (ТЗ п.6-7: никакого кэша) — смешанный
 * комплект из двух путей рендерится честно.
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
        if (this.modelThick == null) {
            this.modelThick = new unboundtech.client.model.ModelQuantumHybridArmour(1.0f);
        }
        if (this.modelThin == null) {
            this.modelThin = new unboundtech.client.model.ModelQuantumHybridArmour(0.5f);
        }
        // тонкая — только поножи (ТЗ п.5)
        ModelBiped model = this.armorType == EntityEquipmentSlot.LEGS
                ? this.modelThin : this.modelThick;
        model.bipedHead.showModel = armorSlot == EntityEquipmentSlot.HEAD;
        model.bipedHeadwear.showModel = false;
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
        unboundtech.client.model.ModelQuantumHybridArmour quantum =
                (unboundtech.client.model.ModelQuantumHybridArmour) model;
        quantum.path = this.path;   // каждый кадр, без кэша (ТЗ п.7)
        quantum.litLamps = 4;       // прототип: полный заряд
        quantum.prepareSlot(armorSlot);
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
