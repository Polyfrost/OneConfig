package org.polyfrost.oneconfig.api.ui.v1.internal

import java.util.ServiceLoader
import org.apache.logging.log4j.LogManager

private val LOGGER = LogManager.getLogger("OneConfig/UI")

internal fun <T : Any> loadBridge(type: Class<T>, consequence: String): T? {
    val bridge = try {
        ServiceLoader.load(type, type.classLoader).iterator().let { if (it.hasNext()) it.next() else null }
    } catch (t: Throwable) {
        LOGGER.warn("Failed to load ${type.simpleName}; $consequence", t)
        return null
    }
    if (bridge == null) LOGGER.warn("No ${type.simpleName} is available; $consequence")
    return bridge
}
