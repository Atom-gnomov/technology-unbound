package unboundtech.common.items;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

/**
 * Заряженная Искра (`techno_spirit.md` §4.3): гейт арены босса T5.
 * Не складывается больше 8 и медленно РАЗРЯЖАЕТСЯ вне инвентаря — склад
 * искр держать нельзя, но пройти данж можно.
 */
public class ItemChargedSpark extends Item {

    /** ~2 минуты жизни на земле. */
    private static final int DECAY_TICKS = 2400;

    public ItemChargedSpark() {
        this.setMaxStackSize(8);
    }

    @Override
    public boolean onEntityItemUpdate(EntityItem entity) {
        if (!entity.world.isRemote && entity.getAge() > DECAY_TICKS) {
            entity.setDead();
            return true;
        }
        if (entity.world.isRemote && entity.world.rand.nextInt(10) == 0) {
            entity.world.spawnParticle(
                    net.minecraft.util.EnumParticleTypes.REDSTONE,
                    entity.posX, entity.posY + 0.3, entity.posZ, 1.0, 0.8, 0.2);
        }
        return false;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> lines, ITooltipFlag flag) {
        lines.add("§7" + I18n.translateToLocal("unboundtech.tooltip.charged_spark"));
    }
}
