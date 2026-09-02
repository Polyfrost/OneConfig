//? skyblocker_compat {
package org.polyfrost.oneconfig.internal.compat

import de.hysky.skyblocker.skyblock.fancybars.BarPositioner
import de.hysky.skyblocker.skyblock.fancybars.FancyStatusBars
import de.hysky.skyblocker.skyblock.fancybars.StatusBar
import de.hysky.skyblocker.skyblock.fancybars.StatusBarType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.visualizer
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.hud.v1.HudResize
import org.polyfrost.oneconfig.api.hud.v1.OneConfigHudWrapper
import org.polyfrost.oneconfig.api.hud.v1.events.HudEditorToggleEvent
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer
import java.awt.Color
import java.util.function.Consumer
import kotlin.math.abs

object SkyblockerCompat {
    private val LOGGER = LogManager.getLogger("OneConfig/Skyblocker-Compat")

    private const val BAR_MIN_WIDTH = 30f

    private const val SNAP_SLACK = 4f

    private const val ANCHOR_HINT_COLOR = 0x66FFFFFF

    private var initialized = false
    private var dirty = false

    @Volatile
    private var redrawing = false

    @Volatile
    private var dragged: StatusBar? = null

    //? if skyblocker_hud_v2 {
    private fun statusBars(): Map<StatusBarType, StatusBar> = FancyStatusBars.INSTANCE.statusBars

    private fun positioner(): BarPositioner = FancyStatusBars.INSTANCE.barPositioner

    private fun saveBars() = FancyStatusBars.INSTANCE.saveBarConfig()

    private fun placeBars() = FancyStatusBars.INSTANCE.placeBarsInPositioner()

    private fun updatePositions(ignoreVisibility: Boolean) = FancyStatusBars.INSTANCE.updatePositions(ignoreVisibility)

    private fun healthFancyBarEnabled(): Boolean = FancyStatusBars.INSTANCE.isHealthFancyBarEnabled()

    private fun renderStatusBars(ctx: GuiGraphicsExtractor, mc: Minecraft) {
        FancyStatusBars.INSTANCE.extractRenderState(ctx, mc)
    }
    //?} else {
    /*private fun statusBars(): Map<StatusBarType, StatusBar> = FancyStatusBars.statusBars

    private fun positioner(): BarPositioner = FancyStatusBars.barPositioner

    private fun saveBars() = FancyStatusBars.saveBarConfig()

    private fun placeBars() = FancyStatusBars.placeBarsInPositioner()

    private fun updatePositions(ignoreVisibility: Boolean) = FancyStatusBars.updatePositions(ignoreVisibility)

    private fun healthFancyBarEnabled(): Boolean = FancyStatusBars.isHealthFancyBarEnabled()

    private fun renderStatusBars(ctx: GuiGraphicsExtractor, mc: Minecraft) {
        //~ if >= 26.1 'render' -> 'extractRenderState'
        FancyStatusBars.extractRenderState(ctx, mc)
    }
    *///?}

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
            if (!event.open) {
                dragged?.let { endDrag(it) }
                save()
            }
        })

        CompatOverlayRenderer.register(::renderBars)
    }

    private fun renderBars(ctx: GuiGraphicsExtractor) {
        val mc = Minecraft.getInstance()
        redrawing = true
        try {
            runCatching {
                renderAnchorHints(ctx)
                renderStatusBars(ctx, mc)
            }.onFailure { LOGGER.debug("Failed to render Skyblocker status bars above the blur", it) }
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

    private fun save() {
        if (!dirty) return
        dirty = false
        runCatching { saveBars() }
            .onFailure { LOGGER.warn("Failed to save Skyblocker status bar config", it) }
    }

    internal fun buildSettings(type: StatusBarType): List<Property<*>> {
        val out = ArrayList<Property<*>>()
        val id = { suffix: String -> "skyblocker_bar_${type.name.lowercase()}_$suffix" }

        out.add(
            functional(
                id("enabled"), "Enabled",
                get = { bar(type)?.enabled ?: false },
                set = { value -> setEnabled(type, value) },
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
                get = { bar(type)?.getWidth()?.toFloat() ?: 0f },
                set = { value -> setWidth(type, value) },
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

    internal fun setWidth(type: StatusBarType, widthPx: Float) {
        val bar = bar(type) ?: return
        val anchor = bar.anchor
        if (anchor == null) {
            val screen = guiScaledWidth()
            if (screen <= 0f) return
            bar.width = (widthPx / screen).coerceIn(BAR_MIN_WIDTH / screen, 1f)
            reflow()
            return
        }

        val rule = anchor.sizeRule
        val widthPerSize =
            if (rule.isTargetSize) rule.totalWidth().toFloat() / rule.targetSize() else rule.widthPerSize().toFloat()
        if (widthPerSize <= 0f) return

        val desired = Math.round(widthPx / widthPerSize).coerceIn(rule.minSize(), rule.maxSize())
        if (desired == bar.size) return

        if (!rule.isTargetSize) {
            bar.size = desired
            reflow()
            return
        }

        val neighbour = rowNeighbour(bar, anchor) ?: return
        val delta = (desired - bar.size).coerceIn(
            maxOf(rule.minSize() - bar.size, neighbour.size - rule.maxSize()),
            minOf(rule.maxSize() - bar.size, neighbour.size - rule.minSize()),
        )
        if (delta == 0) return
        bar.size += delta
        neighbour.size -= delta
        reflow()
    }

    private fun rowNeighbour(bar: StatusBar, anchor: BarPositioner.BarAnchor): StatusBar? {
        val row = runCatching { positioner().getRow(anchor, bar.gridY) }.getOrNull() ?: return null
        return row.getOrNull(bar.gridX + 1) ?: row.getOrNull(bar.gridX - 1)
    }

    internal fun setEnabled(type: StatusBarType, value: Boolean) {
        val bar = bar(type) ?: return
        if (bar.enabled == value) return
        if (value) {
            bar.enabled = true
            if (bar.anchor != null) runCatching { placeBars() }
        } else {
            detach(bar)
            bar.enabled = false
        }
        reflow()
        markDirty()
    }

    internal fun bar(type: StatusBarType): StatusBar? = statusBars()[type]

    internal fun guiScaledWidth(): Float =
        Minecraft.getInstance().window?.guiScaledWidth?.toFloat() ?: 0f

    internal fun guiScaledHeight(): Float =
        Minecraft.getInstance().window?.guiScaledHeight?.toFloat() ?: 0f

    internal fun detach(bar: StatusBar) {
        val anchor = bar.anchor ?: return
        runCatching { positioner().removeBar(anchor, bar.gridY, bar) }
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
        runCatching { updatePositions(true) }
    }

    internal fun beginDrag(bar: StatusBar) {
        dragged = bar
        detach(bar)
        reflow()
    }

    internal fun endDrag(bar: StatusBar) {
        dragged = null
        if (bar.enabled) snapToPositioner(bar)
        reflow()
    }

    private fun snapToPositioner(bar: StatusBar) {
        val screenW = guiScaledWidth().toInt()
        val screenH = guiScaledHeight().toInt()
        if (screenW <= 0 || screenH <= 0) return

        val x = bar.getX().toFloat()
        val y = bar.getY().toFloat()
        val w = bar.getWidth().toFloat()
        val h = bar.getHeight().toFloat()
        val cx = x + w / 2f
        val cy = y + h / 2f

        val target = statusBars().values
            .filter { it !== bar && it.enabled && it.anchor != null }
            .filter {
                overlaps(
                    x, y, w, h,
                    it.getX() - SNAP_SLACK, it.getY() - SNAP_SLACK,
                    it.getWidth() + 2f * SNAP_SLACK, it.getHeight() + 2f * SNAP_SLACK,
                )
            }
            .minByOrNull { squaredDistance(cx, cy, it.getX() + it.getWidth() / 2f, it.getY() + it.getHeight() / 2f) }
        if (target != null) {
            insertNextTo(bar, target)
            return
        }

        for (hint in emptyAnchors(screenW, screenH)) {
            if (!overlaps(
                    x, y, w, h,
                    hint.x - SNAP_SLACK, hint.y - SNAP_SLACK,
                    hint.hitbox.width() + 2f * SNAP_SLACK, hint.hitbox.height() + 2f * SNAP_SLACK,
                )
            ) continue
            runCatching {
                positioner().addRow(hint.anchor)
                positioner().addBar(hint.anchor, 0, bar)
            }.onFailure { LOGGER.warn("Failed to anchor Skyblocker status bar to '{}'", hint.anchor.name, it) }
            return
        }
    }

    private fun insertNextTo(bar: StatusBar, target: StatusBar) {
        val anchor = target.anchor ?: return
        val tw = target.getWidth().toFloat()
        val th = target.getHeight().toFloat()
        if (tw <= 0f || th <= 0f) return

        val dx = (bar.getX() + bar.getWidth() / 2f - (target.getX() + tw / 2f)) / (tw / 2f)
        val dy = (bar.getY() + bar.getHeight() / 2f - (target.getY() + th / 2f)) / (th / 2f)

        runCatching {
            if (abs(dx) >= abs(dy)) {
                if (!rowHasRoom(anchor, target.gridY)) return
                val gridX = neighbourInsertX(anchor, target.gridX, right = dx > 0f)
                positioner().addBar(anchor, target.gridY, gridX, bar)
            } else {
                val gridY = neighbourInsertY(anchor, target.gridY, up = dy < 0f)
                positioner().addRow(anchor, gridY)
                positioner().addBar(anchor, gridY, bar)
            }
        }.onFailure { LOGGER.warn("Failed to snap Skyblocker status bar next to '{}'", target.name.string, it) }
    }

    private fun rowHasRoom(anchor: BarPositioner.BarAnchor, row: Int): Boolean {
        val rule = anchor.sizeRule
        if (!rule.isTargetSize) return true
        val halved = anchor == BarPositioner.BarAnchor.HOTBAR_TOP && !healthFancyBarEnabled()
        val target = if (halved) rule.targetSize() / 2 else rule.targetSize()
        val occupants = runCatching { positioner().getRow(anchor, row).size }.getOrDefault(0)
        val room = (occupants + 1) * rule.minSize() <= target
        if (!room) LOGGER.debug("Refusing to snap a Skyblocker status bar into a full '{}' row", anchor.name)
        return room
    }

    private fun neighbourInsertX(anchor: BarPositioner.BarAnchor, gridX: Int, right: Boolean): Int =
        if (right == anchor.isRight) gridX + 1 else gridX

    private fun neighbourInsertY(anchor: BarPositioner.BarAnchor, gridY: Int, up: Boolean): Int =
        if (up == anchor.isUp) gridY + 1 else gridY

    private data class AnchorHint(val anchor: BarPositioner.BarAnchor, val hitbox: ScreenRectangle, val x: Float, val y: Float)

    private fun emptyAnchors(screenW: Int, screenH: Int): List<AnchorHint> =
        BarPositioner.BarAnchor.allAnchors().mapNotNull { anchor ->
            runCatching {
                if (positioner().getRowCount(anchor) != 0) return@runCatching null
                val hitbox = anchor.getAnchorHitbox(anchor.getAnchorPosition(screenW, screenH))
                AnchorHint(anchor, hitbox, hitbox.position().x().toFloat(), hitbox.position().y().toFloat())
            }.getOrNull()
        }

    private fun overlaps(
        ax: Float, ay: Float, aw: Float, ah: Float,
        bx: Float, by: Float, bw: Float, bh: Float,
    ): Boolean = ax < bx + bw && bx < ax + aw && ay < by + bh && by < ay + ah

    private fun squaredDistance(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = ax - bx
        val dy = ay - by
        return dx * dx + dy * dy
    }

    private fun renderAnchorHints(ctx: GuiGraphicsExtractor) {
        if (dragged == null) return
        val screenW = guiScaledWidth().toInt()
        val screenH = guiScaledHeight().toInt()
        if (screenW <= 0 || screenH <= 0) return
        for (hint in emptyAnchors(screenW, screenH)) {
            val hx = hint.hitbox.position().x()
            val hy = hint.hitbox.position().y()
            ctx.fill(hx, hy, hx + hint.hitbox.width(), hy + hint.hitbox.height(), ANCHOR_HINT_COLOR)
        }
    }
}

private class SkyblockerBarWrapper(private val type: StatusBarType) : OneConfigHudWrapper {
    private val bar: StatusBar? get() = SkyblockerCompat.bar(type)

    override var id: String = "skyblocker_bar_${type.name.lowercase()}"

    override var name: String = type.getName().string

    override val modId: String = "skyblocker"

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

    override val placementReady: Boolean
        get() = runCatching { (bar?.getWidth() ?: 0) > 0 }.getOrDefault(false)

    override val resizeAxes: HudResize get() = HudResize.Width

    override var hidden: Boolean
        get() = bar?.enabled == false
        set(value) = SkyblockerCompat.setEnabled(type, !value)

    override var scaledWidth: Float
        get() = bar?.getWidth()?.toFloat() ?: 0f
        set(value) = SkyblockerCompat.setWidth(type, value)

    override var scaledHeight: Float
        get() = bar?.getHeight()?.toFloat() ?: 0f
        set(_) {}

    override fun onDragStart() {
        SkyblockerCompat.beginDrag(bar ?: return)
    }

    override fun onDragEnd() {
        SkyblockerCompat.endDrag(bar ?: return)
    }

    override fun linkedProperties(): List<Property<*>> = SkyblockerCompat.buildSettings(type)

    override fun save() = SkyblockerCompat.flush()
}
//? }
