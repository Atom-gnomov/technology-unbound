package unboundtech.client.gui;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.common.gui.ContainerMachine;
import unboundtech.common.tiles.TileInductionCrucible;

/**
 * Экран Индукционного Тигля (#15): EU-шкала каркаса + жар (оранжевый),
 * вода (синий), заполненность аспектами (лиловый) и до трёх плашек
 * аспектов цветом реестра с количеством.
 */
@SideOnly(Side.CLIENT)
public class GuiInductionCrucible extends GuiMachine {

    public GuiInductionCrucible(ContainerMachine container) {
        super(container);
    }

    @Override
    protected void drawWidgets(int mouseX, int mouseY) {
        TileInductionCrucible crucible = (TileInductionCrucible) this.tile;
        this.drawFramedBar(28, GAUGE_Y, 8, GAUGE_H,
                crucible.guiHeat() / (double) TileInductionCrucible.HEAT_MAX,
                0xE0641F);
        this.drawFramedBar(42, GAUGE_Y, 8, GAUGE_H,
                crucible.guiWater() / (double) TileInductionCrucible.TANK_CAPACITY,
                0x2E6FD8);
        this.drawFramedBar(56, GAUGE_Y, 8, GAUGE_H,
                crucible.guiTags() / (double) TileInductionCrucible.MAX_TAGS,
                0x9A5BD0);
        // плашки аспектов содержимого
        for (int i = 0; i < 3; i++) {
            int colour = crucible.guiAspectColor(i);
            if (crucible.guiAspectAmount(i) <= 0 || colour < 0) {
                continue;
            }
            int x = this.guiLeft + 76 + i * 22;
            int y = this.guiTop + GAUGE_Y + 8;
            drawRect(x - 1, y - 1, x + 13, y + 13, 0xFF2A2136);
            drawRect(x, y, x + 12, y + 12, 0xFF000000 | colour);
        }
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        TileInductionCrucible crucible = (TileInductionCrucible) this.tile;
        for (int i = 0; i < 3; i++) {
            int amount = crucible.guiAspectAmount(i);
            if (amount <= 0 || crucible.guiAspectColor(i) < 0) {
                continue;
            }
            String text = String.valueOf(amount);
            this.fontRenderer.drawString(text,
                    76 + i * 22 + (12 - this.fontRenderer.getStringWidth(text)) / 2,
                    GAUGE_Y + 24, 0xE8E0F5);
        }
    }
}
