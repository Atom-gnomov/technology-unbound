package unboundtech.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Флюкс-Аркебуза — модель по ТЗ адверсарного воркфлоу
 * (`docs/concepts/flux_arquebus_model.md`): 35 боксов, самое длинное
 * оружие пака (78 удвоенных юнитов, рендер 1/32, дуло в -X, Y вверх).
 *
 * Группы: приклад-ложа великодрева со сплошной замочной колодкой,
 * звёздчатый ствол (45°-child, «союз двух квадратов» — 8-конечный
 * фасонный профиль школы Флана), ГЛАДКИЙ казённик под glow-щели,
 * латунный замок, ПОДВИЖНЫЙ серпентин с эмиссив-угольком, шомпол
 * (только offsetX), переключаемые щели гнезда.
 *
 * Текстура — полосы материалов 128x64 (как у револьвера):
 * сталь y0..14, таумий y16..30, великодрево y32..46, латунь y48..56,
 * свечение y58..63.
 */
@SideOnly(Side.CLIENT)
public class ModelFluxArquebus extends ModelBase {

    private final ModelRenderer stock;
    private final ModelRenderer barrel;
    private final ModelRenderer lock;
    public final ModelRenderer serpentine;
    public final ModelRenderer matchTip;
    public final ModelRenderer ramrod;
    public final ModelRenderer glowTop;
    public final ModelRenderer glowSide;

    public ModelFluxArquebus() {
        this.textureWidth = 128;
        this.textureHeight = 64;

        // === stock: великодрево + латунь (10 боксов) ===
        this.stock = new ModelRenderer(this, 0, 0);
        this.stock.setTextureOffset(0, 32).addBox(-30.0F, -5.0F, -3.0F, 26, 3, 6);
        this.stock.setTextureOffset(0, 48).addBox(-31.0F, -5.0F, -3.0F, 1, 3, 6);
        // замочная колодка — мостик казённик→шейка, силуэт сплошной
        this.stock.setTextureOffset(70, 32).addBox(-4.0F, -5.0F, -2.5F, 10, 7, 5);
        this.stock.setTextureOffset(70, 32).addBox(6.0F, -4.0F, -2.5F, 8, 6, 5);
        this.stock.setTextureOffset(20, 32).addBox(14.0F, -7.0F, -3.0F, 16, 8, 6);
        this.stock.setTextureOffset(40, 32).addBox(16.0F, 1.0F, -2.0F, 14, 2, 4);
        this.stock.setTextureOffset(50, 38).addBox(22.0F, -10.0F, -2.5F, 8, 3, 5);
        this.stock.setTextureOffset(16, 48).addBox(30.0F, -10.0F, -3.0F, 1, 13, 6);
        this.stock.setTextureOffset(34, 48).addBox(-28.0F, -7.0F, -1.0F, 2, 2, 2);
        this.stock.setTextureOffset(34, 48).addBox(-20.0F, -7.0F, -1.0F, 2, 2, 2);

        // === barrel: закалённый таумий + латунь (9 боксов) ===
        this.barrel = new ModelRenderer(this, 0, 0);
        this.barrel.setTextureOffset(0, 16).addBox(-48.0F, -2.0F, -2.0F, 34, 4, 4);
        // звёздчатый 45°-child: союз двух квадратов, рёбра на радиусе 2.83
        ModelRenderer star = new ModelRenderer(this, 0, 16);
        star.setRotationPoint(-31.0F, 0.0F, 0.0F);
        star.rotateAngleX = 0.7853982F;
        star.addBox(-15.99F, -2.0F, -2.0F, 32, 4, 4);
        this.barrel.addChild(star);
        // казённик — ГЛАДКИЙ (блокер критиков: ребро прошивало glow-щели)
        this.barrel.setTextureOffset(80, 16).addBox(-14.0F, -3.0F, -3.0F, 10, 6, 6);
        this.barrel.setTextureOffset(44, 48).addBox(-49.0F, -3.0F, -3.0F, 2, 6, 6);
        this.barrel.setTextureOffset(60, 48).addBox(-26.0F, -6.0F, -3.5F, 2, 9, 7);
        this.barrel.setTextureOffset(60, 48).addBox(-18.0F, -6.0F, -3.5F, 2, 9, 7);
        this.barrel.setTextureOffset(78, 48).addBox(-49.0F, 3.0F, -0.5F, 2, 3, 1);
        this.barrel.setTextureOffset(78, 48).addBox(-6.0F, 3.0F, -1.5F, 2, 1, 3);
        // штыковой хомут — узел штыка живёт в геометрии всегда
        this.barrel.setTextureOffset(86, 48).addBox(-45.0F, -3.5F, -3.5F, 2, 7, 7);

        // === lock: латунь (7 боксов) ===
        this.lock = new ModelRenderer(this, 0, 0);
        this.lock.setTextureOffset(0, 48).addBox(-2.0F, -4.0F, 2.5F, 10, 6, 1);
        this.lock.setTextureOffset(34, 48).addBox(-2.0F, -1.0F, 3.0F, 3, 2, 2);
        this.lock.setTextureOffset(34, 48).addBox(-1.0F, -3.5F, 3.5F, 1, 1, 1);
        this.lock.setTextureOffset(34, 48).addBox(6.5F, -3.5F, 3.5F, 1, 1, 1);
        this.lock.setTextureOffset(34, 48).addBox(2.0F, -7.0F, -0.5F, 1, 3, 1);
        this.lock.setTextureOffset(0, 55).addBox(0.0F, -9.0F, -1.0F, 9, 1, 2);
        this.lock.setTextureOffset(0, 55).addBox(0.0F, -9.0F, -1.0F, 1, 4, 2);

        // === serpentine: ПОДВИЖНАЯ, сталь мода (5 боксов) ===
        this.serpentine = new ModelRenderer(this, 0, 0);
        this.serpentine.setRotationPoint(3.0F, -1.0F, 3.5F);
        this.serpentine.setTextureOffset(0, 0).addBox(-1.0F, -1.0F, -0.5F, 2, 7, 1);
        this.serpentine.setTextureOffset(8, 0).addBox(-3.0F, 6.0F, -0.5F, 4, 2, 1);
        this.serpentine.setTextureOffset(20, 0).addBox(-4.0F, 5.0F, -1.0F, 2, 2, 2);
        this.serpentine.setTextureOffset(30, 0).addBox(-4.5F, 3.5F, -0.5F, 1, 2, 1);
        // тлеющий кончик — ЭМИССИВ №1: НЕ child, а близнец с тем же
        // пивотом; рендер копирует ему угол серпентина и рисует отдельным
        // fullbright-проходом, не окрашивая сталь плеча
        this.matchTip = new ModelRenderer(this, 0, 58);
        this.matchTip.setRotationPoint(3.0F, -1.0F, 3.5F);
        this.matchTip.addBox(-4.5F, 2.5F, -0.5F, 1, 1, 1);

        // === ramrod: ПОДВИЖНАЯ, только offsetX (2 бокса) ===
        this.ramrod = new ModelRenderer(this, 0, 0);
        this.ramrod.setTextureOffset(40, 0).addBox(-46.0F, -6.5F, -0.5F, 30, 1, 1);
        this.ramrod.setTextureOffset(34, 48).addBox(-48.0F, -7.0F, -1.0F, 2, 2, 2);

        // === chamberGlow: ЭМИССИВ №2, greyscale + тонировка цветом ===
        this.glowTop = new ModelRenderer(this, 8, 58);
        this.glowTop.addBox(-10.0F, 3.01F, -1.5F, 6, 1, 3);
        this.glowSide = new ModelRenderer(this, 8, 58);
        this.glowSide.addBox(-10.0F, -1.5F, 3.01F, 5, 3, 1);
    }

    /** Базовый проход: всё, кроме эмиссивов. */
    public void renderBody(float scale) {
        this.stock.render(scale);
        this.barrel.render(scale);
        this.lock.render(scale);
        this.serpentine.render(scale);
        this.ramrod.render(scale);
    }

    /** Fullbright-проход №1: тлеющий уголёк — угол серпентина скопирован. */
    public void renderMatchTip(float scale) {
        this.matchTip.rotateAngleZ = this.serpentine.rotateAngleZ;
        this.matchTip.render(scale);
    }

    /** Fullbright-проход №2: щели гнезда (если ствол заряжен). */
    public void renderChamberGlow(float scale) {
        this.glowTop.render(scale);
        this.glowSide.render(scale);
    }
}
