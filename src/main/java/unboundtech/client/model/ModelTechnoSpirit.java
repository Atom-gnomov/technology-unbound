package unboundtech.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Техно-дух (`techno_spirit.md` §8): сгусток с вращающимся кольцом из
 * четырёх «искр», как у схемы IC2. Прототип — форма «Механизм» (коробка
 * на ножках, §4.2); остальные семь обличий придут со своими моделями
 * (чужие ассеты нельзя — §4.2 ⚠️: лицензии).
 */
@SideOnly(Side.CLIENT)
public class ModelTechnoSpirit extends ModelBase {

    private final ModelRenderer core;
    private final ModelRenderer legR;
    private final ModelRenderer legL;
    private final ModelRenderer ring;

    public ModelTechnoSpirit() {
        this.textureWidth = 32;
        this.textureHeight = 32;

        this.core = new ModelRenderer(this, 0, 0);
        this.core.addBox(-3.0F, -3.0F, -3.0F, 6, 6, 6);
        this.core.setRotationPoint(0.0F, 16.0F, 0.0F);

        this.legR = new ModelRenderer(this, 0, 12);
        this.legR.addBox(-0.5F, 0.0F, -0.5F, 1, 4, 1);
        this.legR.setRotationPoint(-1.5F, 19.0F, 0.0F);
        this.legL = new ModelRenderer(this, 0, 12);
        this.legL.addBox(-0.5F, 0.0F, -0.5F, 1, 4, 1);
        this.legL.setRotationPoint(1.5F, 19.0F, 0.0F);

        // кольцо: четыре искры на невидимой оси
        this.ring = new ModelRenderer(this, 0, 18);
        this.ring.setRotationPoint(0.0F, 16.0F, 0.0F);
        for (int i = 0; i < 4; i++) {
            ModelRenderer spark = new ModelRenderer(this, 16, 18);
            double a = Math.PI / 2 * i;
            spark.addBox((float) (Math.cos(a) * 6.0) - 1.0F, -1.0F,
                    (float) (Math.sin(a) * 6.0) - 1.0F, 2, 2, 2);
            this.ring.addChild(spark);
        }
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount,
                       float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scale, entity);
        this.core.render(scale);
        this.legR.render(scale);
        this.legL.render(scale);
        this.ring.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount,
                                  float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale, Entity entity) {
        // кольцо крутится всегда; ножки болтаются в полёте
        this.ring.rotateAngleY = ageInTicks * 0.12F;
        this.legR.rotateAngleX = (float) Math.sin(ageInTicks * 0.3F) * 0.4F;
        this.legL.rotateAngleX = -(float) Math.sin(ageInTicks * 0.3F) * 0.4F;
        this.core.rotateAngleY = netHeadYaw * 0.017453292F;
    }
}
