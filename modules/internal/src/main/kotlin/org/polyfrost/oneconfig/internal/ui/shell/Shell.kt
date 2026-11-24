package org.polyfrost.oneconfig.internal.ui.shell

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import org.polyfrost.compose.backend.PolyImageBitmap
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.DESIGN_HEIGHT_DP
import org.polyfrost.oneconfig.internal.ui.DESIGN_WIDTH_DP
import org.polyfrost.oneconfig.internal.ui.components.ContentSlide
import org.polyfrost.oneconfig.internal.ui.components.GlobalSearchBar
import org.polyfrost.oneconfig.internal.ui.components.Header
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.ReducedMotion
import org.polyfrost.oneconfig.internal.ui.components.Sidebar
import org.polyfrost.oneconfig.internal.ui.components.SidebarWidth
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.accentGradientFill
import org.polyfrost.oneconfig.internal.ui.components.drawLive
import org.polyfrost.oneconfig.internal.ui.components.liquidGlass
import org.polyfrost.oneconfig.internal.ui.components.rememberAccentBrandTint
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.rememberSvgResourcePainter
import org.polyfrost.oneconfig.internal.ui.components.splitFrostedGlass
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.PreferencesGraph
import org.polyfrost.oneconfig.internal.ui.navigation.navigation
import org.polyfrost.oneconfig.internal.ui.screens.SearchResultsScreen
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.DEFAULT_PAGE_OPACITY
import org.polyfrost.oneconfig.internal.ui.themes.DEFAULT_SIDEBAR_OPACITY
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.glassOpacity
import org.polyfrost.oneconfig.internal.ui.themes.surfaceOpacity
import org.polyfrost.oneconfig.internal.ui.util.cachedDropShadow
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private const val GLOW_RADIUS = 500f
private const val GLOW_EXTENT = 1400f

private val glowProfile = floatArrayOf(0f, .2f, .4f, .5f, .6f, .7f, .8f, 1f) to
    floatArrayOf(.95f, .77f, .42f, .25f, .13f, .055f, .018f, 0f)

/**
 * One accent glow, in pixels of the shell like [GLOW_EXTENT]
 *
 * It drifts [drift] around ([x], [y]) on two slow periods, breathes between full and [breath] less than full
 * [strength], and shifts against the pointer by [depth] so nearer glows move more
 */
private class Glow(
    val x: Float, val y: Float, val strength: Float,
    val drift: Float, val periodX: Float, val periodY: Float,
    val breath: Float, val breathPeriod: Float,
    val depth: Float, val phase: Float,
)

private val glows = arrayOf(
    Glow(147f, -199f, 1f, drift = 60f, periodX = 23f, periodY = 29f, breath = .18f, breathPeriod = 11f, depth = .04f, phase = 0f),
    Glow(893f, 736f, 1f, drift = 80f, periodX = 31f, periodY = 19f, breath = .18f, breathPeriod = 13f, depth = .07f, phase = 2.1f),
    Glow(1330f, 110f, .55f, drift = 110f, periodX = 17f, periodY = 26f, breath = .3f, breathPeriod = 9f, depth = .11f, phase = 4.2f),
)

/** How quickly the parallax catches up with the pointer, per second */
private const val PARALLAX_EASE = 4f

private const val TAU = 6.283185307179586

/** Animated glow centers and strengths, three floats per [glows] entry */
private class GlowMotion {
    /** Last pointer position inside the shell, written by [track] and read each frame */
    var pointer: Offset? = null

    private var parallax = Offset.Zero
    private var lastNanos = 0L
    private val out = FloatArray(glows.size * 3)

    val track = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull()?.let { pointer = it.position }
        }
    }

    fun frame(nanos: Long, size: Size): FloatArray {
        val dt = if (lastNanos == 0L) 0f else ((nanos - lastNanos) / 1e9f).coerceIn(0f, .1f)
        lastNanos = nanos
        val target = pointer?.let { it - Offset(size.width / 2f, size.height / 2f) } ?: Offset.Zero
        parallax += (target - parallax) * (1f - exp(-dt * PARALLAX_EASE))
        val t = nanos / 1e9
        for ((i, g) in glows.withIndex()) {
            out[i * 3] = g.x + g.drift * sin(t * TAU / g.periodX + g.phase).toFloat() - parallax.x * g.depth
            out[i * 3 + 1] = g.y + g.drift * cos(t * TAU / g.periodY + g.phase).toFloat() - parallax.y * g.depth
            out[i * 3 + 2] = g.strength * (1f - g.breath * (.5f + .5f * sin(t * TAU / g.breathPeriod + g.phase).toFloat()))
        }
        return out
    }
}

private const val GLOW_SPRITE = 512

/** One radial glow at full strength, drawn [GLOW_EXTENT] in radius wherever a glow currently sits */
private val glowSprite: PolyImageBitmap by lazy {
    val half = GLOW_SPRITE / 2f
    val px = ByteArray(GLOW_SPRITE * GLOW_SPRITE * 4)
    for (y in 0 until GLOW_SPRITE) for (x in 0 until GLOW_SPRITE) {
        val o = (y * GLOW_SPRITE + x) * 4
        px[o] = -1; px[o + 1] = -1; px[o + 2] = -1
        px[o + 3] = (glowAlphaAt(hypot(x + .5f - half, y + .5f - half) / half) * 255f).roundToInt().toByte()
    }
    PolyImageBitmap(PolyBitmap(GLOW_SPRITE, GLOW_SPRITE, px))
}

private fun glowAlphaAt(t: Float): Float {
    val (pos, a) = glowProfile
    if (t >= 1f) return 0f
    var i = 1
    while (pos[i] < t) i++
    return a[i - 1] + (a[i] - a[i - 1]) * (t - pos[i - 1]) / (pos[i] - pos[i - 1])
}

private fun erf(x: Float): Float {
    val z = abs(x)
    val t = 1f / (1f + 0.3275911f * z)
    val y = 1f - ((((1.0614054f * t - 1.4531521f) * t + 1.4214138f) * t - 0.28449672f) * t + 0.2548296f) * t * exp(-z * z)
    return if (x < 0f) -y else y
}

private data class ShadowBake(val l: Float, val t: Float, val r: Float, val b: Float, val sigma: Float, val color: Color) {
    private fun cover(x: Float, lo: Float, hi: Float): Float {
        val k = 0.70710677f / sigma
        return 0.5f * (erf((hi - x) * k) - erf((lo - x) * k))
    }

    fun alphaAt(x: Float, y: Float) = color.alpha * cover(x, l, r) * cover(y, t, b)
}

// one color with a baked alpha, so colors that animate (the accent, fill opacity) are applied when drawing instead of re-baking
private class BakedLayer {
    private var key: List<Any?>? = null
    private var bitmap: PolyImageBitmap? = null

    fun get(key: List<Any?>, w: Float, h: Float, x0: Float, x1: Float, color: Color, alphaAt: (x: Float, y: Float) -> Float): PolyImageBitmap {
        bitmap?.let { if (key == this.key) return it }
        val bw = ceil(GLOW_BITMAP * (x1 - x0) / w).toInt().coerceIn(2, GLOW_BITMAP)
        val bh = (GLOW_BITMAP * h / w).roundToInt().coerceIn(2, GLOW_BITMAP)
        val px = ByteArray(bw * bh * 4)
        val r = (color.red * 255f).roundToInt().toByte()
        val g = (color.green * 255f).roundToInt().toByte()
        val b = (color.blue * 255f).roundToInt().toByte()
        for (y in 0 until bh) for (x in 0 until bw) {
            val o = (y * bw + x) * 4
            px[o] = r; px[o + 1] = g; px[o + 2] = b
            px[o + 3] = (alphaAt(x0 + (x + .5f) * (x1 - x0) / bw, (y + .5f) * h / bh) * 255f).roundToInt().toByte()
        }
        return PolyImageBitmap(PolyBitmap(bw, bh, px)).also { bitmap = it; this.key = key }
    }
}

// shadow, accent glow and fill, bottom to top; each glow is the same full-strength sprite tinted when drawn, so
// stacking them composites the same as combining them in one bake while letting them move every frame
private class GlowLayers {
    private val shadowLayer = BakedLayer()

    fun DrawScope.draw(x0: Float, x1: Float, accent: Color, alpha: Float, fill: Color, shadow: ShadowBake?, centers: FloatArray) {
        val w = size.width
        val h = size.height
        val dstOffset = IntOffset(x0.roundToInt(), 0)
        val dstSize = IntSize((x1 - x0).roundToInt(), h.roundToInt())
        if (shadow != null) drawImage(shadowLayer.get(listOf(w, h, x0, x1, shadow), w, h, x0, x1, shadow.color, shadow::alphaAt), dstOffset = dstOffset, dstSize = dstSize)
        val spriteSize = IntSize((GLOW_EXTENT * 2f).roundToInt(), (GLOW_EXTENT * 2f).roundToInt())
        clipRect(x0, 0f, x1, h) {
            for (i in glows.indices) {
                val at = IntOffset((centers[i * 3] - GLOW_EXTENT).roundToInt(), (centers[i * 3 + 1] - GLOW_EXTENT).roundToInt())
                drawImage(glowSprite, dstOffset = at, dstSize = spriteSize, colorFilter = ColorFilter.tint(accent.copy(alpha = alpha * centers[i * 3 + 2])))
            }
        }
        drawRect(fill, Offset(x0, 0f), Size(x1 - x0, h))
    }
}

private const val GLOW_BITMAP = 256

internal val PageGutter = 25.dp

@Composable
fun Shell(
    windowWidth: Float,
    windowHeight: Float,
    backdrop: DrawScope.(Offset) -> Unit = {}
) {
    var windowOffset by remember { mutableStateOf(Offset.Zero) }

    val theme = LocalTheme.current

    val backgroundShadow = remember(theme.shadowColor) {
        Shadow(50.dp, theme.shadowColor, 4.dp, DpOffset(0.dp, 13.dp), .42f, BlendMode.SrcOver)
    }

    val shell = LocalShell.current
    if (shell.layout.minimal) {
        MinimalShell()
        return
    }
    val glowAlpha = if (theme.glowEnabled) (shell.appearance.glowOpacity / 100f).coerceIn(0f, 1f) else 0f
    val glowMotion = remember { GlowMotion() }
    val sidebarColor = theme.surfaceOpacity(theme.sidebarBackground, shell.appearance.sidebarOpacity, DEFAULT_SIDEBAR_OPACITY)
    val pageColor = theme.surfaceOpacity(theme.pageBackground, shell.appearance.pageOpacity, DEFAULT_PAGE_OPACITY)
    Box(
        modifier = Modifier.onGloballyPositioned {
            windowOffset = it.positionInRoot()
        }.sizeIn(
            DESIGN_WIDTH_DP.dp, DESIGN_HEIGHT_DP.dp, DESIGN_WIDTH_DP.dp, DESIGN_HEIGHT_DP.dp
        ).let { mod ->
                if (theme.shadowEnabled) mod.cachedDropShadow(theme.backgroundShape, backgroundShadow, hole = glowAlpha > 0f) else mod
            }.clip(theme.backgroundShape)
            .liquidGlass(theme.backgroundShape)
            .splitFrostedGlass(
                theme.backgroundShape,
                SidebarWidth,
                glassOpacity(shell.appearance.sidebarOpacity, DEFAULT_SIDEBAR_OPACITY),
                glassOpacity(shell.appearance.pageOpacity, DEFAULT_PAGE_OPACITY),
            )
            .border(1.dp, theme.borderColor, theme.backgroundShape)
            .then(if (glowAlpha > 0f && !ReducedMotion.enabled) glowMotion.track else Modifier)
            .drawBehind { backdrop(windowOffset) }
    ) {
        if (glowAlpha > 0f) {
            val sidebarGlow = remember { GlowLayers() }
            val pageGlow = remember { GlowLayers() }
            Box(Modifier.matchParentSize().drawBehind {
                val w = size.width.roundToInt()
                val sw = SidebarWidth.roundToPx().coerceIn(0, w)
                val accent = Accent.copy(alpha = 1f)
                val shadow = if (theme.shadowEnabled) {
                    val sp = backgroundShadow.spread.toPx()
                    val ox = backgroundShadow.offset.x.toPx()
                    val oy = backgroundShadow.offset.y.toPx()
                    val c = backgroundShadow.color
                    ShadowBake(ox - sp, oy - sp, size.width + ox + sp, size.height + oy + sp, backgroundShadow.radius.toPx() * 0.57735f, c.copy(alpha = c.alpha * backgroundShadow.alpha))
                } else null
                val border = theme.borderColor
                fun DrawScope.glow(nanos: Long) {
                    val centers = glowMotion.frame(nanos, size)
                    with(sidebarGlow) { draw(0f, sw.toFloat(), accent, glowAlpha, sidebarColor, shadow, centers) }
                    drawLine(border, Offset(sw.toFloat(), 0f), Offset(sw.toFloat(), size.height), 1f)
                    with(pageGlow) { draw(sw.toFloat(), w.toFloat(), accent, glowAlpha, pageColor, shadow, centers) }
                }
                if (ReducedMotion.enabled) glow(0L) else drawLive { glow(System.nanoTime()) }
            })
        }
        Row(Modifier.matchParentSize()) {
            Sidebar(background = glowAlpha <= 0f)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(if (glowAlpha > 0f) Modifier else Modifier.background(pageColor)),
            ) {
                val backStackEntry by LocalShellNavController.current.currentBackStackEntryAsState()
                val isModsMenu = backStackEntry?.destination?.hasRoute(ModsRoute::class) == true
                val searchQuery = shell.search.query
                val isSearching = isModsMenu && searchQuery.isNotBlank()
                Column(
                    modifier = Modifier.weight(1f)
                        .padding(vertical = 19.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isSearching) 0.dp else 19.dp)
                ) {
                    Header(Modifier.padding(horizontal = PageGutter))
                    Box(
                        modifier = Modifier.weight(1f).fillMaxSize()
                            // a dragged mod card is drawn in an overlay that must be able to leave the page
                            // area; keep the clip otherwise so page content cannot overflow the pane
                            .then(if (shell.layout.modDragging) Modifier else Modifier.clipToBounds())
                            .alpha(if (shell.transitions.awaitingInitialRoute) 0f else 1f)
                    ) {
                        if (isSearching) SearchResultsScreen(searchQuery) else ShellPages()
                    }
                }
            }
        }
    }
}

@Composable
private fun ShellPages() {
    val shell = LocalShell.current
    // the first transition after the menu opens follows "Show opening page animation"
    // and later ones follow "Show Page Animations"
    val transitions = shell.transitions

    // Arrow-back routes through navigate() on purpose (this NavHost version never
    // plays its pop transitions from a resting state), so the shared enter/exit
    // transition mirrors the old pop direction while lastNavWasBack is set.
    NavHost(
        modifier = Modifier.fillMaxSize(),
        navController = LocalShellNavController.current,
        startDestination = ModsGraph,

        enterTransition = { if (transitions.animates()) ContentSlide.enter(!transitions.lastNavWasBack) else EnterTransition.None },
        exitTransition = { if (transitions.animates()) ContentSlide.exit(!transitions.lastNavWasBack) else ExitTransition.None },
    ) {
        navigation()
    }
}

private val MinimalWidth = 760.dp
private val MinimalResultsHeight = 440.dp

/** A Spotlight-style search bar with results under it, see [ShellLayout.minimal] */
@Composable
private fun MinimalShell() {
    val theme = LocalTheme.current
    val shell = LocalShell.current
    val query = shell.search.query
    // same footprint as the full shell so the panel's top edge stays put while results grow under it
    Box(Modifier.size(DESIGN_WIDTH_DP.dp, DESIGN_HEIGHT_DP.dp), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.padding(top = 120.dp).width(MinimalWidth)
                .clip(theme.backgroundShape)
                .liquidGlass(theme.backgroundShape)
                .background(theme.surfaceOpacity(theme.pageBackground, shell.appearance.pageOpacity, DEFAULT_PAGE_OPACITY))
                .border(1.dp, theme.borderColor, theme.backgroundShape)
                .padding(12.dp),
        ) {
            GlobalSearchBar(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                fontSize = 16.sp,
                placeholderText = translated("oneconfig.search.anything", "Search for anything"),
                leading = {
                    rememberSvgResourcePainter(ICON_PATH)?.let {
                        Image(it, null, Modifier.size(20.dp).accentGradientFill(), colorFilter = rememberAccentBrandTint(ICON_PATH))
                    }
                },
            )
            if (query.isNotBlank()) {
                Box(Modifier.fillMaxWidth().height(MinimalResultsHeight).padding(top = 12.dp)) {
                    SearchResultsScreen(query, columns = 2)
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // navigating leaves the minimal menu, so this expands into the full menu on Preferences
                MinimalAction("cog", translated("oneconfig.minimal.settings", "Settings")) { shell.navigation.navigate(PreferencesGraph) }
                val keybind = OneConfigConfig.oneConfigKeybind
                MinimalAction("hud", translated("oneconfig.sidebar.edit_hud", "Edit HUD"), keybind.displayName().takeIf { keybind.isBound }) {
                    HudManager.openEditor()
                }
            }
            // composed but never placed so the nav graph exists for a result to navigate into, which expands the menu
            Box(Modifier.layout { m, c -> m.measure(Constraints.fixed(c.maxWidth, MinimalResultsHeight.roundToPx())); layout(0, 0) {} }) {
                ShellPages()
            }
        }
    }
}

private const val ICON_PATH = "/assets/oneconfig/brand/oneconfig-icon.svg"

@Composable
private fun MinimalAction(icon: String, label: String, keyHint: String? = null, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val hovered by interactionSource.collectIsHoveredAsState()
    val color = if (hovered) theme.textColor else theme.textColorSecondary
    Row(
        Modifier.oneConfigClickable(label, onClick, indication = false, interactionSource = interactionSource).padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, color = color, modifier = Modifier.size(14.dp))
        Text(label, color = color, fontSize = 12.sp)
        keyHint?.let {
            Text(
                it, color = theme.textColorSecondary, fontSize = 11.sp,
                modifier = Modifier.border(1.dp, theme.borderColor, theme.buttonShape).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}
