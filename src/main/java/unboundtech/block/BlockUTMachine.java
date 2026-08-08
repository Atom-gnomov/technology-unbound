package unboundtech.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import unboundtech.UTCreativeTab;
import unboundtech.tile.TileNodeWorker;

/**
 * База для машин мода: горизонтальный FACING + ACTIVE.
 * Мета: биты 0-1 — направление, бит 2 — активность.
 *
 * Шаблон снят с блоков порта (BlockAnimationTablet/BlockRepairer);
 * важное отличие BlockContainer — рендер по умолчанию INVISIBLE,
 * поэтому getRenderType переопределён на MODEL.
 */
public abstract class BlockUTMachine extends BlockContainer {

    public static final PropertyDirection FACING =
            PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    protected BlockUTMachine() {
        super(Material.IRON);
        this.setHardness(3.5F);
        this.setResistance(10.0F);
        this.setSoundType(SoundType.METAL);
        this.setHarvestLevel("pickaxe", 1);
        this.setCreativeTab(UTCreativeTab.INSTANCE);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(ACTIVE, Boolean.FALSE));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ACTIVE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        // stable_39: именно byHorizontalIndex (getHorizontal — имя из 1.8–1.11).
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(ACTIVE, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex()
                | (state.getValue(ACTIVE) ? 4 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        EnumFacing look = placer == null ? EnumFacing.NORTH : placer.getHorizontalFacing().getOpposite();
        return this.getDefaultState().withProperty(FACING, look).withProperty(ACTIVE, Boolean.FALSE);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        // Свет выводится из переданного состояния: движок света сравнивает
        // «до» и «после» и на повторном чтении мира разницы бы не увидел.
        return state.getBlock() == this && state.getValue(ACTIVE) ? 7 : 0;
    }

    /**
     * Переключает визуальное состояние блока, сохраняя направление.
     * Тайл переживает смену состояния благодаря переопределённому
     * {@code shouldRefresh} в {@link unboundtech.tile.TileNodeWorker}
     * (у модовых тайлов Forge по умолчанию пересоздаёт тайл при ЛЮБОЙ смене
     * состояния — без переопределения буфер EU обнулялся бы).
     */
    public static void setActiveState(World world, BlockPos pos, boolean active) {
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockUTMachine)) {
            return;
        }
        if (state.getValue(ACTIVE) == active) {
            return;
        }
        world.setBlockState(pos, state.withProperty(ACTIVE, active), 3);
    }

    /**
     * ПКМ пустой рукой — краткий статус машины в чат.
     * Полноценный GUI не входит в фазу 3а, но игрок должен понимать,
     * почему машина стоит (например, интерференция генераторов).
     */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote || !player.getHeldItem(hand).isEmpty()) {
            return false;
        }
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileNodeWorker) {
            player.sendStatusMessage(((TileNodeWorker) te).getStatusMessage(), false);
            return true;
        }
        return false;
    }
}
