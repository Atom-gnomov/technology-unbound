package unboundtech.common.blocks;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import unboundtech.common.tiles.TileThaumGenerator;

/** Таум-Генератор: вис узла → EU (спека фазы 3а §1). */
public class BlockThaumGenerator extends BlockMachineBase {

    @Override
    protected int activeLightLevel() {
        // Работающий генератор тлеет висом — слабое свечение по канону спеки.
        return 7;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileThaumGenerator();
    }
}
