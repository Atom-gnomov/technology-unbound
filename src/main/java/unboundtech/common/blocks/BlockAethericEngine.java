package unboundtech.common.blocks;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import unboundtech.common.tiles.TileAethericEngine;

/** Эфирный Двигатель: EU → вис узла (спека фазы 3а §2). */
public class BlockAethericEngine extends BlockMachineBase {

    @Override
    protected int activeLightLevel() {
        return 5;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileAethericEngine();
    }
}
