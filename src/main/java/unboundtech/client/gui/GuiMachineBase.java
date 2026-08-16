package unboundtech.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.UnboundTech;

/**
 * Каркас экрана машины: панель 176×166, две вертикальные шкалы и подписи.
 *
 * Геометрия панели и слотов — ванильная (скил {@code minecraft-basic-texture}):
 * рамка 4 пикселя, слот 18×18, инвентарь игрока с зазором в 4 пикселя перед
 * поясом. «Полные» полосы шкал лежат донорами в свободной части холста
 * (x=200 и x=216), отсюда и рисуются нужной высотой снизу вверх.
 */
@SideOnly(Side.CLIENT)
public abstract class GuiMachineBase extends GuiContainer {

    protected static final int GAUGE_X_LEFT = 8;
    protected static final int GAUGE_X_RIGHT = 160;
    protected static final int GAUGE_Y = 20;
    protected static final int GAUGE_W = 8;
    protected static final int GAUGE_H = 48;

    /** Позиции доноров «полной» полосы на текстуре. */
    protected static final int DONOR_LEFT = 200;
    protected static final int DONOR_RIGHT = 216;

    private final ResourceLocation texture;
    private final String titleKey;

    protected GuiMachineBase(Container container, String name, String titleKey) {
        super(container);
        this.texture = new ResourceLocation(UnboundTech.MODID, "textures/gui/" + name + ".png");
        this.titleKey = titleKey;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(this.texture);
        int left = this.guiLeft;
        int top = this.guiTop;
        this.drawTexturedModalRect(left, top, 0, 0, this.xSize, this.ySize);
        this.drawGauges(left, top);
    }

    protected abstract void drawGauges(int left, int top);

    /**
     * Рисует шкалу снизу вверх на долю {@code filled} (0…1).
     *
     * @param x     левый край шкалы на панели
     * @param donor x донора «полной» полосы на текстуре
     */
    protected void drawGauge(int left, int top, int x, int donor, double filled) {
        int height = (int) Math.round(Math.max(0.0, Math.min(1.0, filled)) * GAUGE_H);
        if (height <= 0) {
            return;
        }
        int y = GAUGE_Y + GAUGE_H - height;
        this.drawTexturedModalRect(left + x, top + y, donor, GAUGE_H - height, GAUGE_W, height);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format(this.titleKey);
        this.fontRenderer.drawString(title, (this.xSize - this.fontRenderer.getStringWidth(title)) / 2,
                6, 0x404040);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 94, 0x404040);
        this.drawReadout();
    }

    /** Строки показаний машины; координаты локальные для панели. */
    protected abstract void drawReadout();

    protected void line(int y, String text) {
        this.fontRenderer.drawString(text, 22, y, 0x404040);
    }
}
