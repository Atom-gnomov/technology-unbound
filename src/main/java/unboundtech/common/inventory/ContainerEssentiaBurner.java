package unboundtech.common.inventory;

import net.minecraft.entity.player.InventoryPlayer;
import unboundtech.common.tiles.TileEssentiaBurner;
import unboundtech.energy.EnergyCanon;

/** Контейнер Эссент. Горелки: слотов нет, только показания. */
public class ContainerEssentiaBurner extends ContainerMachineBase {

    public static final int VALUE_ENERGY = 0;
    public static final int VALUE_ESSENTIA = 1;
    public static final int VALUE_ASPECT = 2;
    public static final int VALUE_RATE = 3;
    private static final int VALUES = 4;

    private final TileEssentiaBurner burner;

    /** Показания на клиенте: их присылает сервер свойствами окна. */
    private int energy;
    private int essentia;
    private int aspectId;
    private int rate;

    public ContainerEssentiaBurner(InventoryPlayer playerInventory, TileEssentiaBurner burner) {
        super(playerInventory, burner, VALUES, 84);
        this.burner = burner;
    }

    @Override
    protected int[] trackedValues() {
        return new int[]{
                (int) this.burner.getEnergyStored(),
                this.burner.getEssentiaAmount(null),
                AspectIndex.idOf(this.burner.getBurningAspect()),
                EnergyCanon.essentiaValue(this.burner.getBurningAspect()),
        };
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
            case VALUE_ASPECT:
                this.aspectId = value;
                break;
            case VALUE_RATE:
                this.rate = value;
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

    public thaumcraft.api.aspects.Aspect getAspect() {
        return AspectIndex.byId(this.aspectId);
    }

    public int getRate() {
        return this.rate;
    }
}
