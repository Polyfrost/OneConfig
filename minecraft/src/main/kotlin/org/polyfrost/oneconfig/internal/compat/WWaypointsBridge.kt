//? wwaypoints_compat {
package org.polyfrost.oneconfig.internal.compat

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.Screen
import org.polyfrost.oneconfig.utils.v1.WrappingUtils
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * reflective access to wWaypoints which is closed-source and not a compile-time dependency so every class
 * field and method is looked up by name and cached
 *
 * any lookup failure leaves [available] false and [WWaypointsCompat] registers nothing
 */
internal object WWaypointsBridge {

    private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/wWaypoints-Compat")

    private const val CLIENT = "com.wwaypoints.WaypointsClient"
    private const val CONFIG = "com.wwaypoints.client.ModConfig"
    private const val PRESET_STORE = "com.wwaypoints.client.label.preset.LabelPresetStore"
    private const val BLOCK_MODEL_HIGHLIGHT = "com.wwaypoints.client.BlockModelHighlightGeometry"
    private const val LABEL_RENDERER = "com.wwaypoints.render.WaypointOverlayLabelRenderer"

    private const val SETTINGS_SCREEN = "com.wwaypoints.client.screen.SettingsScreen"

    const val IMPORT_EXPORT_SCREEN = "com.wwaypoints.client.screen.ImportExportScreen"
    const val IMPORT_FROM_WORLD_SCREEN = "com.wwaypoints.client.screen.ImportFromWorldScreen"
    const val XAERO_IMPORT_SCREEN = "com.wwaypoints.importers.xaero.XaeroImportScreen"
    const val PRESET_LIBRARY_SCREEN = "com.wwaypoints.client.screen.PresetLibraryScreen"

    private val classes = HashMap<String, Class<*>?>()
    private val fields = HashMap<String, Field?>()
    private val methods = HashMap<String, Method?>()

    private fun cls(name: String): Class<*>? = classes.getOrPut(name) {
        runCatching { Class.forName(name) }
            .onFailure { LOGGER.warn("wWaypoints class '{}' not found", name) }
            .getOrNull()
    }

    private fun method(owner: String, name: String, vararg params: Class<*>): Method? =
        methods.getOrPut("$owner#$name/${params.size}") {
            runCatching { cls(owner)!!.getMethod(name, *params).apply { isAccessible = true } }
                .onFailure { LOGGER.warn("wWaypoints method '{}#{}' not found", owner, name) }
                .getOrNull()
        }

    private fun configField(name: String): Field? = fields.getOrPut(name) {
        runCatching { cls(CONFIG)!!.getField(name).apply { isAccessible = true } }
            .onFailure { LOGGER.warn("wWaypoints config field '{}' not found", name) }
            .getOrNull()
    }

    /** true when every entry point [WWaypointsCompat] depends on resolved successfully */
    val available: Boolean by lazy {
        cls(CLIENT) != null && cls(CONFIG) != null &&
            method(CLIENT, "getConfig") != null &&
            method(CLIENT, "saveConfig") != null &&
            config() != null
    }

    fun config(): Any? = runCatching { method(CLIENT, "getConfig")?.invoke(null) }.getOrNull()

    fun <T> get(name: String): T? {
        val cfg = config() ?: return null
        @Suppress("UNCHECKED_CAST")
        return runCatching { configField(name)?.get(cfg) as T? }.getOrNull()
    }

    fun set(name: String, value: Any?) {
        val cfg = config() ?: return
        runCatching { configField(name)?.set(cfg, value) }
            .onFailure { LOGGER.warn("Failed to write wWaypoints config field '{}'", name, it) }
    }

    private val pristineConfig: Any? by lazy {
        runCatching { cls(CONFIG)!!.getDeclaredConstructor().apply { isAccessible = true }.newInstance() }
            .onFailure { LOGGER.warn("Could not construct a pristine wWaypoints config", it) }
            .getOrNull()
    }

    fun configDefault(name: String): Any? {
        val field = configField(name) ?: return null
        if (!WrappingUtils.isSimpleClass(field.type) || Modifier.isStatic(field.modifiers)) return null
        return runCatching { field.get(pristineConfig) }.getOrNull()
    }

    fun bool(name: String, fallback: Boolean = false): Boolean = get<Boolean>(name) ?: fallback

    fun int(name: String, fallback: Int = 0): Int = get<Number>(name)?.toInt() ?: fallback

    fun float(name: String, fallback: Float = 0f): Float = get<Number>(name)?.toFloat() ?: fallback

    /** the nested `ModConfig$<name>` enum class or null if it is missing */
    fun enumClass(name: String): Class<*>? = cls("$CONFIG\$$name")

    /** the constant of nested enum [enumName] named [constant] or null */
    fun enumConstant(enumName: String, constant: String): Any? =
        enumClass(enumName)?.enumConstants?.firstOrNull { (it as Enum<*>).name == constant }

    private fun typeDefaults(which: String): Any? = get<Any>(which)

    fun defaults(name: String): Any? = runCatching {
        val defaults = typeDefaults("singleWaypointDefaults") ?: return null
        defaults.javaClass.getField(name).apply { isAccessible = true }.get(defaults)
    }.getOrNull()

    /**
     * writes a field on the single-waypoint defaults and mirrors it into the clump and group defaults like the
     * mod's own Defaults page does because 0.7.3 always keeps the three in sync under `UNIFIED` mode
     */
    fun setDefaults(name: String, value: Any?) {
        runCatching {
            val single = typeDefaults("singleWaypointDefaults") ?: return
            single.javaClass.getField(name).apply { isAccessible = true }.set(single, value)
            set("defaultsMode", enumConstant("DefaultsMode", "UNIFIED"))
            val copyFrom = single.javaClass.getMethod("copyFrom", single.javaClass)
            listOf("clumpDefaults", "groupDefaults").forEach { other ->
                typeDefaults(other)?.let { copyFrom.invoke(it, single) }
            }
        }.onFailure { LOGGER.warn("Failed to write wWaypoints default '{}'", name, it) }
    }

    /** the label presets offered for new waypoints as `id to displayName` */
    fun visiblePresets(): List<Pair<String, String>> = runCatching {
        val presets = method(PRESET_STORE, "visiblePresets")?.invoke(null) as? List<*> ?: return emptyList()
        val presetClass = cls("com.wwaypoints.client.label.preset.LabelPreset") ?: return emptyList()
        val displayName = method(PRESET_STORE, "displayName", presetClass)
        val idField = presetClass.getField("id")
        presets.mapNotNull { preset ->
            preset ?: return@mapNotNull null
            val id = idField.get(preset) as? String ?: return@mapNotNull null
            if (id.isBlank()) return@mapNotNull null
            id to (displayName?.invoke(null, preset) as? String ?: id)
        }
    }.getOrDefault(emptyList())

    fun saveConfig() = invokeClient("saveConfig")

    fun saveWaypoints() = invokeClient("saveWaypoints")

    fun resetConfigToDefaults() = invokeClient("resetConfigToDefaults")

    fun enforceDeathWaypointLimitNow() = invokeClient("enforceDeathWaypointLimitNow")

    fun syncPresetCreateKeybindings() = invokeClient("syncPresetCreateKeybindings")

    fun syncDeathWaypointsHiddenStateAfterToggle(enabled: Boolean) {
        runCatching {
            method(CLIENT, "syncDeathWaypointsHiddenStateAfterToggle", Boolean::class.javaPrimitiveType!!)
                ?.invoke(null, enabled)
        }.onFailure { LOGGER.warn("wWaypoints death waypoint sync failed", it) }
    }

    fun clearLastAirShapeCache() {
        runCatching { method(BLOCK_MODEL_HIGHLIGHT, "clearLastAirShapeCache")?.invoke(null) }
            .onFailure { LOGGER.warn("wWaypoints block model cache clear failed", it) }
    }

    fun clearSpriteCache() {
        runCatching { method(LABEL_RENDERER, "clearSpriteCache")?.invoke(null) }
            .onFailure { LOGGER.warn("wWaypoints sprite cache clear failed", it) }
    }

    private fun invokeClient(name: String) {
        runCatching { method(CLIENT, name)?.invoke(null) }
            .onFailure { LOGGER.warn("wWaypoints '{}' failed", name, it) }
    }

    fun keyMapping(accessor: String): KeyMapping? =
        runCatching { method(CLIENT, accessor)?.invoke(null) as? KeyMapping }.getOrNull()

    /** the mod's dynamic per-preset "create waypoint" mapping or null when it has not been registered yet */
    fun presetCreateKey(presetId: String): KeyMapping? = runCatching {
        method(CLIENT, "getPresetCreateWaypointKey", String::class.java)?.invoke(null, presetId) as? KeyMapping
    }.getOrNull()

    fun rebindKeyMapping(mapping: KeyMapping, key: InputConstants.Key) {
        runCatching {
            method(CLIENT, "rebindKeyMapping", KeyMapping::class.java, InputConstants.Key::class.java)
                ?.invoke(null, mapping, key)
        }.onFailure { LOGGER.warn("wWaypoints rebind failed", it) }
    }

    private const val HIDE_CYCLE = "com.wwaypoints.client.HideWaypointCycle"

    private fun cycleConstant(name: String, fallback: Int): Int =
        runCatching { cls(HIDE_CYCLE)?.getField(name)?.getInt(null) }.getOrNull() ?: fallback

    fun cycleMaxBlocks(): Int = cycleConstant("MAX_BLOCKS", 100000)

    fun cycleMaxEntries(): Int = cycleConstant("MAX_BLOCKS_IN_CYCLE", 5)

    private fun legacyThreshold(): Int = int("hideWaypointsDistanceThreshold")

    /** runs a cycle through the mod's own normalizer which owns every invariant the editor must respect */
    fun normalizedCycle(raw: IntArray?): IntArray = runCatching {
        method(HIDE_CYCLE, "normalize", IntArray::class.java, Int::class.javaPrimitiveType!!)
            ?.invoke(null, raw, legacyThreshold()) as? IntArray
    }.getOrNull() ?: raw ?: intArrayOf(-1, 0)

    fun nextCycleThreshold(current: IntArray): Int = runCatching {
        val next = method(HIDE_CYCLE, "addNextThreshold", IntArray::class.java, Int::class.javaPrimitiveType!!)
            ?.invoke(null, current, legacyThreshold()) as? IntArray ?: return@runCatching null
        // addNextThreshold returns the whole cycle in its own canonical order so the new entry is found by
        // difference rather than by position
        val existing = current.toHashSet()
        next.firstOrNull { it !in existing }
    }.getOrNull() ?: cycleConstant("DEFAULT_WITHIN_BLOCKS", 100)

    /** the unit the first-party editor draws after a distance because `HideWaypointCycle.label` leaves it off */
    const val CYCLE_UNIT = "m"

    fun cycleLabel(value: Int): String {
        val label = runCatching {
            method(HIDE_CYCLE, "label", Int::class.javaPrimitiveType!!)?.invoke(null, value) as? String
        }.getOrNull() ?: value.toString()
        return if (value > 0) label + CYCLE_UNIT else label
    }

    private const val CONFIG_STORAGE = "com.wwaypoints.client.ConfigStorage"
    private const val IMPORT_EXPORT = "com.wwaypoints.client.screen.ImportExportScreen"
    private const val TEMPLATE_IO = "com.wwaypoints.client.screen.TemplateImportExportService"

    /** looks up a method the mod does not expose publicly using package-private statics only and never UI state */
    private fun internalMethod(owner: String, name: String, vararg params: Class<*>): Method? =
        methods.getOrPut("$owner#$name/${params.size}") {
            runCatching {
                cls(owner)!!.getDeclaredMethod(name, *params).apply { isAccessible = true }
            }.onFailure { LOGGER.warn("wWaypoints method '{}#{}' not found", owner, name) }.getOrNull()
        }

    /** the `.wsettings` payload with settings and defaults and pinned icons and label presets */
    fun settingsJson(): String? = runCatching {
        method(CONFIG_STORAGE, "toSettingsJson", cls(CONFIG)!!)?.invoke(null, config()) as? String
    }.onFailure { LOGGER.warn("Failed to serialize wWaypoints settings", it) }.getOrNull()

    /** applies a `.wsettings` payload and note the mod's parser also replaces the stored label presets */
    fun applySettingsJson(json: String): Boolean = runCatching {
        val parsed = method(CONFIG_STORAGE, "fromJson", String::class.java)?.invoke(null, json) ?: return false
        method(CLIENT, "applyConfig", cls(CONFIG)!!)?.invoke(null, parsed)
        true
    }.onFailure { LOGGER.warn("Failed to apply wWaypoints settings", it) }.getOrDefault(false)

    fun currentWorldId(): String? =
        runCatching { method(CLIENT, "getCurrentWorldId")?.invoke(null) as? String }.getOrNull()
            ?.takeIf { it.isNotBlank() }

    /**
     * the current world's waypoints in the mod's own `.wwaypoint` envelope
     *
     * the per-world object comes from the mod so dimension payloads stay authoritative and only the envelope
     * around it is built here because the method that would build it is private
     */
    fun exportCurrentWorldJson(exportedAtMillis: Long): String? {
        val worldId = currentWorldId() ?: return null
        val world = runCatching {
            internalMethod(IMPORT_EXPORT, "exportWorld", String::class.java)?.invoke(null, worldId)
        }.onFailure { LOGGER.warn("Failed to export wWaypoints world '{}'", worldId, it) }.getOrNull() ?: return null
        return """{"format":"wwaypoints","formatVersion":1,"exportedAt":$exportedAtMillis,""" +
            """"scope":"CURRENT_WORLD","worlds":[$world]}"""
    }

    /** imports a `.wtemplate` file and returns the name the template was stored under */
    fun importTemplateFile(path: java.nio.file.Path): String? = runCatching {
        internalMethod(TEMPLATE_IO, "importTemplateFile", java.nio.file.Path::class.java)
            ?.invoke(null, path) as? String
    }.onFailure { LOGGER.warn("Failed to import a wWaypoints template", it) }.getOrNull()

    /** shows a message through the mod's own toast so feedback matches its first-party screens */
    fun toast(message: String) {
        runCatching { method(CLIENT, "toast", String::class.java)?.invoke(null, message) }
            .onFailure { LOGGER.warn("wWaypoints toast failed", it) }
    }

    /** the user's label presets as `id to displayName` in store order */
    fun myPresets(): List<Pair<String, String>> = runCatching {
        val presets = method(PRESET_STORE, "myPresets")?.invoke(null) as? List<*> ?: return emptyList()
        val presetClass = cls("com.wwaypoints.client.label.preset.LabelPreset") ?: return emptyList()
        val displayName = method(PRESET_STORE, "displayName", presetClass)
        val idField = presetClass.getField("id")
        presets.mapNotNull { preset ->
            preset ?: return@mapNotNull null
            val id = idField.get(preset) as? String ?: return@mapNotNull null
            if (id.isBlank()) return@mapNotNull null
            id to (displayName?.invoke(null, preset) as? String ?: id)
        }
    }.getOrDefault(emptyList())

    /** instantiates one of the mod's own screens returning to [parent] on close */
    fun screen(fqcn: String, parent: Screen?): Screen? = runCatching {
        cls(fqcn)?.getConstructor(Screen::class.java)?.newInstance(parent) as? Screen
    }.onFailure { LOGGER.warn("Failed to open wWaypoints screen '{}'", fqcn, it) }.getOrNull()

    /**
     * the mod's settings screen opened on the named `SettingsScreen.Page` and Data sub-tab where relevant
     *
     * the page fields are plain assignable fields the mod's own deep-link factories write the same way so if the
     * shape ever changes the screen simply opens on its default page
     */
    fun settingsScreen(parent: Screen?, page: String, importSubTab: Int = -1): Screen? {
        val screen = screen(SETTINGS_SCREEN, parent) ?: return null
        runCatching {
            val pageClass = cls("$SETTINGS_SCREEN\$Page") ?: return screen
            val target = pageClass.enumConstants?.firstOrNull { (it as Enum<*>).name == page } ?: return screen
            screen.javaClass.getDeclaredField("page").apply { isAccessible = true }.set(screen, target)
            screen.javaClass.getDeclaredField("lastPage").apply { isAccessible = true }.set(screen, target)
            if (importSubTab >= 0) {
                screen.javaClass.getDeclaredField("importSubTab").apply { isAccessible = true }
                    .setInt(screen, importSubTab)
            }
        }.onFailure { LOGGER.warn("Could not target wWaypoints settings page '{}'", page, it) }
        return screen
    }

    /** the mod's stylist for one of its special waypoint kinds `SUPPLY_DROP` / `AUTO_PICK` / `DEATH` */
    fun specialLabelStylist(kind: String, parent: Screen?): Screen? = runCatching {
        val kindClass = cls("com.wwaypoints.client.screen.HopliteAppearanceScreen\$Kind") ?: return null
        val constant = kindClass.enumConstants?.firstOrNull { (it as Enum<*>).name == kind } ?: return null
        cls(SETTINGS_SCREEN)
            ?.getMethod("specialLabelStylist", Screen::class.java, kindClass)
            ?.invoke(null, parent, constant) as? Screen
    }.onFailure { LOGGER.warn("Failed to open the wWaypoints '{}' stylist", kind, it) }.getOrNull()
}

//? }
