package unboundtech.client;

import java.util.List;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Обёртка запечённой модели, записывающая ТЕКУЩИЙ TransformType перед
 * TEISR-рендером (ТЗ аркебузы §1: TEISR сам не знает контекста — а
 * анимации положены только в руках). Ставится на модель предмета в
 * ModelBakeEvent; всё делегирует, в handlePerspective пишет контекст.
 */
@SideOnly(Side.CLIENT)
public class PerspectiveCapture implements IBakedModel {

    private final IBakedModel parent;

    public PerspectiveCapture(IBakedModel parent) {
        this.parent = parent;
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(
            ItemCameraTransforms.TransformType type) {
        RenderFluxArquebus.context = type;
        Pair<? extends IBakedModel, Matrix4f> pair = this.parent.handlePerspective(type);
        return Pair.of(this, pair.getRight());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state,
                                    @Nullable EnumFacing side, long rand) {
        return this.parent.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return this.parent.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.parent.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return this.parent.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.parent.getParticleTexture();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return this.parent.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return this.parent.getOverrides();
    }
}
