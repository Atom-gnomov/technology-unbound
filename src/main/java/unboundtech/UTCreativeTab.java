package unboundtech;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.init.UTItems;

/** Вкладка креатива мода. */
public final class UTCreativeTab extends CreativeTabs {

    public static final UTCreativeTab INSTANCE = new UTCreativeTab();

    private UTCreativeTab() {
        super(UnboundTech.MODID);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public ItemStack createIcon() {
        return UTItems.thaumSteelIngot == null
                ? new ItemStack(net.minecraft.init.Items.IRON_INGOT)
                : new ItemStack(UTItems.thaumSteelIngot);
    }
}
