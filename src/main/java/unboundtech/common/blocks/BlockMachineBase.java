package unboundtech.common.blocks;

import javax.annotation.Nullable;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Общий каркас блока-машины Unbound Technology (шаблон из
 * phase3_converters_spec.md §0.6): горизонтальный FACING + собственный
 * ACTIVE, мета = {@code facing | (active ? 8 : 0)}.
 *
 * Свечение при работе задаётся {@link #activeLightLevel()} — по канону
 * спеки генератор светится слабо (7), пока перерабатывает вис.
 */
public abstract class BlockMachineBase extends BlockContainer {

    public static final PropertyDirection FACING = PropertyDirection.create(
            "facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    protected BlockMachineBase() {
        super(Material.IRON);
        this.setHardness(3.0f);
        this.setResistance(15.0f);
        this.setSoundType(net.minecraft.block.SoundType.METAL);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(ACTIVE, Boolean.FALSE));
    }

    /** Уровень света работающей машины (0 — не светится). */
    protected int activeLightLevel() {
        return 0;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ACTIVE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(ACTIVE, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex()
                | (state.getValue(ACTIVE) ? 8 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
            float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        // Морда машины смотрит на игрока, как у машин IC2.
        return this.getDefaultState()
                .withProperty(FACING, placer.getHorizontalFacing().getOpposite())
                .withProperty(ACTIVE, Boolean.FALSE);
    }

    @Override
    public IBlockState withRotation(IBlockState state, net.minecraft.util.Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, net.minecraft.util.Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(ACTIVE) ? this.activeLightLevel() : 0;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    /**
     * Переключает ACTIVE. Тайл переживает смену состояния сам: {@code Chunk
     * .setBlockState} зовёт {@code breakBlock} и пересоздаёт TileEntity только
     * когда МЕНЯЕТСЯ САМ БЛОК, а у нас блок тот же — меняется лишь свойство.
     *
     * Поэтому здесь НЕТ пляски ванильной печи (сохранить тайл → setBlockState →
     * validate + setTileEntity): печи она нужна лишь потому, что горящая и
     * потухшая печь — два разных блока. Нам она была бы вредна: setTileEntity
     * во время тика тайлов кладёт тайл в {@code addedTileEntityList} повторно,
     * и машина начинает тикать дважды за тик.
     */
    public static void setActive(World world, BlockPos pos, boolean active) {
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockMachineBase)
                || state.getValue(ACTIVE) == active) {
            return;
        }
        world.setBlockState(pos, state.withProperty(ACTIVE, active), 3);
    }

    @Override
    @Nullable
    public abstract net.minecraft.tileentity.TileEntity createNewTileEntity(World world, int meta);

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this);
    }
}
