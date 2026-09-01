package unboundtech.common.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import unboundtech.common.tiles.TileCartridgeLine;

/**
 * Линия — первая машина каркаса с настоящими слотами (`cartridge_line.md`
 * §9): гильзы, сырьё, выход + инвентарь игрока. Синк полей наследуется
 * от {@link ContainerMachine}; slotClick-гард базы пропускает клики,
 * когда слоты есть.
 */
public class ContainerCartridgeLine extends ContainerMachine {

    public ContainerCartridgeLine(TileCartridgeLine tile,
                                  EntityPlayer player) {
        super(tile);
        this.addSlotToContainer(new SlotItemHandler(tile.inventory,
                TileCartridgeLine.SLOT_CASING, 44, 30));
        this.addSlotToContainer(new SlotItemHandler(tile.inventory,
                TileCartridgeLine.SLOT_RAW, 44, 52));
        this.addSlotToContainer(new SlotItemHandler(tile.inventory,
                TileCartridgeLine.SLOT_OUT, 116, 41) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;   // выход — только наружу
            }
        });
        // инвентарь игрока — стандартная сетка контейнеров 176x166
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(player.inventory,
                        col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(player.inventory, col,
                    8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack before = stack.copy();
        if (index < 3) {
            // из машины — в инвентарь игрока
            if (!this.mergeItemStack(stack, 3, 39, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // из инвентаря — в подходящий слот машины
            int target = stack.getItem() == unboundtech.common.UTItems.casing
                    ? 0 : TileCartridgeLine.rawType(stack) >= 0 ? 1 : -1;
            if (target < 0 || !this.mergeItemStack(stack,
                    target, target + 1, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        if (stack.getCount() == before.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return before;
    }
}
