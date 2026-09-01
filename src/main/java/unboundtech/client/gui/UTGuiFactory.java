package unboundtech.client.gui;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import unboundtech.common.gui.ContainerMachine;

/**
 * Фабрика клиентских экранов каркаса: по типу тайла выбирает GUI-класс.
 * Очереди #14—#16 добавляют сюда ветки своих экранов; всё остальное
 * получает базовый {@link GuiMachine} (заголовок + EU + статус).
 */
@SideOnly(Side.CLIENT)
public final class UTGuiFactory {

    private UTGuiFactory() {
    }

    public static GuiMachine create(ContainerMachine container) {
        net.minecraft.tileentity.TileEntity tile = container.getTile();
        if (tile instanceof unboundtech.common.tiles.TileInductionCrucible) {
            return new GuiInductionCrucible(container);
        }
        if (tile instanceof unboundtech.common.tiles.TileFluxCondenser) {
            return new GuiFluxCondenser(container);
        }
        if (tile instanceof unboundtech.common.tiles.TileResonantSplitter) {
            return new GuiResonantSplitter(container);
        }
        if (tile instanceof unboundtech.common.tiles.TileBusNode) {
            return new GuiBusNode(container);
        }
        if (tile instanceof unboundtech.common.tiles.TileEssentiaVaultController) {
            return new GuiVaultController(container);
        }
        if (tile instanceof unboundtech.common.tiles.TileSingulator) {
            return new GuiSingulator(container);
        }
        if (tile instanceof unboundtech.common.tiles.TileCartridgeLine) {
            return new GuiCartridgeLine(container);
        }
        return new GuiMachine(container);
    }
}
