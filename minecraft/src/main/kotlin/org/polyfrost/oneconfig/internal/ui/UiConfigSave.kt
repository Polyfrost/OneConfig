package org.polyfrost.oneconfig.internal.ui

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.ConfigManager

object UiConfigSave {
    private val LOGGER = LogManager.getLogger("OneConfig/UiConfigSave")

    private val SAVE_EXECUTOR = Executors.newSingleThreadExecutor { r ->
        Thread(r, "OneConfig-ConfigSave").apply { isDaemon = true }
    }

    private val savePending = AtomicBoolean(false)

    @JvmStatic
    fun schedule() {
        if (!savePending.compareAndSet(false, true)) return
        SAVE_EXECUTOR.execute {
            savePending.set(false)
            try {
                ConfigManager.active().saveAll()
            } catch (t: Throwable) {
                LOGGER.error("Failed to save configs on OneConfig UI close", t)
            }
        }
    }
}
