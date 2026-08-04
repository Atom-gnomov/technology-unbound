package unboundtech.module;

import java.util.StringJoiner;
import unboundtech.UTLog;
import unboundtech.compat.ic2.IC2Aspects;
import unboundtech.compat.ic2.IC2Recipes;
import unboundtech.research.UTResearch;

/**
 * Диспетчер модулей. Фаза 1: реализован только CORE
 * (аспекты IC2, вкладка исследований, рецепты машин IC2).
 * Каждая следующая фаза добавляет свою ветку в соответствующий метод.
 */
public final class ModuleManager {

    private ModuleManager() {
    }

    public static void preInit() {
        for (UTModule module : UTModule.values()) {
            if (!module.isEnabled()) {
                UTLog.info("Module {} disabled (config or missing companion mod)", module);
            }
        }
    }

    public static void init() {
        // Фаза 1: нечего регистрировать на init.
    }

    /**
     * Вызывается из postInit нашего мода. Благодаря after:thaumcraft и
     * after:ic2 к этому моменту Thaumcraft уже зарегистрировал свои аспекты
     * и исследования, а IC2 — свои предметы и менеджеры рецептов.
     */
    public static void postInit() {
        if (UTModule.CORE.isEnabled()) {
            IC2Aspects.register();
            UTResearch.register();
            IC2Recipes.register();
        }
    }

    public static String enabledSummary() {
        StringJoiner joiner = new StringJoiner(", ");
        for (UTModule module : UTModule.values()) {
            if (module.isEnabled()) {
                joiner.add(module.name());
            }
        }
        return joiner.toString();
    }
}
