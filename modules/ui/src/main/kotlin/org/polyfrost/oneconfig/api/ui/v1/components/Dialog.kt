@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
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

private const val SCRIM_FADE_MILLIS = 150

private val WindowOrigin = PointPosition(IntOffset.Zero, false)

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
@Composable
public fun ModalDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = DialogDefaults.width,
    title: (@Composable () -> Unit)? = null,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    dismissOnScrim: Boolean = true,
    dismissOnEscape: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    DialogLayer(onDismiss, dismissOnScrim, dismissOnEscape) {
        ModalPanel(modifier.heightIn(max = popupMaxHeight()), width) {
            title?.invoke()
            content()
            if (buttons != null) DialogButtons(content = buttons)
        }
    }
}

@Composable
private fun DialogLayer(onDismiss: () -> Unit, dismissOnScrim: Boolean, dismissOnEscape: Boolean, panel: @Composable () -> Unit) {
    if (dismissOnEscape) EscapeCloses(onDismiss)
    val latest by rememberUpdatedState(onDismiss)
    val scrimColor = OneConfigTheme.current.scrimColor
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fade.animateTo(1f, Motion.finiteSpec(tween(SCRIM_FADE_MILLIS))) }
    componentsBridge().Popup(WindowOrigin, null, true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(scrimColor, alpha = fade.value) }
                .then(
                    if (dismissOnScrim) {
                        Modifier.notFocusable().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { latest() }
                    } else {
                        Modifier.swallowClicks()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.graphicsLayer { alpha = fade.value }) {
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
    Column(
        modifier = componentsBridge().popupSurface(sized, OneConfigTheme.current.popupShape)
            .swallowClicks()
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

@ExperimentalOneConfigUiApi
@Composable
public fun DialogTitle(text: String, modifier: Modifier = Modifier) {
    Announce(text)
    val theme = OneConfigTheme.current
    Text(text, modifier.semantics { heading() }, theme.textColor, theme.typography.title)
}

@ExperimentalOneConfigUiApi
@Composable
public fun DialogButtons(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(DialogDefaults.buttonSpacing, Alignment.End),
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
    modifier: Modifier = Modifier,
    dismissLabel: String = DialogDefaults.cancelLabel,
    style: ControlStyle = ControlStyle.Primary,
    danger: Boolean = false,
    icon: String? = null,
    confirmEnabled: Boolean = true,
) {
    val confirmStyle = if (danger) ControlStyle.Danger else style
    ModalDialog(
        onDismiss,
        modifier,
        title = { DialogTitle(title) },
        buttons = {
            TextButton(dismissLabel, onDismiss)
            if (confirmLabel != null) {
                if (icon != null) {
                    TextButton(confirmLabel, icon, onConfirm, style = confirmStyle, enabled = confirmEnabled)
                } else {
                    TextButton(confirmLabel, onConfirm, style = confirmStyle, enabled = confirmEnabled)
                }
            }
        },
    ) {
        DialogBody(text)
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
public fun ModalDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalDialog(onDismiss, Modifier, content = content)
}

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
public fun DialogTitle(text: String) {
    DialogTitle(text, Modifier)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun DialogButtons(content: @Composable RowScope.() -> Unit) {
    DialogButtons(Modifier, content)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(title, text, confirmLabel, onConfirm, onDismiss, Modifier)
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
