package unboundtech.client.gui;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.common.gui.ContainerMachine;
import unboundtech.common.tiles.TileResonantSplitter;

/**
 * Экран Резонансного Расщепителя (#15): EU-шкала каркаса + вход
 * (цвет аспекта), горизонтальный прогресс цикла, два выхода цветами
 * компонентов. Серый бар = аспект не выбран.
 */
@SideOnly(Side.CLIENT)
public class GuiResonantSplitter extends GuiMachine {

    public GuiResonantSplitter(ContainerMachine container) {
        super(container);
    }

    private static int colourOr(int colour, int fallback) {
        return colour >= 0 ? colour : fallback;
    }

    @Override
    protected void drawWidgets(int mouseX, int mouseY) {
        TileResonantSplitter splitter = (TileResonantSplitter) this.tile;
        double buffer = TileResonantSplitter.BUFFER;
        this.drawFramedBar(30, GAUGE_Y, 8, GAUGE_H,
                splitter.guiInAmount() / buffer,
                colourOr(splitter.guiInColor(), 0x555566));
        this.drawFramedBarH(48, GAUGE_Y + GAUGE_H / 2 - 3, 56, 6,
                splitter.guiProgress()
                        / (double) TileResonantSplitter.CYCLE_TICKS,
                0xB48E3C);
        this.drawFramedBar(112, GAUGE_Y, 8, GAUGE_H,
                splitter.guiAmtA() / buffer,
                colourOr(splitter.guiColA(), 0x555566));
        this.drawFramedBar(128, GAUGE_Y, 8, GAUGE_H,
                splitter.guiAmtB() / buffer,
                colourOr(splitter.guiColB(), 0x555566));
    }
}
