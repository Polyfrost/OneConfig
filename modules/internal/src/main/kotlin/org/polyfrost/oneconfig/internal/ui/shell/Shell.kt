package org.polyfrost.oneconfig.internal.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import org.apache.logging.log4j.LogManager
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.animation.core.tween
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.align
import org.polyfrost.compose.foundation.layout.fillMaxHeight
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.text.TextFieldState
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.draw.dropShadow
import org.polyfrost.compose.ui.draw.scale
import org.polyfrost.compose.ui.graphics.ImageBitmap
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.graphics.graphicsLayer
import org.polyfrost.compose.ui.graphics.shadow.Shadow
import org.polyfrost.compose.ui.input.key.KeyEvent
import org.polyfrost.compose.ui.input.key.onKeyDown
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.platform.LocalDensity
import org.polyfrost.compose.ui.unit.DpOffset
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.page.OneConfigPages
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.components.Header
import org.polyfrost.oneconfig.internal.ui.components.Sidebar
import org.polyfrost.oneconfig.internal.ui.components.SidebarWidth
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudDragLayer
import org.polyfrost.oneconfig.internal.ui.poly.WINDOW_HEIGHT
import org.polyfrost.oneconfig.internal.ui.poly.WINDOW_WIDTH
import org.polyfrost.oneconfig.internal.ui.screens.Changelog
import org.polyfrost.oneconfig.internal.ui.screens.Credits
import org.polyfrost.oneconfig.internal.ui.screens.Keybinds
import org.polyfrost.oneconfig.internal.ui.screens.ModConfig
import org.polyfrost.oneconfig.internal.ui.screens.Mods
import org.polyfrost.oneconfig.internal.ui.screens.Preferences
import org.polyfrost.oneconfig.internal.ui.screens.Profiles
import org.polyfrost.oneconfig.internal.ui.screens.SearchResultsScreen
import org.polyfrost.oneconfig.internal.ui.screens.Themes
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.withOpacityPercent

private const val GLOW_BLUR_SIGMA = 300f
private const val GLOW_RADIUS = 500f
private const val GLOW_EXTENT = GLOW_RADIUS + 3f * GLOW_BLUR_SIGMA

private val GLOW_CENTRES = listOf(147f to -199f, 893f to 736f)

internal val PageGutter = 25.dp

private val LOGGER = LogManager.getLogger("OneConfig/UI")

private val glowDisc: ImageBitmap by lazy {
    val n = 128
    val texel = GLOW_EXTENT * 2f / n
    val r = GLOW_RADIUS / texel
    val sigma = GLOW_BLUR_SIGMA / texel
    val c = n / 2f
    val src = FloatArray(n * n) { i ->
        val dx = i % n + 0.5f - c; val dy = i / n + 0.5f - c
        (r + 0.5f - sqrt(dx * dx + dy * dy)).coerceIn(0f, 1f)
    }
    val e = (sigma * 3f).roundToInt()
    val k = FloatArray(e * 2 + 1) { exp(-((it - e) * (it - e)) / (2f * sigma * sigma)) }
    val sum = k.sum()
    for (i in k.indices) k[i] /= sum
    fun blur(a: FloatArray, horizontal: Boolean) = FloatArray(n * n) { i ->
        val x = i % n; val y = i / n
        var acc = 0f
        for (j in k.indices) {
            val sx = if (horizontal) x + j - e else x
            val sy = if (horizontal) y else y + j - e
            if (sx in 0 until n && sy in 0 until n) acc += a[sy * n + sx] * k[j]
        }
        acc
    }
    val out = blur(blur(src, true), false)
    val px = ByteArray(n * n * 4)
    for (i in 0 until n * n) {
        px[i * 4] = -1; px[i * 4 + 1] = -1; px[i * 4 + 2] = -1
        px[i * 4 + 3] = (out[i] * 255f).roundToInt().coerceIn(0, 255).toByte()
    }
    ImageBitmap(n, n, px)
}

class ShellHost(
    var openModConfig: (ConfigData, category: String?) -> Unit = { _, _ -> },
    var navigate: (PolyPage, Any?) -> Unit = { _, _ -> },
    val editHud: () -> Unit,
    val close: () -> Unit,
    var toggleClose: () -> Unit = {},
    val openMs: Int = if (OneConfigConfig.guiOpenAnimation) (OneConfigConfig.animationTime * 1000f).toInt().coerceAtLeast(1) else 0,
    val closeMs: Int = if (OneConfigConfig.guiClosingAnimation) (OneConfigConfig.animationTime * 1000f).toInt().coerceIn(1, 160) else 0,
    val pageMs: Int = (OneConfigConfig.pageAnimationDuration * 1000f).toInt().coerceAtLeast(1),
    val pageAnimations: Boolean = OneConfigConfig.showPageAnimations,
    val openingPageAnimation: Boolean = OneConfigConfig.showOpeningPageAnimation,
    val surfaceRatio: Float = 1f,
    val restoring: Boolean = false,
    val resumed: Boolean = false,
) {
    val hudFrame = mutableIntStateOf(0)
}

private fun easeOutExpo(t: Float) = if (t >= 1f) 1f else 1f - 2f.pow(-10f * t)

private fun easeOutCubic(t: Float) = 1f - (1f - t).pow(3)

private fun bezier(s: Float, p1: Float, p2: Float): Float {
    val u = 1f - s
    return 3f * u * u * s * p1 + 3f * u * s * s * p2 + s * s * s
}

internal fun fastOutSlowIn(t: Float): Float {
    if (t <= 0f) return 0f
    if (t >= 1f) return 1f
    var lo = 0f
    var hi = 1f
    repeat(24) { val m = (lo + hi) / 2f; if (bezier(m, 0.4f, 0.2f) < t) lo = m else hi = m }
    return bezier((lo + hi) / 2f, 0f, 1f)
}

@Composable
private fun animateTween(target: Float, durationMs: Int, initial: Float = target, easing: (Float) -> Float = ::easeOutCubic): Float {
    var value by remember { mutableFloatStateOf(initial) }
    LaunchedEffect(target, durationMs) {
        val from = value
        if (durationMs <= 0) { value = target; return@LaunchedEffect }
        val start = withFrameNanos { it }
        while (value != target) {
            val t = ((withFrameNanos { it } - start) / 1e6f / durationMs).coerceIn(0f, 1f)
            value = if (t >= 1f) target else from + (target - from) * easing(t)
        }
    }
    return value
}

@Composable
fun Shell(host: ShellHost, start: PolyPage = PolyPage.Mods, startArg: Any? = null) {
    val nav = remember { ShellNav(PolyPage.Mods).also { if (start != PolyPage.Mods || startArg != null) it.navigate(start, startArg) } }
    remember(host, nav) {
        host.openModConfig = { mod, category -> nav.navigate(PolyPage.ModConfig, ModConfigTarget(mod, category)) }
        host.navigate = nav::navigate
    }
    val search = remember {
        TextFieldState(if (host.restoring) ShellState.lastSearch else "").also { nav.restoreSearch(it.text) }
    }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        UiSounds.play(UiSoundEvent.OPEN)
        when {
            host.resumed -> if (ShellState.lastSearchFocused) search.requestFocus()
            OneConfigConfig.instantSearch -> search.requestFocus(selectAll = true)
            host.restoring -> if (ShellState.lastSearchFocused) search.requestFocus()
        }
        snapshotFlow { search.focused }.collect { ShellState.lastSearchFocused = it }
    }
    DisposableEffect(Unit) {
        UiSounds.acquireAmbience()
        ShellState.searchField = search
        onDispose {
            if (ShellState.searchField === search) ShellState.searchField = null
            ShellState.lastSearch = search.text
            UiSounds.releaseAmbience()
            ShellState.lastClosedAt = System.currentTimeMillis()
        }
    }
    SideEffect { ShellState.lastPage = nav.current to nav.arg }
    val open = animateTween(if (visible) 1f else 0f, if (visible) host.openMs else host.closeMs, 0f, if (visible) ::easeOutExpo else ::easeOutCubic)
    var closing by remember { mutableStateOf(false) }
    if (closing && open <= 0f) LaunchedEffect(Unit) { host.close() }
    val requestClose = { if (!closing) { closing = true; visible = false; UiSounds.play(UiSoundEvent.CLOSE); UiSounds.releaseAmbience() } }
    host.toggleClose = {
        if (!closing) requestClose()
        else if (open > 0f) { closing = false; visible = true; UiSounds.play(UiSoundEvent.OPEN); UiSounds.acquireAmbience() }
    }
    nav.currentSearch = { search.text }
    LaunchedEffect(pageKey(nav.current, nav.arg)) { if (search.text != nav.search) { search.replaceText(nav.search); search.moveCaret(nav.search.length) } }

    val theme = LocalTheme.current

    val backgroundShadow = remember(theme.shadowColor) {
        Shadow(50.dp, theme.shadowColor, 4.dp, DpOffset(0.dp, 13.dp), .42f)
    }

    Box(
        Modifier.fillMaxSize()
            .onKeyDown { key, _ -> if (key == KeyEvent.KEY_ESCAPE) { requestClose(); true } else false }
            .onPointerEvent {
                if (it.type != PointerEventType.Press) return@onPointerEvent false
                when (it.button) {
                    3 -> { UiSounds.play(UiSoundEvent.CLICK); nav.back(); true }
                    4 -> { UiSounds.play(UiSoundEvent.CLICK); nav.forward(); true }
                    else -> false
                }
            }
    ) {
        HudDragLayer(host.hudFrame.intValue)
        val dragAlpha by animateFloatAsState(if (ShellState.hudDragging) OneConfigConfig.hudDragUiOpacity.coerceIn(0f, 1f) else 1f, tween(150))
        Box(
            modifier = Modifier.size(WINDOW_WIDTH.dp, WINDOW_HEIGHT.dp).align(Alignment.Center)
                .clip(theme.backgroundShape).alpha(open * dragAlpha).scale(0.9f + 0.1f * open)
        ) {
            if (theme.shadowEnabled) Box(Modifier.fillMaxSize().dropShadow(theme.backgroundShape, backgroundShadow))
            Glow()
            Row(Modifier.fillMaxSize()) {
                Sidebar(nav, host) { search.replaceText(""); search.moveCaret(0) }

                val searchQuery = search.text.trim()
                val isSearching = nav.current == PolyPage.Mods && searchQuery.isNotEmpty()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(theme.pageBackground.withOpacityPercent(ShellState.pageOpacity))
                        .padding(vertical = 19.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isSearching) 0.dp else 19.dp)
                ) {
                    Header(nav, search, requestClose, Modifier.padding(horizontal = PageGutter))
                    Pages(nav, host, searchQuery)
                }
            }
            Box(Modifier.fillMaxSize().border(1.dp, theme.borderColor, theme.backgroundShape))
        }
    }
}

@Composable
private fun Pages(nav: ShellNav, host: ShellHost, query: String) {
    val opening = remember { booleanArrayOf(host.openingPageAnimation && !host.restoring && nav.canGoBack()) }
    var shown by remember { mutableStateOf(if (opening[0]) PolyPage.Mods to null else nav.current to nav.arg) }
    var leaving by remember { mutableStateOf<Pair<PolyPage, Any?>?>(null) }
    var leavingQuery by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(1f) }
    val dir = nav.direction
    LaunchedEffect(nav.current, nav.arg) {
        if (nav.current to nav.arg == shown) return@LaunchedEffect
        if (pageKey(nav.current, nav.arg) == pageKey(shown.first, shown.second)) { shown = nav.current to nav.arg; return@LaunchedEffect }
        leaving = shown
        leavingQuery = query
        shown = nav.current to nav.arg
        val animate = opening[0] || host.pageAnimations
        opening[0] = false
        if (!animate || host.pageMs <= 0) { leaving = null; return@LaunchedEffect }
        progress = 0f
        val start = withFrameNanos { it }
        while (progress < 1f) {
            val t = ((withFrameNanos { it } - start) / 1e6f / host.pageMs).coerceIn(0f, 1f)
            progress = fastOutSlowIn(t)
        }
        leaving = null
    }
    val slide = (WINDOW_WIDTH - SidebarWidth.value) / 5f
    Box(Modifier.weight(1f).fillMaxWidth().then(if (ShellState.modDragging) Modifier else Modifier.clip())) {
        leaving?.let { old ->
            Box(Modifier.fillMaxSize().alpha(1f - progress).graphicsLayer(translationX = -dir * slide * progress)) {
                key(pageKey(old.first, old.second)) { Page(old.first, old.second, host, leavingQuery, nav) }
            }
        }
        Box(Modifier.fillMaxSize().alpha(if (leaving != null) progress else 1f).graphicsLayer(translationX = if (leaving != null) dir * slide * (1f - progress) else 0f)) {
            key(pageKey(shown.first, shown.second)) { Page(shown.first, shown.second, host, query, nav) }
        }
    }
}

private fun pageKey(page: PolyPage, arg: Any?): Any? = when (page) {
    PolyPage.ModConfig -> page to (arg as? ModConfigTarget)?.mod
    PolyPage.ChangelogEntry, PolyPage.Page -> page to arg
    else -> page
}

internal class PageScope(val host: ShellHost, val nav: ShellNav, val query: String)

internal val LocalPageScope = compositionLocalOf<PageScope?> { null }

@Composable
internal fun Page(page: PolyPage, arg: Any?, host: ShellHost, query: String, nav: ShellNav) {
    CompositionLocalProvider(LocalPageScope provides remember(host, nav, query) { PageScope(host, nav, query) }) {
        PageContent(page, arg, host, query, nav)
    }
}

@Composable
private fun PageContent(page: PolyPage, arg: Any?, host: ShellHost, query: String, nav: ShellNav) {
    when (page) {
        PolyPage.Mods -> if (query.isNotEmpty()) SearchResultsScreen(query, host) else Mods(host)
        PolyPage.Keybinds -> Keybinds(query)
        PolyPage.Credits -> Credits()
        PolyPage.Changelog -> Changelog(searchQuery = query, nav = nav)
        PolyPage.ChangelogEntry -> Changelog(arg as? Int ?: 0)
        PolyPage.Preferences -> Preferences(arg as? String, query, nav::tab)
        PolyPage.Themes -> Themes(arg as? String, query, nav::tab)
        PolyPage.Profiles -> Profiles(query)
        PolyPage.ModConfig -> (arg as? ModConfigTarget)?.let { t -> ModConfig(t.mod.id, t.category, query) { from, to -> nav.tab(t.copy(category = from), t.copy(category = to)) } }
        PolyPage.Page -> {
            val page = (arg as? String)?.let(OneConfigPages::byId)
            if (page != null) page.Content()
            else LaunchedEffect(arg) {
                LOGGER.warn("OneConfig page {} is not registered, opening the mods page instead", arg)
                nav.discard(PolyPage.Page, arg, PolyPage.Mods)
            }
        }
        else -> {}
    }
}

@Composable
private fun Glow() {
    val glowAlpha = (ShellState.glowOpacity / 100f).coerceIn(0f, 1f)
    if (glowAlpha <= 0f) return
    val tint = Accent.copy(alpha = glowAlpha)
    val pxPerDp = LocalDensity.current.density
    Canvas(Modifier.fillMaxSize()) { x, y, _, _ ->
        val size = GLOW_EXTENT * 2f / pxPerDp
        for ((cx, cy) in GLOW_CENTRES) {
            image(glowDisc, x + cx / pxPerDp - size / 2f, y + cy / pxPerDp - size / 2f, size, size, 0f, 0f, 1f, 1f, tint)
        }
    }
}
