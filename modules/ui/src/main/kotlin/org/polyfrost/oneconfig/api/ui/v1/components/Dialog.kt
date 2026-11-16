@file:OptIn(ExperimentalOneConfigUiApi::class, ExperimentalVersionOverloading::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Announce
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.a11y.notFocusable
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private const val PANEL_HIDDEN_SCALE = 0.96f

private val WindowOrigin = PointPosition(IntOffset.Zero, false)

@ExperimentalOneConfigUiApi
public val LocalAnnounceTitles: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }

@ExperimentalOneConfigUiApi
public object DialogDefaults {
    public val width: Dp = 420.dp

    public val padding: PaddingValues = PaddingValues(20.dp)

    public val spacing: Dp = 12.dp

    public val buttonSpacing: Dp = 8.dp

    public val cancelLabel: String
        get() = kitString("gui.cancel", "Cancel")

    public val saveLabel: String
        get() = kitString("oneconfig.ui.dialog.save", "Save")
}

@ExperimentalOneConfigUiApi
public enum class DialogButtonLayout {
    End,

    Fill,
}

@ExperimentalOneConfigUiApi
@Composable
public fun ModalDialog(
    onDismiss: () -> Unit,
    @IntroducedAt("1.8.0") modifier: Modifier = Modifier,
    @IntroducedAt("1.8.0") width: Dp = DialogDefaults.width,
    @IntroducedAt("1.8.0") title: (@Composable () -> Unit)? = null,
    @IntroducedAt("1.8.0") buttons: (@Composable RowScope.() -> Unit)? = null,
    @IntroducedAt("1.8.0") dismissOnScrim: Boolean = true,
    @IntroducedAt("1.8.0") dismissOnEscape: Boolean = true,
    @IntroducedAt("1.8.0") padding: PaddingValues = DialogDefaults.padding,
    @IntroducedAt("1.8.0") spacing: Dp = DialogDefaults.spacing,
    @IntroducedAt("1.8.0") scrimColor: Color = Color.Unspecified,
    @IntroducedAt("1.8.0") buttonLayout: DialogButtonLayout = DialogButtonLayout.End,
    @IntroducedAt("1.8.0") scrollBody: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    DialogLayer(onDismiss, dismissOnScrim, dismissOnEscape, scrimColor) {
        ModalPanel(modifier.heightIn(max = popupMaxHeight()), width, padding = padding, spacing = spacing) {
            title?.invoke()
            if (scrollBody) ScrollingDialogBody(spacing, content) else content()
            if (buttons != null) DialogButtons(Modifier, buttonLayout, buttons)
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun ModalDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = DialogDefaults.width,
    title: (@Composable () -> Unit)? = null,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    dismissOnScrim: Boolean = true,
    dismissOnEscape: Boolean = true,
    padding: PaddingValues = DialogDefaults.padding,
    spacing: Dp = DialogDefaults.spacing,
    scrimColor: Color = Color.Unspecified,
    buttonLayout: DialogButtonLayout = DialogButtonLayout.End,
    scrollBody: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val fade = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) {
        if (visible) {
            shown = true
            fade.animateTo(1f, Motion.finiteSpec(tween(Motion.standard)))
        } else {
            fade.animateTo(0f, Motion.finiteSpec(tween(Motion.fast)))
            shown = false
        }
    }
    if (!visible && !shown) return
    DialogLayer(fade, visible, onDismiss, dismissOnScrim, dismissOnEscape, scrimColor) {
        ModalPanel(modifier.heightIn(max = popupMaxHeight()), width, padding = padding, spacing = spacing) {
            title?.invoke()
            if (scrollBody) ScrollingDialogBody(spacing, content) else content()
            if (buttons != null) DialogButtons(Modifier, buttonLayout, buttons)
        }
    }
}

@Composable
private fun ColumnScope.ScrollingDialogBody(spacing: Dp, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    var overflowing by remember { mutableStateOf(false) }
    Column(
        Modifier
            .weight(1f, fill = false)
            .layout { measurable, constraints ->
                if (constraints.hasBoundedHeight) {
                    val natural = runCatching { measurable.maxIntrinsicHeight(constraints.maxWidth) }.getOrNull()
                    val over = natural != null && natural > constraints.maxHeight
                    if (over != overflowing) overflowing = over
                }
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .then(if (overflowing) Modifier.smoothWheelScroll(scroll).verticalScroll(scroll) else Modifier),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

@Composable
private fun DialogLayer(
    onDismiss: () -> Unit,
    dismissOnScrim: Boolean,
    dismissOnEscape: Boolean,
    scrimColor: Color = Color.Unspecified,
    panel: @Composable () -> Unit,
) {
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fade.animateTo(1f, Motion.finiteSpec(tween(Motion.standard))) }
    DialogLayer(fade, true, onDismiss, dismissOnScrim, dismissOnEscape, scrimColor, panel)
}

@Composable
private fun DialogLayer(
    fade: Animatable<Float, AnimationVector1D>,
    interactive: Boolean,
    onDismiss: () -> Unit,
    dismissOnScrim: Boolean,
    dismissOnEscape: Boolean,
    scrimColor: Color,
    panel: @Composable () -> Unit,
) {
    if (interactive) {
        if (dismissOnEscape) EscapeCloses(onDismiss)
        PopupSounds()
    }
    val latest by rememberUpdatedState(onDismiss)
    val scrim = if (scrimColor.isSpecified) scrimColor else OneConfigTheme.current.scrimColor
    componentsBridge().Popup(WindowOrigin, null, interactive) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(scrim, alpha = fade.value) }
                .then(
                    if (!interactive) {
                        Modifier.pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                            }
                        }
                    } else if (dismissOnScrim) {
                        Modifier.notFocusable().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { latest() }
                    } else {
                        Modifier.swallowClicks()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.graphicsLayer {
                    val progress = fade.value
                    val scale = PANEL_HIDDEN_SCALE + (1f - PANEL_HIDDEN_SCALE) * progress
                    alpha = progress
                    scaleX = scale
                    scaleY = scale
                },
            ) {
                panel()
            }
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun ModalPanel(
    modifier: Modifier = Modifier,
    width: Dp = DialogDefaults.width,
    height: Dp = Dp.Unspecified,
    padding: PaddingValues = DialogDefaults.padding,
    spacing: Dp = DialogDefaults.spacing,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sized = modifier
        .width(width)
        .then(if (height == Dp.Unspecified) Modifier else Modifier.height(height))
    CompositionLocalProvider(LocalAnnounceTitles provides true) {
        Column(
            modifier = componentsBridge().popupSurface(sized, OneConfigTheme.current.popupShape)
                .swallowClicks()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun DialogTitle(text: String, @IntroducedAt("1.8.0") modifier: Modifier = Modifier) {
    Announce(text)
    val theme = OneConfigTheme.current
    Text(text, modifier.semantics { heading() }, theme.textColor, theme.typography.title)
}

@ExperimentalOneConfigUiApi
@Composable
public fun DialogButtons(@IntroducedAt("1.8.0") modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    DialogButtons(modifier, DialogButtonLayout.End, content)
}

@ExperimentalOneConfigUiApi
@Composable
public fun DialogButtons(modifier: Modifier, layout: DialogButtonLayout, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = when (layout) {
            DialogButtonLayout.End -> Arrangement.spacedBy(DialogDefaults.buttonSpacing, Alignment.End)
            DialogButtonLayout.Fill -> Arrangement.spacedBy(DialogDefaults.buttonSpacing)
        },
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@ExperimentalOneConfigUiApi
@Composable
public fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    @IntroducedAt("1.8.0") modifier: Modifier = Modifier,
    @IntroducedAt("1.8.0") dismissLabel: String = DialogDefaults.cancelLabel,
    @IntroducedAt("1.8.0") style: ControlStyle = ControlStyle.Primary,
    @IntroducedAt("1.8.0") danger: Boolean = false,
    @IntroducedAt("1.8.0") icon: String? = null,
    @IntroducedAt("1.8.0") confirmEnabled: Boolean = true,
) {
    ModalDialog(
        onDismiss,
        modifier,
        title = { DialogTitle(title) },
        buttons = { ConfirmButtons(confirmLabel, dismissLabel, onConfirm, onDismiss, if (danger) ControlStyle.Danger else style, icon, confirmEnabled) },
    ) {
        DialogBody(text)
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun ConfirmDialog(
    visible: Boolean,
    title: String,
    text: String,
    confirmLabel: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = DialogDefaults.cancelLabel,
    style: ControlStyle = ControlStyle.Primary,
    danger: Boolean = false,
    icon: String? = null,
    confirmEnabled: Boolean = true,
) {
    ModalDialog(
        visible,
        onDismiss,
        modifier,
        title = { DialogTitle(title) },
        buttons = { ConfirmButtons(confirmLabel, dismissLabel, onConfirm, onDismiss, if (danger) ControlStyle.Danger else style, icon, confirmEnabled) },
    ) {
        DialogBody(text)
    }
}

@Composable
private fun ConfirmButtons(
    confirmLabel: String?,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmStyle: ControlStyle,
    icon: String?,
    confirmEnabled: Boolean,
) {
    TextButton(dismissLabel, onDismiss)
    if (confirmLabel != null) {
        if (icon != null) {
            TextButton(confirmLabel, icon, onConfirm, style = confirmStyle, enabled = confirmEnabled)
        } else {
            TextButton(confirmLabel, onConfirm, style = confirmStyle, enabled = confirmEnabled)
        }
    }
}

@Composable
internal fun DialogBody(text: String) {
    val theme = OneConfigTheme.current
    Text(text, Modifier, theme.textColorSecondary, theme.typography.body)
}

internal fun kitString(key: String, fallback: String, vararg args: Any): String =
    runCatching { Platform.i18n().translateString(key, *args) }.getOrNull()?.takeIf { it != key } ?: fallback

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun ModalDialog(onDismiss: () -> Unit, width: Dp, dismissOnScrim: Boolean, content: @Composable ColumnScope.() -> Unit) {
    ModalDialog(onDismiss, Modifier, width, dismissOnScrim = dismissOnScrim, content = content)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun ModalDialog(
    onDismiss: () -> Unit,
    width: Dp,
    height: Dp,
    padding: PaddingValues,
    spacing: Dp,
    dismissOnScrim: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    DialogLayer(onDismiss, dismissOnScrim, true) {
        ModalPanel(Modifier, width, height, padding, spacing, content)
    }
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun ModalPanel(width: Dp, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    ModalPanel(modifier, width, content = content)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun ModalPanel(
    width: Dp,
    height: Dp,
    padding: PaddingValues,
    spacing: Dp,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalPanel(modifier, width, height, padding, spacing, content)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String?,
    dismissLabel: String,
    icon: String?,
    danger: Boolean,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(title, text, confirmLabel, onConfirm, onDismiss, Modifier, dismissLabel, ControlStyle.Primary, danger, icon, confirmEnabled)
}
