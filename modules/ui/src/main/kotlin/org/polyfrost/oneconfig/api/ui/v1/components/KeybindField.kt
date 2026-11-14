@file:OptIn(ExperimentalOneConfigUiApi::class, InternalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.internal.keybindRecordingBridge
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeyModifiers
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.api.ui.v1.keybind.TextInputFocus
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private val KEY_ICON_SIZE = 16.dp
private const val FADE_MILLIS = 150
private const val RECORDING_FILL_ALPHA = 0.2f
private const val HOVER_FILL_ALPHA = 0.8f
private const val HOVER_BORDER_ALPHA = 0.2f

@ExperimentalOneConfigUiApi
@Composable
public fun KeybindField(
    keybind: OneConfigKeybind?,
    onChange: (OneConfigKeybind?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    conflict: Boolean? = null,
    singleKey: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
) {
    val theme = OneConfigTheme.current
    val accent = OneConfigTheme.accent
    val latestKeybind by rememberUpdatedState(keybind)
    val latestOnChange by rememberUpdatedState(onChange)
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val focusRequester = remember { FocusRequester() }
    var recording by remember { mutableStateOf(false) }
    val recordedKeys = remember { mutableStateListOf<Int>() }
    val recordedMouse = remember { mutableStateListOf<Int>() }
    val heldKeys = remember { HashSet<Int>() }
    val heldMouse = remember { HashSet<Int>() }
    val typingToken = remember { Any() }

    fun emit(keys: IntArray?, mouse: IntArray?, mods: Byte) {
        val current = latestKeybind
        val result = if (keys == null && mouse == null) {
            current?.copyWith(null, null, KeyModifiers.NONE)
        } else {
            current?.copyWith(keys, mouse, mods) ?: OneConfigKeybind(keys, mouse, mods, 0L) { true }
        }
        recording = false
        latestOnChange(result)
    }

    fun clear() = emit(null, null, KeyModifiers.NONE)

    fun commitIfReleased() {
        if (heldKeys.isNotEmpty() || heldMouse.isNotEmpty()) return
        if (recordedKeys.isEmpty() && recordedMouse.isEmpty()) return
        val mouse = recordedMouse.toIntArray().takeIf { it.isNotEmpty() }
        val (mods, keys) = splitModifiers(recordedKeys)
        if (keys.isEmpty() && mouse == null) emit(recordedKeys.toIntArray(), null, KeyModifiers.NONE)
        else emit(keys.takeIf { it.isNotEmpty() }, mouse, mods)
    }

    LaunchedEffect(enabled) { if (!enabled) recording = false }

    LaunchedEffect(recording) {
        if (recording) {
            recordedKeys.clear()
            recordedMouse.clear()
            heldKeys.clear()
            heldMouse.clear()
            runCatching { focusRequester.requestFocus() }
        }
    }

    if (recording) EscapeCloses { clear() }

    DisposableEffect(recording) {
        if (!recording) return@DisposableEffect onDispose { }
        val onEscape: () -> Unit = { clear() }
        val onMouse: (Int, Boolean) -> Unit = { button, pressed ->
            if (pressed && singleKey) {
                emit(null, intArrayOf(button), KeyModifiers.NONE)
            } else if (pressed) {
                if (button !in recordedMouse) recordedMouse.add(button)
                heldMouse.add(button)
            } else {
                heldMouse.remove(button)
                commitIfReleased()
            }
        }
        val bridge = runCatching { keybindRecordingBridge() }.getOrNull()
        TextInputFocus.acquire(typingToken)
        bridge?.startRecording(onEscape, onMouse)
        onDispose {
            bridge?.stopRecording(onEscape, onMouse)
            TextInputFocus.release(typingToken)
        }
    }

    val showConflict = !recording && (conflict ?: (keybind != null && KeybindManager.findConflicts(keybind).any { it.action !== keybind.action }))
    val fill by animateColorAsState(
        when {
            recording -> accent.copy(alpha = RECORDING_FILL_ALPHA)
            hovered && enabled -> theme.componentBackground.copy(alpha = theme.componentBackground.alpha * HOVER_FILL_ALPHA)
            else -> theme.componentBackground
        },
        Motion.spec(tween(FADE_MILLIS)),
        label = "keybindFill",
    )
    val border by animateColorAsState(
        when {
            recording -> accent
            showConflict -> theme.error
            hovered && enabled -> theme.textColor.copy(alpha = HOVER_BORDER_ALPHA)
            else -> theme.borderColor
        },
        Motion.spec(tween(FADE_MILLIS)),
        label = "keybindBorder",
    )
    val content by animateColorAsState(
        when {
            recording -> accent
            showConflict -> theme.error
            else -> theme.textColor
        },
        Motion.spec(tween(FADE_MILLIS)),
        label = "keybindText",
    )

    val shown = when {
        recording && recordedKeys.isEmpty() && recordedMouse.isEmpty() -> formControlString("oneconfig.keybind.recording", "Press keys...")
        recording -> (recordedKeys.map(OneConfigKeybind::keyName) + recordedMouse.map(::mouseButtonName)).joinToString(" + ")
        keybind != null && keybind.isBound -> keybind.displayName()
        else -> formControlString("oneconfig.keybind.none", "None")
    }
    val shape = theme.shapes.field
    val typography = theme.typography

    Row(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .heightIn(min = ControlSize.Regular.height)
            .onPreviewKeyEvent { event -> recording && handleRecordingKey(event, recordedKeys, heldKeys, singleKey, ::clear, ::commitIfReleased) { code -> emit(intArrayOf(code), null, KeyModifiers.NONE) } }
            .focusRequester(focusRequester)
            .formFrame(shape, theme.accentBrush, fill = { fill }, border = { border })
            .oneConfigClickable(
                shown,
                { if (!recording) recording = true },
                role = A11yRole.Button,
                enabled = enabled,
                shape = shape,
                indication = false,
                interactionSource = source,
            )
            .padding(horizontal = theme.spacing.l, vertical = theme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.m),
    ) {
        Icon("keyboard", null, Modifier.size(KEY_ICON_SIZE), tint = content)
        Text(
            shown,
            Modifier,
            content,
            OneConfigTheme.typography.body.copy(fontSize = typography.label.fontSize, fontWeight = typography.label.fontWeight ?: FontWeight.Normal),
            Int.MAX_VALUE,
            TextOverflow.Clip,
            textAlign = TextAlign.Start,
        )
    }
}

private fun handleRecordingKey(
    event: KeyEvent,
    recordedKeys: MutableList<Int>,
    heldKeys: MutableSet<Int>,
    singleKey: Boolean,
    clear: () -> Unit,
    commitIfReleased: () -> Unit,
    commitSingle: (Int) -> Unit,
): Boolean {
    if (event.awtKeyEventId() == java.awt.event.KeyEvent.KEY_TYPED) return true
    return when (event.type) {
        KeyEventType.KeyDown -> {
            if (event.key == Key.Escape || ((event.key == Key.Backspace || event.key == Key.Delete) && recordedKeys.isEmpty())) {
                clear()
                return true
            }
            val keyCode = event.utf16CodePoint
            if (keyCode <= 0) return true
            if (singleKey) {
                commitSingle(keyCode)
                return true
            }
            if (keyCode !in recordedKeys) recordedKeys.add(keyCode)
            heldKeys.add(keyCode)
            true
        }
        KeyEventType.KeyUp -> {
            heldKeys.remove(event.utf16CodePoint)
            commitIfReleased()
            true
        }
        else -> false
    }
}

private fun splitModifiers(codes: List<Int>): Pair<Byte, IntArray> {
    var mods = KeyModifiers.NONE
    val keys = ArrayList<Int>(codes.size)
    for (code in codes) {
        val bit = KeyModifiers.of(code)
        if (bit != KeyModifiers.NONE) mods = (mods.toInt() or bit.toInt()).toByte() else keys += code
    }
    return mods to keys.toIntArray()
}

private fun mouseButtonName(button: Int): String =
    runCatching { Platform.compatibility().keys().mouseName(button) }.getOrNull() ?: button.toString()

private fun KeyEvent.awtKeyEventId(): Int? = runCatching {
    val internal = nativeKeyEvent
    val field = internal.javaClass.getDeclaredField("nativeEvent").apply { isAccessible = true }
    (field.get(internal) as? java.awt.event.KeyEvent)?.id
}.getOrNull()
