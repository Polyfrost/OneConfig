package org.polyfrost.oneconfig.internal.ui.hud.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import org.apache.logging.log4j.LogManager
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.runtime.PolyComposeHost
import org.polyfrost.compose.runtime.PolyComposeRuntime
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.platform.LocalDensity
import org.polyfrost.compose.ui.text.style.TextAlign
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.localizedValue
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private const val MAX_PREVIEW_SCALE = 2f

private const val MEASURE_BOUNDS = 2000f
private val LOGGER = LogManager.getLogger("OneConfig/HUD-Preview")

@Suppress("DEPRECATION")
private fun Throwable.isFatalPreviewFailure(): Boolean = this is VirtualMachineError || this is ThreadDeath

internal class HudPreviewState(val runtime: PolyComposeRuntime) {
    var naturalWidth by mutableStateOf(0f)
        internal set
    var naturalHeight by mutableStateOf(0f)
        internal set
    var measured by mutableStateOf(false)
        internal set

    val ready: Boolean get() = naturalWidth > 0f && naturalHeight > 0f

    fun dispose() {
        runtime.dispose()
    }
}

private object HudPreviewCache {
    private val cache = HashMap<Hud, HudPreviewState>()
    private var generation = Int.MIN_VALUE

    internal fun get(hud: Hud, revision: Int, build: () -> HudPreviewState): HudPreviewState {
        if (revision != generation) {
            releaseAll()
            generation = revision
        }
        return cache.getOrPut(hud, build)
    }

    private fun releaseAll() {
        if (cache.isEmpty()) return
        for (state in cache.values) {
            try {
                state.dispose()
            } catch (failure: Throwable) {
                if (failure.isFatalPreviewFailure()) throw failure
                LOGGER.warn("Failed to dispose HUD preview", failure)
            }
        }
        cache.clear()
    }
}

@Composable
internal fun rememberHudPreview(hud: Hud): HudPreviewState {
    val revision = HudManager.revision
    val state = remember(hud, revision) {
        HudPreviewCache.get(hud, revision) {
            hud.update()
            HudPreviewState(
                PolyComposeRuntime(PolyComposeHost.previews).also { rt ->
                    rt.setContent { hud.Content() }
                }
            )
        }
    }
    LaunchedEffect(state) {
        val static = hud.staticWidth && hud.staticW > 0f && hud.staticH > 0f
        if (!state.ready) {
            hud.update()
            if (static) {
                state.runtime.frame(hud.staticW, hud.staticH)
                state.naturalWidth = hud.staticW
                state.naturalHeight = hud.staticH
            } else {
                state.runtime.frame(MEASURE_BOUNDS, MEASURE_BOUNDS)
                state.naturalWidth = state.runtime.root.width
                state.naturalHeight = state.runtime.root.height
            }
            // Zero-size previews still count as measured so they don't block shortcut navigation
            state.measured = true
        }
        if (static) return@LaunchedEffect
        // Values like FPS widen after the first measure. Never shrink, or the library reflows
        // whenever a value flips between digit counts.
        snapshotFlow { HudManager.previewRevision.intValue }.collect {
            state.runtime.layout(MEASURE_BOUNDS, MEASURE_BOUNDS)
            state.naturalWidth = maxOf(state.naturalWidth, state.runtime.root.width)
            state.naturalHeight = maxOf(state.naturalHeight, state.runtime.root.height)
        }
    }
    return state
}

internal fun hudPreviewScale(naturalW: Float, naturalH: Float, availableW: Float, availableH: Float): Float {
    if (naturalW <= 0f || naturalH <= 0f) return MAX_PREVIEW_SCALE
    return minOf(MAX_PREVIEW_SCALE, availableW / naturalW, availableH / naturalH).coerceAtLeast(0.05f)
}

@Composable
internal fun HudPreviewCanvas(
    state: HudPreviewState,
    scale: Float,
    alwaysRedraw: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density / Platform.screen().surfaceRatio()
    HudManager.previewRevision.intValue

    Canvas(modifier, live = alwaysRedraw) { x, y, width, height ->
        val s = scale / density
        save()
        clipRect(x, y, width, height)
        translate(x, y)
        this.scale(s, s)
        Snapshot.withoutReadObservation {
            state.runtime.root.render(this)
        }
        restore()
    }
}

@Composable
internal fun HudPreview(hud: Hud, modifier: Modifier = Modifier) {
    val state = rememberHudPreview(hud)
    val density = LocalDensity.current.density / Platform.screen().surfaceRatio()
    HudManager.previewRevision.intValue

    if (!state.ready) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                localizedValue(hud.title) ?: hud.title,
                color = LocalTheme.current.textColor,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    val naturalW = state.naturalWidth
    val naturalH = state.naturalHeight
    Canvas(modifier, live = hud.alwaysRedraw) { x, y, availableW, availableH ->
        if (availableW <= 0f || availableH <= 0f) return@Canvas
        val scale = hudPreviewScale(naturalW, naturalH, availableW * density, availableH * density) / density
        save()
        clipRect(x, y, availableW, availableH)
        translate(x + (availableW - naturalW * scale) / 2f, y + (availableH - naturalH * scale) / 2f)
        this.scale(scale, scale)
        Snapshot.withoutReadObservation {
            state.runtime.root.render(this)
        }
        restore()
    }
}
