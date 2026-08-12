package unboundtech.common.items;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

/**
 * Простой материал мода. Пока такой один — закалённый таумий
 * (`05_objects/tempered_thaumium.md`).
 *
 * ⚠️ Отступление от карточки, осознанное: строка «Класс» предлагала
 * мета-предмет (`ItemUTResource (мета)`), но карточка же задаёт registry-имя
 * `unboundtech:tempered_thaumium_ingot`. Мета-предмет имеет ОДНО registry-имя
 * на все подтипы, то есть слиток назывался бы `unboundtech:resource#0`.
 * Registry-имя — жёсткое поле карточки, «мета» — подсказка по реализации,
 * поэтому выбрано имя: один класс, по экземпляру на материал.
 *
 * Тултип (§9) подсказывает про второй путь получения: игроки редко
 * перечитывают Таумономикон.
 */
public class ItemUTResource extends Item {

    private final String tooltipKey;

    /**
     * @param tooltipKey ключ строки-подсказки или {@code null}, если её нет
     */
    public ItemUTResource(String tooltipKey) {
        this.tooltipKey = tooltipKey;
        this.setMaxStackSize(64);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        if (this.tooltipKey != null) {
            tooltip.add("§7" + I18n.translateToLocal(this.tooltipKey));
        }
    }
}
