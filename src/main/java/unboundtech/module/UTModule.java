package unboundtech.module;

import java.util.Locale;
import net.minecraftforge.fml.common.Loader;
import unboundtech.CompatIds;
import unboundtech.config.UTConfig;

/**
 * Контент-модули мода. Модуль активен, если: включён в конфиге И его
 * мод-компаньон загружен (для core-модулей компаньоны — обязательные
 * зависимости, так что решает только конфиг).
 *
 * SRP заморожен решением v5 §7 — модуля для него НЕТ намеренно.
 */
public enum UTModule {

    CORE(null),
    PRODCHAINS(null),
    ENTITIES(null),
    WEAPONS(null),
    ARMOR(null),
    DUNGEON(null),
    RADIATION(null),
    METS(CompatIds.METS),
    ASP(CompatIds.ASP);

    private final String requiredMod;

    UTModule(String requiredMod) {
        this.requiredMod = requiredMod;
    }

    public String configKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean isEnabled() {
        if (!UTConfig.moduleEnabled(this)) {
            return false;
        }
        return requiredMod == null || Loader.isModLoaded(requiredMod);
    }
}
