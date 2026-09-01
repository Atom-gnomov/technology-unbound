package unboundtech.common.gui;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Кнопки экрана машины без своего сетевого канала: клиент шлёт ванильный
 * {@code CPacketEnchantItem} (sendEnchantPacket), сервер получает его в
 * {@link ContainerMachine#enchantItem} и передаёт тайлу. Id — смысл
 * кнопки, валидацию делает тайл.
 */
public interface IMachineButtons {

    /** @return true, если id принят (для звука подтверждения ванили). */
    boolean onButton(EntityPlayer player, int id);
}
