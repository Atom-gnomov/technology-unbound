package unboundtech.common.items;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import unboundtech.common.UTItems;

/**
 * Патронная лента (`cartridge_line.md` §4.1, `cartridges.md` §4.3):
 * 60 патронов одного типа, собирается ТОЛЬКО Линией за 800 тиков.
 * Корм Пулемёта (T5); разборке обратно не подлежит — обратимость
 * обесценила бы смысл ленты (§12.2). Тип — в NBT.
 */
public class ItemCartridgeBelt extends Item {

    public static final int SIZE = 60;

    public ItemCartridgeBelt() {
        this.setMaxStackSize(4);
    }

    public static ItemStack of(int bulletType) {
        ItemStack stack = new ItemStack(UTItems.cartridgeBelt);
        stack.setTagInfo("UTType",
                new net.minecraft.nbt.NBTTagInt(bulletType));
        return stack;
    }

    public static int type(ItemStack stack) {
        return stack.hasTagCompound()
                ? stack.getTagCompound().getInteger("UTType") : 0;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> lines, ITooltipFlag flag) {
        lines.add("§7" + I18n.translateToLocal(
                "item.unboundtech.cartridge_" + ItemCartridge.key(type(stack))
                        + ".name") + " × " + SIZE);
        lines.add("§8" + I18n.translateToLocal(
                "unboundtech.tooltip.cartridge_belt"));
    }
}
