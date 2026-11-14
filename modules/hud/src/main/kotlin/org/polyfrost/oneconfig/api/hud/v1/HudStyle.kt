@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.runtime.snapshots.Snapshot
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.copy
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
)

@ExperimentalOneConfigUiApi
public object HudStyles {
    private val defaults = ConcurrentHashMap<String, HudStyle>()
    private val builtInPadding = ConcurrentHashMap<Class<out Hud>, FloatArray>()

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
        builtInPadding.putIfAbsent(hud.javaClass, floatArrayOf(hud.padTop, hud.padBottom, hud.padLeft, hud.padRight))
    }

    private inline fun forEachHud(modId: String, block: (Hud) -> Unit) {
        val seen = Collections.newSetFromMap(IdentityHashMap<Hud, Boolean>())
        for (hud in HudManager.providers().toList()) if (hud.configId == modId && seen.add(hud)) block(hud)
        for (hud in HudManager.activeInstances.toList()) if (hud.configId == modId && seen.add(hud)) block(hud)
    }

    private fun applyStyle(hud: Hud, previous: HudStyle?, to: HudStyle) {
        val from = previous ?: HudStyle()
        val pad = if (previous == null) builtInPadding[hud.javaClass] else null
        if (hud.font == from.font) hud.font = to.font
        if (hud.textPolyColor.sameAs(from.textColor)) hud.textPolyColor = to.textColor
        if (hud.showShadow == from.shadow) hud.showShadow = to.shadow
        if (hud.shadowPolyColor.sameAs(from.shadowColor)) hud.shadowPolyColor = to.shadowColor
        if (hud.hasBackground()) {
            if (hud.showBackground == from.background) hud.showBackground = to.background
            if (hud.bgPolyColor.sameAs(from.backgroundColor)) hud.bgPolyColor = to.backgroundColor
            if (hud.borderEnabled == from.border) hud.borderEnabled = to.border
            if (hud.borderPolyColor.sameAs(from.borderColor)) hud.borderPolyColor = to.borderColor
            if (hud.borderSize == from.borderSize) hud.borderSize = to.borderSize
            if (hud.bgRadius == from.radius) hud.bgRadius = to.radius
        }
        if (hud.padTop == (pad?.get(0) ?: from.padding)) hud.padTop = to.padding
        if (hud.padBottom == (pad?.get(1) ?: from.padding)) hud.padBottom = to.padding
        if (hud.padLeft == (pad?.get(2) ?: from.padding)) hud.padLeft = to.padding
        if (hud.padRight == (pad?.get(3) ?: from.padding)) hud.padRight = to.padding
        if (hud.customScale == from.scale) hud.customScale = to.scale
    }

    private fun PolyColor.sameAs(other: PolyColor): Boolean =
        rawArgb == other.rawArgb && chroma == other.chroma && chromaSpeed == other.chromaSpeed

    private fun HudStyle.deepCopy(): HudStyle = copy(
        textColor = textColor.copy(),
        shadowColor = shadowColor.copy(),
        backgroundColor = backgroundColor.copy(),
        borderColor = borderColor.copy(),
    )
}
