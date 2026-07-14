//? skyblocker_compat {
package org.polyfrost.oneconfig.internal.compat

import de.hysky.skyblocker.skyblock.fancybars.FancyStatusBars
import de.hysky.skyblocker.skyblock.fancybars.StatusBar
import de.hysky.skyblocker.skyblock.fancybars.StatusBarType
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
import java.awt.Color
import java.util.function.Consumer

object SkyblockerCompat {
    private val LOGGER = LogManager.getLogger("OneConfig/Skyblocker-Compat")

    private const val BAR_MIN_WIDTH = 30f

    private var initialized = false
    private var dirty = false

    @Volatile
    private var redrawing = false

    @JvmStatic
    fun isRedrawing(): Boolean = redrawing

    @JvmStatic
    fun initialize() {
        if (initialized) return
        initialized = true
        CompatLoader.requireTranslations(skip = true) { register() }
    }

    private fun register() {
        var count = 0
        for (type in StatusBarType.values()) {
            runCatching {
                SkyblockerBarWrapper(type).register()
                count++
            }.onFailure { LOGGER.warn("Failed to register Skyblocker status bar '{}'", type.name, it) }
        }
        LOGGER.info("Registered {} Skyblocker status bars into the OneConfig HUD editor", count)

        EventManager.register(HudEditorToggleEvent::class.java, Consumer { event ->
            if (!event.open) save()
        })

        CompatOverlayRenderer.register(::renderBars)
    }

    private fun renderBars(ctx: GuiGraphicsExtractor) {
        val mc = Minecraft.getInstance()
        redrawing = true
        try {
            runCatching {
                //~ if >= 26.1 'render' -> 'extractRenderState'
                FancyStatusBars.extractRenderState(ctx, mc)
            }.onFailure { LOGGER.debug("Failed to render Skyblocker status bars above the blur", it) }
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
        runCatching { FancyStatusBars.saveBarConfig() }
            .onFailure { LOGGER.warn("Failed to save Skyblocker status bar config", it) }
    }

    internal fun buildSettings(type: StatusBarType): List<Property<*>> {
        val out = ArrayList<Property<*>>()
        val id = { suffix: String -> "skyblocker_bar_${type.name.lowercase()}_$suffix" }

        out.add(
            functional(
                id("enabled"), "Enabled",
                get = { bar(type)?.enabled ?: false },
                set = { value ->
                    val bar = bar(type) ?: return@functional
                    if (value) {
                        bar.enabled = true
                        if (bar.anchor != null) runCatching { FancyStatusBars.placeBarsInPositioner() }
                    } else {
                        detach(bar)
                        bar.enabled = false
                    }
                    reflow()
                },
            ).apply { visualizer = Visualizer.SwitchVisualizer::class.java }
        )

        out.add(
            functional(
                id("icon_position"), "Icon Position",
                get = { bar(type)?.iconPosition ?: StatusBar.IconPosition.LEFT },
                set = { value -> bar(type)?.iconPosition = value },
                type = StatusBar.IconPosition::class.java,
            ).apply {
                visualizer = Visualizer.DropdownVisualizer::class.java
                addMetadata("optionLabels", StatusBar.IconPosition.values().map { it.toString() })
            }
        )

        out.add(
            functional(
                id("text_position"), "Text Position",
                get = { bar(type)?.textPosition ?: StatusBar.TextPosition.BAR_CENTER },
                set = { value -> bar(type)?.textPosition = value },
                type = StatusBar.TextPosition::class.java,
            ).apply {
                visualizer = Visualizer.DropdownVisualizer::class.java
                addMetadata("optionLabels", StatusBar.TextPosition.values().map { it.toString() })
            }
        )

        if (type.hasMax()) {
            out.add(
                functional(
                    id("show_max"), "Show Max",
                    get = { bar(type)?.showMax ?: false },
                    set = { value -> bar(type)?.showMax = value },
                ).apply { visualizer = Visualizer.SwitchVisualizer::class.java }
            )
        }

        if (type.hasOverflow()) {
            out.add(
                functional(
                    id("show_overflow"), "Show Overflow",
                    get = { bar(type)?.showOverflow ?: false },
                    set = { value -> bar(type)?.showOverflow = value },
                ).apply { visualizer = Visualizer.SwitchVisualizer::class.java }
            )
        }

        out.add(colorProperty(type, id("main_color"), "Main Color", 0))
        if (type.hasOverflow()) {
            out.add(colorProperty(type, id("overflow_color"), "Overflow Color", 1))
        }

        out.add(
            functional(
                id("text_color"), "Text Color",
                get = { bar(type)?.let { it.textColor ?: it.colors.getOrNull(0) } ?: Color.WHITE },
                set = { value -> bar(type)?.textColor = value },
                type = Color::class.java,
            ).apply {
                visualizer = Visualizer.ColorVisualizer::class.java
                addMetadata("noAlpha", Unit)
            }
        )

        out.add(
            functional(
                id("width"), "Width",
                get = { bar(type)?.width?.times(guiScaledWidth()) ?: 0f },
                set = { value ->
                    val bar = bar(type) ?: return@functional
                    val screen = guiScaledWidth()
                    if (screen <= 0f) return@functional
                    detach(bar)
                    bar.width = (value / screen).coerceIn(BAR_MIN_WIDTH / screen, 1f)
                    reflow()
                },
                type = Float::class.javaPrimitiveType,
            ).apply {
                visualizer = Visualizer.SliderVisualizer::class.java
                addMetadata("min", BAR_MIN_WIDTH)
                addMetadata("max", 240f)
                addMetadata("step", 1f)
            }
        )

        for (prop in out) prop.addMetadata("subcategory", "Settings")
        return out
    }

    private fun colorProperty(type: StatusBarType, id: String, name: String, index: Int): Property<*> =
        functional(
            id, name,
            get = { bar(type)?.colors?.getOrNull(index) ?: Color.WHITE },
            set = { value -> bar(type)?.colors?.let { if (index < it.size) it[index] = value } },
            type = Color::class.java,
        ).apply {
            visualizer = Visualizer.ColorVisualizer::class.java
            addMetadata("noAlpha", Unit)
        }

    private fun <T> functional(
        id: String,
        name: String,
        get: () -> T,
        set: (T) -> Unit,
        type: Class<T>? = null,
    ): Property<T> = Properties.functional(
        getter = { get() },
        setter = { value -> set(value); markDirty() },
        id = id,
        name = name,
        type = type,
    )

    internal fun bar(type: StatusBarType): StatusBar? = FancyStatusBars.statusBars[type]

    internal fun guiScaledWidth(): Float =
        Minecraft.getInstance().window?.guiScaledWidth?.toFloat() ?: 0f

    internal fun guiScaledHeight(): Float =
        Minecraft.getInstance().window?.guiScaledHeight?.toFloat() ?: 0f

    internal fun detach(bar: StatusBar) {
        val anchor = bar.anchor ?: return
        runCatching { FancyStatusBars.barPositioner.removeBar(anchor, bar.gridY, bar) }
        bar.anchor = null
        val screenW = guiScaledWidth()
        val screenH = guiScaledHeight()
        if (screenW > 0f && screenH > 0f) {
            bar.x = bar.getX() / screenW
            bar.y = bar.getY() / screenH
            bar.width = (bar.getWidth() / screenW).coerceAtLeast(BAR_MIN_WIDTH / screenW)
        }
    }

    internal fun reflow() {
        markDirty()
        runCatching { FancyStatusBars.updatePositions(true) }
    }
}

private class SkyblockerBarWrapper(private val type: StatusBarType) : OneConfigHudWrapper {
    private val bar: StatusBar? get() = SkyblockerCompat.bar(type)

    override var id: String = "skyblocker_bar_${type.name.lowercase()}"

    override var name: String = type.getName().string

    override var x: Float
        get() = bar?.getX()?.toFloat() ?: 0f
        set(value) {
            val bar = bar ?: return
            val screen = SkyblockerCompat.guiScaledWidth()
            if (screen <= 0f) return
            SkyblockerCompat.detach(bar)
            bar.x = value / screen
            SkyblockerCompat.reflow()
        }

    override var y: Float
        get() = bar?.getY()?.toFloat() ?: 0f
        set(value) {
            val bar = bar ?: return
            val screen = SkyblockerCompat.guiScaledHeight()
            if (screen <= 0f) return
            SkyblockerCompat.detach(bar)
            bar.y = value / screen
            SkyblockerCompat.reflow()
        }

    override var scale: Float
        get() = 1f
        set(_) {}

    override val supportsScale: Boolean get() = false

    override var scaledWidth: Float
        get() = bar?.takeIf { it.enabled }?.getWidth()?.toFloat() ?: 0f
        set(_) {}

    override var scaledHeight: Float
        get() = bar?.takeIf { it.enabled }?.getHeight()?.toFloat() ?: 0f
        set(_) {}

    override fun linkedProperties(): List<Property<*>> = SkyblockerCompat.buildSettings(type)
}
//? }
