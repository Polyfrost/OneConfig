@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
public sealed interface ShortcutInput {
    @Immutable
    public class Key(public val label: String) : ShortcutInput

    public object Arrows : ShortcutInput

    @Immutable
    public class Mouse(public val gesture: MouseGesture, public val label: String) : ShortcutInput

    @Immutable
    public class Label(public val text: String) : ShortcutInput
}

@ExperimentalOneConfigUiApi
@Immutable
public class Shortcut(
    public val description: String,
    public val inputs: List<ShortcutInput>,
) {
    public constructor(description: String, vararg keys: String) : this(description, keys.map { ShortcutInput.Key(it) })
}

@ExperimentalOneConfigUiApi
@Immutable
public class ShortcutGroup(
    public val title: String?,
    public val shortcuts: List<Shortcut>,
)

@ExperimentalOneConfigUiApi
public enum class ShortcutSheetDensity {
    Comfortable,

    Compact,
}

@ExperimentalOneConfigUiApi
public object ShortcutSheetDefaults {
    public val rowMinHeight: Dp = 46.dp

    public val compactKeyHeight: Dp = 18.dp

    public val dialogWidth: Dp = 820.dp

    public const val dividerAlpha: Float = 0.5f

    public val title: String
        get() = layoutString("oneconfig.ui.shortcuts.title", "Keyboard shortcuts")
}

@ExperimentalOneConfigUiApi
public fun shortcutKeys(name: String): List<ShortcutInput> =
    name.split("+").map { it.trim() }.filter { it.isNotEmpty() }.map { ShortcutInput.Key(it) }

@ExperimentalOneConfigUiApi
@Composable
public fun ShortcutKeys(
    inputs: List<ShortcutInput>,
    modifier: Modifier = Modifier,
    density: ShortcutSheetDensity = ShortcutSheetDensity.Comfortable,
) {
    val theme = OneConfigTheme.current
    val spacing = theme.spacing
    val compact = density == ShortcutSheetDensity.Compact
    val keyHeight = if (compact) ShortcutSheetDefaults.compactKeyHeight else null
    val secondary = theme.textColorSecondary
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) spacing.xs else spacing.s),
    ) {
        inputs.forEachIndexed { i, input ->
            val joined = i > 0 && input !is ShortcutInput.Label && inputs[i - 1] !is ShortcutInput.Label
            if (joined) LayoutText("+", theme.typography.caption, secondary, maxLines = 1)
            when (input) {
                is ShortcutInput.Key -> KeyCap(input.label, height = keyHeight)
                ShortcutInput.Arrows -> if (compact) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                        for (direction in ArrowDirection.entries) ArrowKey(direction, size = keyHeight)
                    }
                } else {
                    ArrowKeys()
                }
                is ShortcutInput.Mouse -> if (compact) {
                    KeyCap(input.label, height = keyHeight)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                        MouseGlyph(input.gesture)
                        LayoutText(input.label, theme.typography.caption, secondary, maxLines = 1)
                    }
                }
                is ShortcutInput.Label -> LayoutText(input.text, theme.typography.caption, secondary, maxLines = 1)
            }
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun ShortcutSheet(
    groups: List<ShortcutGroup>,
    modifier: Modifier = Modifier,
    columns: Int = 1,
    density: ShortcutSheetDensity = ShortcutSheetDensity.Comfortable,
    columnGap: Dp = OneConfigTheme.current.spacing.xxl * 2,
) {
    val visible = groups.filter { it.shortcuts.isNotEmpty() }
    if (visible.isEmpty()) return
    val split = columns.coerceIn(1, visible.size)
    if (split == 1) {
        ShortcutColumn(visible, modifier, density)
        return
    }
    val perColumn = (visible.size + split - 1) / split
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(columnGap)) {
        for (chunk in visible.chunked(perColumn)) ShortcutColumn(chunk, Modifier.weight(1f), density)
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun ShortcutSheetPanel(
    groups: List<ShortcutGroup>,
    modifier: Modifier = Modifier,
    title: String? = null,
    columns: Int = 1,
    density: ShortcutSheetDensity = ShortcutSheetDensity.Compact,
) {
    val theme = OneConfigTheme.current
    val spacing = theme.spacing
    Column(
        modifier
            .themedSurface(SurfaceKind.Popup)
            .padding(horizontal = spacing.l, vertical = spacing.m),
        verticalArrangement = Arrangement.spacedBy(spacing.s),
    ) {
        if (title != null) LayoutText(title, theme.typography.label, theme.textColor, maxLines = 1)
        ShortcutSheet(groups, columns = columns, density = density)
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun ShortcutSheetDialog(
    groups: List<ShortcutGroup>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = ShortcutSheetDefaults.title,
    columns: Int = 2,
    width: Dp = ShortcutSheetDefaults.dialogWidth,
    closeLabel: String = layoutString("oneconfig.ui.shortcuts.close", "Close"),
) {
    val spacing = OneConfigTheme.current.spacing
    ModalDialog(
        onDismiss,
        modifier,
        width = width,
        scrollBody = false,
        title = {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.l),
            ) {
                DialogTitle(title, Modifier.weight(1f))
                IconButton("close", closeLabel, onDismiss, size = ControlSize.Compact)
            }
        },
    ) {
        ShortcutSheet(groups, Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), columns)
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun ShortcutHint(
    label: String,
    inputs: List<ShortcutInput>,
    modifier: Modifier = Modifier,
) {
    val theme = OneConfigTheme.current
    Row(
        modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.m),
    ) {
        LayoutText(label, theme.typography.caption, theme.textColorSecondary, maxLines = 1)
        if (inputs.isNotEmpty()) ShortcutKeys(inputs)
    }
}

@Composable
private fun ShortcutColumn(groups: List<ShortcutGroup>, modifier: Modifier, density: ShortcutSheetDensity) {
    val spacing = OneConfigTheme.current.spacing
    val compact = density == ShortcutSheetDensity.Compact
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (compact) spacing.m else spacing.xxl + spacing.xs)) {
        for (group in groups) ShortcutGroupBlock(group, density)
    }
}

@Composable
private fun ShortcutGroupBlock(group: ShortcutGroup, density: ShortcutSheetDensity) {
    val theme = OneConfigTheme.current
    val spacing = theme.spacing
    val compact = density == ShortcutSheetDensity.Compact
    Column(verticalArrangement = if (compact) Arrangement.spacedBy(spacing.xxs) else Arrangement.Top) {
        if (group.title != null) {
            LayoutText(group.title.uppercase(), theme.typography.eyebrow, theme.textColorSecondary, Modifier.padding(bottom = if (compact) 0.dp else spacing.s), maxLines = 1)
        }
        group.shortcuts.forEachIndexed { i, shortcut ->
            if (compact) CompactShortcutRow(shortcut) else ShortcutRow(shortcut, divider = i > 0)
        }
    }
}

@Composable
private fun ShortcutRow(shortcut: Shortcut, divider: Boolean) {
    val theme = OneConfigTheme.current
    val spacing = theme.spacing
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        if (divider) {
            val line = theme.borderColor
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(line.copy(alpha = line.alpha * ShortcutSheetDefaults.dividerAlpha)))
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = ShortcutSheetDefaults.rowMinHeight).padding(vertical = spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            LayoutText(shortcut.description, theme.typography.small, theme.textColor, Modifier.weight(1f), maxLines = 2)
            ShortcutKeys(shortcut.inputs)
        }
    }
}

@Composable
private fun CompactShortcutRow(shortcut: Shortcut) {
    val theme = OneConfigTheme.current
    Row(
        Modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.m),
    ) {
        ShortcutKeys(shortcut.inputs, density = ShortcutSheetDensity.Compact)
        Text(shortcut.description, color = theme.textColorSecondary, style = theme.typography.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
