package org.polyfrost.oneconfig.internal.compat

/**
 * Dispatches an editor that was captured without knowing which relocated MoulConfig copy it belongs to,
 * to the matching generated `MoulConfigCompat_<target>` class.
 *
 * This deliberately lives outside [MoulConfigCompat]: that class is duplicated per relocation target by
 * the relocator, which blanket-renames every occurrence of its own name in the copied source, so the
 * `MoulConfigCompat_<target>` literals below would come out mangled (`MoulConfigCompat_skyhanni_firmament`)
 * in every copy. Nothing here touches MoulConfig types, so it is never relocated.
 */
object MoulConfigDispatch {

    private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/MoulConfigDispatch")

    private const val COMPAT_PACKAGE = "org.polyfrost.oneconfig.internal.compat"
    private const val COMPAT_PREFIX = "MoulConfigCompat_"

    @JvmStatic
    fun parseMoulconfigFromUnknownEditor(categories: Collection<*>, config: Any?) {
        if (config == null) return
        val configClass = config::class.java.name
        val (candidates, forcedModIds) = when {
            configClass.startsWith("moe.nea.firmament.deps.moulconfig.") ->
                listOf("firmament") to listOf("firmament")

            configClass.startsWith("moe.nea.firmament.compat.moulconfig.") ->
                listOf("firmament") to listOf("firmament")

            configClass.startsWith("at.hannibal2.skyhanni.deps.moulconfig.") ->
                listOf("skyhanni") to listOf("skyhanni")

            else -> emptyList<String>() to emptyList()
        }
        if (candidates.isEmpty()) {
            LOGGER.debug("No relocation target known for MoulConfig editor of {}", configClass)
            return
        }
        val forcedModId = forcedModIds.firstOrNull { CompatLoader.hasMod(it) }

        var failure: Throwable? = null
        for (target in candidates) {
            val fqcn = "$COMPAT_PACKAGE.$COMPAT_PREFIX$target"
            runCatching {
                val compatClass = Class.forName(fqcn)
                val method = compatClass.methods.firstOrNull {
                    it.name == "parseMoulconfigFromEditor" && it.parameterCount == 2
                } ?: error("parseMoulconfigFromEditor not found on $fqcn")
                CompatLoader.withForcedModId(forcedModId) {
                    method.invoke(null, categories, config)
                }
                return
            }.onFailure { failure = it }
        }
        LOGGER.warn("No usable compat class for MoulConfig editor of {} (tried {})", configClass, candidates, failure)
    }
}
