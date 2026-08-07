package org.polyfrost.oneconfig.internal.ui.hud.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.runtime.PolyComposeRuntime
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.localizedValue
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private const val MAX_PREVIEW_SCALE = 2f

private const val MEASURE_BOUNDS = 2000f

internal class HudPreviewState(val runtime: PolyComposeRuntime) {
    var naturalWidth by mutableStateOf(0f)
        internal set
    var naturalHeight by mutableStateOf(0f)
        internal set

    val ready: Boolean get() = naturalWidth > 0f && naturalHeight > 0f
}

@Composable
internal fun rememberHudPreview(hud: Hud): HudPreviewState {
    val state = remember(hud) {
        hud.update()
        HudPreviewState(PolyComposeRuntime().also { rt -> rt.setContent { hud.Content() } })
    }
    DisposableEffect(state) {
        onDispose { state.runtime.dispose() }
    }
    LaunchedEffect(state) {
        hud.update()
        if (hud.staticWidth && hud.staticW > 0f && hud.staticH > 0f) {
            state.runtime.frame(hud.staticW, hud.staticH)
            state.naturalWidth = hud.staticW
            state.naturalHeight = hud.staticH
        } else {
            state.runtime.frame(MEASURE_BOUNDS, MEASURE_BOUNDS)
            state.naturalWidth = state.runtime.root.width
            state.naturalHeight = state.runtime.root.height
        }
    }
    return state
}

internal fun hudPreviewScale(naturalW: Float, naturalH: Float, availableW: Float, availableH: Float): Float {
    if (naturalW <= 0f || naturalH <= 0f) return MAX_PREVIEW_SCALE
    return minOf(MAX_PREVIEW_SCALE, availableW / naturalW, availableH / naturalH).coerceAtLeast(0.05f)
}

@Composable
internal fun HudPreviewCanvas(state: HudPreviewState, scale: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawIntoCanvas { canvas ->
            val skia = canvas.skiaCanvas
            skia.save()
            skia.clipRect(org.jetbrains.skia.Rect.makeWH(size.width, size.height))
            skia.scale(scale, scale)
            state.runtime.root.render(RenderContext(skia))
            skia.restore()
        }
    }
}

private object IntrinsicSafeMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val placeables = measurables.map { it.measure(constraints) }
        val width = placeables.maxOfOrNull { it.width } ?: constraints.minWidth
        val height = placeables.maxOfOrNull { it.height } ?: constraints.minHeight
        return layout(width, height) { placeables.forEach { it.place(0, 0) } }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) = 0
    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) = 0
    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) = 0
    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) = 0
}

@Composable
internal fun HudPreview(hud: Hud, modifier: Modifier = Modifier) {
    val state = rememberHudPreview(hud)
    val density = LocalDensity.current.density

    Layout(modifier = modifier, measurePolicy = IntrinsicSafeMeasurePolicy, content = {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val availableW = maxWidth.value * density
            val availableH = maxHeight.value * density
            if (!state.ready || availableW <= 0f || availableH <= 0f) {
                Text(
                    localizedValue(hud.title) ?: hud.title,
                    color = LocalTheme.current.textColor,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
                return@BoxWithConstraints
            }
            val scale = hudPreviewScale(state.naturalWidth, state.naturalHeight, availableW, availableH)
            val w = (state.naturalWidth * scale / density).dp
            val h = (state.naturalHeight * scale / density).dp
            HudPreviewCanvas(state, scale, Modifier.size(w, h))
        }
    })
}
