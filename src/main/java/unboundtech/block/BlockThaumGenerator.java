package unboundtech.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import unboundtech.tile.TileThaumGenerator;

/** Таум-Генератор: узел → EU (docs phase3_converters_spec §1). */
public class BlockThaumGenerator extends BlockUTMachine {

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileThaumGenerator();
    }
}
