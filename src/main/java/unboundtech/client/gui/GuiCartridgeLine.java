package unboundtech.client.gui;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.common.gui.ContainerMachine;
import unboundtech.common.tiles.TileCartridgeLine;

/**
 * Экран Патронной Линии (`cartridge_line.md` §9): первая машина каркаса
 * со слотами — гильзы/сырьё слева, выход справа, между ними прогресс
 * цикла; в режиме ленты снизу растёт полоса набранных патронов.
 * Ячейки слотов и сетка инвентаря рисуются поверх базовой панели.
 */
@SideOnly(Side.CLIENT)
public class GuiCartridgeLine extends GuiMachine {

    public GuiCartridgeLine(ContainerMachine container) {
        super(container);
    }

    private void slotCell(int x, int y) {
        drawRect(this.guiLeft + x - 1, this.guiTop + y - 1,
                this.guiLeft + x + 17, this.guiTop + y + 17, 0xFF2A2136);
        drawRect(this.guiLeft + x, this.guiTop + y,
                this.guiLeft + x + 16, this.guiTop + y + 16, 0xFF120E1C);
    }

    @Override
    protected void drawWidgets(int mouseX, int mouseY) {
        TileCartridgeLine line = (TileCartridgeLine) this.tile;
        // ячейки машины (координаты контейнера)
        this.slotCell(44, 30);
        this.slotCell(44, 52);
        this.slotCell(116, 41);
        // сетка инвентаря игрока
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.slotCell(8 + col * 18, 84 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            this.slotCell(8 + col * 18, 142);
        }
        // прогресс цикла — каретка ползёт от сырья к выходу
        this.drawFramedBarH(66, 44, 46, 6,
                line.guiProgress() / (double) TileCartridgeLine.CYCLE_TICKS,
                0xB48E3C);
        // полоса ленты — под прогрессом
        if (line.guiBeltMode()) {
            this.drawFramedBarH(66, 54, 46, 4,
                    line.guiBeltFilled() / (double) TileCartridgeLine.BELT_SIZE,
                    0x9A5BD0);
        }
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Низ панели занят инвентарём игрока — статус каркаса туда не
     * влезает; рисуем ОДНОЙ строкой над инвентарём (хвост с EU виден
     * шкалой слева, потери информации нет).
     */
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = this.tile.getBlockType().getLocalizedName();
        this.fontRenderer.drawString(title,
                (this.xSize - this.fontRenderer.getStringWidth(title)) / 2, 6,
                0x3A2A52);
        if (this.tile instanceof unboundtech.common.tiles.IMachineStatus) {
            String status = ((unboundtech.common.tiles.IMachineStatus) this.tile)
                    .getStatusLine();
            java.util.List<String> lines = this.fontRenderer
                    .listFormattedStringToWidth(status, this.xSize - 16);
            if (!lines.isEmpty()) {
                this.fontRenderer.drawString(lines.get(0), 8, 72, 0xFFFFFF);
            }
        }
    }
}
