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

    // Курсы эссенции (Эссент. Горелка) и цена фиала удалены вместе с самими
    // объектами: решением владельца Фиал-станция и Эссент. Горелка вычеркнуты
    // из канона и из мода как бесполезные. Строка «Permutatio -> усиление UU»
    // была отменена раньше и по той же причине здесь отсутствует.

    /**
     * Флюкс-Конденсатор (T3): EU за 1 ед. `Praecantatio` с родного
     * Флюкс-Скруббера ТК (канон §3.1). ⚠️ Предварительно до замера темпа
     * скруббера. После вычеркивания горелки это ЕДИНСТВЕННЫЙ сток эссенции
     * в моде — и он принимает только Praecantatio, это принципиально.
     */
    public static int EU_PER_FLUX_ESSENTIA = 2_000;

    /**
     * Резонансный Расщепитель (T3): EU за операцию «1 составной → оба
     * компонента» (канон §3.4). Правило §5.1 карточки: цена обязана быть
     * больше 2 × лучшего курса любого будущего стока эссенции.
     */
    public static int EU_PER_SPLIT = 6_000;

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
