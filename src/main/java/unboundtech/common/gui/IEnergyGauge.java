package unboundtech.common.gui;

/**
 * Шкала EU для GUI-каркаса (ХФ-7: индикатор энергии — на каждом экране).
 * На сервере читает буфер IC2-синка, на клиенте — поле, принятое через
 * {@link ISyncedMachine#applySyncField}.
 */
public interface IEnergyGauge {

    double gaugeEnergy();

    double gaugeCapacity();
}
