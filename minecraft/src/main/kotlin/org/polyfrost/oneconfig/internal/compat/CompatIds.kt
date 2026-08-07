package org.polyfrost.oneconfig.internal.compat

/**
 * Create stable ids for a mod
 */
internal object CompatIds {
    private val notAllowedIdRegex = Regex("[^a-z0-9._-]+")

    /**
     * Take a string and turn it into something allowed in a node id
     */
    fun idPart(raw: String?, fallback: String): String {
        val cleaned = raw?.trim()?.lowercase()?.replace(notAllowedIdRegex, "_")?.trim('_')
        return cleaned?.takeIf { it.isNotEmpty() } ?: fallback
    }

    /**
     * Increment the id until it is unique, should be the same every launch because they are added in declaration order
     */
    fun uniqueId(used: MutableSet<String>, base: String): String {
        if (used.add(base)) return base
        var i = 2
        while (!used.add("${base}_$i")) i++
        return "${base}_$i"
    }

    /**
     * Try to get the translation key of a component, preferred over the displayed text so ids do not
     * change with the active language
     */
    fun componentKey(value: Any?): String? {
        if (value == null || value is String) return null
        val contents = runCatching { value.javaClass.getMethod("getContents").invoke(value) }.getOrNull()
        if (contents != null) {
            runCatching { contents.javaClass.getMethod("getKey").invoke(contents) as? String }
                .getOrNull()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return runCatching { value.javaClass.getMethod("getKey").invoke(value) as? String }
            .getOrNull()?.takeIf { it.isNotBlank() }
    }
}