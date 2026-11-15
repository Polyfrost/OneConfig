@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.runtime.snapshots.Snapshot
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.copy
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public data class HudStyle(
    public val font: Font = Font.Minecraft,
    public val textColor: PolyColor = PolyColor(0xFFFFFFFF.toInt()),
    public val shadow: Boolean = false,
    public val shadowColor: PolyColor = PolyColor(0x40000000),
    public val background: Boolean = true,
    public val backgroundColor: PolyColor = PolyColor(0x80000000.toInt()),
    public val border: Boolean = false,
    public val borderColor: PolyColor = PolyColor(0xFF000000.toInt()),
    public val borderSize: Float = 1f,
    public val radius: Float = 4f,
    public val padding: Float = 4f,
    public val scale: Float = 1f,
    public val textScale: Float = 1f,
    public val textWeight: Weight = Weight.Regular,
    public val caseType: Int = 0,
    public val textBold: Boolean = false,
    public val textItalic: Boolean = false,
    public val textUnderline: Boolean = false,
    public val shadowOffsetX: Float = 2f,
    public val shadowOffsetY: Float = 2f,
    public val paddingTop: Float? = null,
    public val paddingBottom: Float? = null,
    public val paddingLeft: Float? = null,
    public val paddingRight: Float? = null,
    public val useGuiScale: Boolean = true,
) {
    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(
        font: Font = Font.Minecraft,
        textColor: PolyColor = PolyColor(0xFFFFFFFF.toInt()),
        shadow: Boolean = false,
        shadowColor: PolyColor = PolyColor(0x40000000),
        background: Boolean = true,
        backgroundColor: PolyColor = PolyColor(0x80000000.toInt()),
        border: Boolean = false,
        borderColor: PolyColor = PolyColor(0xFF000000.toInt()),
        borderSize: Float = 1f,
        radius: Float = 4f,
        padding: Float = 4f,
        scale: Float = 1f,
    ) : this(
        font = font,
        textColor = textColor,
        shadow = shadow,
        shadowColor = shadowColor,
        background = background,
        backgroundColor = backgroundColor,
        border = border,
        borderColor = borderColor,
        borderSize = borderSize,
        radius = radius,
        padding = padding,
        scale = scale,
        textScale = 1f,
    )

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public fun copy(
        font: Font = this.font,
        textColor: PolyColor = this.textColor,
        shadow: Boolean = this.shadow,
        shadowColor: PolyColor = this.shadowColor,
        background: Boolean = this.background,
        backgroundColor: PolyColor = this.backgroundColor,
        border: Boolean = this.border,
        borderColor: PolyColor = this.borderColor,
        borderSize: Float = this.borderSize,
        radius: Float = this.radius,
        padding: Float = this.padding,
        scale: Float = this.scale,
    ): HudStyle = copy(
        font = font,
        textColor = textColor,
        shadow = shadow,
        shadowColor = shadowColor,
        background = background,
        backgroundColor = backgroundColor,
        border = border,
        borderColor = borderColor,
        borderSize = borderSize,
        radius = radius,
        padding = padding,
        scale = scale,
        textScale = textScale,
    )
}

@ExperimentalOneConfigUiApi
public object HudStyles {
    private val defaults = ConcurrentHashMap<String, HudStyle>()
    private val builtInStyles = ConcurrentHashMap<Class<out Hud>, HudStyle>()

    @JvmStatic
    public val builtIn: HudStyle get() = HudStyle()

    @JvmStatic
    public fun getDefault(modId: String): HudStyle = defaults[modId]?.deepCopy() ?: HudStyle()

    @JvmStatic
    public fun setDefault(modId: String, style: HudStyle) {
        val next = style.deepCopy()
        val previous = defaults.put(modId, next)
        if (previous == next) return
        Snapshot.withMutableSnapshot { forEachHud(modId) { applyStyle(it, previous, next) } }
    }

    @JvmStatic
    public fun applyToAll(modId: String, block: (Hud) -> Unit) {
        Snapshot.withMutableSnapshot { forEachHud(modId, block) }
    }

    internal fun applyDefaultTo(hud: Hud) {
        val modId = hud.configId ?: return
        val style = defaults[modId] ?: return
        applyStyle(hud, null, style)
    }

    internal fun captureBuiltIn(hud: Hud) {
        if (builtInStyles.containsKey(hud.javaClass)) return
        builtInStyles.putIfAbsent(hud.javaClass, styleOf(hud))
    }

    private fun styleOf(hud: Hud): HudStyle = HudStyle(
        font = hud.font,
        textColor = hud.textPolyColor.copy(),
        shadow = hud.showShadow,
        shadowColor = hud.shadowPolyColor.copy(),
        background = hud.showBackground,
        backgroundColor = hud.bgPolyColor.copy(),
        border = hud.borderEnabled,
        borderColor = hud.borderPolyColor.copy(),
        borderSize = hud.borderSize,
        radius = hud.bgRadius,
        scale = hud.customScale,
        textScale = hud.textScale,
        textWeight = hud.textWeight,
        caseType = hud.caseType,
        textBold = hud.textBold,
        textItalic = hud.textItalic,
        textUnderline = hud.textUnderline,
        shadowOffsetX = hud.shadowOffsetX,
        shadowOffsetY = hud.shadowOffsetY,
        paddingTop = hud.padTop,
        paddingBottom = hud.padBottom,
        paddingLeft = hud.padLeft,
        paddingRight = hud.padRight,
        useGuiScale = hud.useGuiScale,
    )

    private inline fun forEachHud(modId: String, block: (Hud) -> Unit) {
        val seen = Collections.newSetFromMap(IdentityHashMap<Hud, Boolean>())
        for (hud in HudManager.providers().toList()) if (hud.configId == modId && seen.add(hud)) block(hud)
        for (hud in HudManager.activeInstances.toList()) if (hud.configId == modId && seen.add(hud)) block(hud)
    }

    private fun applyStyle(hud: Hud, previous: HudStyle?, to: HudStyle) {
        val from = previous ?: builtInStyles[hud.javaClass] ?: HudStyle()
        fun <T> kept(current: T, pick: (HudStyle) -> T): Boolean = current == pick(from)
        fun keptColor(current: PolyColor, pick: (HudStyle) -> PolyColor): Boolean = current.sameAs(pick(from))
        if (kept(hud.font) { it.font }) hud.font = to.font
        if (keptColor(hud.textPolyColor) { it.textColor }) hud.textPolyColor = to.textColor
        if (kept(hud.showShadow) { it.shadow }) hud.showShadow = to.shadow
        if (keptColor(hud.shadowPolyColor) { it.shadowColor }) hud.shadowPolyColor = to.shadowColor
        if (hud.hasBackground()) {
            if (kept(hud.showBackground) { it.background }) hud.showBackground = to.background
            if (keptColor(hud.bgPolyColor) { it.backgroundColor }) hud.bgPolyColor = to.backgroundColor
            if (hud.hasBorder()) {
                if (kept(hud.borderEnabled) { it.border }) hud.borderEnabled = to.border
                if (keptColor(hud.borderPolyColor) { it.borderColor }) hud.borderPolyColor = to.borderColor
                if (kept(hud.borderSize) { it.borderSize }) hud.borderSize = to.borderSize
            }
            if (kept(hud.bgRadius) { it.radius }) hud.bgRadius = to.radius
        }
        if (kept(hud.padTop) { it.top }) hud.padTop = to.top
        if (kept(hud.padBottom) { it.bottom }) hud.padBottom = to.bottom
        if (kept(hud.padLeft) { it.left }) hud.padLeft = to.left
        if (kept(hud.padRight) { it.right }) hud.padRight = to.right
        if (kept(hud.customScale) { it.scale }) hud.customScale = to.scale
        if (kept(hud.textScale) { it.textScale }) hud.textScale = to.textScale
        if (kept(hud.textWeight) { it.textWeight }) hud.textWeight = to.textWeight
        if (kept(hud.caseType) { it.caseType }) hud.caseType = to.caseType
        if (kept(hud.textBold) { it.textBold }) hud.textBold = to.textBold
        if (kept(hud.textItalic) { it.textItalic }) hud.textItalic = to.textItalic
        if (kept(hud.textUnderline) { it.textUnderline }) hud.textUnderline = to.textUnderline
        if (kept(hud.shadowOffsetX) { it.shadowOffsetX }) hud.shadowOffsetX = to.shadowOffsetX
        if (kept(hud.shadowOffsetY) { it.shadowOffsetY }) hud.shadowOffsetY = to.shadowOffsetY
        if (kept(hud.useGuiScale) { it.useGuiScale } && hud.useGuiScale != to.useGuiScale) {
            if (!to.useGuiScale) hud.fixedGuiScale = Platform.compatibility().options().guiScale.takeIf { it > 0f } ?: 1f
            hud.useGuiScale = to.useGuiScale
        }
    }

    private val HudStyle.top: Float get() = paddingTop ?: padding
    private val HudStyle.bottom: Float get() = paddingBottom ?: padding
    private val HudStyle.left: Float get() = paddingLeft ?: padding
    private val HudStyle.right: Float get() = paddingRight ?: padding

    private fun PolyColor.sameAs(other: PolyColor): Boolean =
        rawArgb == other.rawArgb && chroma == other.chroma && chromaSpeed == other.chromaSpeed

    private fun HudStyle.deepCopy(): HudStyle = copy(
        textColor = textColor.copy(),
        shadowColor = shadowColor.copy(),
        backgroundColor = backgroundColor.copy(),
        borderColor = borderColor.copy(),
    )
}
