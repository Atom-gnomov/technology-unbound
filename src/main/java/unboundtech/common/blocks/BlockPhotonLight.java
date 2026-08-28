package unboundtech.common.blocks;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Свет осветительного патрона (`cartridges.md` §4.2): невидимый блок
 * света на 60 секунд в точке попадания. Ставится только снарядом,
 * предмета не имеет, гаснет по расписанию.
 */
public class BlockPhotonLight extends Block {

    public BlockPhotonLight() {
        super(Material.GLASS);
        this.setLightLevel(0.93f);   // 14 — как факел с запасом
        this.setHardness(0.0f);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        world.setBlockToAir(pos);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
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
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state,
            IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return null;
    }
}
