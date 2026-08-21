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
 * Wraps Skyblocker's [WidgetManager] tab/HUD widgets into the OneConfig HUD editor
 *
 * Separate from [SkyblockerCompat] which handles the unrelated [de.hysky.skyblocker.skyblock.fancybars]
 * status bars
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
        // resource reloads re-run the deferred init but the HUDs only need registering once
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
     * Widgets that can live on the HUD layer either declare locations of their own or are already pinned
     * to the HUD by some location
     *
     * That pinning is how tab-sourced widgets like commissions and powders end up on the Dwarven Mines HUD
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
        applyDeferred()
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
        // tab-sourced widgets have no per-location enable switch of their own
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

internal class SkyblockerWidgetWrapper(private val widget: HudWidget) : OneConfigHudWrapper {
    override var id: String = "skyblocker_widget_${SkyblockerWidgetCompat.sanitize(widget.internalID)}"

    override var name: String = SkyblockerWidgetCompat.displayName(widget)

    override val modId: String = "skyblocker"

    private var deferredX: Float? = null
    private var deferredY: Float? = null

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

    // Skyblocker scales every widget together via its own 'Tab HUD Scale' option so nothing per-widget exists to expose
    override var scale: Float
        get() = 1f
        set(_) {}

    override val supportsScale: Boolean get() = false

    override var hidden: Boolean
        get() = if (widget.availableLocations().isEmpty()) false
        else runCatching { !widget.isEnabledIn(Utils.getLocation()) }.getOrDefault(false)
        set(value) {
            if (widget.availableLocations().isEmpty()) return
            runCatching { widget.setEnabledIn(Utils.getLocation(), !value) }
        }

    override var scaledWidth: Float
        get() = if (!SkyblockerWidgetCompat.isOnHud(widget)) 0f else widget.width * SkyblockerWidgetCompat.tabHudScale()
        set(_) {}

    override var scaledHeight: Float
        get() = if (!SkyblockerWidgetCompat.isOnHud(widget)) 0f else widget.height * SkyblockerWidgetCompat.tabHudScale()
        set(_) {}

    override fun linkedProperties(): List<Property<*>> = SkyblockerWidgetCompat.buildSettings(widget)

    override fun save() = SkyblockerWidgetCompat.flush()

    /**
     * Rewrites the widget's [PositionRule] to the requested screen position keeping its existing
     * parent/anchor/layer so moving a parent still drags its children along like Skyblocker's own editor
     */
    private fun move(targetX: Float?, targetY: Float?): Boolean {
        // position rules are per-location so writing before the player is on Skyblock would pin the widget
        // in Location.UNKNOWN
        // Skyblocker owns these positions so only mirror edits back once a real location is known
        if (!Utils.isOnSkyblock()) return false
        val scale = SkyblockerWidgetCompat.tabHudScale()
        if (scale <= 0f) return false
        val builder = SkyblockerWidgetCompat.builder() ?: return false
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
            // the live position is the result of applying the current rule so shifting the rule offset by
            // the requested delta works for every parent/anchor combination without re-deriving them
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

        // applying the rule above lands the widget exactly here so move it now rather than waiting a frame
        // which keeps the getters and hence the delta above exact for the rest of this frame's edits
        widget.x = newX
        widget.y = newY
        return true
    }
}
//? }
