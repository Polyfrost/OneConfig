//? skyblocker_hud_v2 {
package org.polyfrost.oneconfig.internal.compat

import com.google.gson.JsonObject
import de.hysky.skyblocker.config.SkyblockerConfigManager
import de.hysky.skyblocker.skyblock.tabhud.screenbuilder.LayerBuilder
import de.hysky.skyblocker.skyblock.tabhud.screenbuilder.PositionedWidget
import de.hysky.skyblocker.skyblock.tabhud.screenbuilder.WidgetConfig
import de.hysky.skyblocker.skyblock.tabhud.screenbuilder.WidgetManager
import de.hysky.skyblocker.skyblock.tabhud.screenbuilder.pipeline.PositionRule
import de.hysky.skyblocker.skyblock.tabhud.widget.HudWidget
import de.hysky.skyblocker.utils.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.visualizer
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.hud.v1.OneConfigHudWrapper
import org.polyfrost.oneconfig.api.hud.v1.events.HudEditorToggleEvent
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer
import java.util.function.Consumer
import kotlin.math.roundToInt

object SkyblockerWidgetCompat {
    private val LOGGER = LogManager.getLogger("OneConfig/Skyblocker-Widget-Compat")

    private var initialized = false
    private var registered = false
    private var dirty = false

    @Volatile
    private var redrawing = false

    @Volatile
    private var visibleIds: Set<String> = emptySet()

    @JvmStatic
    fun isRedrawing(): Boolean = redrawing

    @JvmStatic
    fun initialize() {
        if (initialized) return
        initialized = true
        CompatLoader.requireTranslations(skip = true) { register() }
    }

    private fun register() {
        if (registered) return
        registered = true
        val seen = HashSet<String>()
        var count = 0
        for (widget in WidgetManager.WIDGET_INSTANCES.values.sortedBy { it.internalID }) {
            if (!editable(widget)) continue
            if (!seen.add(sanitize(widget.internalID))) {
                LOGGER.warn("Skipping Skyblocker widget '{}': id clashes with an already registered widget", widget.internalID)
                continue
            }
            runCatching {
                SkyblockerWidgetWrapper(widget).register()
                count++
            }.onFailure { LOGGER.warn("Failed to register Skyblocker HUD widget '{}'", widget.internalID, it) }
        }
        LOGGER.info("Registered {} Skyblocker HUD widgets into the OneConfig HUD editor", count)

        EventManager.register(HudEditorToggleEvent::class.java, Consumer { event ->
            if (!event.open) save()
        })

        CompatOverlayRenderer.register(::renderWidgets)
    }

    private fun editable(widget: HudWidget): Boolean {
        val available = runCatching { widget.information.available() }.getOrNull() ?: return false
        return WidgetManager.ALLOWED_LOCATIONS.any { available.test(it) }
    }

    private fun renderWidgets(ctx: GuiGraphicsExtractor) {
        if (!Utils.isOnSkyblock()) {
            visibleIds = emptySet()
            return
        }
        applyDeferred()
        val window = Minecraft.getInstance().window
        val scale = tabHudScale()
        if (scale <= 0f) return
        val layer = layer()
        redrawing = true
        try {
            val pose = ctx.pose()
            pose.pushMatrix()
            try {
                pose.scale(scale, scale)
                layer.extractRenderStates(
                    ctx,
                    (window.guiScaledWidth / scale).toInt(),
                    (window.guiScaledHeight / scale).toInt(),
                    false,
                )
            } finally {
                pose.popMatrix()
            }
            visibleIds = layer.getRendered()
                .filter { it.widget.shouldRender() }
                .mapTo(HashSet()) { it.widget.internalID }
        } catch (t: Throwable) {
            LOGGER.debug("Failed to render Skyblocker HUD widgets above the blur", t)
        } finally {
            redrawing = false
        }
    }

    internal fun markDirty() {
        dirty = true
    }

    internal fun flush() {
        markDirty()
        save()
    }

    private val deferred = LinkedHashSet<SkyblockerWidgetWrapper>()

    internal fun defer(wrapper: SkyblockerWidgetWrapper) {
        deferred.add(wrapper)
    }

    private fun applyDeferred() {
        if (deferred.isEmpty()) return
        deferred.removeAll { it.applyDeferredPosition() }
        save()
    }

    private fun save() {
        if (!dirty) return
        dirty = false
        runCatching { WidgetManager.saveConfig() }
            .onFailure { LOGGER.warn("Failed to save Skyblocker HUD widget positions", it) }
    }

    internal fun layer(): LayerBuilder =
        WidgetManager.SCREEN_BUILDER.get(WidgetManager.ScreenLayer.HUD)

    internal fun hudWidgets(): MutableMap<String, WidgetConfig> =
        WidgetManager.getScreenConfig(Utils.getLocation()).hud().widgets()

    internal fun positioned(widget: HudWidget): PositionedWidget? =
        runCatching { layer().getRendered().firstOrNull { it.widget.internalID == widget.internalID } }.getOrNull()

    internal fun tabHudScale(): Float =
        runCatching { SkyblockerConfigManager.get().uiAndVisuals.tabHud.tabHudScale / 100f }.getOrDefault(1f)

    internal fun isOnHud(widget: HudWidget): Boolean = widget.internalID in visibleIds

    internal fun sanitize(internalID: String): String =
        internalID.lowercase().replace(Regex("[^a-z0-9_]"), "_")

    internal fun displayName(widget: HudWidget): String =
        runCatching { widget.information.displayName.string }.getOrNull()?.takeIf { it.isNotBlank() } ?: widget.internalID

    internal fun isEnabledHere(widget: HudWidget): Boolean {
        if (!Utils.isOnSkyblock()) return false
        return runCatching { hudWidgets()[widget.internalID]?.config()?.isPresent == true }.getOrDefault(false)
    }

    internal fun setEnabledHere(widget: HudWidget, value: Boolean) {
        if (!Utils.isOnSkyblock()) return
        if (!editableHere(widget)) return
        runCatching {
            val widgets = hudWidgets()
            val id = widget.internalID
            if (value) {
                if (widgets[id]?.config()?.isPresent == true) return@runCatching
                val rule = widgets[id]?.position()?.orElse(null) ?: PositionRule.DEFAULT
                widgets[id] = WidgetConfig(JsonObject(), rule)
            } else if (widgets.remove(id) == null) {
                return@runCatching
            }
            layer().update()
            markDirty()
        }.onFailure { LOGGER.warn("Failed to toggle Skyblocker HUD widget '{}'", widget.internalID, it) }
    }

    internal fun editableHere(widget: HudWidget): Boolean =
        runCatching { widget.information.available().test(Utils.getLocation()) }.getOrDefault(false)

    internal fun persistRule(widget: HudWidget, rule: PositionRule) {
        runCatching {
            val widgets = hudWidgets()
            val id = widget.internalID
            val existing = widgets[id]
            widgets[id] = existing?.withPosition(rule) ?: WidgetConfig(JsonObject(), rule)
            markDirty()
        }.onFailure { LOGGER.warn("Failed to store the position of Skyblocker HUD widget '{}'", widget.internalID, it) }
    }

    internal fun reflow() {
        val window = Minecraft.getInstance().window
        val scale = tabHudScale()
        if (scale <= 0f) return
        runCatching {
            layer().updatePositions(
                (window.guiScaledWidth / scale).toInt(),
                (window.guiScaledHeight / scale).toInt(),
            )
        }
    }

    internal fun buildSettings(widget: HudWidget): List<Property<*>> {
        val prop = Properties.functional<Boolean>(
            getter = { isEnabledHere(widget) },
            setter = { value -> setEnabledHere(widget, value) },
            id = "skyblocker_widget_${sanitize(widget.internalID)}_enabled",
            name = "Enabled Here",
            description = "Whether Skyblocker shows this widget in the location you are currently in.",
        ).apply {
            visualizer = Visualizer.SwitchVisualizer::class.java
            addMetadata("subcategory", "Settings")
        }
        return listOf(prop)
    }
}

internal class SkyblockerWidgetWrapper(private val widget: HudWidget) : OneConfigHudWrapper {
    override var id: String = "skyblocker_widget_${SkyblockerWidgetCompat.sanitize(widget.internalID)}"

    override var name: String = SkyblockerWidgetCompat.displayName(widget)

    override val modId: String = "skyblocker"

    private var deferredX: Float? = null
    private var deferredY: Float? = null

    override val placementReady: Boolean
        get() = runCatching { Utils.isOnSkyblock() && SkyblockerWidgetCompat.positioned(widget) != null }.getOrDefault(false)

    override val ownsPlacement: Boolean get() = true

    override var x: Float
        get() = deferredX ?: (widget.x * SkyblockerWidgetCompat.tabHudScale())
        set(value) = place(value, null)

    override var y: Float
        get() = deferredY ?: (widget.y * SkyblockerWidgetCompat.tabHudScale())
        set(value) = place(null, value)

    private fun place(targetX: Float?, targetY: Float?) {
        if (move(targetX, targetY)) {
            if (targetX != null) deferredX = null
            if (targetY != null) deferredY = null
            return
        }
        if (targetX != null) deferredX = targetX
        if (targetY != null) deferredY = targetY
        SkyblockerWidgetCompat.defer(this)
    }

    internal fun applyDeferredPosition(): Boolean {
        val pendingX = deferredX
        val pendingY = deferredY
        if (pendingX == null && pendingY == null) return true
        if (!move(pendingX, pendingY)) return false
        deferredX = null
        deferredY = null
        return true
    }

    override var scale: Float
        get() = 1f
        set(_) {}

    override val supportsScale: Boolean get() = false

    override var hidden: Boolean
        get() = if (!SkyblockerWidgetCompat.editableHere(widget)) false
        else !SkyblockerWidgetCompat.isEnabledHere(widget)
        set(value) = SkyblockerWidgetCompat.setEnabledHere(widget, !value)

    override var scaledWidth: Float
        get() = if (!SkyblockerWidgetCompat.isOnHud(widget)) 0f else widget.width * SkyblockerWidgetCompat.tabHudScale()
        set(_) {}

    override var scaledHeight: Float
        get() = if (!SkyblockerWidgetCompat.isOnHud(widget)) 0f else widget.height * SkyblockerWidgetCompat.tabHudScale()
        set(_) {}

    override fun linkedProperties(): List<Property<*>> = SkyblockerWidgetCompat.buildSettings(widget)

    override fun save() = SkyblockerWidgetCompat.flush()

    private fun move(targetX: Float?, targetY: Float?): Boolean {
        if (!Utils.isOnSkyblock()) return false
        val scale = SkyblockerWidgetCompat.tabHudScale()
        if (scale <= 0f) return false
        val positioned = SkyblockerWidgetCompat.positioned(widget) ?: return false

        val newX = targetX?.let { (it / scale).roundToInt() } ?: widget.x
        val newY = targetY?.let { (it / scale).roundToInt() } ?: widget.y

        val rule = positioned.rule
        val updated = PositionRule(
            rule.parent(),
            rule.parentPoint(),
            rule.thisPoint(),
            rule.relativeX() + (newX - widget.x),
            rule.relativeY() + (newY - widget.y),
        )

        positioned.rule = updated
        SkyblockerWidgetCompat.persistRule(widget, updated)

        widget.x = newX
        widget.y = newY
        SkyblockerWidgetCompat.reflow()
        return true
    }
}
//? }
