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
        return new GuiMachine(container);
    }
}
