//? odin_compat {
/*package org.polyfrost.oneconfig.internal.compat

import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.clickgui.settings.RenderableSetting
import com.odtheking.odin.clickgui.settings.Setting
import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.HUDSetting
import com.odtheking.odin.clickgui.settings.impl.KeybindSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.utils.Color
import java.util.Collections
import java.util.IdentityHashMap
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Property.Display
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.category
import org.polyfrost.oneconfig.api.config.v1.dsl.noCache
import org.polyfrost.oneconfig.api.config.v1.dsl.saveFunction
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.api.config.v1.dsl.visualizer
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.hud.v1.OneConfigHudWrapper
import org.polyfrost.oneconfig.api.hud.v1.events.HudEditorToggleEvent
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.internal.compat.CompatIds.idPart
import org.polyfrost.oneconfig.internal.compat.CompatIds.uniqueId
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindConflicts

object OdinCompat {
    private val LOGGER = LogManager.getLogger("OneConfig/Odin-Compat")

    private const val ODIN_ID = "odin"

    private var initialized = false
    private var treeRegistered = false

    private var treeModules = -1
    private val wrapped: MutableSet<HUDSetting> =
        Collections.newSetFromMap(IdentityHashMap<HUDSetting, Boolean>())

    @JvmStatic
    fun ensureRegistered() {
        if (!initialized) {
            initialized = true
            registerTree()
            runCatching { unbindConflictingKeybinds() }
                .onFailure { LOGGER.error("Failed to unbind conflicting Odin keybinds", it) }
            EventManager.register(HudEditorToggleEvent::class.java) { e ->
                if (e.open) registerAll() else flush()
            }
            CompatOverlayRenderer.register(::renderExamples)
        }
        registerAll()
    }

    @JvmStatic
    fun registerTree() {
        if (treeRegistered) {
            if (treeModules >= 0 && treeModules != ModuleManager.modules.size) buildTree()
            return
        }
        treeRegistered = true
        CompatLoader.nativeLoadedConfigs.add(ODIN_ID)
        CompatLoader.requireTranslations(skip = true, init = ::buildTree)
    }

    private fun buildTree() {
        treeModules = ModuleManager.modules.size
        runCatching { CompatSnapshots.register(OdinSettingsAdapter.tree(ODIN_ID)) }
            .onFailure { LOGGER.error("Failed to register Odin config tree", it) }
    }

    /**
     * Clears Odin keybinds, such as the Click GUI's, that conflict with OneConfig's own open keybind.
     * Odin drives these itself instead of registering them with Minecraft, so they are invisible to the vanilla
     * keybind sweep and have to be handled here.
     *
     * @see KeybindConflicts
     */
    private fun unbindConflictingKeybinds() {
        val occupied = KeybindConflicts.key()
        if (occupied == InputConstants.UNKNOWN) return
        val unbound = ArrayList<String>()

        for ((_, module) in ModuleManager.modules) {
            for (setting in module.settings.values) {
                if (setting !is KeybindSetting) continue
                if (!KeybindConflicts.isNew("odin:${module.name}:${setting.name}")) continue
                if (setting.value != occupied) continue
                setting.unbind()
                unbound.add("${module.name}/${setting.name}")
            }
        }

        if (unbound.isNotEmpty()) {
            runCatching { ModuleManager.saveConfigurations() }
                .onFailure { LOGGER.error("Failed to save Odin config after unbinding keybinds", it) }
            LOGGER.info("Unbound ${unbound.size} Odin keybind(s) using ${occupied.name}: $unbound")
        }
        KeybindConflicts.save()
    }

    private fun KeybindSetting.unbind() {
        value = InputConstants.UNKNOWN
        // KeybindSetting caches the width of the rendered key name, and only refreshes it from its own private setter.
        runCatching {
            val field = KeybindSetting::class.java.getDeclaredField("keyNameWidth")
            field.isAccessible = true
            field.setFloat(this, -1f)
        }
    }

    private fun renderExamples(ctx: GuiGraphicsExtractor) {
        val sf = Minecraft.getInstance().window.guiScale.toFloat().coerceAtLeast(1f)
        val pose = ctx.pose()
        pose.pushMatrix()
        pose.scale(1f / sf, 1f / sf)
        try {
            for (setting in ArrayList(ModuleManager.hudSettingsCache)) {
                if (!setting.isEnabled) continue
                runCatching { setting.value.draw(ctx, true) }
            }
        } finally {
            pose.popMatrix()
        }
    }

    internal fun flush() {
        runCatching { ModuleManager.saveConfigurations() }
            .onFailure { LOGGER.error("Failed to save Odin config", it) }
    }

    private fun registerAll() {
        for (setting in ArrayList(ModuleManager.hudSettingsCache)) {
            if (!setting.isEnabled) continue
            if (!wrapped.add(setting)) continue
            runCatching { OdinHudWrapper(setting).register() }
                .onFailure { LOGGER.error("Failed to register Odin HUD '${setting.name}'", it) }
        }
    }
}

private class OdinHudWrapper(private val setting: HUDSetting) : OneConfigHudWrapper {
    private val element get() = setting.value

    private val guiScale: Float
        get() = Minecraft.getInstance().window.guiScale.toFloat().coerceAtLeast(1f)

    override var id: String = buildId(setting)
    override var name: String = setting.name
    override val modId: String = "odin"

    override var x: Float
        get() = element.x / guiScale
        set(value) { element.x = Math.round(value * guiScale) }

    override var y: Float
        get() = element.y / guiScale
        set(value) { element.y = Math.round(value * guiScale) }

    override var scale: Float
        get() = element.scale
        set(value) { element.scale = value }

    override var hidden: Boolean
        get() = !element.enabled
        set(value) { element.enabled = !value }

    override var scaledWidth: Float
        get() = if (setting.isEnabled) element.width * element.scale / guiScale else 0f
        set(_) {}

    override var scaledHeight: Float
        get() = if (setting.isEnabled) element.height * element.scale / guiScale else 0f
        set(_) {}

    override fun linkedProperties(): List<Property<*>> = OdinSettingsAdapter.build(setting)

    override fun save() = OdinCompat.flush()

    private companion object {
        fun buildId(setting: HUDSetting): String {
            val raw = "odin_${setting.module.name}_${setting.name}"
            return raw.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        }
    }
}

private object OdinSettingsAdapter {
    fun build(hud: HUDSetting): List<Property<*>> {
        val module = hud.module
        val huds = module.settings.values.filterIsInstance<HUDSetting>()
        val out = ArrayList<Property<*>>()
        for ((index, setting) in module.settings.values.withIndex()) {
            if (setting !is RenderableSetting<*>) continue
            if (setting is HUDSetting) continue
            if (setting is DropdownSetting) continue
            if (setting is KeybindSetting) continue
            if (!ownedByHud(setting, hud, huds)) continue
            runCatching { buildProperty(setting, settingId(setting, index)) }.getOrNull()
                ?.let { it.addMetadata("subcategory", "Settings"); out.add(it) }
        }
        linkDisplays(out)
        return out
    }

    fun tree(id: String): Tree {
        val tree = Tree.tree()
        tree.id = id
        tree.title = "Odin"
        tree.noCache = true
        tree.saveFunction = Runnable { OdinCompat.flush() }
        ModInfo.loadedMods.firstOrNull { it.id == id }?.extractIconFile()?.let { tree.addMetadata("icon_path", it) }

        val usedIds = HashSet<String>()
        for (module in ArrayList(ModuleManager.modules.values)) {
            if (module.isDevModule) continue
            val base = idPart(module.name, "module")
            val props = ArrayList<Property<*>>()
            if (!module.alwaysActive) {
                props += Properties.functional<Boolean>(
                    { module.enabled }, { if (it != module.enabled) module.toggle() },
                    id = uniqueId(usedIds, "$base/enabled"), type = Boolean::class.javaPrimitiveType,
                    name = "Enabled", description = module.description,
                ).apply { visualizer = Visualizer.SwitchVisualizer::class.java }
            }
            for ((index, setting) in module.settings.values.withIndex()) {
                if (setting !is RenderableSetting<*>) continue
                if (setting is HUDSetting || setting is DropdownSetting || setting is KeybindSetting) continue
                val settingId = uniqueId(usedIds, "$base/${idPart(setting.name, index.toString())}")
                runCatching { buildProperty(setting, settingId) }.getOrNull()?.let(props::add)
            }
            linkDisplays(props)
            for (prop in props) {
                prop.category = module.category.name
                prop.subcategory = module.name
                tree.put(prop)
            }
        }
        return tree
    }

    @Suppress("UNCHECKED_CAST")
    private fun linkDisplays(props: List<Property<*>>) {
        for (prop in props) (prop as Property<Any?>).addCallback { props.forEach(Property<*>::revaluateDisplay); false }
    }

    private fun ownedByHud(setting: Setting<*>, hud: HUDSetting, huds: List<HUDSetting>): Boolean {
        if (huds.size <= 1) return true
        val owner = ownerHud(setting, huds)
        return owner == null || owner === hud
    }

    private fun ownerHud(setting: Setting<*>, huds: List<HUDSetting>): HUDSetting? {
        val name = setting.name.lowercase()
        var best: HUDSetting? = null
        var bestLen = 0
        for (candidate in huds) {
            val token = baseToken(candidate.name)
            if (token.isNotEmpty() && name.startsWith(token) && token.length > bestLen) {
                best = candidate
                bestLen = token.length
            }
        }
        return best
    }

    private fun baseToken(hudName: String): String =
        hudName.trim().removeSuffix("HUD").removeSuffix("Hud").removeSuffix("hud").trim().lowercase()

    private fun buildProperty(setting: RenderableSetting<*>, id: String): Property<*>? {
        val prop: Property<*> = when (setting) {
            is BooleanSetting -> Properties.functional<Boolean>(
                { setting.value }, { setting.value = it },
                id = id, type = Boolean::class.javaPrimitiveType,
                name = setting.name, description = setting.description,
            ).apply { visualizer = Visualizer.SwitchVisualizer::class.java }

            is ColorSetting -> Properties.functional<Int>(
                { setting.value.rgba }, { setting.value = Color(it) },
                id = id, type = Int::class.javaPrimitiveType,
                name = setting.name, description = setting.description,
            ).apply {
                visualizer = Visualizer.ColorVisualizer::class.java
                if (!colorAllowsAlpha(setting)) addMetadata("noAlpha", Unit)
            }

            is SelectorSetting -> Properties.functional<Int>(
                { setting.value }, { setting.value = it },
                id = id, type = Int::class.javaPrimitiveType,
                name = setting.name, description = setting.description,
            ).apply {
                visualizer = Visualizer.DropdownVisualizer::class.java
                addMetadata("options", selectorOptions(setting))
            }

            is NumberSetting<*> -> {
                @Suppress("UNCHECKED_CAST")
                val number = setting as NumberSetting<Double>
                val bounds = numberBounds(setting)
                Properties.functional<Float>(
                    { number.value.toFloat() }, { number.value = it.toDouble() },
                    id = id, type = Float::class.javaPrimitiveType,
                    name = setting.name, description = setting.description,
                ).apply {
                    visualizer = Visualizer.SliderVisualizer::class.java
                    addMetadata("min", bounds.first)
                    addMetadata("max", bounds.second)
                    addMetadata("step", bounds.third)
                }
            }

            is StringSetting -> Properties.functional<String>(
                { setting.value }, { setting.value = it },
                id = id, type = String::class.java,
                name = setting.name, description = setting.description,
            ).apply { visualizer = Visualizer.TextVisualizer::class.java }

            is ActionSetting -> Properties.dummy(id = id, name = setting.name, description = setting.description).apply {
                visualizer = Visualizer.ButtonVisualizer::class.java
                addMetadata("text", setting.name)
                metadata?.put("runnable", Runnable { runCatching { setting.action() } })
            }

            else -> return null
        }
        prop.addDisplayCondition { if (setting.isVisible) Display.SHOWN else Display.HIDDEN }
        return prop
    }

    // Fallback to position in config if name is empty
    private fun settingId(setting: RenderableSetting<*>, index: Int): String {
        val normalized = setting.name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        return "odin_setting_" + normalized.ifEmpty { index.toString() }
    }

    private fun colorAllowsAlpha(setting: ColorSetting): Boolean = runCatching {
        ColorSetting::class.java.getDeclaredField("allowAlpha").apply { isAccessible = true }.getBoolean(setting)
    }.getOrDefault(true)

    @Suppress("UNCHECKED_CAST")
    private fun selectorOptions(setting: SelectorSetting): List<String> = runCatching {
        SelectorSetting::class.java.getDeclaredField("options").apply { isAccessible = true }
            .get(setting) as List<String>
    }.getOrDefault(emptyList())

    private fun numberBounds(setting: NumberSetting<*>): Triple<Float, Float, Float> {
        fun field(name: String, fallback: Double): Double = runCatching {
            NumberSetting::class.java.getDeclaredField(name).apply { isAccessible = true }.getDouble(setting)
        }.getOrDefault(fallback)
        return Triple(
            field("minDouble", 0.0).toFloat(),
            field("maxDouble", 100.0).toFloat(),
            field("incrementDouble", 1.0).toFloat(),
        )
    }
}
*///? }
