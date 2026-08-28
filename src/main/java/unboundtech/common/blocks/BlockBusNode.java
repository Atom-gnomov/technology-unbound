package unboundtech.common.blocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import unboundtech.common.tiles.TileBusNode;

/**
 * Шинный Узел (`05_objects/bus_node.md`): вход и выход многоканальной
 * шины — перекрёсток, которому объяснили расписание. Прототипная модель —
 * компактная коробка (полноценные патрубки по соединениям — вместе с
 * кастомной моделью, §8).
 */
public class BlockBusNode extends BlockMachineBase {

    private static final AxisAlignedBB NODE_AABB =
            new AxisAlignedBB(2 / 16.0, 2 / 16.0, 2 / 16.0,
                    14 / 16.0, 14 / 16.0, 14 / 16.0);

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileBusNode();
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return NODE_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }
}
