package unboundtech.client.gui;

import java.util.List;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.UnboundTech;
import unboundtech.common.gui.ContainerMachine;
import unboundtech.common.gui.IEnergyGauge;
import unboundtech.common.tiles.IMachineStatus;

/**
 * Базовый экран машины (ХФ-7): панель IC2-школы с таум-рамкой, палитра
 * скила minecraft-basic-texture. Каркас рисует: заголовок (имя блока),
 * шкалу EU слева (если тайл {@link IEnergyGauge}) и причину простоя
 * словами внизу — ту же строку {@link IMachineStatus}, исполненную на
 * КЛИЕНТСКОМ тайле (живость полей обеспечивает контейнер).
 *
 * Наследники экранов машин (#14—#16) добавляют свои виджеты в
 * {@link #drawWidgets(int, int)}.
 */
@SideOnly(Side.CLIENT)
public class GuiMachine extends GuiContainer {

    protected static final ResourceLocation PANEL = new ResourceLocation(
            UnboundTech.MODID, "textures/gui/machine_frame.png");

    /** Геометрия панели (генерится tools/gen_gui_assets.py). */
    protected static final int GAUGE_X = 10;
    protected static final int GAUGE_Y = 20;
    protected static final int GAUGE_W = 12;
    protected static final int GAUGE_H = 48;
    /** Заливка шкалы в текстуре: u 200, v 0, 12x48 (снизу вверх). */
    private static final int GAUGE_FILL_U = 200;
    private static final int GAUGE_FILL_V = 0;

    protected final TileEntity tile;

    public GuiMachine(ContainerMachine container) {
        super(container);
        this.tile = container.getTile();
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
        // тултип шкалы — точные числа EU
        if (this.tile instanceof IEnergyGauge
                && this.inGauge(mouseX - this.guiLeft, mouseY - this.guiTop)) {
            IEnergyGauge gauge = (IEnergyGauge) this.tile;
            this.drawHoveringText((int) gauge.gaugeEnergy() + " / "
                    + (int) gauge.gaugeCapacity() + " EU", mouseX, mouseY);
        }
    }

    private boolean inGauge(int x, int y) {
        return x >= GAUGE_X && x < GAUGE_X + GAUGE_W
                && y >= GAUGE_Y && y < GAUGE_Y + GAUGE_H;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks,
                                                   int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(PANEL);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0,
                this.xSize, this.ySize);
        if (this.tile instanceof IEnergyGauge) {
            IEnergyGauge gauge = (IEnergyGauge) this.tile;
            double cap = Math.max(1.0, gauge.gaugeCapacity());
            int filled = (int) Math.round(
                    GAUGE_H * Math.min(1.0, gauge.gaugeEnergy() / cap));
            if (filled > 0) {
                // заливка растёт снизу вверх
                this.drawTexturedModalRect(this.guiLeft + GAUGE_X,
                        this.guiTop + GAUGE_Y + GAUGE_H - filled,
                        GAUGE_FILL_U, GAUGE_FILL_V + GAUGE_H - filled,
                        GAUGE_W, filled);
            }
        }
        this.drawWidgets(mouseX, mouseY);
    }

    /** Виджеты конкретной машины — фоновой слой, наследники. */
    protected void drawWidgets(int mouseX, int mouseY) {
    }

    /** Вертикальный бар с рамкой: заливка снизу вверх цветом машины. */
    protected void drawFramedBar(int x, int y, int w, int h,
                                 double frac, int rgb) {
        int left = this.guiLeft + x;
        int top = this.guiTop + y;
        drawRect(left - 1, top - 1, left + w + 1, top + h + 1, 0xFF2A2136);
        drawRect(left, top, left + w, top + h, 0xFF120E1C);
        int filled = (int) Math.round(
                h * Math.max(0.0, Math.min(1.0, frac)));
        if (filled > 0) {
            drawRect(left, top + h - filled, left + w, top + h,
                    0xFF000000 | rgb);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Горизонтальный бар прогресса: заливка слева направо. */
    protected void drawFramedBarH(int x, int y, int w, int h,
                                  double frac, int rgb) {
        int left = this.guiLeft + x;
        int top = this.guiTop + y;
        drawRect(left - 1, top - 1, left + w + 1, top + h + 1, 0xFF2A2136);
        drawRect(left, top, left + w, top + h, 0xFF120E1C);
        int filled = (int) Math.round(
                w * Math.max(0.0, Math.min(1.0, frac)));
        if (filled > 0) {
            drawRect(left, top, left + filled, top + h, 0xFF000000 | rgb);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // заголовок — локализованное имя блока
        String title = this.tile.getBlockType().getLocalizedName();
        this.fontRenderer.drawString(title,
                (this.xSize - this.fontRenderer.getStringWidth(title)) / 2, 6,
                0x3A2A52);
        // причина простоя словами (ХФ-7) — клиентский IMachineStatus,
        // цветовые коды строки статуса работают и здесь
        if (this.tile instanceof IMachineStatus) {
            String status = ((IMachineStatus) this.tile).getStatusLine();
            List<String> lines = this.fontRenderer
                    .listFormattedStringToWidth(status, this.xSize - 16);
            int y = this.ySize - 10 - lines.size() * 9;
            for (String line : lines) {
                this.fontRenderer.drawString(line, 8, y, 0xFFFFFF);
                y += 9;
            }
        }
    }
}
