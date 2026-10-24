/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.notifications.v1

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.align
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.shape.RoundedCornerShape
import org.polyfrost.compose.foundation.text.Text
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.ImageBitmap
import org.polyfrost.compose.ui.layout.layoutId
import org.polyfrost.compose.ui.text.font.FontFamily
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp

private const val CARD_WIDTH = 340f
private const val CARD_PAD_H = 14f
private const val CARD_PAD_V = 16f
private const val CONTENT_WIDTH = CARD_WIDTH - CARD_PAD_H * 2f
private const val ICON_SIZE = 24f
private const val ICON_GAP = 12f

private const val TITLE_SIZE = 14f
private const val MESSAGE_SIZE = 12f

private const val ICON_RASTER_SCALE = 2f

internal sealed interface ToastHit {
    val notification: Notification

    data class Body(override val notification: Notification) : ToastHit
    data class Action(override val notification: Notification, val action: NotificationAction) : ToastHit
}

private const val ENTER_NANOS = 220_000_000L
private const val EXIT_NANOS = 220_000_000L
private const val PROGRESS_LINGER_NANOS = 600_000_000L

internal object ToastViewport {
    var width by mutableStateOf(0f)
    var height by mutableStateOf(0f)
    var scale by mutableStateOf(1f)
}

@Composable
fun NotificationToasts() {
    val w = ToastViewport.width
    val h = ToastViewport.height
    if (w <= 0f || h <= 0f) return

    Box(modifier = Modifier.width(w.dp).height(h.dp)) {
        Column(
            modifier = Modifier.padding(16.dp).align(Alignment.BottomEnd),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (notification in NotificationsManager.active) {
                key(notification.id) { Toast(notification) }
            }
        }
    }
}

@Composable
private fun Toast(notification: Notification) {
    var appear by remember { mutableStateOf(0f) }
    val timeFraction = remember { mutableStateOf(1f) }
    val progressValue = remember { mutableStateOf(quantize(notification.progressOrNull() ?: 0f)) }

    LaunchedEffect(notification.id) {
        val tracksProgress = notification.progress != null

        fun sampleProgress(): Float {
            val p = notification.progressOrNull() ?: 0f
            progressValue.value = quantize(p)
            return p
        }

        val enterStart = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val t = ((now - enterStart).coerceAtLeast(0L).toFloat() / ENTER_NANOS).coerceIn(0f, 1f)
            appear = easeOutCubic(t)
            if (tracksProgress) sampleProgress()
            if (t >= 1f) break
        }
        appear = 1f

        when {
            notification.progress != null -> {
                while (!notification.dismissRequested) {
                    if (sampleProgress() >= 1f) break
                    withFrameNanos { }
                }
                val lingerStart = withFrameNanos { it }
                while (withFrameNanos { it } - lingerStart < PROGRESS_LINGER_NANOS && !notification.dismissRequested) { }
            }
            notification.persistent -> {
                while (!notification.dismissRequested) withFrameNanos { }
            }
            else -> {
                val durationNanos = (notification.duration * 1_000_000L).toLong().coerceAtLeast(1L)
                var remaining = durationNanos
                var last = withFrameNanos { it }
                while (remaining > 0L && !notification.dismissRequested) {
                    val now = withFrameNanos { it }
                    val dt = now - last
                    last = now
                    if (!notification.hovered) remaining -= dt
                    timeFraction.value = quantize((remaining.toFloat() / durationNanos).coerceIn(0f, 1f))
                }
                timeFraction.value = 0f
            }
        }

        val exitStart = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val t = ((now - exitStart).coerceAtLeast(0L).toFloat() / EXIT_NANOS).coerceIn(0f, 1f)
            appear = 1f - easeInCubic(t)
            if (t >= 1f) break
        }
        NotificationsManager.dismiss(notification)
    }

    val a = appear
    val hasProgress = notification.progress != null
    val bgBase = if (notification.hovered) NotificationTheme.background.lighten(0.06f) else NotificationTheme.background
    val cardBg = bgBase.multiplyAlpha(a)

    Column(
        modifier = Modifier
            .width(CARD_WIDTH.dp)
            .background(cardBg, RoundedCornerShape(NotificationTheme.radiusCard.dp))
            .border(1.dp, NotificationTheme.border.multiplyAlpha(a), RoundedCornerShape(NotificationTheme.radiusCard.dp))
            .padding(horizontal = CARD_PAD_H.dp, vertical = CARD_PAD_V.dp)
            .layoutId(ToastHit.Body(notification)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(ICON_GAP.dp)) {
            ToastIcon(notification, a)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(notification.title, color = NotificationTheme.accentFor(notification.type).multiplyAlpha(a), fontSize = TITLE_SIZE.sp, fontFamily = fontFamily(NotificationTheme.fontTitle))
                val bodyFont = messageFont()
                val lines = remember(notification.message, bodyFont) {
                    wrapText(notification.message, TEXT_WIDTH, bodyFont)
                }
                for (line in lines) {
                    Text(line, color = NotificationTheme.textSecondary.multiplyAlpha(a), fontSize = MESSAGE_SIZE.sp, fontFamily = fontFamily(NotificationTheme.fontBody))
                }
            }
        }

        val showTimer = !hasProgress && !notification.persistent && notification.actions.isEmpty()
        if (hasProgress) {
            ProgressBar({ progressValue.value }, a)
        } else if (showTimer) {
            ProgressBar({ timeFraction.value }, a)
        }

        if (notification.actions.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (action in notification.actions) {
                    ActionButton(notification, action, a)
                }
            }
        }
    }
}

private fun quantize(value: Float): Float {
    val barPixels = (CONTENT_WIDTH * ToastViewport.scale).coerceAtLeast(1f)
    return Math.round(value * barPixels) / barPixels
}

@Composable
private fun ProgressBar(progress: () -> Float, a: Float) {
    val accent = NotificationTheme.accent
    Box(
        modifier = Modifier
            .width(CONTENT_WIDTH.dp)
            .height(6.dp)
            .background(accent.multiplyAlpha(0.5f * a), RoundedCornerShape(NotificationTheme.radiusProgress.dp)),
    ) {
        Canvas(Modifier.width(CONTENT_WIDTH.dp).height(6.dp), live = true) { x, y, _, h ->
            val fillWidth = (CONTENT_WIDTH * progress()).coerceIn(0f, CONTENT_WIDTH)
            if (fillWidth > 0f) rect(x, y, fillWidth, h, accent.multiplyAlpha(a), NotificationTheme.radiusProgress)
        }
    }
}

@Composable
private fun ActionButton(notification: Notification, action: NotificationAction, a: Float) {
    val hovered = ToastInput.hoveredAction === action
    val base = if (action.primary) NotificationTheme.accent else NotificationTheme.subtleButton
    val bg = (if (hovered) base.lighten(0.12f) else base).multiplyAlpha(a)
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(NotificationTheme.radiusButton.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .layoutId(ToastHit.Action(notification, action)),
    ) {
        val textColor = if (action.primary) NotificationTheme.actionPrimaryText else NotificationTheme.actionSubtleText
        Text(action.label, color = textColor.multiplyAlpha(a), fontSize = MESSAGE_SIZE.sp, fontFamily = fontFamily(NotificationTheme.fontTitle))
    }
}

@Composable
private fun ToastIcon(notification: Notification, a: Float) {
    val bitmap = notification.icon
    if (bitmap != null) {
        Canvas(modifier = Modifier.size(ICON_SIZE.dp, ICON_SIZE.dp).align(Alignment.Top)) { x, y, w, h ->
            val s = minOf(w / bitmap.width, h / bitmap.height)
            fadedImage(bitmap, x + (w - bitmap.width * s) / 2f, y + (h - bitmap.height * s) / 2f, bitmap.width * s, bitmap.height * s, a)
        }
    } else {
        val accent = NotificationTheme.accentFor(notification.type).argb
        Canvas(modifier = Modifier.size(ICON_SIZE.dp, ICON_SIZE.dp).align(Alignment.Top)) { x, y, w, h ->
            val pixelSize = Math.ceil((w * ToastViewport.scale * ICON_RASTER_SCALE).toDouble()).toInt()
            val img = SvgMask.get("/assets/oneconfig/ico/${notification.type.iconName}.svg", pixelSize, accent)
            if (img != null) fadedImage(img, x, y, w, h, a)
        }
    }
}

private fun RenderContext.fadedImage(bitmap: ImageBitmap, x: Float, y: Float, w: Float, h: Float, a: Float) {
    if (a >= 1f) return image(bitmap, x, y, w, h)
    saveLayerAlpha(a)
    image(bitmap, x, y, w, h)
    restore()
}

private const val TEXT_WIDTH = CONTENT_WIDTH - ICON_SIZE - ICON_GAP

private fun fontFamily(name: String?): FontFamily = name?.let { FontFamily(it) } ?: FontFamily.Default

private fun messageFont(): PolyFont = NotificationTheme.fontBody?.let { PolyFont(it, MESSAGE_SIZE) } ?: PolyFont.default(MESSAGE_SIZE)

private fun wrapText(text: String, maxWidth: Float, font: PolyFont): List<String> {
    if (text.isEmpty()) return emptyList()
    if (font.measureTextWidth(text) <= maxWidth) return listOf(text)
    val lines = ArrayList<String>()
    var current = StringBuilder()
    for (word in text.split(' ')) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (font.measureTextWidth(candidate) <= maxWidth) {
            current = StringBuilder(candidate)
        } else {
            if (current.isNotEmpty()) lines.add(current.toString())
            current = StringBuilder(word)
        }
    }
    if (current.isNotEmpty()) lines.add(current.toString())
    return lines
}

private fun easeOutCubic(t: Float): Float {
    val x = 1f - t
    return 1f - x * x * x
}

private fun easeInCubic(t: Float): Float = t * t * t
