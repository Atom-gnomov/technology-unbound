package unboundtech.client.gui;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.common.gui.ContainerMachine;
import unboundtech.common.tiles.TileBusNode;

/**
 * Экран Шинного Узла (#16): EU-шкала каркаса + шесть плашек боковых
 * буферов (цвет аспекта из реестра, количество под плашкой, буквы
 * сторон над ней). Причины простоя — строкой статуса.
 */
@SideOnly(Side.CLIENT)
public class GuiBusNode extends GuiMachine {

    private static final String[] SIDES = {"D", "U", "N", "S", "W", "E"};

    public GuiBusNode(ContainerMachine container) {
        super(container);
    }

    @Override
    protected void drawWidgets(int mouseX, int mouseY) {
        TileBusNode node = (TileBusNode) this.tile;
        for (int i = 0; i < 6; i++) {
            int x = this.guiLeft + 34 + i * 22;
            int y = this.guiTop + GAUGE_Y + 12;
            int colour = node.guiBufferColor(i);
            drawRect(x - 1, y - 1, x + 13, y + 13, 0xFF2A2136);
            drawRect(x, y, x + 12, y + 12,
                    colour >= 0 ? 0xFF000000 | colour : 0xFF120E1C);
            // заполненность буфера — тонкая полоска под плашкой
            int amount = node.guiBufferAmount(i);
            int w = amount * 12 / TileBusNode.CHANNEL_BUFFER;
            drawRect(x, y + 14, x + 12, y + 16, 0xFF120E1C);
            if (w > 0) {
                drawRect(x, y + 14, x + Math.min(12, w), y + 16, 0xFFB48E3C);
            }
        }
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        TileBusNode node = (TileBusNode) this.tile;
        for (int i = 0; i < 6; i++) {
            int x = 34 + i * 22;
            this.fontRenderer.drawString(SIDES[i],
                    x + (12 - this.fontRenderer.getStringWidth(SIDES[i])) / 2,
                    GAUGE_Y, 0x8A7FA5);
            int amount = node.guiBufferAmount(i);
            if (amount > 0) {
                String text = String.valueOf(amount);
                this.fontRenderer.drawString(text,
                        x + (12 - this.fontRenderer.getStringWidth(text)) / 2,
                        GAUGE_Y + 32, 0xE8E0F5);
            }
        }
    }
}
