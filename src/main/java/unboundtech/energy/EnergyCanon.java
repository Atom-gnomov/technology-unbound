package unboundtech.energy;

import unboundtech.UTLog;

/**
 * Канонические курсы конвертации энергии (ревизия v5,
 * docs/integration/ic2_v5_decisions.md §2 в репо порта).
 *
 * ВСЕ будущие конвертеры обязаны брать курсы отсюда — никакой предмет
 * не хардкодит собственный обменник. Значения переопределяются конфигом.
 */
public final class EnergyCanon {

    /** Узел → EU: Таум-Генератор, EU за 1 ед. аспекта узла (Ignis/Potentia). */
    public static int EU_PER_AURA_SELL = 2_000;

    /**
     * EU → узел: Эфирный Двигатель, EU за 1 ед. аспекта узла.
     * (Ауры чанков в порте не существует — вся «аура» это узлы.)
     */
    public static int EU_PER_AURA_BUY = 8_000;

    /** EU → вис в жезле: Сингулятор / иридиевый стержень. */
    public static int EU_PER_VIS = 20_000;

    /**
     * Справочно (кода не требует): алюментум горит 6400 тиков (парити TC4,
     * восстановлено в порте 1.2.8.0) — генератор IC2 сам даёт с него ~16,000 EU.
     */
    public static final int EU_ALUMENTUM_REFERENCE = 16_000;

    // Курсы будущих фаз — здесь же, чтобы канон жил в одном классе.
    /** Эссент. Горелка (фаза 4): EU за 1 ед. Ignis/Potentia. */
    public static int EU_ESSENTIA_HOT = 2_000;
    /** Эссент. Горелка: EU за 1 ед. Perditio. */
    public static int EU_ESSENTIA_PERDITIO = 1_250;
    /** Эссент. Горелка: EU за 1 ед. Arbor/Herba. */
    public static int EU_ESSENTIA_PLANT = 500;
    /** Массфабрикатор (фаза 10): EU-эквивалент усиления за 1 ед. Permutatio. */
    public static int EU_PERMUTATIO_AMPLIFIER = 5_000;

    private EnergyCanon() {
    }

    /**
     * «Второй закон таумодинамики»: цикл аура → EU → аура обязан терять >= 75%.
     * Кривой конфиг не роняет игру, но громко ругается в лог.
     */
    public static void validateSecondLaw() {
        if (EU_PER_AURA_BUY < 4 * EU_PER_AURA_SELL) {
            UTLog.warn("Energy config breaks the second law of thaumodynamics: "
                    + "restoring aura costs {} EU while generating yields {} EU/unit "
                    + "(round-trip > 25%). Perpetuum mobile possible — fix unboundtech.cfg.",
                    EU_PER_AURA_BUY, EU_PER_AURA_SELL);
        }
        if (EU_PER_VIS < EU_PER_AURA_SELL) {
            UTLog.warn("Energy config: wand vis ({} EU) is cheaper than raw aura ({} EU) — "
                    + "direct wand charging would obsolete nodes. Fix unboundtech.cfg.",
                    EU_PER_VIS, EU_PER_AURA_SELL);
        }
    }
}
