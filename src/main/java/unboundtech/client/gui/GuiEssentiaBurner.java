package unboundtech.client.gui;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;
import unboundtech.common.inventory.ContainerEssentiaBurner;
import unboundtech.common.tiles.TileEssentiaBurner;

/** Экран Эссент. Горелки: заряд, эссенция и курс текущего аспекта. */
@SideOnly(Side.CLIENT)
public class GuiEssentiaBurner extends GuiMachineBase {

    private final ContainerEssentiaBurner container;

    public GuiEssentiaBurner(InventoryPlayer playerInventory, TileEssentiaBurner burner) {
        super(new ContainerEssentiaBurner(playerInventory, burner), "essentia_burner",
                "container.unboundtech.essentia_burner");
        this.container = (ContainerEssentiaBurner) this.inventorySlots;
    }

    @Override
    protected void drawGauges(int left, int top) {
        this.drawGauge(left, top, GAUGE_X_LEFT, DONOR_LEFT,
                this.container.getEnergy() / TileEssentiaBurner.CAPACITY);
        this.drawGauge(left, top, GAUGE_X_RIGHT, DONOR_RIGHT,
                this.container.getEssentia() / (double) TileEssentiaBurner.ESSENTIA_BUFFER);
    }

    @Override
    protected void drawReadout() {
        Aspect aspect = this.container.getAspect();
        this.line(20, I18n.format("gui.unboundtech.energy",
                this.container.getEnergy(), (int) TileEssentiaBurner.CAPACITY));
        this.line(32, I18n.format("gui.unboundtech.essentia",
                this.container.getEssentia(), TileEssentiaBurner.ESSENTIA_BUFFER));
        if (aspect == null) {
            this.line(44, I18n.format("gui.unboundtech.burner.idle"));
        } else {
            this.line(44, I18n.format("gui.unboundtech.burner.burning",
                    aspect.getName(), this.container.getRate()));
        }
        // Подсказка «что с этим делать» — по просьбе владельца: без неё игрок
        // не догадается, что машине нужна ТРУБА, а не слот.
        this.line(58, I18n.format("gui.unboundtech.burner.hint1"));
        this.line(68, I18n.format("gui.unboundtech.burner.hint2"));
    }
}
