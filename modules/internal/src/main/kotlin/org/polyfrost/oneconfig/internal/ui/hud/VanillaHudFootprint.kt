package org.polyfrost.oneconfig.internal.ui.hud

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Rect
import org.polyfrost.oneconfig.api.hud.v1.HudManager

object VanillaHudFootprint {
    private const val MAX_RECTS = 1024
    private const val MAX_SCREEN_SHARE = 0.4f

    var supported: Boolean = false
        private set

    private var capturing = false
    private var sampling = false
    private var pending = FloatArray(MAX_RECTS * 4)
    private var pendingCount = 0

    var rects: List<Rect> by mutableStateOf(emptyList())
        private set

    var sampleRects: List<Rect> by mutableStateOf(emptyList())
        private set

    var measuredWidth: Float = 0f
        private set

    var measuredHeight: Float = 0f
        private set

    @JvmStatic
    @JvmOverloads
    fun begin(sample: Boolean = false) {
        capturing = true
        sampling = sample
        pendingCount = 0
    }

    @JvmStatic
    fun isCapturing(): Boolean = capturing

    @JvmStatic
    fun isSampling(): Boolean = capturing && sampling

    @JvmStatic
    fun add(left: Int, top: Int, right: Int, bottom: Int) {
        add(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    }

    @JvmStatic
    fun add(left: Float, top: Float, right: Float, bottom: Float) {
        if (!capturing || pendingCount >= MAX_RECTS || right <= left || bottom <= top) return
        supported = true
        val i = pendingCount * 4
        pending[i] = left
        pending[i + 1] = top
        pending[i + 2] = right
        pending[i + 3] = bottom
        pendingCount++
    }

    @JvmStatic
    fun end(guiWidth: Float, guiHeight: Float) {
        if (!capturing) return
        capturing = false
        val sample = sampling
        sampling = false
        if (!supported) return
        val maxArea = guiWidth * guiHeight * MAX_SCREEN_SHARE
        val owned = HudManager.activeInstances.filter { !it.hidden && it.renderedW > 0f && it.renderedH > 0f }
            .map { Rect(it.x, it.y, it.x + it.renderedW, it.y + it.renderedH) }
        val next = ArrayList<Rect>(pendingCount)
        for (k in 0 until pendingCount) {
            val i = k * 4
            val rect = Rect(pending[i], pending[i + 1], pending[i + 2], pending[i + 3])
            if (rect.width * rect.height > maxArea) continue
            if (owned.any { it.contains(rect.center) }) continue
            next.add(rect)
        }
        if (sample) {
            if (next != sampleRects) Snapshot.withMutableSnapshot { sampleRects = next }
            return
        }
        measuredWidth = guiWidth
        measuredHeight = guiHeight
        if (next != rects) Snapshot.withMutableSnapshot { rects = next }
    }

    @JvmStatic
    fun clearSamples() {
        if (sampleRects.isNotEmpty()) Snapshot.withMutableSnapshot { sampleRects = emptyList() }
    }
}
