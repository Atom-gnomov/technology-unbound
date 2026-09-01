package unboundtech.common.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/**
 * GUI-каркас (ХФ-7): один ID на все машины — экран определяется тайлом
 * на позиции. Тайл без {@link ISyncedMachine} экрана не получает (блок
 * тогда показывает статус-строку, как раньше).
 */
public class UTGuiHandler implements IGuiHandler {

    public static final int MACHINE = 0;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world,
                                      int x, int y, int z) {
        if (id != MACHINE) {
            return null;
        }
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (tile instanceof unboundtech.common.tiles.TileCartridgeLine) {
            return new ContainerCartridgeLine(
                    (unboundtech.common.tiles.TileCartridgeLine) tile, player);
        }
        return tile instanceof ISyncedMachine ? new ContainerMachine(tile) : null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world,
                                      int x, int y, int z) {
        if (id != MACHINE) {
            return null;
        }
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (!(tile instanceof ISyncedMachine)) {
            return null;
        }
        if (tile instanceof unboundtech.common.tiles.TileCartridgeLine) {
            return unboundtech.client.gui.UTGuiFactory.create(
                    new ContainerCartridgeLine(
                            (unboundtech.common.tiles.TileCartridgeLine) tile,
                            player));
        }
        return unboundtech.client.gui.UTGuiFactory.create(new ContainerMachine(tile));
    }
}
