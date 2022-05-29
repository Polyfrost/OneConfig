package cc.polyfrost.oneconfig.utils.dsl

import cc.polyfrost.oneconfig.utils.JsonUtils
import com.google.gson.JsonElement

/**
 * Returns the [JsonElement] of the given [String].
 *
 * @see JsonUtils.parseString
 */
fun String.asJsonElement(catchExceptions: Boolean = true): JsonElement? = JsonUtils.parseString(this, catchExceptions)