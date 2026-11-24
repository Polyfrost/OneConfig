@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
public object PanelHeaderDefaults {
    public val iconSize: Dp = 24.dp
}

@ExperimentalOneConfigUiApi
@Composable
public fun PanelHeader(
    title: String? = null,
    modifier: Modifier = Modifier,
    icon: String? = null,
    subtitle: String? = null,
    onClose: (() -> Unit)? = null,
    closeLabel: String = layoutString("oneconfig.ui.panel.close", "Close"),
    onBack: (() -> Unit)? = null,
    backLabel: String? = null,
    iconTint: Color = OneConfigTheme.accent,
    titleStyle: TextStyle = OneConfigTheme.typography.titleMedium,
    actions: (@Composable RowScope.() -> Unit)? = null,
    primaryAction: (@Composable RowScope.() -> Unit)? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    spacing: Dp? = null,
    titleWeight: Boolean = true,
) {
    PanelHeader(
        title?.let { AnnotatedString(it) },
        modifier,
        icon,
        subtitle?.let { AnnotatedString(it) },
        onClose,
        closeLabel,
        onBack,
        backLabel,
        iconTint,
        titleStyle,
        actions,
        primaryAction,
        leading,
        spacing,
        titleWeight,
    )
}

@ExperimentalOneConfigUiApi
@Composable
public fun PanelHeader(
    title: AnnotatedString?,
    modifier: Modifier = Modifier,
    icon: String? = null,
    subtitle: AnnotatedString? = null,
    onClose: (() -> Unit)? = null,
    closeLabel: String = layoutString("oneconfig.ui.panel.close", "Close"),
    onBack: (() -> Unit)? = null,
    backLabel: String? = null,
    iconTint: Color = OneConfigTheme.accent,
    titleStyle: TextStyle = OneConfigTheme.typography.titleMedium,
    actions: (@Composable RowScope.() -> Unit)? = null,
    primaryAction: (@Composable RowScope.() -> Unit)? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    spacing: Dp? = null,
    titleWeight: Boolean = true,
) {
    val theme = OneConfigTheme.current
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing ?: theme.spacing.l),
    ) {
        if (onBack != null) {
            if (backLabel != null) {
                TextButton(backLabel, "left-arrow", onBack)
            } else {
                IconButton("left-arrow", layoutString("oneconfig.ui.panel.back", "Back"), onBack)
            }
        }
        leading?.invoke(this)
        if (icon != null) Icon(icon, null, Modifier.size(PanelHeaderDefaults.iconSize), iconTint)
        if (title != null || subtitle != null) {
            Column((if (titleWeight) Modifier.weight(1f) else Modifier).semantics(mergeDescendants = true) { heading() }) {
                if (title != null) LayoutText(title, titleStyle, theme.textColor, maxLines = 1)
                if (subtitle != null) LayoutText(subtitle, theme.typography.small, theme.textColorSecondary, maxLines = 2)
            }
        } else if (titleWeight) {
            Spacer(Modifier.weight(1f))
        }
        actions?.invoke(this)
        primaryAction?.invoke(this)
        if (onClose != null) IconButton("close", closeLabel, onClose)
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun PanelSection(
    title: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier) {
        SectionHeader(title, Modifier.padding(bottom = OneConfigTheme.current.spacing.xs), icon = icon, trailing = trailing)
        content()
    }
}

@ExperimentalOneConfigUiApi
public enum class PaneSide {
    Start,

    End,
}

@ExperimentalOneConfigUiApi
public object MasterDetailDefaults {
    public val listWidth: Dp = 320.dp

    public val stackedListHeight: Dp = 230.dp
}

@ExperimentalOneConfigUiApi
@Composable
public fun MasterDetailLayout(
    list: @Composable () -> Unit,
    detail: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    listWidth: Dp = MasterDetailDefaults.listWidth,
    listSide: PaneSide = PaneSide.Start,
    detailWidth: Dp = Dp.Unspecified,
    gap: Dp = OneConfigTheme.current.spacing.xl,
    divider: Boolean = false,
    stackBelow: Dp = Dp.Unspecified,
    stackedListHeight: Dp = MasterDetailDefaults.stackedListHeight,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (stackBelow.isSpecified && maxWidth < stackBelow) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                val listPane: @Composable () -> Unit = { Box(Modifier.fillMaxWidth().height(stackedListHeight)) { list() } }
                if (listSide == PaneSide.Start) listPane()
                Box(Modifier.fillMaxWidth().weight(1f)) { detail() }
                if (listSide == PaneSide.End) listPane()
            }
            return@BoxWithConstraints
        }
        val fixedDetail = detailWidth.isSpecified
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            val listPane: @Composable RowScope.() -> Unit = {
                Box(if (fixedDetail) Modifier.weight(1f).fillMaxHeight() else Modifier.width(listWidth).fillMaxHeight()) { list() }
            }
            val detailPane: @Composable RowScope.() -> Unit = {
                Box(if (fixedDetail) Modifier.width(detailWidth).fillMaxHeight() else Modifier.weight(1f).fillMaxHeight()) { detail() }
            }
            if (listSide == PaneSide.Start) listPane() else detailPane()
            if (divider) Divider(vertical = true)
            if (listSide == PaneSide.Start) detailPane() else listPane()
        }
    }
}
