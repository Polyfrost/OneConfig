//? apec_compat {
/*package org.polyfrost.oneconfig.internal.compat

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.visualizer
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.hud.v1.OneConfigHudWrapper
import org.polyfrost.oneconfig.api.hud.v1.events.HudEditorToggleEvent
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer
import uk.co.hexeption.apec.Apec
import uk.co.hexeption.apec.hud.Element
import uk.co.hexeption.apec.hud.ElementType
import uk.co.hexeption.apec.settings.SettingID
import uk.co.hexeption.apec.settings.SettingsManager
import java.io.File
import java.util.function.Consumer
import kotlin.math.abs

object ApecCompat {
    private val LOGGER = LogManager.getLogger("OneConfig/Apec-Compat")

    private val ELEMENT_SETTINGS: Map<ElementType, List<SettingID>> = mapOf(
        ElementType.HP_BAR to listOf(SettingID.HP_BAR),
        ElementType.HP_TEXT to listOf(SettingID.HP_TEXT),
        ElementType.MP_BAR to listOf(SettingID.MP_BAR),
        ElementType.MP_TEXT to listOf(SettingID.MP_TEXT),
        ElementType.XP_BAR to listOf(SettingID.XP_BAR),
        ElementType.XP_TEXT to listOf(SettingID.XP_TEXT),
        ElementType.DRILL_FUEL_BAR to listOf(SettingID.DRILL_FUEL_BAR),
        ElementType.DRILL_FUEL_TEXT to listOf(SettingID.DRILL_FUEL_TEXT),
        ElementType.AIR_BAR to listOf(SettingID.SHOW_AIR_BAR),
        ElementType.AIR_TEXT to listOf(SettingID.AIR_TEXT),
        ElementType.PRESSURE_BAR to listOf(SettingID.SHOW_PRESSURE_BAR),
        ElementType.PRESSURE_TEXT to listOf(SettingID.PRESSURE_TEXT),
        ElementType.SKILL_BAR to listOf(SettingID.SHOW_SKILL_XP),
        ElementType.SKILL_TEXT to listOf(SettingID.SKILL_TEXT),
        ElementType.WARNING_ICONS to listOf(SettingID.SHOW_WARNING),
        ElementType.TOOL_TIP_TEXT to listOf(SettingID.ITEM_HIGHLIGHT_TEXT),
        ElementType.ITEM_HOT_BAR to listOf(SettingID.COMPATIBILITY_SAFETY),
    )

    private val ELEMENT_NAMES: Map<ElementType, String> = mapOf(
        ElementType.HP_BAR to "Health Bar",
        ElementType.HP_TEXT to "Health Text",
        ElementType.MP_BAR to "Mana Bar",
        ElementType.MP_TEXT to "Mana Text",
        ElementType.XP_BAR to "XP Bar",
        ElementType.XP_TEXT to "XP Text",
        ElementType.DRILL_FUEL_BAR to "Drill Fuel Bar",
        ElementType.DRILL_FUEL_TEXT to "Drill Fuel Text",
        ElementType.AIR_BAR to "Air Bar",
        ElementType.AIR_TEXT to "Air Text",
        ElementType.PRESSURE_BAR to "Pressure Bar",
        ElementType.PRESSURE_TEXT to "Pressure Text",
        ElementType.SKILL_BAR to "Skill XP Bar",
        ElementType.SKILL_TEXT to "Skill XP Text",
        ElementType.EXTRA_INFO to "Extra Info",
        ElementType.ITEM_HOT_BAR to "Item Hotbar",
        ElementType.TOOL_TIP_TEXT to "Item Highlight Text",
        ElementType.WARNING_ICONS to "Warning Icons",
    )

    private var initialized = false
    private var dirty = false

    private var registering = false

    @JvmStatic
    fun initialize() {
        if (initialized) return
        initialized = true
        register()
    }

    private fun register() {
        val menu = Apec.apecMenu ?: return
        var count = 0
        registering = true
        try {
            for (element in ArrayList(menu.guiElements)) {
                val type = element.type ?: continue
                if (type == ElementType.BOTTOM_BAR || element.hasSubComponents()) continue
                runCatching {
                    ApecElementWrapper(type).register()
                    count++
                }.onFailure { LOGGER.warn("Failed to register Apec element '{}'", type.name, it) }
            }
        } finally {
            registering = false
        }
        LOGGER.info("Registered {} Apec elements into the OneConfig HUD editor", count)

        EventManager.register(HudEditorToggleEvent::class.java, Consumer { event ->
            if (event.open) {
                runCatching { Apec.apecMenu?.CustomizationMenuOpened() }
                    .onFailure { LOGGER.debug("Failed to run Apec's editor-open hook", it) }
            } else {
                save()
            }
        })

        CompatOverlayRenderer.register(::renderElements)
    }

    private fun renderElements(ctx: GuiGraphicsExtractor) {
        val menu = Apec.apecMenu ?: return
        if (!menu.shouldShowHUD()) return
        for (element in ArrayList(menu.guiElements)) {
            val pose = ctx.pose()
            pose.pushMatrix()
            pose.scale(element.scale, element.scale)
            runCatching { element.drawText(ctx, true) }
            pose.popMatrix()
        }
    }

    internal fun markDirty() {
        if (!registering) dirty = true
    }

    internal fun element(type: ElementType): Element? =
        runCatching { Apec.apecMenu?.getGuiComponent<Element>(type) }.getOrNull()

    private fun save() {
        if (!dirty) return
        dirty = false
        runCatching { saveDeltas() }
            .onFailure { LOGGER.warn("Failed to save Apec GUI deltas", it) }
    }

    private fun saveDeltas() {
        val menu = Apec.apecMenu ?: return
        val array = JsonArray()
        for (element in ArrayList(menu.guiElements)) {
            val type = element.type ?: continue
            val delta = element.deltaPosition
            val obj = JsonObject()
            obj.addProperty("type", type.name)
            obj.addProperty("deltaX", delta.x)
            obj.addProperty("deltaY", delta.y)
            obj.addProperty("scale", element.scale)
            if (element.hasSubComponents()) {
                val subArray = JsonArray()
                for (i in 0 until element.subComponentCount()) {
                    val subDelta = element.getSubElementDeltaPosition(i)
                    val subObj = JsonObject()
                    subObj.addProperty("index", i)
                    subObj.addProperty("deltaX", subDelta.x)
                    subObj.addProperty("deltaY", subDelta.y)
                    subArray.add(subObj)
                }
                obj.add("subComponents", subArray)
            }
            array.add(obj)
        }
        val dir = File("config/Apec")
        dir.mkdirs()
        File(dir, "GuiDeltas.json").writeText(GsonBuilder().setPrettyPrinting().create().toJson(array))
    }

    internal fun buildSettings(type: ElementType): List<Property<*>> {
        val ids = ELEMENT_SETTINGS[type] ?: return emptyList()
        return ids.mapNotNull { id ->
            runCatching { settingProperty(id) }
                .onFailure { LOGGER.warn("Failed to build Apec setting '{}'", id.name, it) }
                .getOrNull()
        }
    }

    private fun settingProperty(id: SettingID): Property<*>? {
        val data = SettingsManager.getNameAndDesc(id) ?: return null
        return Properties.functional<Boolean>(
            { Apec.INSTANCE?.settingsManager?.getSettingState(id) ?: false },
            { value -> Apec.INSTANCE?.settingsManager?.setSettingState(id, value) },
            id = "apec_setting_${id.name.lowercase()}",
            name = data.a,
            description = data.b,
            type = Boolean::class.javaPrimitiveType,
        ).apply {
            visualizer = Visualizer.SwitchVisualizer::class.java
            addMetadata("subcategory", "Settings")
        }
    }

    internal fun displayName(type: ElementType): String =
        ELEMENT_NAMES[type] ?: type.name.split('_').joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
}

private class ApecElementWrapper(private val type: ElementType) : OneConfigHudWrapper {
    private companion object {
        const val FALLBACK_WIDTH = 60f
        const val FALLBACK_HEIGHT = 12f

        const val POSITION_EPSILON = 0.01f

        const val POSITION_PASSES = 3
    }

    private val element: Element? get() = ApecCompat.element(type)

    override var id: String = "apec_${type.name.lowercase()}"

    override var name: String = ApecCompat.displayName(type)

    override val modId: String = "apec"

    override var x: Float
        get() = element?.let { topLeft(it).x } ?: 0f
        set(value) = moveTo(value, null)

    override var y: Float
        get() = element?.let { topLeft(it).y } ?: 0f
        set(value) = moveTo(null, value)

    override var scale: Float
        get() = element?.scale ?: 1f
        set(value) {
            val element = element ?: return
            element.scale = value
            ApecCompat.markDirty()
        }

    override val supportsScale: Boolean get() = element?.isScalable ?: true

    override var scaledWidth: Float
        get() = element?.let { size(it).x } ?: 0f
        set(_) {}

    override var scaledHeight: Float
        get() = element?.let { size(it).y } ?: 0f
        set(_) {}

    override fun linkedProperties(): List<Property<*>> = ApecCompat.buildSettings(type)

    private fun topLeft(element: Element): Vector2f {
        val anchor = element.currentAnchorPoint
        val bound = element.currentBoundingPoint
        return Vector2f(minOf(anchor.x, bound.x), minOf(anchor.y, bound.y))
    }

    private fun size(element: Element): Vector2f {
        val bound = element.boundingPoint
        val scale = element.scale
        val width = abs(bound.x)
        val height = abs(bound.y)
        return Vector2f(
            if (width > 0f) width else FALLBACK_WIDTH * scale,
            if (height > 0f) height else FALLBACK_HEIGHT * scale,
        )
    }

    private fun moveTo(targetX: Float?, targetY: Float?) {
        val element = element ?: return
        repeat(POSITION_PASSES) {
            val current = topLeft(element)
            val dx = if (targetX != null) targetX - current.x else 0f
            val dy = if (targetY != null) targetY - current.y else 0f
            if (abs(dx) < POSITION_EPSILON && abs(dy) < POSITION_EPSILON) return
            val delta = element.deltaPosition
            element.setDeltaPosition(Vector2f(delta.x + dx, delta.y + dy))
            ApecCompat.markDirty()
        }
    }
}
*///? }
