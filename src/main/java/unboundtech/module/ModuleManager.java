package unboundtech.module;

import java.util.StringJoiner;
import unboundtech.UTLog;
import unboundtech.aspects.UTAspects;
import unboundtech.compat.asp.ASPAspects;
import unboundtech.compat.ic2.IC2Aspects;
import unboundtech.compat.ic2.IC2Recipes;
import unboundtech.common.UTItems;
import unboundtech.common.UTRecipes;
import unboundtech.common.UTRecipesT2;
import unboundtech.common.UTRecipesT3;
import unboundtech.compat.mets.METSAspects;
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
        // Оредикт и материалы починки: предметы уже созданы (регистрация идёт
        // по событию реестра, до init), а Thaumcraft свой ingotThaumium к
        // этому моменту тоже объявил.
        UTItems.init();
    }

    /**
     * Вызывается из postInit нашего мода. Благодаря after:thaumcraft и
     * after:ic2 к этому моменту Thaumcraft уже зарегистрировал свои аспекты
     * и исследования, а IC2 — свои предметы и менеджеры рецептов.
     */
    public static void postInit() {
        if (!UTModule.CORE.isEnabled()) {
            return; // core=false отключает всё содержимое (приёмка фазы 1, п.6)
        }
        IC2Aspects.register();
        UTResearch.register();
        IC2Recipes.register();

        // Тир T2: сперва два пути материала (домна + тигель), затем записи —
        // страницы держат сам объект рецепта тигля. Кузнечные рецепты T2 уже
        // зарегистрированы событием реестра, до postInit.
        UTRecipesT2.register();
        UTResearch.registerTemperedTier();

        // Фаза 3а: сначала рецепты, потом исследования — страницы записей
        // держат сами объекты рецептов. Корпус конвертеров теперь из
        // закалённого таумия, поэтому строго после T2.
        UTRecipes.register();
        UTResearch.registerConverters();

        // Тир T3: рецепты, затем запись — страница держит объект рецепта.
        UTRecipesT3.register();
        UTResearch.registerT3();

        // Аспекты наших объектов считаются по формуле из состава рецепта,
        // поэтому строго после UTRecipes и после аспектов IC2.
        UTAspects.register();

        // Аддон-модули: двойной гейт конфиг × наличие мода уже внутри
        // isEnabled(); вкладка исследований принадлежит CORE, поэтому
        // аддон-контент живёт только под ним.
        if (UTModule.ASP.isEnabled()) {
            ASPAspects.register();
            UTResearch.registerAspLore();
        }
        if (UTModule.METS.isEnabled()) {
            METSAspects.register();
            UTResearch.registerMetsLore();
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
