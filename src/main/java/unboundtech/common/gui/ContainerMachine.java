package unboundtech.common.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Generic-контейнер GUI-каркаса (ХФ-7): слотов у машин мода нет — только
 * индикаторы, поэтому контейнер занимается одним: возит int-поля тайла
 * ({@link ISyncedMachine}) на клиент через sendWindowProperty. Первую
 * посылку каждому слушателю шлёт целиком, дальше — только дельты.
 */
public class ContainerMachine extends Container {

    private final TileEntity tile;
    private final ISyncedMachine machine;
    private int[] last;

    public ContainerMachine(TileEntity tile) {
        this.tile = tile;
        this.machine = (ISyncedMachine) tile;
    }

    public TileEntity getTile() {
        return this.tile;
    }

    /**
     * Слотов нет — ванильный slotClick на пустом списке кидает
     * IndexOutOfBoundsException от враждебного пакета (ревью #17):
     * клики по слотам глушим, наружный клик (-999) остаётся ванили.
     */
    @Override
    public ItemStack slotClick(int slotId, int dragType,
                               net.minecraft.inventory.ClickType type,
                               EntityPlayer player) {
        // В-2 ревью #24: ваниль не проверяет границы — глушим и клики
        // мимо списка слотов (у безслотовых это любые slotId >= 0)
        if (slotId >= 0 && slotId >= this.inventorySlots.size()) {
            return ItemStack.EMPTY;
        }
        return super.slotClick(slotId, dragType, type, player);
    }

    /**
     * Кнопки экранов машин без своего пакета: клиент шлёт ванильный
     * CPacketEnchantItem, тайл-{@link IMachineButtons} валидирует id.
     */
    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        return this.tile instanceof unboundtech.common.tiles.IMachineStatus
                && this.tile instanceof IMachineButtons
                && ((IMachineButtons) this.tile).onButton(player, id);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        // до ЦЕНТРА блока: BlockPos.add(0.5,...) флорит в no-op (ревью №6)
        return !this.tile.isInvalid()
                && player.getDistanceSq(this.tile.getPos().getX() + 0.5,
                        this.tile.getPos().getY() + 0.5,
                        this.tile.getPos().getZ() + 0.5) <= 64.0;
    }

    // Первую посылку целиком делает ванильный super.addListener ->
    // detectAndSendChanges: путь last == null шлёт всё (ревью №4).

    /**
     * ⚠️ SPacketWindowProperty возит value КАК SHORT (ревью №1): буфер
     * двигателя 40 000 EU в него не влезает. Каждое поле едет парой
     * свойств — 2i младшие 16 бит, 2i+1 старшие; клиент склеивает.
     */
    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        int[] fields = this.machine.syncFields();
        if (this.last == null || this.last.length != fields.length) {
            this.last = new int[fields.length];
            java.util.Arrays.fill(this.last, Integer.MIN_VALUE);
        }
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] != this.last[i]) {
                for (IContainerListener listener : this.listeners) {
                    listener.sendWindowProperty(this, 2 * i,
                            fields[i] & 0xFFFF);
                    listener.sendWindowProperty(this, 2 * i + 1,
                            fields[i] >>> 16);
                }
                this.last[i] = fields[i];
            }
        }
    }

    /** Клиентский кэш половинок для склейки (индексы полей, не свойств). */
    private int[] loHalves = new int[0];
    private int[] hiHalves = new int[0];

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data) {
        int field = id / 2;
        if (field >= this.loHalves.length) {
            this.loHalves = java.util.Arrays.copyOf(this.loHalves, field + 1);
            this.hiHalves = java.util.Arrays.copyOf(this.hiHalves, field + 1);
        }
        if ((id & 1) == 0) {
            this.loHalves[field] = data & 0xFFFF;
        } else {
            this.hiHalves[field] = data & 0xFFFF;
        }
        this.machine.applySyncField(field,
                (this.hiHalves[field] << 16) | this.loHalves[field]);
    }

    /** Слотов нет — шифт-клик не делает ничего (и не крашит). */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return ItemStack.EMPTY;
    }
}
