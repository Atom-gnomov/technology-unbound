package unboundtech.common.blocks;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Эссентиальный Кабель (`05_objects/essentia_conduit.md`): пассивная
 * магистраль между шинными узлами. Сам ничего не делает — только задаёт,
 * сколько каналов может идти по линии (2/4/8 по тиру); за перекачку
 * платит узел. К трубам ТК не подключается — это работа узла (§2).
 */
public class BlockEssentiaConduit extends Block {

    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyBool UP = PropertyBool.create("up");
    public static final PropertyBool DOWN = PropertyBool.create("down");

    private static final AxisAlignedBB CORE_AABB =
            new AxisAlignedBB(5 / 16.0, 5 / 16.0, 5 / 16.0,
                    11 / 16.0, 11 / 16.0, 11 / 16.0);

    /** Каналов на линии (§5): тир читается глазом по жилам на торце. */
    public final int channels;

    public BlockEssentiaConduit(int channels) {
        super(Material.IRON);
        this.channels = channels;
        this.setHardness(1.0f);
        this.setResistance(5.0f);
        this.setSoundType(net.minecraft.block.SoundType.METAL);
        this.setHarvestLevel("pickaxe", 1);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(NORTH, false).withProperty(SOUTH, false)
                .withProperty(EAST, false).withProperty(WEST, false)
                .withProperty(UP, false).withProperty(DOWN, false));
    }

    /** Кабель стыкуется только со своими: кабель, узел, контроллер (§2). */
    public static boolean joins(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        return block instanceof BlockEssentiaConduit
                || block instanceof BlockBusNode
                || block instanceof BlockVaultController;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;   // соединения вычисляются, в мету не пишутся
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState();
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state
                .withProperty(NORTH, joins(world, pos.north()))
                .withProperty(SOUTH, joins(world, pos.south()))
                .withProperty(EAST, joins(world, pos.east()))
                .withProperty(WEST, joins(world, pos.west()))
                .withProperty(UP, joins(world, pos.up()))
                .withProperty(DOWN, joins(world, pos.down()));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        double x0 = 5 / 16.0, y0 = 5 / 16.0, z0 = 5 / 16.0;
        double x1 = 11 / 16.0, y1 = 11 / 16.0, z1 = 11 / 16.0;
        if (joins(source, pos.west())) {
            x0 = 0;
        }
        if (joins(source, pos.east())) {
            x1 = 1;
        }
        if (joins(source, pos.down())) {
            y0 = 0;
        }
        if (joins(source, pos.up())) {
            y1 = 1;
        }
        if (joins(source, pos.north())) {
            z0 = 0;
        }
        if (joins(source, pos.south())) {
            z1 = 1;
        }
        return new AxisAlignedBB(x0, y0, z0, x1, y1, z1);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> lines, ITooltipFlag flag) {
        lines.add("§7Каналов: " + this.channels);
    }
}
