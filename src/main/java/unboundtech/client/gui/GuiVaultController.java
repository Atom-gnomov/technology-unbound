package unboundtech.client.gui;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.common.gui.ContainerMachine;
import unboundtech.common.tiles.TileEssentiaVaultController;

/**
 * Экран Контроллера Накопителя (#16): EU-шкала каркаса + бар общей
 * занятости библиотеки и до пяти плашек главных аспектов с числами.
 */
@SideOnly(Side.CLIENT)
public class GuiVaultController extends GuiMachine {

    public GuiVaultController(ContainerMachine container) {
        super(container);
    }

    @Override
    protected void drawWidgets(int mouseX, int mouseY) {
        TileEssentiaVaultController vault =
                (TileEssentiaVaultController) this.tile;
        this.drawFramedBar(28, GAUGE_Y, 8, GAUGE_H,
                vault.guiTotal()
                        / (double) TileEssentiaVaultController.TOTAL_CAP,
                0x9A5BD0);
        for (int i = 0; i < 5; i++) {
            int colour = vault.guiAspectColor(i);
            if (vault.guiAspectAmount(i) <= 0 || colour < 0) {
                continue;
            }
            int x = this.guiLeft + 48 + i * 22;
            int y = this.guiTop + GAUGE_Y + 8;
            drawRect(x - 1, y - 1, x + 13, y + 13, 0xFF2A2136);
            drawRect(x, y, x + 12, y + 12, 0xFF000000 | colour);
        }
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        TileEssentiaVaultController vault =
                (TileEssentiaVaultController) this.tile;
        for (int i = 0; i < 5; i++) {
            int amount = vault.guiAspectAmount(i);
            if (amount <= 0 || vault.guiAspectColor(i) < 0) {
                continue;
            }
            String text = String.valueOf(amount);
            this.fontRenderer.drawString(text,
                    48 + i * 22 + (12 - this.fontRenderer.getStringWidth(text)) / 2,
                    GAUGE_Y + 24, 0xE8E0F5);
        }
    }
}
