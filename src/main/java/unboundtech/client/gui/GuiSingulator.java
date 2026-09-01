package unboundtech.client.gui;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.common.gui.ContainerMachine;
import unboundtech.common.tiles.TileSingulator;

/**
 * Экран Сингулятора (`singulator.md` §9): EU-шкала каркаса, шесть
 * полосок вис жезла цветами прималов, кнопки режима — «авто» + шесть
 * прималов (клик шлёт ванильный enchant-пакет, сервер валидирует в
 * тайле). Выбранный режим обведён светлой рамкой.
 */
@SideOnly(Side.CLIENT)
public class GuiSingulator extends GuiMachine {

    private static final int BTN_X = 28;
    private static final int BTN_Y = 76;
    private static final int BTN_STEP = 18;
    private static final int BAR_X = 34;
    private static final int BAR_STEP = 16;

    public GuiSingulator(ContainerMachine container) {
        super(container);
    }

    @Override
    protected void drawWidgets(int mouseX, int mouseY) {
        TileSingulator singulator = (TileSingulator) this.tile;
        // шесть полосок вис (сантивис → доля потолка стержня)
        double max = Math.max(1, singulator.guiMaxVis());
        for (int i = 0; i < 6; i++) {
            int colour = TileSingulator.PRIMALS[i].getColor();
            this.drawFramedBar(BAR_X + i * BAR_STEP, GAUGE_Y, 8, GAUGE_H,
                    singulator.guiHasWand()
                            ? singulator.guiVis(i) / max : 0.0,
                    colour);
        }
        // кнопки: 0 — авто (серая), 1..6 — прималы
        for (int id = 0; id <= 6; id++) {
            int x = this.guiLeft + BTN_X + id * BTN_STEP;
            int y = this.guiTop + BTN_Y;
            boolean chosen = singulator.guiMode() == id;
            drawRect(x - 1, y - 1, x + 13, y + 13,
                    chosen ? 0xFFE8E0F5 : 0xFF2A2136);
            int fill = id == 0 ? 0x555566
                    : TileSingulator.PRIMALS[id - 1].getColor();
            drawRect(x, y, x + 12, y + 12, 0xFF000000 | fill);
        }
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        // «A» на кнопке авто
        this.fontRenderer.drawString("A",
                BTN_X + (12 - this.fontRenderer.getStringWidth("A")) / 2,
                BTN_Y + 2, 0xE8E0F5);
        // тултипы кнопок — имя примала
        int mx = mouseX - this.guiLeft;
        int my = mouseY - this.guiTop;
        int id = this.buttonAt(mx, my);
        if (id >= 0) {
            this.drawHoveringText(TileSingulator.modeName(id),
                    mx, my);
        }
    }

    private int buttonAt(int x, int y) {
        if (y < BTN_Y || y >= BTN_Y + 12) {
            return -1;
        }
        for (int id = 0; id <= 6; id++) {
            int bx = BTN_X + id * BTN_STEP;
            if (x >= bx && x < bx + 12) {
                return id;
            }
        }
        return -1;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
            throws java.io.IOException {
        int id = this.buttonAt(mouseX - this.guiLeft, mouseY - this.guiTop);
        if (id >= 0 && mouseButton == 0) {
            // ванильный канал кнопок контейнера — без своего пакета
            this.mc.playerController.sendEnchantPacket(
                    this.inventorySlots.windowId, id);
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
