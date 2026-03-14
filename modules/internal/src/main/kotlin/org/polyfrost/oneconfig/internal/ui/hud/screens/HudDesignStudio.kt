package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.Designer
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.Settings
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.IconButton
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

enum class StudioCategory(val title: String, val icon: String) {
    Designer("Designer", "paintbrush"),
    Settings("HUD Settings", "qol");
}

@Composable
fun HudDesignStudio() = Box(Modifier.fillMaxSize()) {
    var activeCategory by remember { mutableStateOf(StudioCategory.Designer) }

    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(500.dp)
            .padding(16.dp)
            .background(Color(17, 23, 28).copy(0.95f),
                RoundedCornerShape(16.dp)
            )
            .border(1.dp, Color.White.copy(.10f),RoundedCornerShape(16.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton("left-arrow") {}
                    SearchBar()
                }
                Text("HUD Design Studio", color = LocalTheme.current.textColor, fontSize = 24.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StudioCategory.entries.forEach {
                        Chip(it.title, it == activeCategory, it.icon) { activeCategory = it }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (activeCategory) {
                        StudioCategory.Designer -> Designer()
                        StudioCategory.Settings -> Settings()
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar() {
    var searchText by remember { mutableStateOf("") }
    val interactionSource = rememberInteractionSource()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        if (isFocused) Color.White.copy(.20f) else Color.White.copy(.10f)
    )
    val iconColor by animateColorAsState(
        if (isFocused) Color(223, 234, 255).copy(0.70f) else Color(223, 234, 255).copy(0.50f)
    )

    BasicTextField(
        searchText,
        { searchText = it },
        interactionSource = interactionSource,
        textStyle = TextStyle(
            color = iconColor, fontSize = 12.sp,
            fontFamily = LocalTheme.current.typography.family
        ),
        cursorBrush = SolidColor(iconColor)
    ) { innerTextField ->
        Row(
            modifier = Modifier.size(256.dp, 32.dp)
                .border(1.dp, borderColor,RoundedCornerShape(8.dp))
                .background(Color(35, 45, 50).copy(0.95f), RoundedCornerShape(8.dp)),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon("search", color = iconColor, modifier = Modifier.padding(start = 8.dp).size(12.dp))
            Box {
                if (!isFocused && searchText.isEmpty())
                    Text("Search for something...", color = iconColor, fontSize = 12.sp)
                innerTextField()
            }
        }
    }
}