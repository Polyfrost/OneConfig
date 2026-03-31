package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.api.ProfileData
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

enum class ProfileCategory(val title: String, val icon: String?) {
    All("All profiles", null),
    Favorited("Favorites", "star"),
}

@Composable
fun Profiles() {
    var activeCategory by remember { mutableStateOf(ProfileCategory.All) }

    Column(
        verticalArrangement = Arrangement.spacedBy(19.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileCategory.entries.forEach {
                Chip(
                    label = it.title,
                    selected = activeCategory == it,
                    icon = it.icon,
                    onClick = { activeCategory = it }
                )
            }
        }
        ProfilesGrid(activeCategory)
    }
}

private data class TestProfileData(
    override val name: String = "Default",
    override val icon: String? = null,
) : ProfileData {
    override var liked by mutableStateOf(false)
    override var selected by mutableStateOf(false)
}

@Composable
fun ColumnScope.ProfilesGrid(category: ProfileCategory) {
    LazyVerticalGrid(
        modifier = Modifier.weight(1f),
        columns = GridCells.Fixed(5),
        verticalArrangement = Arrangement.spacedBy(19.dp),
        horizontalArrangement = Arrangement.spacedBy(19.dp),
    ) {
        val count = if (category == ProfileCategory.Favorited) 3 else 8
        repeat(count) {
            item {
                ProfileCard(TestProfileData(name = "Profile ${it + 1}"))
            }
        }
    }
}

@Composable
fun ProfileCard(profile: ProfileData) {
    val interactionSource = rememberInteractionSource()
    val starInteractionSource = rememberInteractionSource()

    val theme = LocalTheme.current
    val shape = theme.modCardShape

    val selectionBorderColor by animateColorAsState(
        if (profile.selected) Accent else Color.Transparent
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(theme.modCardBackground, shape)
            .border(3.dp, selectionBorderColor, shape)
            .border(
                1.dp, Brush.verticalGradient(
                    listOf(theme.borderColor, theme.borderColor.copy(0f))
                ), shape
            )
            .onClick(interactionSource) { profile.selected = !profile.selected }
            .clip(shape)
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        val vignetteColor = theme.textColor
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val gradient = Brush.radialGradient(
                        colors = listOf(
                            vignetteColor.copy(alpha = 0f),
                            vignetteColor.copy(alpha = 0.02f),
                            vignetteColor.copy(alpha = 0.05f)
                        ),
                        center = size.center,
                        radius = size.minDimension * 0.9f
                    )
                    onDrawBehind {
                        drawRect(gradient)
                    }
                }
        )

        // unsure about this one's flow
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 13.dp, end = 13.dp)
                .onClick(starInteractionSource) { profile.liked = !profile.liked }
                .pointerHoverIcon(PointerIcon.Hand)
        ) {
            Icon("star", color = if (profile.liked) Color(0xFFFFD700) else theme.textColor.copy(0.5f))
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (profile.icon != null) {
                Icon(profile.icon!!, modifier = Modifier.size(64.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(5.dp, theme.textColor, LocalTheme.current.circleShape)
                        .clip(LocalTheme.current.circleShape)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                profile.name,
                color = theme.textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
