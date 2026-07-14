//? skyblocker_compat {
package org.polyfrost.oneconfig.internal.compat

import de.hysky.skyblocker.config.SkyblockerConfigManager
import de.hysky.skyblocker.skyblock.tabhud.screenbuilder.ScreenBuilder
import de.hysky.skyblocker.skyblock.tabhud.screenbuilder.WidgetManager
import de.hysky.skyblocker.skyblock.tabhud.screenbuilder.pipeline.PositionRule
import de.hysky.skyblocker.skyblock.tabhud.widget.HudWidget
import de.hysky.skyblocker.utils.Location
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

/**
 * Wraps Skyblocker's [WidgetManager] HUD widgets (the tab/HUD widget system, e.g. the Dwarven Mines
 * commissions and powder widgets, the Crystal Hollows map, ...) into the OneConfig HUD editor.
 *
 * This is separate from [SkyblockerCompat], which handles the unrelated [de.hysky.skyblocker.skyblock.fancybars]
 * status bars.
 */
object SkyblockerWidgetCompat {
    private val LOGGER = LogManager.getLogger("OneConfig/Skyblocker-Widget-Compat")

    private const val LAYER_HUD = "HUD"

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
        // Resource reloads re-run the deferred init, but the HUDs only ever need registering once.
        if (registered) return
        registered = true
        val seen = HashSet<String>()
        var count = 0
        for (widget in WidgetManager.widgetInstances.values.sortedBy { it.internalID }) {
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

    /**
     * Widgets that can live on the HUD layer: either they declare locations of their own, or some location
     * already pins them to the HUD (this is how the tab-sourced widgets, e.g. commissions and powders, end
     * up on the Dwarven Mines HUD).
     */
    private fun editable(widget: HudWidget): Boolean {
        if (widget.availableLocations().isNotEmpty()) return true
        val id = widget.internalID
        return Location.values().any { location ->
            WidgetManager.getScreenBuilder(location)?.getPositionRule(id)?.screenLayer()?.name == LAYER_HUD
        }
    }

    private fun renderWidgets(ctx: GuiGraphicsExtractor) {
        if (!Utils.isOnSkyblock()) {
            visibleIds = emptySet()
            return
        }
        val window = Minecraft.getInstance().window ?: return
        val scale = tabHudScale()
        if (scale <= 0f) return
        val builder = builder() ?: return
        redrawing = true
        try {
            val pose = ctx.pose()
            pose.pushMatrix()
            try {
                pose.scale(scale, scale)
                builder.run(
                    ctx,
                    (window.guiScaledWidth / scale).toInt(),
                    (window.guiScaledHeight / scale).toInt(),
                    WidgetManager.ScreenLayer.HUD,
                )
            } finally {
                pose.popMatrix()
            }
            visibleIds = builder.getHudWidgets(WidgetManager.ScreenLayer.HUD)
                .filter { it.isVisible }
                .mapTo(HashSet()) { it.internalID }
        } catch (t: Throwable) {
            LOGGER.debug("Failed to render Skyblocker HUD widgets above the blur", t)
        } finally {
            redrawing = false
        }
    }

    internal fun markDirty() {
        dirty = true
    }

    private fun save() {
        if (!dirty) return
        dirty = false
        runCatching { WidgetManager.saveConfig() }
            .onFailure { LOGGER.warn("Failed to save Skyblocker HUD widget positions", it) }
    }

    internal fun builder(): ScreenBuilder? =
        runCatching { WidgetManager.getScreenBuilder(Utils.getLocation()) }.getOrNull()

    internal fun tabHudScale(): Float =
        runCatching { SkyblockerConfigManager.get().uiAndVisuals.tabHud.tabHudScale / 100f }.getOrDefault(1f)

    internal fun isOnHud(widget: HudWidget): Boolean = widget.internalID in visibleIds

    internal fun sanitize(internalID: String): String =
        internalID.lowercase().replace(Regex("[^a-z0-9_]"), "_")

    internal fun displayName(widget: HudWidget): String =
        runCatching { widget.displayName.string }.getOrNull()?.takeIf { it.isNotBlank() } ?: widget.internalID

    internal fun buildSettings(widget: HudWidget): List<Property<*>> {
        // Tab-sourced widgets have no per-location enable switch of their own.
        if (widget.availableLocations().isEmpty()) return emptyList()
        val prop = Properties.functional<Boolean>(
            getter = { runCatching { widget.isEnabledIn(Utils.getLocation()) }.getOrDefault(false) },
            setter = { value -> runCatching { widget.setEnabledIn(Utils.getLocation(), value) } },
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

private class SkyblockerWidgetWrapper(private val widget: HudWidget) : OneConfigHudWrapper {
    override var id: String = "skyblocker_widget_${SkyblockerWidgetCompat.sanitize(widget.internalID)}"

    override var name: String = SkyblockerWidgetCompat.displayName(widget)

    override var x: Float
        get() = widget.x * SkyblockerWidgetCompat.tabHudScale()
        set(value) = move(value, null)

    override var y: Float
        get() = widget.y * SkyblockerWidgetCompat.tabHudScale()
        set(value) = move(null, value)

    // Skyblocker scales every widget together via its own 'Tab HUD Scale' option, so there is nothing per-widget to expose.
    override var scale: Float
        get() = 1f
        set(_) {}

    override val supportsScale: Boolean get() = false

    override var scaledWidth: Float
        get() = if (!SkyblockerWidgetCompat.isOnHud(widget)) 0f else widget.width * SkyblockerWidgetCompat.tabHudScale()
        set(_) {}

    override var scaledHeight: Float
        get() = if (!SkyblockerWidgetCompat.isOnHud(widget)) 0f else widget.height * SkyblockerWidgetCompat.tabHudScale()
        set(_) {}

    override fun linkedProperties(): List<Property<*>> = SkyblockerWidgetCompat.buildSettings(widget)

    /**
     * Rewrites the widget's [PositionRule] so it lands on the requested screen position, keeping whatever
     * parent/anchor/layer it already had - moving a parent widget still drags its children along, exactly as
     * it does in Skyblocker's own editor.
     */
    private fun move(targetX: Float?, targetY: Float?) {
        // Position rules are per-location, so a write made before the player is on Skyblock (OneConfig
        // restoring its own saved copy of these HUDs at startup) would pin the widget in Location.UNKNOWN.
        // Skyblocker owns these positions; only mirror edits back once a real location is known.
        if (!Utils.isOnSkyblock()) return
        val scale = SkyblockerWidgetCompat.tabHudScale()
        if (scale <= 0f) return
        val builder = SkyblockerWidgetCompat.builder() ?: return
        val internalID = widget.internalID
        val current = builder.getPositionRule(internalID)

        val newX = targetX?.let { (it / scale).roundToInt() } ?: widget.x
        val newY = targetY?.let { (it / scale).roundToInt() } ?: widget.y

        val rule = if (current == null) {
            PositionRule(
                "screen",
                PositionRule.Point.DEFAULT,
                PositionRule.Point.DEFAULT,
                newX,
                newY,
                WidgetManager.ScreenLayer.HUD,
            )
        } else {
            // The widget's live position is the result of applying the current rule, so shifting the rule's
            // offset by the requested delta works for every parent/anchor combination without re-deriving them.
            PositionRule(
                current.parent(),
                current.parentPoint(),
                current.thisPoint(),
                current.relativeX() + (newX - widget.x),
                current.relativeY() + (newY - widget.y),
                current.screenLayer(),
            )
        }

        builder.setPositionRule(internalID, rule)
        SkyblockerWidgetCompat.markDirty()
        ScreenBuilder.markDirty()

        // Applying the rule above would land the widget exactly here, so move it now rather than waiting a
        // frame: it keeps the getters (and hence the delta above) exact for the rest of this frame's edits.
        widget.x = newX
        widget.y = newY
    }
}
//? }
