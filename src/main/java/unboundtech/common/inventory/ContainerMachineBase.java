package unboundtech.common.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

/**
 * Общий каркас контейнера машины: инвентарь игрока, проверка дистанции и
 * рассылка чисел в GUI.
 *
 * Числа (буфер EU, эссенция, режим, аспект) идут «свойствами окна»: их шлёт
 * {@link #detectAndSendChanges()} только тем, у кого GUI открыт, и только при
 * изменении. Своего сетевого канала для этого заводить не нужно.
 *
 * ⚠️ Свойство окна — {@code short}. Все наши величины в него влезают
 * (наибольшая — буфер горелки, 10 000), но при добавлении новых это надо
 * проверять заново.
 */
public abstract class ContainerMachineBase extends Container {

    protected final TileEntity tile;
    private final int[] lastSent;

    protected ContainerMachineBase(InventoryPlayer playerInventory, TileEntity tile,
                                   int trackedValues, int inventoryY) {
        this.tile = tile;
        this.lastSent = new int[trackedValues];
        for (int i = 0; i < trackedValues; i++) {
            this.lastSent[i] = Integer.MIN_VALUE;
        }
        this.addPlayerInventory(playerInventory, inventoryY);
    }

    /** Три ряда рюкзака и пояс — ванильная раскладка. */
    private void addPlayerInventory(InventoryPlayer inventory, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(inventory, col + row * 9 + 9,
                        8 + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(inventory, col, 8 + col * 18, y + 58));
        }
    }

    /** @return значения, которые GUI показывает; порядок = номер свойства */
    protected abstract int[] trackedValues();

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        int[] values = this.trackedValues();
        for (int id = 0; id < values.length && id < this.lastSent.length; id++) {
            if (this.lastSent[id] == values[id]) {
                continue;
            }
            this.lastSent[id] = values[id];
            for (IContainerListener listener : this.listeners) {
                listener.sendWindowProperty(this, id, values[id]);
            }
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.tile != null && !this.tile.isInvalid()
                && this.tile.getWorld().getTileEntity(this.tile.getPos()) == this.tile
                && player.getDistanceSq(this.tile.getPos().getX() + 0.5,
                        this.tile.getPos().getY() + 0.5,
                        this.tile.getPos().getZ() + 0.5) <= 64.0;
    }

    /**
     * Shift-клик. Машинные слоты идут ПЕРЕД инвентарём игрока в нумерации
     * подклассов, поэтому здесь считаем их по {@link #machineSlots()}.
     */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        int machine = this.machineSlots();
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack inSlot = slot.getStack();
        ItemStack copy = inSlot.copy();
        int total = this.inventorySlots.size();

        if (index < machine) {
            // из машины — в инвентарь игрока
            if (!this.mergeItemStack(inSlot, machine, total, true)) {
                return ItemStack.EMPTY;
            }
        } else if (machine > 0) {
            // из инвентаря — во ВХОДНОЙ слот машины (выходной принимать не должен)
            if (!this.mergeItemStack(inSlot, 0, this.inputSlots(), false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (inSlot.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        return copy;
    }

    /** Сколько слотов у самой машины (идут первыми). */
    protected int machineSlots() {
        return 0;
    }

    /** Сколько из них принимают shift-клик игрока. */
    protected int inputSlots() {
        return 0;
    }
}
