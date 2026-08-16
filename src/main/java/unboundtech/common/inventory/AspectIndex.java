package unboundtech.common.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import thaumcraft.api.aspects.Aspect;

/**
 * Аспект по номеру и обратно — чтобы отдавать его в GUI обычным числом.
 *
 * Свойства окна ({@code IContainerListener.sendWindowProperty}) умеют
 * передавать только {@code short}, а строку аспекта передать нечем. Список
 * строится сортировкой ключей {@link Aspect#aspects}: на клиенте и сервере
 * одной игры набор аспектов одинаков, значит одинаков и порядок.
 *
 * Ноль зарезервирован под «аспекта нет», поэтому индексы сдвинуты на единицу.
 */
public final class AspectIndex {

    private static List<String> tags;

    private AspectIndex() {
    }

    private static synchronized List<String> tags() {
        if (tags == null) {
            List<String> keys = new ArrayList<>(Aspect.aspects.keySet());
            Collections.sort(keys);
            tags = keys;
        }
        return tags;
    }

    /** @return номер аспекта для передачи в GUI; 0 — «аспекта нет» */
    public static int idOf(Aspect aspect) {
        if (aspect == null) {
            return 0;
        }
        int at = tags().indexOf(aspect.getTag());
        return at < 0 ? 0 : at + 1;
    }

    /** @return аспект по номеру или {@code null} */
    public static Aspect byId(int id) {
        List<String> all = tags();
        if (id <= 0 || id > all.size()) {
            return null;
        }
        return Aspect.getAspect(all.get(id - 1));
    }
}
