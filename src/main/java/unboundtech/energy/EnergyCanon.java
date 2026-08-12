package unboundtech.energy;

import unboundtech.UTLog;

/**
 * Канонические курсы конвертации энергии.
 *
 * Источник истины — канон дизайна в репо порта:
 * {@code docs/design/02_world_model.md} §3 (единая таблица курсов).
 *
 * ВСЕ конвертеры обязаны брать курсы отсюда — никакой предмет не хардкодит
 * собственный обменник. Значения переопределяются конфигом.
 *
 * <p><b>Терминология.</b> В моде НЕТ «ауры чанка»: классы
 * {@code AuraHelper}/{@code AuraChunk}/{@code AuraHandler} в порте — мёртвый
 * TC6-совместимый фасад (память, ничего не сохраняется и не читается).
 * Единственный источник вис в мире — <b>узлы</b> ({@code INode}) с
 * по-аспектной ёмкостью, поэтому курсы измеряются в «единицах аспекта узла».
 * См. канон §1.1.
 */
public final class EnergyCanon {

    /** Узел → EU: Таум-Генератор, EU за 1 ед. аспекта узла (Ignis/Potentia). */
    public static int EU_PER_NODE_ASPECT_SELL = 2_000;

    /** EU → узел: Эфирный Двигатель, EU за 1 ед. аспекта узла. */
    public static int EU_PER_NODE_ASPECT_BUY = 8_000;

    /**
     * EU → вис в жезле: Сингулятор / иридиевый стержень (канон §3.1, строка 3).
     * Было 20 000; решением владельца курс снижен вдвое — обходной маршрут
     * (Двигатель за 8 000 плюс бесплатный набор жезлом) всё равно остаётся
     * дешевле, разрыв 25 %.
     */
    public static int EU_PER_VIS = 10_000;

    /**
     * Вис в жезле → EU: Фокус Заряда (канон §3.1, строка 4). Вдвое хуже
     * Таум-Генератора намеренно. Цикл EU → жезл → EU возвращает 10 %.
     */
    public static int EU_PER_WAND_VIS_BACK = 1_000;

    /**
     * Справочно (кода не требует): алюментум горит 6400 тиков (парити TC4,
     * восстановлено в порте 1.2.8.0) — генератор IC2 сам даёт с него ~16,000 EU.
     */
    public static final int EU_ALUMENTUM_REFERENCE = 16_000;

    // Курсы будущих фаз — здесь же, чтобы канон жил в одном классе.
    /** Эссент. Горелка (T2): EU за 1 ед. Ignis/Potentia. ⚠️ Курс предварительный. */
    public static int EU_ESSENTIA_HOT = 2_000;
    /** Эссент. Горелка: EU за 1 ед. Perditio. ⚠️ Курс предварительный. */
    public static int EU_ESSENTIA_PERDITIO = 1_250;
    /** Эссент. Горелка: EU за 1 ед. Arbor/Herba. ⚠️ Курс предварительный. */
    public static int EU_ESSENTIA_PLANT = 500;
    // Строка «Permutatio → усиление UU в массфабрикаторе» УДАЛЕНА из канона
    // (§3.1, решение владельца): она была единственной без системы и карточки.
    // Нишу «электричество делает материю» закрыл Молекулярный Преобразователь
    // ASP с листами преобразования, у каждого своя строка курса.

    /** Фиал-станция (T2): EU за один фиал, розлив или слив (канон §3.4). */
    public static int EU_PER_PHIAL = 200;

    /**
     * Сколько EU даёт единица эссенции в горелке.
     * Аспекты вне таблицы горелка не принимает вовсе — здесь это ноль
     * (`essentia_burner.md` §4).
     */
    public static int essentiaValue(thaumcraft.api.aspects.Aspect aspect) {
        if (aspect == null) {
            return 0;
        }
        if (aspect == thaumcraft.api.aspects.Aspect.FIRE
                || aspect == thaumcraft.api.aspects.Aspect.ENERGY) {
            return EU_ESSENTIA_HOT;          // Ignis / Potentia
        }
        if (aspect == thaumcraft.api.aspects.Aspect.ENTROPY) {
            return EU_ESSENTIA_PERDITIO;     // Perditio
        }
        if (aspect == thaumcraft.api.aspects.Aspect.TREE
                || aspect == thaumcraft.api.aspects.Aspect.PLANT) {
            return EU_ESSENTIA_PLANT;        // Arbor / Herba
        }
        return 0;
    }

    /**
     * Ёмкость буфера Таум-Генератора. Дублируется здесь намеренно: тайл нельзя
     * трогать из проверки конфига (она бежит в preInit, до реестра блоков), а
     * курс больше буфера делает машину мёртвой — условие «есть место под целую
     * порцию» не выполнится никогда.
     */
    public static final int THAUM_GENERATOR_CAPACITY = 20_000;

    private EnergyCanon() {
    }

    /**
     * «Второй закон таумодинамики»: цикл узел → EU → узел обязан терять >= 75%.
     * Кривой конфиг не роняет игру, но громко ругается в лог.
     */
    public static void validateSecondLaw() {
        if (EU_PER_NODE_ASPECT_BUY < 4 * EU_PER_NODE_ASPECT_SELL) {
            UTLog.warn("Energy config breaks the second law of thaumodynamics: "
                    + "recharging a node costs {} EU while draining it yields {} EU/unit "
                    + "(round-trip > 25%). Perpetuum mobile possible — fix unboundtech.cfg.",
                    EU_PER_NODE_ASPECT_BUY, EU_PER_NODE_ASPECT_SELL);
        }
        if (EU_PER_NODE_ASPECT_SELL > THAUM_GENERATOR_CAPACITY) {
            UTLog.warn("Energy config: one node aspect is worth {} EU, which does not fit the "
                    + "Thaumic Alternator buffer ({} EU) — the machine would never start. "
                    + "Lower eu_per_node_aspect_generated or the machine stays dead.",
                    EU_PER_NODE_ASPECT_SELL, THAUM_GENERATOR_CAPACITY);
        }
        if (EU_PER_VIS < EU_PER_NODE_ASPECT_SELL) {
            UTLog.warn("Energy config: wand vis ({} EU) is cheaper than node vis ({} EU) — "
                    + "direct wand charging would obsolete nodes. Fix unboundtech.cfg.",
                    EU_PER_VIS, EU_PER_NODE_ASPECT_SELL);
        }
    }
}
