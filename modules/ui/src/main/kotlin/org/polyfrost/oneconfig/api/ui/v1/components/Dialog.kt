@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Announce
import org.polyfrost.oneconfig.api.ui.v1.a11y.notFocusable
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private val DIALOG_WIDTH = 420.dp
private val DIALOG_PADDING = PaddingValues(20.dp)
private val DIALOG_SPACING = 12.dp
private val SCRIM = Color.Black.copy(alpha = 0.45f)

@ExperimentalOneConfigUiApi
@Composable
public fun ModalDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalDialog(onDismiss, DIALOG_WIDTH, Dp.Unspecified, DIALOG_PADDING, DIALOG_SPACING, true, content)
}

@ExperimentalOneConfigUiApi
@Composable
public fun ModalDialog(onDismiss: () -> Unit, width: Dp, dismissOnScrim: Boolean, content: @Composable ColumnScope.() -> Unit) {
    ModalDialog(onDismiss, width, Dp.Unspecified, DIALOG_PADDING, DIALOG_SPACING, dismissOnScrim, content)
}

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
    EscapeCloses(onDismiss)
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = dismissOnScrim),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SCRIM)
                .then(
                    if (dismissOnScrim) {
                        Modifier.notFocusable().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                    } else {
                        Modifier.swallowClicks()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            ModalPanel(width, height, padding, spacing, Modifier, content)
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun ModalPanel(width: Dp, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    ModalPanel(width, Dp.Unspecified, DIALOG_PADDING, DIALOG_SPACING, modifier, content)
}

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
public fun DialogTitle(text: String) {
    Announce(text)
    Text(text, Modifier, OneConfigTheme.current.textColor, 18.sp, null, BaselineShift.None, FontWeight.SemiBold, TextAlign.Start, Int.MAX_VALUE, TextOverflow.Clip)
}

@ExperimentalOneConfigUiApi
@Composable
public fun DialogButtons(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), content = content)
}

@ExperimentalOneConfigUiApi
@Composable
public fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(title, text, confirmLabel, cancelLabel(), null, true, true, onConfirm, onDismiss)
}

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
    ModalDialog(onDismiss) {
        DialogTitle(title)
        Text(text, Modifier, OneConfigTheme.current.textColorSecondary, 14.sp, null, BaselineShift.None, FontWeight.Normal, TextAlign.Start, Int.MAX_VALUE, TextOverflow.Clip)
        DialogButtons {
            TextButton(dismissLabel, null, Modifier, ButtonStyle.Secondary, true, onDismiss)
            if (confirmLabel != null) {
                TextButton(confirmLabel, icon, Modifier, if (danger) ButtonStyle.Danger else ButtonStyle.Primary, confirmEnabled, onConfirm)
            }
        }
    }
}

private fun cancelLabel(): String = runCatching { Platform.i18n().translateString("gui.cancel") }.getOrNull()?.takeIf { it != "gui.cancel" } ?: "Cancel"
