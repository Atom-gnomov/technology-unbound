package unboundtech.client.gui;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.common.gui.ContainerMachine;
import unboundtech.common.tiles.TileFluxCondenser;

/**
 * Экран Флюкс-Конденсатора (#15): EU-шкала каркаса + бар мути
 * (флюкс-лиловый). Режим и причина простоя — строкой статуса.
 */
@SideOnly(Side.CLIENT)
public class GuiFluxCondenser extends GuiMachine {

    public GuiFluxCondenser(ContainerMachine container) {
        super(container);
    }

    @Override
    protected void drawWidgets(int mouseX, int mouseY) {
        TileFluxCondenser condenser = (TileFluxCondenser) this.tile;
        this.drawFramedBar(28, GAUGE_Y, 8, GAUGE_H,
                condenser.guiEssentia()
                        / (double) TileFluxCondenser.ESSENTIA_BUFFER,
                0x7B4FA0);
    }
}
