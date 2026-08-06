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
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Property.Display
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.visualizer
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.hud.v1.OneConfigHudWrapper
import org.polyfrost.oneconfig.api.hud.v1.events.HudEditorToggleEvent
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer
import org.polyfrost.oneconfig.internal.ui.keybind.RightShiftConflicts
import java.util.IdentityHashMap

object OdinCompat {
    private val LOGGER = LogManager.getLogger("OneConfig/Odin-Compat")

    private var initialized = false
    private val wrapped: MutableSet<HUDSetting> =
        java.util.Collections.newSetFromMap(IdentityHashMap<HUDSetting, Boolean>())

    @JvmStatic
    fun ensureRegistered() {
        if (!initialized) {
            initialized = true
            runCatching { unbindRightShiftKeybinds() }
                .onFailure { LOGGER.error("Failed to unbind conflicting Odin keybinds", it) }
            EventManager.register(HudEditorToggleEvent::class.java) { e ->
                if (e.open) registerAll() else runCatching { ModuleManager.saveConfigurations() }
            }
            CompatOverlayRenderer.register(::renderExamples)
        }
        registerAll()
    }

    /**
     * Clears Odin keybinds, such as the Click GUI's, that conflict with OneConfig's own Right Shift keybind.
     * Odin drives these itself instead of registering them with Minecraft, so they are invisible to the vanilla
     * keybind sweep and have to be handled here.
     *
     * @see RightShiftConflicts
     */
    private fun unbindRightShiftKeybinds() {
        val rightShift = RightShiftConflicts.key()
        val unbound = ArrayList<String>()

        for ((_, module) in ModuleManager.modules) {
            for (setting in module.settings.values) {
                if (setting !is KeybindSetting) continue
                if (!RightShiftConflicts.isNew("odin:${module.name}:${setting.name}")) continue
                if (setting.value != rightShift) continue
                setting.unbind()
                unbound.add("${module.name}/${setting.name}")
            }
        }

        if (unbound.isNotEmpty()) {
            runCatching { ModuleManager.saveConfigurations() }
                .onFailure { LOGGER.error("Failed to save Odin config after unbinding keybinds", it) }
            LOGGER.info("Unbound ${unbound.size} Odin keybind(s) using Right Shift: $unbound")
        }
        RightShiftConflicts.save()
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
        for (setting in module.settings.values) {
            if (setting !is RenderableSetting<*>) continue
            if (setting is HUDSetting) continue
            if (setting is DropdownSetting) continue
            if (setting is KeybindSetting) continue
            if (!ownedByHud(setting, hud, huds)) continue
            runCatching { buildProperty(setting) }.getOrNull()?.let(out::add)
        }
        return out
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

    private fun buildProperty(setting: RenderableSetting<*>): Property<*>? {
        val id = settingId(setting)
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
        prop.addMetadata("subcategory", "Settings")
        prop.addDisplayCondition { if (setting.isVisible) Display.SHOWN else Display.DISABLED }
        return prop
    }

    private fun settingId(setting: RenderableSetting<*>): String {
        val normalized = setting.name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        return "odin_setting_" + normalized.ifEmpty { System.identityHashCode(setting).toString() }
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
