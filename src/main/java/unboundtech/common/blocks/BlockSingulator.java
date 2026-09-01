package unboundtech.common.blocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import thaumcraft.common.items.wands.ItemWandCasting;
import unboundtech.common.tiles.TileSingulator;

/**
 * Сингулятор (`singulator.md` §9): ПКМ жезлом — вставить в вилку;
 * Shift-ПКМ пустой рукой — забрать; ПКМ пустой рукой — GUI (ХФ-7).
 * Слом блока с жезлом внутри роняет жезл отдельным предметом (§10).
 */
public class BlockSingulator extends BlockMachineBase {

    /** §5: свет 10 при работе. */
    @Override
    protected int activeLightLevel() {
        return 10;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileSingulator();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileSingulator)) {
            return false;
        }
        TileSingulator singulator = (TileSingulator) tile;
        ItemStack held = player.getHeldItem(hand);
        // жезлом — в вилку (быстрый путь §9)
        if (held.getItem() instanceof ItemWandCasting) {
            if (!world.isRemote) {
                singulator.insertWand(player, held);
            }
            return true;
        }
        // Shift-ПКМ пустой рукой — забрать жезл
        if (held.isEmpty() && player.isSneaking() && singulator.hasWand()) {
            if (!world.isRemote) {
                singulator.extractWand(player);
            }
            return true;
        }
        // пустая рука без шифта — GUI каркаса
        return super.onBlockActivated(world, pos, state, player, hand,
                facing, hitX, hitY, hitZ);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileSingulator) {
            ItemStack wand = ((TileSingulator) tile).takeWandForDrop();
            if (!wand.isEmpty()) {
                net.minecraft.inventory.InventoryHelper.spawnItemStack(world,
                        pos.getX(), pos.getY(), pos.getZ(), wand);
            }
        }
        super.breakBlock(world, pos, state);
    }
}
