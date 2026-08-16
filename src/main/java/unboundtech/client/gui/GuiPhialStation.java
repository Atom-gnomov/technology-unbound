package unboundtech.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;
import unboundtech.common.inventory.ContainerPhialStation;
import unboundtech.common.tiles.TilePhialStation;

/** Экран Фиал-станции: слоты, заряд, эссенция, режим и фильтр. */
@SideOnly(Side.CLIENT)
public class GuiPhialStation extends GuiMachineBase {

    private static final int BUTTON_ID = 10;

    private final ContainerPhialStation container;
    private GuiButton modeButton;

    public GuiPhialStation(InventoryPlayer playerInventory, TilePhialStation station) {
        super(new ContainerPhialStation(playerInventory, station), "phial_station",
                "container.unboundtech.phial_station");
        this.container = (ContainerPhialStation) this.inventorySlots;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.modeButton = new GuiButton(BUTTON_ID, this.guiLeft + 58, this.guiTop + 58, 60, 20,
                this.modeLabel());
        this.buttonList.add(this.modeButton);
    }

    private String modeLabel() {
        return I18n.format(this.container.getMode() == TilePhialStation.Mode.FILL
                ? "gui.unboundtech.station.fill" : "gui.unboundtech.station.drain");
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id != BUTTON_ID) {
            return;
        }
        // Ванильный канал кнопок контейнера — своего пакета не нужно.
        this.mc.playerController.sendEnchantPacket(this.container.windowId,
                ContainerPhialStation.BUTTON_MODE);
    }

    @Override
    protected void drawGauges(int left, int top) {
        this.drawGauge(left, top, GAUGE_X_LEFT, DONOR_LEFT,
                this.container.getEnergy() / TilePhialStation.CAPACITY);
        this.drawGauge(left, top, GAUGE_X_RIGHT, DONOR_RIGHT,
                this.container.getEssentia() / (double) TilePhialStation.PHIAL_AMOUNT);
    }

    @Override
    protected void drawReadout() {
        if (this.modeButton != null) {
            this.modeButton.displayString = this.modeLabel();
        }
        Aspect filter = this.container.getFilter();
        Aspect held = this.container.getAspect();
        this.line(20, I18n.format("gui.unboundtech.energy",
                this.container.getEnergy(), (int) TilePhialStation.CAPACITY));
        this.line(32, I18n.format("gui.unboundtech.essentia",
                this.container.getEssentia(), TilePhialStation.PHIAL_AMOUNT)
                + (held == null ? "" : " (" + held.getName() + ")"));
        this.line(44, I18n.format("gui.unboundtech.station.filter",
                filter == null ? I18n.format("gui.unboundtech.station.any") : filter.getName()));
        // Подсказка по применению — иначе назначение слотов неочевидно.
        this.line(80, I18n.format(this.container.getMode() == TilePhialStation.Mode.FILL
                ? "gui.unboundtech.station.hint_fill" : "gui.unboundtech.station.hint_drain"));
    }
}
