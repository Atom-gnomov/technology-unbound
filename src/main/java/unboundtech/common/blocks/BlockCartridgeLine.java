package unboundtech.common.blocks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import unboundtech.common.tiles.TileCartridgeLine;

/**
 * Патронная Линия (`cartridge_line.md`): ПКМ ключом — режим
 * «патроны ↔ лента» (приём Конденсатора: оба ключа зовут setFacing,
 * поворот блока после установки этим потерян — цена решения та же).
 */
public class BlockCartridgeLine extends BlockMachineBase {

    @Override
    protected int activeLightLevel() {
        return 7;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileCartridgeLine();
    }

    @Override
    public boolean setFacing(World world, BlockPos pos, EnumFacing newDirection,
                             EntityPlayer player) {
        if (world.isRemote) {
            return true;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileCartridgeLine)) {
            return false;
        }
        TileCartridgeLine line = (TileCartridgeLine) tile;
        line.toggleMode();
        player.sendStatusMessage(new TextComponentString(line.isBeltMode()
                ? "§eЛиния: режим ЛЕНТЫ — 60 патронов за 750 тиков,"
                        + " прерывание сбрасывает прогресс"
                : "§eЛиния: режим ПАТРОНОВ — 8 за цикл"), false);
        return true;
    }
}
