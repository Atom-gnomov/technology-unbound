package unboundtech.common.items;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

/**
 * Патрон (`05_objects/cartridges.md`): та же гильза с цветной головкой.
 * Сам по себе не используется — заряжается в стволы. Тип патрона
 * определяет эффект в цели, ствол задаёт базовый урон (§4.2).
 */
public class ItemCartridge extends Item {

    /** Индекс типа для снаряда ({@code EntityFluxBullet.TYPE_*}). */
    public final int bulletType;
    private final String effectKey;

    public ItemCartridge(int bulletType, String effectKey) {
        this.bulletType = bulletType;
        this.effectKey = effectKey;
        this.setMaxStackSize(64);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> lines, ITooltipFlag flag) {
        lines.add("§7" + I18n.translateToLocal(this.effectKey));
    }
}
