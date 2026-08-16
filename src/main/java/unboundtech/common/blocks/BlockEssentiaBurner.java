package unboundtech.common.blocks;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import unboundtech.common.tiles.TileEssentiaBurner;

/** Эссентиальная горелка: эссенция → EU (`05_objects/essentia_burner.md`). */
public class BlockEssentiaBurner extends BlockMachineBase {

    @Override
    protected int activeLightLevel() {
        // §8 карточки: свет при работе 7 — «видно, что горит», но помещение
        // этим не осветить (`machine_feedback.md` §4).
        return 7;
    }

    @Override
    protected boolean hasGui() {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEssentiaBurner();
    }
}
