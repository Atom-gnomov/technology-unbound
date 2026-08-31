package unboundtech.client.model;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.UnboundTech;

/**
 * Квант-Гибридная броня — общая геометрия двух эндгейм-путей по ТЗ
 * адверсарного воркфлоу (`docs/concepts/quantum_hybrid_armour_model.md`)
 * и канону `quantum_hybrid_armour.md` §8: «библиарий в силовой броне».
 *
 * ~44 именованных детали: корона из четырёх наклонных пластин с
 * самоцветом и капюшон-гребень, рунные височные полосы, разъём со
 * жгутом за левым ухом, многослойный нагрудник с КАЧАЮЩИМСЯ амулетом
 * на цепи и шкалой из четырёх ламп, рунные пломбы-таблички ТК, рёбра-
 * радиаторы спины, двухъярусные наплечники со светящейся щелью,
 * наручи с кристаллами, гримуар на левом бедре, сабатоны с соплами.
 * Путь А (Пустотный) несёт рваный плащ; путь Б (Ихорный) — без плаща,
 * различие материала и света — текстурой (§8.2: геометрия одна).
 *
 * Канон запрещает наследовать фортресс: extends ModelBiped, базовый
 * бипед рендерим сами (поддоспешник, полнотелость), оси: +X — ЛЕВАЯ
 * сторона персонажа, техническая сторона тела — левая.
 *
 * Эмиссив — ВТОРОЙ проход внутри render(): покадровые glow-текстуры
 * (8 кадров на путь), lightmap 240/240 с восстановлением (ТЗ п.7).
 */
@SideOnly(Side.CLIENT)
public class ModelQuantumHybridArmour extends ModelBiped {

    /** Путь А — Пустотный, путь Б — Ихорный. */
    public static final int PATH_VOID = 0;
    public static final int PATH_ICHOR = 1;

    private static final ResourceLocation[][] GLOW = new ResourceLocation[2][8];

    static {
        for (int f = 0; f < 8; f++) {
            GLOW[PATH_VOID][f] = new ResourceLocation(UnboundTech.MODID,
                    "textures/models/armor/quant_void_glow_" + f + ".png");
            GLOW[PATH_ICHOR][f] = new ResourceLocation(UnboundTech.MODID,
                    "textures/models/armor/quant_ichor_glow_" + f + ".png");
        }
    }

    /** Ставится предметом каждый кадр (ТЗ п.6-7: никакого кэша). */
    public int path = PATH_VOID;
    public int litLamps = 4;

    // поддоспешник
    private final ModelRenderer suitHead;
    private final ModelRenderer suitBody;
    private final ModelRenderer suitArmL;
    private final ModelRenderer suitArmR;
    private final ModelRenderer suitLegL;
    private final ModelRenderer suitLegR;
    // подвижные пивоты
    private final ModelRenderer amuletPivot;
    private final ModelRenderer hipAmuletPivot;
    private final ModelRenderer capePivot;
    private final ModelRenderer sealPlateR;
    private final ModelRenderer sealLegPlate;
    // группы по слотам
    private final List<ModelRenderer> headParts = new ArrayList<>();
    private final List<ModelRenderer> chestParts = new ArrayList<>();
    private final List<ModelRenderer> legParts = new ArrayList<>();
    private final List<ModelRenderer> feetParts = new ArrayList<>();
    // эмиссивы
    private final ModelRenderer crownGem;
    private final ModelRenderer slitGlowL;
    private final ModelRenderer slitGlowR;
    private final ModelRenderer[] lamps = new ModelRenderer[4];
    private final List<ModelRenderer> crystals = new ArrayList<>();
    private final ModelRenderer amuletMedallion;
    private final ModelRenderer hipMedallion;
    private final ModelRenderer nozzleL;
    private final ModelRenderer nozzleR;

    public ModelQuantumHybridArmour(float scale) {
        super(scale, 0.0F, 128, 64);

        // ===== поддоспешник: полнотелость (лестница инфляций ТЗ) =====
        this.suitHead = part(0, 0);
        this.suitHead.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, 1.0F);
        this.bipedHead.addChild(this.suitHead);
        this.suitBody = part(16, 16);
        this.suitBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4,
                scale >= 1.0F ? 1.05F : 0.3F);
        this.bipedBody.addChild(this.suitBody);
        this.suitArmR = part(40, 16);
        this.suitArmR.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, 0.4F);
        this.bipedRightArm.addChild(this.suitArmR);
        this.suitArmL = part(40, 16);
        this.suitArmL.mirror = true;
        this.suitArmL.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, 0.4F);
        this.bipedLeftArm.addChild(this.suitArmL);
        float legInf = scale >= 1.0F ? 0.55F : 0.35F;
        this.suitLegR = part(0, 16);
        this.suitLegR.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, legInf);
        this.bipedRightLeg.addChild(this.suitLegR);
        this.suitLegL = part(0, 16);
        this.suitLegL.mirror = true;
        this.suitLegL.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, legInf);
        this.bipedLeftLeg.addChild(this.suitLegL);

        // ===== шлем: корона, самоцвет, гребень, руны, разъём =====
        ModelRenderer crownFront = pivoted(64, 0, 0.0F, -8.6F, -4.9F);
        crownFront.addBox(-2.0F, -2.0F, -0.5F, 4, 3, 1);
        crownFront.rotateAngleX = 0.26F;
        ModelRenderer crownBack = pivoted(64, 0, 0.0F, -8.6F, 4.9F);
        crownBack.addBox(-2.0F, -2.0F, -0.5F, 4, 3, 1);
        crownBack.rotateAngleX = -0.26F;
        ModelRenderer crownL = pivoted(80, 0, 4.9F, -8.6F, 0.0F);
        crownL.addBox(-0.5F, -2.0F, -2.0F, 1, 3, 4);
        crownL.rotateAngleZ = 0.26F;
        ModelRenderer crownR = pivoted(80, 0, -4.9F, -8.6F, 0.0F);
        crownR.addBox(-0.5F, -2.0F, -2.0F, 1, 3, 4);
        crownR.rotateAngleZ = -0.26F;
        this.crownGem = part(64, 4);
        this.crownGem.addBox(-1.0F, -9.6F, -5.8F, 2, 2, 1);
        ModelRenderer hoodCrest = part(96, 0);
        hoodCrest.addBox(-1.0F, -11.2F, -4.5F, 2, 3, 9);
        ModelRenderer runeL = part(72, 4);
        runeL.addBox(5.05F, -7.6F, -2.5F, 1, 2, 5);
        ModelRenderer runeR = part(72, 4);
        runeR.mirror = true;
        runeR.addBox(-6.05F, -7.6F, -2.5F, 1, 2, 5);
        ModelRenderer earSocketL = part(64, 8);
        earSocketL.addBox(4.7F, -6.6F, 1.2F, 1, 2, 2);
        ModelRenderer cableBundleL = pivoted(72, 12, 5.15F, -4.6F, 2.0F);
        cableBundleL.addBox(0.0F, 0.0F, -1.0F, 1, 4, 2);
        cableBundleL.rotateAngleX = 0.12F;
        head(crownFront, crownBack, crownL, crownR, this.crownGem, hoodCrest,
                runeL, runeR, earSocketL, cableBundleL);

        // ===== нагрудник =====
        ModelRenderer collarStubL = part(80, 12);
        collarStubL.addBox(3.4F, -0.4F, 2.6F, 2, 1, 1);
        ModelRenderer chestPlate = part(64, 20);
        chestPlate.addBox(-4.0F, 0.5F, -3.8F, 8, 7, 1);
        this.amuletPivot = pivoted(0, 0, 0.0F, 1.0F, -4.15F);
        ModelRenderer chain = part(84, 14);
        chain.addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1);
        this.amuletMedallion = part(90, 14);
        this.amuletMedallion.addBox(-1.0F, 3.0F, -0.9F, 2, 3, 1);
        this.amuletPivot.addChild(chain);
        this.amuletPivot.addChild(this.amuletMedallion);
        for (int i = 0; i < 4; i++) {
            this.lamps[i] = part(98, 14);
            this.lamps[i].addBox(1.4F, 1.2F + i * 1.6F, -4.85F, 1, 1, 2);
        }
        this.sealPlateR = pivoted(106, 14, -3.1F, 1.6F, -3.7F);
        this.sealPlateR.addBox(-0.5F, 0.0F, -0.5F, 1, 4, 1);
        ModelRenderer beltBand = part(64, 28);
        beltBand.addBox(-4.0F, 9.0F, -2.0F, 8, 2, 4, 1.15F);
        // спина: рёбра-радиаторы и жгуты
        ModelRenderer fin1 = part(0, 32);
        fin1.addBox(-3.0F, 1.5F, 3.15F, 1, 6, 1);
        ModelRenderer fin2 = part(0, 32);
        fin2.addBox(-0.5F, 1.5F, 3.15F, 1, 6, 1);
        ModelRenderer fin3 = part(0, 32);
        fin3.addBox(2.0F, 1.5F, 3.15F, 1, 6, 1);
        ModelRenderer backCableL = pivoted(6, 32, 3.7F, 2.0F, 3.6F);
        backCableL.addBox(-0.5F, 0.0F, -0.5F, 1, 7, 1);
        backCableL.rotateAngleZ = 0.08F;
        ModelRenderer backCableR = pivoted(6, 32, -3.7F, 2.0F, 3.6F);
        backCableR.addBox(-0.5F, 0.0F, -0.5F, 1, 7, 1);
        backCableR.rotateAngleZ = -0.08F;
        // плащ пути А — реально за торцом рёбер
        this.capePivot = pivoted(0, 0, 0.0F, 1.5F, 4.3F);
        ModelRenderer capeUpper = part(88, 28);
        capeUpper.addBox(-4.0F, 0.0F, 0.0F, 8, 6, 1);
        ModelRenderer capeLower = part(88, 36);
        capeLower.addBox(-3.0F, 6.0F, 0.2F, 6, 5, 1);
        this.capePivot.addChild(capeUpper);
        this.capePivot.addChild(capeLower);
        body(collarStubL, chestPlate, this.amuletPivot, this.lamps[0],
                this.lamps[1], this.lamps[2], this.lamps[3], this.sealPlateR,
                beltBand, fin1, fin2, fin3, backCableL, backCableR, this.capePivot);

        // ===== наплечники (двухъярусные со светящейся щелью) =====
        ModelRenderer pauldronTopL = part(12, 32);
        pauldronTopL.mirror = true;
        pauldronTopL.addBox(-1.5F, -3.0F, -2.5F, 6, 3, 5, 0.35F);
        pauldronTopL.rotateAngleZ = -0.17F;
        ModelRenderer pauldronTopR = part(12, 32);
        pauldronTopR.addBox(-4.5F, -3.0F, -2.5F, 6, 3, 5, 0.35F);
        pauldronTopR.rotateAngleZ = 0.17F;
        ModelRenderer pauldronLowL = part(12, 42);
        pauldronLowL.mirror = true;
        pauldronLowL.addBox(-1.0F, 1.4F, -2.0F, 5, 2, 4, 0.35F);
        ModelRenderer pauldronLowR = part(12, 42);
        pauldronLowR.addBox(-4.0F, 1.4F, -2.0F, 5, 2, 4, 0.35F);
        this.slitGlowL = part(12, 50);
        this.slitGlowL.mirror = true;
        this.slitGlowL.addBox(-1.0F, 0.0F, -2.0F, 5, 1, 4, 0.25F);
        this.slitGlowR = part(12, 50);
        this.slitGlowR.addBox(-4.0F, 0.0F, -2.0F, 5, 1, 4, 0.25F);
        // наручи с кристаллами и перчаточными жгутами
        ModelRenderer bracerL = part(36, 32);
        bracerL.mirror = true;
        bracerL.addBox(-1.0F, 6.0F, -2.0F, 4, 4, 4, 0.65F);
        ModelRenderer bracerR = part(36, 32);
        bracerR.addBox(-3.0F, 6.0F, -2.0F, 4, 4, 4, 0.65F);
        for (int k = 0; k < 3; k++) {
            ModelRenderer cl = part(54, 32);
            cl.addBox(3.15F, 6.3F + k * 1.2F, -0.5F, 1, 1, 1);
            ModelRenderer cr = part(54, 32);
            cr.addBox(-4.15F, 6.3F + k * 1.2F, -0.5F, 1, 1, 1);
            this.crystals.add(cl);
            this.crystals.add(cr);
            this.bipedLeftArm.addChild(cl);
            this.bipedRightArm.addChild(cr);
            this.chestParts.add(cl);
            this.chestParts.add(cr);
        }
        ModelRenderer gloveCableL = pivoted(58, 32, 3.4F, 9.6F, 0.0F);
        gloveCableL.addBox(-0.5F, 0.0F, -0.5F, 1, 2, 1);
        gloveCableL.rotateAngleX = 0.1F;
        ModelRenderer gloveCableR = pivoted(58, 32, -3.4F, 9.6F, 0.0F);
        gloveCableR.addBox(-0.5F, 0.0F, -0.5F, 1, 2, 1);
        gloveCableR.rotateAngleX = 0.1F;
        armL(pauldronTopL, pauldronLowL, this.slitGlowL, bracerL, gloveCableL);
        armR(pauldronTopR, pauldronLowR, this.slitGlowR, bracerR, gloveCableR);

        // ===== поножи =====
        ModelRenderer thighPlateR = part(36, 42);
        thighPlateR.addBox(-2.0F, 0.4F, -2.95F, 4, 4, 1);
        this.bipedRightLeg.addChild(thighPlateR);
        ModelRenderer thighPlateL = part(36, 42);
        thighPlateL.mirror = true;
        thighPlateL.addBox(-2.0F, 0.4F, -2.95F, 4, 4, 1);
        this.bipedLeftLeg.addChild(thighPlateL);
        ModelRenderer pouchL = part(48, 42);
        pouchL.addBox(1.2F, 9.4F, -3.9F, 2, 3, 1);
        ModelRenderer pouchR = part(48, 42);
        pouchR.mirror = true;
        pouchR.addBox(-3.2F, 9.4F, -3.9F, 2, 3, 1);
        this.bipedBody.addChild(pouchL);
        this.bipedBody.addChild(pouchR);
        this.hipAmuletPivot = pivoted(0, 0, -2.8F, 1.2F, 0.0F);
        ModelRenderer hipChain = part(56, 42);
        hipChain.addBox(-0.5F, 0.0F, -0.5F, 1, 2, 1);
        this.hipMedallion = part(36, 48);
        this.hipMedallion.addBox(-1.0F, 2.0F, -0.6F, 2, 2, 1);
        this.hipAmuletPivot.addChild(hipChain);
        this.hipAmuletPivot.addChild(this.hipMedallion);
        this.bipedRightLeg.addChild(this.hipAmuletPivot);
        this.sealLegPlate = pivoted(44, 48, 0.5F, 4.8F, -2.55F);
        this.sealLegPlate.addBox(-0.5F, 0.0F, -0.5F, 1, 4, 1);
        this.bipedLeftLeg.addChild(this.sealLegPlate);
        ModelRenderer grimoireBody = part(50, 48);
        grimoireBody.addBox(2.55F, 1.0F, -1.5F, 1, 3, 3);
        ModelRenderer grimoireClasp = part(60, 48);
        grimoireClasp.addBox(2.6F, 2.0F, -2.1F, 1, 1, 1);
        this.bipedLeftLeg.addChild(grimoireBody);
        this.bipedLeftLeg.addChild(grimoireClasp);
        this.legParts.add(thighPlateR);
        this.legParts.add(thighPlateL);
        this.legParts.add(pouchL);
        this.legParts.add(pouchR);
        this.legParts.add(this.hipAmuletPivot);
        this.legParts.add(this.sealLegPlate);
        this.legParts.add(grimoireBody);
        this.legParts.add(grimoireClasp);

        // ===== ботинки =====
        this.nozzleR = part(10, 56);
        this.nozzleR.addBox(-1.0F, 11.5F, -0.5F, 2, 1, 2);
        this.nozzleL = part(10, 56);
        this.nozzleL.mirror = true;
        this.nozzleL.addBox(-1.0F, 11.5F, -0.5F, 2, 1, 2);
        foot(this.bipedRightLeg, sab(false), shin(false), this.nozzleR, heel(false));
        foot(this.bipedLeftLeg, sab(true), shin(true), this.nozzleL, heel(true));
    }

    // --- фабрики повторяющихся деталей ботинок ---
    private ModelRenderer sab(boolean mirror) {
        ModelRenderer m = part(72, 52);   // переезд UV: коллизия с pauld_low (ревью №16)
        m.mirror = mirror;
        m.addBox(-2.5F, 8.6F, -2.6F, 5, 3, 5, 0.85F);
        return m;
    }

    private ModelRenderer shin(boolean mirror) {
        ModelRenderer m = part(0, 50);
        m.mirror = mirror;
        m.addBox(-1.5F, 5.4F, -3.5F, 3, 3, 1);
        return m;
    }

    private ModelRenderer heel(boolean mirror) {
        ModelRenderer m = part(20, 56);
        m.mirror = mirror;
        m.addBox(-1.0F, 9.4F, 3.35F, 2, 2, 1);
        return m;
    }

    private ModelRenderer part(int u, int v) {
        return new ModelRenderer(this, u, v);
    }

    private ModelRenderer pivoted(int u, int v, float x, float y, float z) {
        ModelRenderer m = part(u, v);
        m.setRotationPoint(x, y, z);
        return m;
    }

    private void head(ModelRenderer... parts) {
        for (ModelRenderer m : parts) {
            this.bipedHead.addChild(m);
            this.headParts.add(m);
        }
    }

    private void body(ModelRenderer... parts) {
        for (ModelRenderer m : parts) {
            this.bipedBody.addChild(m);
            this.chestParts.add(m);
        }
    }

    private void armL(ModelRenderer... parts) {
        for (ModelRenderer m : parts) {
            this.bipedLeftArm.addChild(m);
            this.chestParts.add(m);
        }
    }

    private void armR(ModelRenderer... parts) {
        for (ModelRenderer m : parts) {
            this.bipedRightArm.addChild(m);
            this.chestParts.add(m);
        }
    }

    private void foot(ModelRenderer leg, ModelRenderer... parts) {
        for (ModelRenderer m : parts) {
            leg.addChild(m);
            this.feetParts.add(m);
        }
    }

    /** Видимость по слоту; ТЗ-чеклист п.6: все 4 комбинации слотов. */
    public void prepareSlot(EntityEquipmentSlot slot) {
        boolean chest = slot == EntityEquipmentSlot.CHEST;
        boolean legs = slot == EntityEquipmentSlot.LEGS;
        boolean feet = slot == EntityEquipmentSlot.FEET;
        for (ModelRenderer m : this.headParts) {
            m.showModel = slot == EntityEquipmentSlot.HEAD;
        }
        for (ModelRenderer m : this.chestParts) {
            m.showModel = chest;
        }
        for (ModelRenderer m : this.legParts) {
            m.showModel = legs;
        }
        for (ModelRenderer m : this.feetParts) {
            m.showModel = feet;
        }
        this.suitArmL.showModel = chest;
        this.suitArmR.showModel = chest;
        this.suitBody.showModel = chest || legs;
        this.suitLegL.showModel = legs || feet;
        this.suitLegR.showModel = legs || feet;
        // плащ — только у Пустотного (§8.2)
        if (this.capePivot.showModel) {
            this.capePivot.showModel = this.path == PATH_VOID;
        }
        // сопла — эмиссив только у Ихорного, геометрия у обоих
    }

    /** Анимации ТЗ: амулеты, плащ, трепет пломб. */
    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount,
                                  float ageInTicks, float netHeadYaw,
                                  float headPitch, float scaleFactor, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scaleFactor, entity);
        // амулет груди: асимметричный кламп — вперёд свободно, назад чуть
        float ax = MathHelper.cos(limbSwing * 0.66F) * limbSwingAmount * 0.25F
                + MathHelper.sin(ageInTicks * 0.067F) * 0.04F;
        this.amuletPivot.rotateAngleX = MathHelper.clamp(ax, -0.3F, 0.05F);
        this.amuletPivot.rotateAngleZ =
                MathHelper.cos(limbSwing * 0.33F) * limbSwingAmount * 0.1F;
        // плащ пути А
        float cape = 0.1F + limbSwingAmount * 0.55F
                + MathHelper.sin(ageInTicks * 0.05F) * 0.035F;
        if (entity != null && entity.isSneaking()) {
            cape = Math.min(cape, 0.45F);
        }
        this.capePivot.rotateAngleX = cape;
        // амулет бедра — противофаза маху ноги
        this.hipAmuletPivot.rotateAngleX =
                -MathHelper.cos(limbSwing * 0.66F) * limbSwingAmount * 0.15F;
        // пломбы трепещут вокруг своих пивотов
        this.sealPlateR.rotateAngleZ = MathHelper.sin(ageInTicks * 0.09F) * 0.05F;
        this.sealLegPlate.rotateAngleZ =
                MathHelper.sin(ageInTicks * 0.09F + 1.7F) * 0.05F;
    }

    /** Рендер: база + эмиссив-проход покадровой glow-текстурой (ТЗ). */
    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount,
                       float ageInTicks, float netHeadYaw, float headPitch,
                       float scale) {
        super.render(entity, limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scale);
        this.renderGlow(entity, scale);
    }

    private void renderGlow(Entity entity, float scale) {
        // Глинт-проход LayerArmorBase зовёт render() повторно с
        // depthFunc(EQUAL) и своей текстурой — туда соваться нельзя
        // (ревью №13: перебинд и disableBlend ломали зачарованную броню)
        if (org.lwjgl.opengl.GL11.glGetInteger(
                org.lwjgl.opengl.GL11.GL_DEPTH_FUNC)
                == org.lwjgl.opengl.GL11.GL_EQUAL) {
            return;
        }
        long time = entity != null && entity.world != null
                ? entity.world.getTotalWorldTime() : 0L;
        int frame = this.path == PATH_ICHOR
                ? (int) (time / 3 % 8) : (int) (time / 10 % 8);
        Minecraft.getMinecraft().getTextureManager()
                .bindTexture(GLOW[this.path][frame]);
        float lastX = OpenGlHelper.lastBrightnessX;
        float lastY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                240.0F, 240.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        // повторяем пролог ванильного ModelBiped.render: без сдвига
        // 0.2 присед разносил glow и броню (ревью №14)
        GlStateManager.pushMatrix();
        if (entity != null && entity.isSneaking()) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
        }

        // Приём прохода: glow-файл ПРОЗРАЧЕН везде, кроме развёрток
        // светящихся деталей — перерисовка всех частей теми же
        // координатами подсвечивает только их (совпадающая глубина
        // проходит LEQUAL, прозрачные текселы не видны из-за бленда).
        // Шкала пути А: свободные лампы гасим на время прохода.
        boolean[] lampState = new boolean[4];
        int lit = this.path == PATH_ICHOR ? 4 : this.litLamps;
        for (int i = 0; i < 4; i++) {
            lampState[i] = this.lamps[i].showModel;
            this.lamps[i].showModel = lampState[i] && i < lit;
        }
        this.bipedHead.render(scale);
        this.bipedBody.render(scale);
        this.bipedRightArm.render(scale);
        this.bipedLeftArm.render(scale);
        this.bipedRightLeg.render(scale);
        this.bipedLeftLeg.render(scale);
        for (int i = 0; i < 4; i++) {
            this.lamps[i].showModel = lampState[i];
        }

        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                lastX, lastY);
    }
}
