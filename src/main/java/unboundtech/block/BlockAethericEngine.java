package unboundtech.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import unboundtech.tile.TileAethericEngine;

/** Эфирный Двигатель: EU → узел (docs phase3_converters_spec §2). */
public class BlockAethericEngine extends BlockUTMachine {

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileAethericEngine();
    }
}
