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
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.polyfrost.compose.composables.PolyBox
import org.polyfrost.compose.composables.PolyCanvas
import org.polyfrost.compose.composables.PolyColumn
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.PolyRect
import org.polyfrost.compose.composables.PolyRow
import org.polyfrost.compose.composables.PolyText
import org.polyfrost.compose.composables.align
import org.polyfrost.compose.composables.background
import org.polyfrost.compose.composables.border
import org.polyfrost.compose.composables.height
import org.polyfrost.compose.composables.margin
import org.polyfrost.compose.composables.padding
import org.polyfrost.compose.composables.radius
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.composables.tag
import org.polyfrost.compose.composables.width
import org.polyfrost.compose.layout.PolyAlign
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyColor

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

    PolyBox(modifier = PolyModifier.width(w).height(h)) {
        PolyColumn(
            gap = 12f,
            modifier = PolyModifier.align(PolyAlign.BottomRight).margin(16f),
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

    PolyColumn(
        gap = 12f,
        modifier = PolyModifier
            .width(CARD_WIDTH)
            .background(cardBg, NotificationTheme.radiusCard)
            .border(NotificationTheme.border.multiplyAlpha(a), 1f, NotificationTheme.radiusCard)
            .padding(horizontal = CARD_PAD_H, vertical = CARD_PAD_V)
            .tag(ToastHit.Body(notification)),
    ) {
        PolyRow(gap = ICON_GAP) {
            ToastIcon(notification, a)
            PolyColumn(gap = 4f) {
                PolyText(notification.title, color = NotificationTheme.accentFor(notification.type).multiplyAlpha(a), fontSize = TITLE_SIZE, font = NotificationTheme.fontTitle)
                val bodyFont = messageFont()
                val lines = remember(notification.message, bodyFont) {
                    wrapText(notification.message, TEXT_WIDTH, bodyFont)
                }
                for (line in lines) {
                    PolyText(line, color = NotificationTheme.textSecondary.multiplyAlpha(a), fontSize = MESSAGE_SIZE, font = NotificationTheme.fontBody)
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
            PolyRow(gap = 16f) {
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
    PolyBox(
        modifier = PolyModifier
            .width(CONTENT_WIDTH)
            .height(6f)
            .background(accent.multiplyAlpha(0.5f * a), NotificationTheme.radiusProgress),
    ) {
        // read here and not in the enclosing body: PolyModifier has no equals, so recomposing the
        // outer PolyBox would rebuild its chain and reapply the whole style on every tick
        val fillWidth = (CONTENT_WIDTH * progress()).coerceIn(0f, CONTENT_WIDTH)
        if (fillWidth > 0f) {
            PolyRect(
                color = accent.multiplyAlpha(a),
                modifier = PolyModifier.width(fillWidth).height(6f).radius(NotificationTheme.radiusProgress),
            )
        }
    }
}

@Composable
private fun ActionButton(notification: Notification, action: NotificationAction, a: Float) {
    val hovered = ToastInput.hoveredAction === action
    val base = if (action.primary) NotificationTheme.accent else NotificationTheme.subtleButton
    val bg = (if (hovered) base.lighten(0.12f) else base).multiplyAlpha(a)
    PolyBox(
        modifier = PolyModifier
            .background(bg, NotificationTheme.radiusButton)
            .padding(horizontal = 12f, vertical = 6f)
            .tag(ToastHit.Action(notification, action)),
    ) {
        val textColor = if (action.primary) NotificationTheme.actionPrimaryText else NotificationTheme.actionSubtleText
        PolyText(action.label, color = textColor.multiplyAlpha(a), fontSize = MESSAGE_SIZE, font = NotificationTheme.fontTitle)
    }
}

@Composable
private fun ToastIcon(notification: Notification, a: Float) {
    val custom = notification.icon
    if (custom != null) {
        val paint = remember { Paint() }
        PolyCanvas(modifier = PolyModifier.size(ICON_SIZE, ICON_SIZE).align(PolyAlign.Top)) { x, y, w, h ->
            val s = minOf(w / custom.width.toFloat(), h / custom.height.toFloat())
            val dw = custom.width * s
            val dh = custom.height * s
            paint.setAlphaf(a)
            image(custom, x + (w - dw) / 2f, y + (h - dh) / 2f, dw, dh, paint)
        }
    } else {
        val accent = NotificationTheme.accentFor(notification.type)
        val paint = remember { Paint() }
        var filterArgb = 0
        PolyCanvas(modifier = PolyModifier.size(ICON_SIZE, ICON_SIZE).align(PolyAlign.Top)) { x, y, w, h ->
            val pixelSize = Math.ceil((w * ToastViewport.scale * ICON_RASTER_SCALE).toDouble()).toInt()
            val img = SvgRasterizer.get(notification.type.iconName, pixelSize)
            if (img != null) {
                val argb = accent.argb
                if (argb != filterArgb) {
                    filterArgb = argb
                    paint.colorFilter = ColorFilter.makeBlend(argb, BlendMode.SRC_IN)
                }
                paint.setAlphaf(a)
                image(img, x, y, w, h, paint)
            }
        }
    }
}

private const val TEXT_WIDTH = CONTENT_WIDTH - ICON_SIZE - ICON_GAP

private fun messageFont(): Font {
    val name = NotificationTheme.fontBody
    return name?.let { FontManager.getFont(MESSAGE_SIZE, it) } ?: FontManager.getFont(MESSAGE_SIZE)
}

private fun wrapText(text: String, maxWidth: Float, font: Font): List<String> {
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
