@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.a11y

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.ThemeStyle

@ExperimentalOneConfigUiApi
public enum class A11yRole(public val spoken: String) {
    Button("button"),
    Tab("tab"),
    Checkbox("checkbox"),
    Switch("switch"),
    Option("option"),
    Link("link"),
    TextField("edit box"),
    ;

    internal val semanticsRole: Role?
        get() = when (this) {
            Button -> Role.Button
            Tab -> Role.Tab
            Checkbox -> Role.Checkbox
            Switch -> Role.Switch
            Option -> Role.DropdownList
            Link, TextField -> null
        }
}

@ExperimentalOneConfigUiApi
public object A11y {
    private const val REPEAT_MS = 1500L

    private var lastAnnounce = ""
    private var lastAnnounceAt = 0L

    private var keyboardInput by mutableStateOf(false)

    @JvmStatic
    public val keyboardMode: Boolean
        get() = keyboardInput

    @ApiStatus.Internal
    @JvmStatic
    public fun reportInput(keyboard: Boolean) {
        if (keyboardInput != keyboard) keyboardInput = keyboard
    }

    @JvmStatic
    public fun narratorActive(): Boolean = runCatching { Platform.screen().narratorActive() }.getOrDefault(false)

    @JvmStatic
    public fun say(text: String): Unit = say(text, false)

    @JvmStatic
    public fun say(text: String, interrupt: Boolean) {
        if (text.isBlank() || !narratorActive()) return
        runCatching { Platform.screen().narrate(text, interrupt) }
    }

    @JvmStatic
    public fun announce(text: String) {
        if (text.isBlank() || !narratorActive()) return
        val now = System.currentTimeMillis()
        if (text == lastAnnounce && now - lastAnnounceAt < REPEAT_MS) return
        lastAnnounce = text
        lastAnnounceAt = now
        say(text, false)
    }

    @JvmStatic
    public fun describe(label: String, role: A11yRole): String = describe(label, role, null, null)

    @JvmStatic
    public fun describe(label: String, role: A11yRole, selected: Boolean?, checked: Boolean?): String = buildString {
        append(label)
        if (role.spoken.isNotEmpty()) append(", ").append(role.spoken)
        if (selected == true) append(", selected")
        checked?.let { append(if (it) ", checked" else ", not checked") }
    }

    @JvmStatic
    public fun focused(label: String?, role: A11yRole, selected: Boolean?, checked: Boolean?) {
        if (label.isNullOrBlank()) return
        say(describe(label, role, selected, checked), true)
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun isKeyboardMode(): Boolean = A11y.keyboardMode

@ExperimentalOneConfigUiApi
@Composable
public fun Announce(text: String?) {
    LaunchedEffect(text) { text?.let(A11y::announce) }
}

private val RING_WIDTH = 2.dp
private val HALO_WIDTH = 1.dp
private const val HALO_ALPHA = 0.7f

@ExperimentalOneConfigUiApi
public fun Modifier.focusRing(): Modifier = focusRing(null)

@ExperimentalOneConfigUiApi
public fun Modifier.focusRing(shape: Shape?): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val theme = OneConfigTheme.current
    val ring = theme.focusRingColor
    val halo = when {
        theme.style != ThemeStyle.Pixel -> theme.pageBackground
        theme.isLight -> Color.White
        else -> Color.Black
    }.copy(alpha = HALO_ALPHA)
    val outline = shape ?: theme.buttonShape
    onFocusChanged { focused = it.isFocused }.drawWithContent {
        drawContent()
        if (focused && A11y.keyboardMode) drawFocusRing(outline, ring, halo)
    }
}

private fun ContentDrawScope.drawFocusRing(shape: Shape, ring: Color, halo: Color) {
    val ringPx = RING_WIDTH.toPx()
    val haloPx = HALO_WIDTH.toPx()
    inset(ringPx / 2f) {
        drawOutline(shape.createOutline(size, layoutDirection, this), ring, style = Stroke(ringPx))
    }
    inset(ringPx + haloPx / 2f) {
        drawOutline(shape.createOutline(size, layoutDirection, this), halo, style = Stroke(haloPx))
    }
}

@ExperimentalOneConfigUiApi
public fun Modifier.a11y(label: String?, role: A11yRole): Modifier = a11y(label, role, null, null, null, true)

@ExperimentalOneConfigUiApi
public fun Modifier.a11y(
    label: String?,
    role: A11yRole,
    selected: Boolean?,
    checked: Boolean?,
    shape: Shape?,
    ring: Boolean,
): Modifier = composed {
    val spoken by rememberUpdatedState(label)
    val state by rememberUpdatedState(selected to checked)
    var focused by remember { mutableStateOf(false) }
    var spokenState by remember { mutableStateOf(selected to checked) }
    LaunchedEffect(selected, checked) {
        val now = selected to checked
        if (now == spokenState) return@LaunchedEffect
        spokenState = now
        if (focused && A11y.keyboardMode) A11y.focused(spoken, role, selected, checked)
    }
    semantics {
        label?.let { contentDescription = it }
        role.semanticsRole?.let { this.role = it }
        selected?.let { this.selected = it }
        checked?.let { toggleableState = ToggleableState(it) }
    }.onFocusChanged {
        if (it.isFocused == focused) return@onFocusChanged
        focused = it.isFocused
        if (focused && A11y.keyboardMode) A11y.focused(spoken, role, state.first, state.second)
    }.then(if (ring) Modifier.focusRing(shape) else Modifier)
}

@ExperimentalOneConfigUiApi
public fun Modifier.a11yField(label: String?): Modifier = a11y(label, A11yRole.TextField, null, null, null, false)

@ExperimentalOneConfigUiApi
public fun Modifier.notFocusable(): Modifier = focusProperties { canFocus = false }
