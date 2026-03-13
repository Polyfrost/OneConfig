package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun Header() {
    val controller = LocalNavController.wrapper

    Row(
        modifier = Modifier.fillMaxWidth().height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton("left-arrow") { controller.back() }
                IconButton("right-arrow") { controller.forward() }
            }
            AnimatedContent(
                ShellState.title,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }
            ) { text ->
                text?.let {
                    Text(it, color = LocalTheme.current.textColor, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            SearchBar()
            IconButton("close") { /* todo: close the window */ }
        }
    }
}

@Composable
fun SearchBar() {
    var searchText by remember { mutableStateOf("") }

    val interactionSource = rememberInteractionSource()

    val isFocused by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = searchText,
        onValueChange = { searchText = it },
        cursorBrush = SolidColor(LocalTheme.current.textColor),
        textStyle = TextStyle(
            color = LocalTheme.current.textColor,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            fontFamily = LocalTheme.current.typography.family,
            baselineShift = BaselineShift(-0.2f)
        ),
        interactionSource = interactionSource
    ) { innerTextField ->

        Box(
            modifier = Modifier
                .size(256.dp, 32.dp)
                .background(
                    LocalTheme.current.chipBackground,
                    LocalTheme.current.sideBarNavigationEntryShape
                )
                .border(
                    1.dp,
                    LocalTheme.current.borderColor,
                    LocalTheme.current.sideBarNavigationEntryShape
                ),
            contentAlignment = Alignment.CenterStart
        ) {

            Row(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon("search", color = LocalTheme.current.textColorSecondary)

                Box {
                    if (searchText.isEmpty() && !isFocused) {
                        Text(
                            "Search for ...",
                            color = LocalTheme.current.textColorSecondary,
                            shift = BaselineShift(+0.2f),
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    innerTextField()
                }
            }
        }
    }
}