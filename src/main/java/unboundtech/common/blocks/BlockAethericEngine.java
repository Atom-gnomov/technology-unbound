package unboundtech.common.blocks;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import unboundtech.common.tiles.TileAethericEngine;

/** Эфирный Двигатель: EU → вис узла (спека фазы 3а §2). */
public class BlockAethericEngine extends BlockMachineBase {

    @Override
    protected int activeLightLevel() {
        // 7 — как у работающего генератора IC2 и как у Таум-Генератора:
        // канон machine_feedback.md §4 задаёт одну светимость всем машинам.
        return 7;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileAethericEngine();
    }
}
