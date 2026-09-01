package unboundtech.common.crafting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.IForgeRegistryEntry;
import unboundtech.common.UTItems;
import unboundtech.common.items.ItemVisEdge;

/**
 * Установка/снятие Вис-Кромки (`vis_edge.md` §4.1) обычным верстаком:
 * - клинок + кромка → тот же клинок с тегом (кромка расходуется);
 * - клинок с тегом + таумиевый ключ → тег снят, ключ возвращается,
 *   кромка НЕ возвращается (§4.1).
 */
public class RecipeVisEdge extends IForgeRegistryEntry.Impl<IRecipe>
        implements IRecipe {

    private static final class Grid {
        ItemStack blade = ItemStack.EMPTY;
        boolean edge;
        boolean wrench;
        boolean junk;
    }

    private Grid scan(InventoryCrafting inv) {
        Grid grid = new Grid();
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() == UTItems.visEdge) {
                if (grid.edge) {
                    grid.junk = true;
                }
                grid.edge = true;
            } else if (stack.getItem() == UTItems.thaumiumWrench) {
                if (grid.wrench) {
                    grid.junk = true;
                }
                grid.wrench = true;
            } else if (ItemVisEdge.acceptsBlade(stack)
                    || ItemVisEdge.hasEdge(stack)) {
                if (!grid.blade.isEmpty()) {
                    grid.junk = true;
                }
                grid.blade = stack;
            } else {
                grid.junk = true;
            }
        }
        return grid;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        Grid grid = this.scan(inv);
        if (grid.junk || grid.blade.isEmpty() || grid.edge == grid.wrench) {
            return false;
        }
        return grid.edge
                ? ItemVisEdge.acceptsBlade(grid.blade)
                : ItemVisEdge.hasEdge(grid.blade);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        Grid grid = this.scan(inv);
        if (grid.blade.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = grid.blade.copy();
        result.setCount(1);
        if (grid.edge) {
            result.setTagInfo(ItemVisEdge.TAG,
                    new net.minecraft.nbt.NBTTagByte((byte) 1));
        } else if (result.hasTagCompound()) {
            result.getTagCompound().removeTag(ItemVisEdge.TAG);
        }
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        NonNullList<ItemStack> left =
                NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
        for (int i = 0; i < left.size(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            // ключ возвращается в верстак; кромка и клинок расходуются
            if (!stack.isEmpty() && stack.getItem() == UTItems.thaumiumWrench) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                left.set(i, copy);
            } else {
                left.set(i, ForgeHooks.getContainerItem(stack));
            }
        }
        return left;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
