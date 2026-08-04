package unboundtech;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Единый логгер мода. */
public final class UTLog {

    public static final Logger LOG = LogManager.getLogger("UNBOUNDTECH");

    private UTLog() {
    }

    public static void info(String message, Object... args) {
        LOG.info(message, args);
    }

    public static void warn(String message, Object... args) {
        LOG.warn(message, args);
    }

    public static void error(String message, Object... args) {
        LOG.error(message, args);
    }
}
