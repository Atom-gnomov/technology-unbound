package unboundtech.common.items;

import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.Multimap;
import ic2.api.item.IElectricItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

/**
 * Вис-Кромка (`vis_edge.md`): надстройка к ЧУЖИМ энергоклинкам — мы не
 * добавляем цифру к чужому оружию, мы меняем форму его урона: часть
 * приходит не сразу (эффект «Резонанс», {@code UTVisEdgeHandler}).
 * Своего клинка мод не делает принципиально (ranged_weapons §4.8).
 */
public class ItemVisEdge extends Item {

    /** NBT-тег установленной кромки на клинке. */
    public static final String TAG = "UTVisEdge";

    public ItemVisEdge() {
        this.setMaxStackSize(16);
    }

    /** §4.1: IElectricItem + оружие ближнего боя (ATTACK_DAMAGE). */
    public static boolean acceptsBlade(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IElectricItem)) {
            return false;
        }
        if (stack.hasTagCompound() && stack.getTagCompound().getBoolean(TAG)) {
            return false;   // кромка уже стоит
        }
        Multimap<String, net.minecraft.entity.ai.attributes.AttributeModifier>
                modifiers = stack.getItem().getAttributeModifiers(
                        EntityEquipmentSlot.MAINHAND, stack);
        return !modifiers.get(SharedMonsterAttributes.ATTACK_DAMAGE.getName())
                .isEmpty();
    }

    public static boolean hasEdge(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTagCompound()
                && stack.getTagCompound().getBoolean(TAG);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> lines, ITooltipFlag flag) {
        lines.add("§5" + I18n.translateToLocal("unboundtech.tooltip.vis_edge.1"));
        lines.add("§8" + I18n.translateToLocal("unboundtech.tooltip.vis_edge.2"));
    }
}
