package unboundtech.common.items;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

/**
 * Пустотный Иридий (`void_iridium.md`): металл, которому объяснили, что
 * он одновременно вещество и отсутствие вещества. Гейт вкладки «Синтез»,
 * материал компонентов жезла и брони вершины. Стек 16 — тяжёлый (§4).
 *
 * ⚠️ §12.1: свойство «не теряет заряд EU в Пустоте» пока ТОЛЬКО тултип —
 * в порте механики утечки заряда во Внешних землях не найдено; если её
 * не появится, свойство заменить на реальное (открытый вопрос канона).
 */
public class ItemVoidIridium extends Item {

    public ItemVoidIridium() {
        this.setMaxStackSize(16);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> lines, ITooltipFlag flag) {
        lines.add("§5" + I18n.translateToLocal("unboundtech.tooltip.void_iridium"));
    }
}
