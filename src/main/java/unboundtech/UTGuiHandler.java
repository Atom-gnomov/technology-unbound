package unboundtech;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import unboundtech.common.inventory.ContainerEssentiaBurner;
import unboundtech.common.inventory.ContainerPhialStation;
import unboundtech.common.tiles.TileEssentiaBurner;
import unboundtech.common.tiles.TilePhialStation;

/**
 * Экраны машин. Один обработчик на мод, id окна выбирается по тайлу — так
 * блоку не нужно знать номер своего GUI.
 */
public class UTGuiHandler implements IGuiHandler {

    /** Единственный id: что именно открывать, решает тайл в этой позиции. */
    public static final int GUI_MACHINE = 0;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world,
                                      int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (tile instanceof TilePhialStation) {
            return new ContainerPhialStation(player.inventory, (TilePhialStation) tile);
        }
        if (tile instanceof TileEssentiaBurner) {
            return new ContainerEssentiaBurner(player.inventory, (TileEssentiaBurner) tile);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world,
                                      int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (tile instanceof TilePhialStation) {
            return new unboundtech.client.gui.GuiPhialStation(
                    player.inventory, (TilePhialStation) tile);
        }
        if (tile instanceof TileEssentiaBurner) {
            return new unboundtech.client.gui.GuiEssentiaBurner(
                    player.inventory, (TileEssentiaBurner) tile);
        }
        return null;
    }
}
