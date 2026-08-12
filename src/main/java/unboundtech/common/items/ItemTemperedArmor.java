package unboundtech.common.items;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.aspects.Aspect;

/**
 * Кузнечная броня из закалённого таумия
 * (`05_objects/tempered_thaumium_armor.md`).
 *
 * Профиль сознательно не «улучшенный алмаз», а другой (§5.2): брони больше
 * (24 против 20), но стойкость вдвое ниже, зачарования беднее, и любой каст
 * обходится дороже.
 *
 * Штраф вис — не хак, а родной API: {@link IVisDiscountGear} с
 * ОТРИЦАТЕЛЬНЫМ значением. Интерфейс взят тот же, что у брони самого порта
 * ({@code thaumcraft.api.IVisDiscountGear}, три аргумента), — движок читает
 * именно его.
 */
public class ItemTemperedArmor extends ItemArmor implements IVisDiscountGear {

    /** Штраф на элемент, в процентах (§5): −5 % за элемент, −20 % за сет. */
    public static final int VIS_PENALTY_PER_PIECE = -5;

    /** Ключ строки «Полный комплект: расход вис +20 %» (§9). */
    private static final String TOOLTIP_KEY = "unboundtech.tooltip.tempered_armor";

    public ItemTemperedArmor(ArmorMaterial material, EntityEquipmentSlot slot) {
        super(material, 0, slot);
    }

    @Override
    public int getVisDiscount(ItemStack stack, EntityPlayer player, Aspect aspect) {
        return VIS_PENALTY_PER_PIECE;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> lines, ITooltipFlag flag) {
        // Строка в стиле ТК — ровно как её печатает броня порта; значение
        // отрицательное, поэтому игрок видит штраф, а не скидку.
        lines.add(TextFormatting.DARK_PURPLE
                + I18n.translateToLocal("tc.visdiscount") + ": "
                + VIS_PENALTY_PER_PIECE + "%");
        lines.add("§7" + I18n.translateToLocal(TOOLTIP_KEY));
    }
}
