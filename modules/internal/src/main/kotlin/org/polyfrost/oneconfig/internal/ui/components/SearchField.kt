package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun searchFieldTextStyle(): TextStyle {
    val theme = LocalTheme.current
    return TextStyle(
        color = theme.textColorSecondary,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        fontFamily = theme.typography.family,
        lineHeight = 12.sp,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
}

/** The search icon, [placeholder] and clear button drawn around a search field's [innerTextField] */
@Composable
fun SearchFieldFrame(
    placeholder: String,
    showPlaceholder: Boolean,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier,
    innerTextField: @Composable () -> Unit,
) {
    val theme = LocalTheme.current
    Row(
        modifier = modifier
            .liquidGlass(theme.sideBarNavigationEntryShape)
            .background(theme.chipBackground, theme.sideBarNavigationEntryShape)
            .border(1.dp, theme.borderColor, theme.sideBarNavigationEntryShape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon("search", color = theme.textColorSecondary, modifier = Modifier.size(14.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (showPlaceholder) {
                Text(placeholder, color = theme.textColorSecondary, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 12.sp)
            }
            innerTextField()
        }
        if (onClear != null) IconButton("close", modifier = Modifier.size(16.dp), onClick = onClear)
    }
}
