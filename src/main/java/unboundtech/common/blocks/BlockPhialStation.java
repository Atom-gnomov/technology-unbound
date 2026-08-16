package unboundtech.common.blocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ConfigItems;
import unboundtech.common.tiles.TilePhialStation;

/** Фиал-станция: труба ↔ фиалы (`05_objects/phial_station.md`). */
public class BlockPhialStation extends BlockMachineBase {

    @Override
    protected int activeLightLevel() {
        return 7;
    }

    @Override
    protected boolean hasGui() {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TilePhialStation();
    }

    /**
     * ПКМ фиалом задаёт фильтр аспекта (§9); всё остальное — базовому классу
     * (пустая рука показывает строку статуса).
     */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty() || held.getItem() != ConfigItems.itemEssence) {
            return super.onBlockActivated(world, pos, state, player, hand,
                    facing, hitX, hitY, hitZ);
        }
        if (world.isRemote) {
            return true;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TilePhialStation)) {
            return false;
        }
        Aspect filter = ((TilePhialStation) tile).applyFilter(held);
        player.sendStatusMessage(new TextComponentString(
                "§bФильтр аспекта: " + (filter == null ? "любой" : filter.getName())), true);
        return true;
    }

    /**
     * ПКМ ключом переключает РЕЖИМ, а не поворачивает блок (§9 карточки и
     * `machine_feedback.md` §5).
     *
     * Сделано через {@code setFacing} намеренно: и бронзовый ключ IC2, и наш
     * таумиевый дёргают именно его, поэтому оба работают одинаково и без
     * своего интерфейса. Цена решения — станцию нельзя повернуть после
     * установки; направление задаётся при постановке блока.
     */
    @Override
    public boolean setFacing(World world, BlockPos pos, EnumFacing newDirection,
                             EntityPlayer player) {
        if (world.isRemote) {
            return true;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TilePhialStation)) {
            return false;
        }
        TilePhialStation.Mode mode = ((TilePhialStation) tile).toggleMode();
        if (player != null) {
            player.sendStatusMessage(new TextComponentString("§bРежим: "
                    + (mode == TilePhialStation.Mode.FILL ? "розлив" : "слив")), true);
        }
        return true;
    }

    /**
     * Фиалы из слотов роняем всегда — и от кирки, и от ключа. «Пусто» в §10
     * относится к буферу машины, а не к чужим предметам: терять их игроку
     * нельзя ни при каком способе разборки.
     */
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TilePhialStation) {
            net.minecraftforge.items.ItemStackHandler slots =
                    ((TilePhialStation) tile).getSlots();
            for (int slot = 0; slot < slots.getSlots(); slot++) {
                ItemStack stack = slots.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    spawnAsEntity(world, pos, stack);
                }
            }
        }
        super.breakBlock(world, pos, state);
    }
}
