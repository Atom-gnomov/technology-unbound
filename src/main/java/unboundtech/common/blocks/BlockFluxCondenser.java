package unboundtech.common.blocks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import unboundtech.common.tiles.TileFluxCondenser;

/** Флюкс-Конденсатор: Praecantatio со скруббера → EU (`flux_condenser.md`). */
public class BlockFluxCondenser extends BlockMachineBase {

    @Override
    protected int activeLightLevel() {
        return 7;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileFluxCondenser();
    }

    /**
     * ПКМ ключом переключает РЕЖИМ (§9), а не поворачивает блок: и бронзовый
     * ключ IC2, и наш зовут {@code setFacing}, поэтому оба работают без
     * своего интерфейса. Цена решения та же, что была у Фиал-станции:
     * повернуть машину после установки нельзя — направление задаётся при
     * постановке.
     */
    @Override
    public boolean setFacing(World world, BlockPos pos, EnumFacing newDirection,
                             EntityPlayer player) {
        if (world.isRemote) {
            return true;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileFluxCondenser)) {
            return false;
        }
        TileFluxCondenser.Mode mode = ((TileFluxCondenser) tile).toggleMode();
        if (player != null) {
            player.sendStatusMessage(new TextComponentString("§5Режим: "
                    + (mode == TileFluxCondenser.Mode.CONDENSE
                            ? "конденсация (муть → ток)"
                            : "сгущение (муть + ток → Флюкс-Заряд)")), true);
        }
        return true;
    }
}
