package unboundtech.common.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import thaumcraft.api.aspects.Aspect;
import unboundtech.common.tiles.TilePhialStation;

/**
 * Контейнер Фиал-станции: вход, выход и показания.
 *
 * Кнопка режима идёт ванильным «щелчком по кнопке» ({@link #enchantItem}) —
 * это штатный канал клиент→сервер у любого контейнера, поэтому своего
 * сетевого канала мод не заводит.
 */
public class ContainerPhialStation extends ContainerMachineBase {

    public static final int VALUE_ENERGY = 0;
    public static final int VALUE_ESSENTIA = 1;
    public static final int VALUE_MODE = 2;
    public static final int VALUE_ASPECT = 3;
    public static final int VALUE_FILTER = 4;
    private static final int VALUES = 5;

    /** Идентификатор кнопки «сменить режим». */
    public static final int BUTTON_MODE = 0;

    private static final int SLOT_IN = 0;
    private static final int SLOT_OUT = 1;

    private final TilePhialStation station;

    private int energy;
    private int essentia;
    private int mode;
    private int aspectId;
    private int filterId;

    public ContainerPhialStation(InventoryPlayer playerInventory, TilePhialStation station) {
        super(playerInventory, station, VALUES, 84);
        this.station = station;
        this.addSlotToContainer(new SlotItemHandler(station.getSlots(), SLOT_IN, 44, 35));
        // Выходной слот только отдаёт: класть туда руками нечего.
        this.addSlotToContainer(new SlotItemHandler(station.getSlots(), SLOT_OUT, 116, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }
        });
    }

    @Override
    protected int machineSlots() {
        return 2;
    }

    @Override
    protected int inputSlots() {
        return 1;   // shift-клик кладёт только во вход
    }

    @Override
    protected int[] trackedValues() {
        return new int[]{
                (int) this.station.getEnergyStored(),
                this.station.getEssentiaAmount(null),
                this.station.getMode().ordinal(),
                AspectIndex.idOf(this.station.getEssentiaType(null)),
                AspectIndex.idOf(this.station.getFilter()),
        };
    }

    /** Ванильный канал кнопок контейнера. */
    @Override
    public boolean enchantItem(EntityPlayer player, int button) {
        if (button == BUTTON_MODE && !this.station.getWorld().isRemote) {
            this.station.toggleMode();
            this.detectAndSendChanges();
            return true;
        }
        return false;
    }

    @Override
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public void updateProgressBar(int id, int value) {
        switch (id) {
            case VALUE_ENERGY:
                this.energy = value;
                break;
            case VALUE_ESSENTIA:
                this.essentia = value;
                break;
            case VALUE_MODE:
                this.mode = value;
                break;
            case VALUE_ASPECT:
                this.aspectId = value;
                break;
            case VALUE_FILTER:
                this.filterId = value;
                break;
            default:
                break;
        }
    }

    public int getEnergy() {
        return this.energy;
    }

    public int getEssentia() {
        return this.essentia;
    }

    public TilePhialStation.Mode getMode() {
        TilePhialStation.Mode[] modes = TilePhialStation.Mode.values();
        return this.mode >= 0 && this.mode < modes.length ? modes[this.mode] : modes[0];
    }

    public Aspect getAspect() {
        return AspectIndex.byId(this.aspectId);
    }

    public Aspect getFilter() {
        return AspectIndex.byId(this.filterId);
    }
}
