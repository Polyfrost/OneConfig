package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.utils.v1.JsonUtils

object ThirdPartyModCategories {
    private const val URL = "https://data.polyfrost.org/oneconfig/third-party-mod-categories.json"
    private val LOGGER = LogManager.getLogger("OneConfig/ThirdPartyModCategories")

    @Volatile
    private var byId: Map<String, Config.Category> = emptyMap()

    @Volatile
    private var initialized = false

    var revision by mutableIntStateOf(0)
        private set

    private val categories = mapOf(
        "hypixel" to Config.Category.HYPIXEL,
        "performance" to Config.Category.PERFORMANCE,
        "visuals" to Config.Category.VISUALS,
        "hud" to Config.Category.HUD,
        "utility" to Config.Category.UTILITY,
        "qol" to Config.Category.QOL,
    )

    fun init() {
        if (initialized) return
        initialized = true
        JsonUtils.parseFromUrlAsync(URL) { element ->
            val loaded = parse(element.asJsonObject)
            if (loaded.isNotEmpty()) {
                byId = loaded
                revision++
            }
        }
    }

    fun categoryFor(id: String?, modInfo: ModInfo?): Config.Category? =
        sequenceOf(id, id?.removeSuffix(".json"), modInfo?.id, modInfo?.id?.removeSuffix(".json"))
            .filterNotNull()
            .map { it.lowercase() }
            .firstNotNullOfOrNull(byId::get)

    private fun parse(root: JsonObject): Map<String, Config.Category> {
        val result = LinkedHashMap<String, Config.Category>()
        categories.forEach { (name, category) ->
            val ids = root.get(name)
            if (ids !is JsonArray) {
                LOGGER.warn("Missing third-party mod category '{}'", name)
                return@forEach
            }
            ids.mapNotNull { it.asString?.lowercase() }
                .forEach { result[it] = category }
        }
        return result
    }
}
