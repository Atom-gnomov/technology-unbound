package unboundtech.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Флюкс-Револьвер — детальная Java-модель школы Flan's Mod (просьба
 * владельца): два десятка боксов, барабан — настоящая группа из шести
 * камор вокруг оси, ПРОВОРАЧИВАЕТСЯ по остатку патронов, светятся
 * только заряженные гнёзда (`flux_revolver.md` §8).
 *
 * Построена в УДВОЕННЫХ юнитах (рендер 1/32): ванильный ModelRenderer
 * принимает только целые размеры бокса, а оружию нужны полутона —
 * тот же приём, что ModelRendererTurbo у Флана, но без чужого кода.
 *
 * Оси: ствол вдоль -X (дуло влево), Y вверх, центр — ось барабана.
 */
@SideOnly(Side.CLIENT)
public class ModelFluxRevolver extends ModelBase {

    private final ModelRenderer body;
    private final ModelRenderer drum;
    private final ModelRenderer[] glows = new ModelRenderer[6];
    private final ModelRenderer hammer;
    private final ModelRenderer grip;

    /** Угол проворота барабана, радианы; ставит рендер по патронам. */
    public float drumAngle;
    /** Сколько гнёзд светится (заряжены), 0..6. */
    public int litChambers;

    public ModelFluxRevolver() {
        this.textureWidth = 128;
        this.textureHeight = 64;

        this.body = new ModelRenderer(this, 0, 0);
        // ствол
        this.body.setTextureOffset(0, 0).addBox(-18.0F, -1.0F, -2.0F, 16, 4, 4);
        // кожух-утолщение у дула
        this.body.setTextureOffset(40, 0).addBox(-19.0F, -2.0F, -3.0F, 4, 6, 6);
        // верхняя планка рамы над барабаном
        this.body.setTextureOffset(0, 10).addBox(-2.0F, 3.0F, -2.0F, 14, 2, 4);
        // передняя стойка рамы
        this.body.setTextureOffset(60, 0).addBox(-2.0F, -5.0F, -2.0F, 3, 8, 4);
        // казённая часть
        this.body.setTextureOffset(74, 0).addBox(8.0F, -6.0F, -2.2F, 4, 11, 4);
        // нижняя перемычка рамы под барабаном
        this.body.setTextureOffset(0, 18).addBox(-1.0F, -6.0F, -1.8F, 10, 2, 4);
        // курок — см. отдельную группу; мушка и целик:
        this.body.setTextureOffset(58, 14).addBox(-17.8F, 3.0F, -0.5F, 2, 2, 1);
        this.body.setTextureOffset(66, 14).addBox(10.0F, 5.0F, -1.0F, 2, 1, 2);
        // боковые щёчки-накладки рамы
        this.body.setTextureOffset(76, 16).addBox(8.6F, -3.0F, -3.0F, 3, 3, 1);
        this.body.setTextureOffset(76, 16).addBox(8.6F, -3.0F, 2.0F, 3, 3, 1);
        // спусковой крючок
        this.body.setTextureOffset(44, 12).addBox(4.4F, -7.4F, -0.6F, 1, 2, 1);
        // экстракторная трубка под стволом (латунь)
        this.body.setTextureOffset(92, 48).addBox(-16.8F, -4.4F, -1.2F, 12, 2, 2);
        // спусковая скоба (латунь): низ и передняя дужка
        this.body.setTextureOffset(0, 48).addBox(0.8F, -9.2F, -1.0F, 7, 1, 2);
        this.body.setTextureOffset(20, 48).addBox(0.8F, -9.2F, -1.0F, 1, 4, 2);

        // --- барабан: шесть камор вокруг оси X ---
        this.drum = new ModelRenderer(this, 0, 0);
        this.drum.setRotationPoint(3.0F, 0.0F, 0.0F);
        this.drum.setTextureOffset(90, 8).addBox(-3.2F, -2.2F, -2.2F, 7, 4, 4);
        // ось (латунь), торчит вперёд и назад
        this.drum.setTextureOffset(40, 48).addBox(-4.4F, -1.0F, -1.0F, 10, 2, 2);
        for (int i = 0; i < 6; i++) {
            double a = Math.PI / 3 * i;
            float oy = (float) (Math.cos(a) * 3.5);
            float oz = (float) (Math.sin(a) * 3.5);
            ModelRenderer chamber = new ModelRenderer(this, 0, 0);
            chamber.setTextureOffset(90, 0).addBox(-3.4F + (i % 2) * 0.01F,
                    oy - 1.5F, oz - 1.5F, 7, 3, 3);
            this.drum.addChild(chamber);
            // светящееся жерло каморы сзади (флюкс)
            ModelRenderer glow = new ModelRenderer(this, 0, 0);
            glow.setTextureOffset(0, 58).addBox(3.2F, oy - 1.0F, oz - 1.0F, 1, 2, 2);
            this.drum.addChild(glow);
            this.glows[i] = glow;
        }

        // --- курок, отведён назад ---
        this.hammer = new ModelRenderer(this, 36, 14);
        this.hammer.setRotationPoint(12.0F, 4.5F, 0.0F);
        this.hammer.addBox(-1.0F, -1.0F, -1.0F, 3, 2, 2);
        this.hammer.setTextureOffset(48, 14).addBox(2.0F, -1.6F, -1.0F, 1, 4, 2);
        this.hammer.rotateAngleZ = -0.35F;

        // --- рукоять под углом: дерево, щёчки, латунный тыльник ---
        this.grip = new ModelRenderer(this, 0, 32);
        this.grip.setRotationPoint(10.4F, -4.4F, 0.0F);
        this.grip.rotateAngleZ = 0.42F;   // ~24° назад
        this.grip.addBox(-2.2F, -11.6F, -2.0F, 5, 12, 4);
        this.grip.setTextureOffset(20, 32).addBox(-1.8F, -10.4F, -2.5F, 4, 9, 1);
        this.grip.setTextureOffset(20, 32).addBox(-1.8F, -10.4F, 1.5F, 4, 9, 1);
        this.grip.setTextureOffset(64, 48).addBox(-2.6F, -13.4F, -2.2F, 6, 2, 4);
    }

    /** Полный рендер; юниты удвоены — масштаб вдвое мельче обычного. */
    public void renderGun(float scale) {
        this.drum.rotateAngleX = this.drumAngle;
        for (int i = 0; i < 6; i++) {
            this.glows[i].showModel = i < this.litChambers;
        }
        this.body.render(scale);
        this.drum.render(scale);
        this.hammer.render(scale);
        this.grip.render(scale);
    }
}
