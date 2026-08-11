package org.polyfrost.oneconfig.internal.compat

/**
 * Dispatches an editor captured without knowing which relocated MoulConfig copy it belongs to
 * to the matching generated `MoulConfigCompat_<target>` class
 *
 * Lives outside [MoulConfigCompat] because the relocator duplicates that class per target and
 * blanket-renames every occurrence of its own name so the `MoulConfigCompat_<target>` literals below
 * would come out mangled in every copy
 *
 * Nothing here touches MoulConfig types so it is never relocated
 */
object MoulConfigDispatch {

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

            configClass.startsWith("net.azureaaron.dandelion_bp.deps.moulconfig.") ->
                listOf("dandelion_bp", "dandelion") to listOf("skyblocker", "dandelion-bp")

            configClass.startsWith("net.azureaaron.dandelion_bp.impl.moulconfig.") ->
                listOf("dandelion_bp", "dandelion") to listOf("skyblocker", "dandelion-bp")

            configClass.startsWith("net.azureaaron.dandelion.deps.moulconfig.") ->
                listOf("dandelion") to listOf("skyblocker", "dandelion-bp")

            configClass.startsWith("at.hannibal2.skyhanni.deps.moulconfig.") ->
                listOf("skyhanni") to listOf("skyhanni")

            else -> emptyList<String>() to emptyList()
        }
        if (candidates.isEmpty()) return
        val forcedModId = forcedModIds.firstOrNull { CompatLoader.hasMod(it) }

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
            }
        }
    }
}
