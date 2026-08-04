package unboundtech.energy;

/**
 * Лимиты Таум-Оверклокера (механика перегрузки, v3 principles §5).
 * Используются с фазы 3; читаются из конфига уже сейчас, чтобы сборки могли
 * преднастроить значения. Теории поднимают лимиты поверх этих базовых.
 */
public final class OverclockRules {

    /** Базовый лимит очков нагрузки на чанк до флюкс-событий. */
    public static int CHUNK_LIMIT = 4;

    /** Базовый лимит оверклокеров на один механизм. */
    public static int PER_MACHINE_LIMIT = 2;

    private OverclockRules() {
    }
}
