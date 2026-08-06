package unboundtech.compat;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import unboundtech.UTLog;

/**
 * Безопасный доступ к предметам сторонних модов через реестр Forge
 * (для аддонов БЕЗ собственного item-API — METS, ASP; предметы IC2
 * берутся через {@link unboundtech.compat.ic2.IC2Handles}).
 *
 * Отсутствующий предмет — WARN и пустой стек, вызывающий код обязан
 * пропустить регистрацию, не роняя игру (тот же контракт, что у IC2Handles).
 */
public final class ModItems {

    private ModItems() {
    }

    /** @return стек предмета {@code modid:path} с метой или пустой стек с WARN. */
    public static ItemStack item(String modid, String path, int meta) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(modid, path));
        if (item == null || item == Items.AIR) {
            UTLog.warn("Item {}:{} not found in the Forge registry — related content "
                    + "skipped (addon version changed its ids?)", modid, path);
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, 1, meta);
    }

    public static ItemStack item(String modid, String path) {
        return item(modid, path, 0);
    }
}
