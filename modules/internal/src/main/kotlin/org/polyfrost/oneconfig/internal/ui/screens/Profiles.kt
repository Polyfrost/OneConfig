package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

enum class ProfileCategory(val title: String, val icon: String?) {
    All("All profiles", null),
    Favorited("Favorites", "star"),
}

private data class UiProfile(
    val id: String,
    val name: String,
    val icon: String,
    val active: Boolean,
    val favorite: Boolean,
) {
    val editable: Boolean get() = id.isNotEmpty()
}

private val ProfileCardHeight = 180.dp
private val ProfileIconOptions = listOf(
    "profiles",
    "star",
    "settings",
    "cog",
    "paintbrush",
    "keyboard",
    "hud",
    "combat",
    "qol",
    "cloud",
)

@Composable
fun Profiles() {
    var activeCategory by remember { mutableStateOf(ProfileCategory.All) }
    var profiles by remember { mutableStateOf(loadProfiles()) }
    var newProfileName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        ConfigRegistry.loadFrom(ConfigManager.active(), ConfigSource.OC)
        profiles = loadProfiles()
    }

    fun runProfileAction(action: () -> Unit): Boolean {
        try {
            action()
            error = null
            refresh()
            return true
        } catch (t: Throwable) {
            error = t.message ?: t::class.java.simpleName
            profiles = loadProfiles()
            return false
        }
    }

    DisposableEffect(Unit) {
        ShellState.title = "Profiles"
        onDispose { }
    }

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

        ProfilesGrid(
            profiles = when (activeCategory) {
                ProfileCategory.All -> profiles
                ProfileCategory.Favorited -> profiles.filter { it.favorite }
            },
            showCreateProfile = activeCategory == ProfileCategory.All,
            newProfileName = newProfileName,
            createError = error,
            onNewProfileNameChange = {
                newProfileName = it
                error = null
            },
            onCreateProfile = {
                val profileName = newProfileName
                val created = runProfileAction {
                    ConfigManager.createProfile(profileName)
                    newProfileName = ""
                }
                created
            },
            onOpen = { profile ->
                if (!profile.active) {
                    runProfileAction { ConfigManager.openProfile(profile.id) }
                }
            },
            onFavorite = { profile ->
                runProfileAction { ConfigManager.setFavoriteProfile(profile.id, !profile.favorite) }
            },
            onRename = { profile, newName ->
                runProfileAction { ConfigManager.renameProfile(profile.id, newName) }
            },
            onIconChange = { profile, icon ->
                runProfileAction { ConfigManager.setProfileIcon(profile.id, icon) }
            },
            onDelete = { profile ->
                runProfileAction { ConfigManager.deleteProfile(profile.id) }
            }
        )
    }
}

private fun loadProfiles(): List<UiProfile> {
    val active = ConfigManager.activeProfile()
    val favorites = ConfigManager.favoriteProfiles().toSet()
    val icons = ConfigManager.profileIcons()
    return ConfigManager.profiles().map { id ->
        UiProfile(
            id = id,
            name = if (id.isEmpty()) "Default" else id,
            icon = icons[id] ?: "profiles",
            active = id == active,
            favorite = id in favorites,
        )
    }
}

@Composable
private fun ColumnScope.ProfilesGrid(
    profiles: List<UiProfile>,
    showCreateProfile: Boolean,
    newProfileName: String,
    createError: String?,
    onNewProfileNameChange: (String) -> Unit,
    onCreateProfile: () -> Boolean,
    onOpen: (UiProfile) -> Unit,
    onFavorite: (UiProfile) -> Unit,
    onRename: (UiProfile, String) -> Unit,
    onIconChange: (UiProfile, String) -> Unit,
    onDelete: (UiProfile) -> Unit,
) {
    if (profiles.isEmpty() && !showCreateProfile) {
        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No favorite profiles.", color = LocalTheme.current.textColorSecondary)
        }
        return
    }

    LazyVerticalGrid(
        modifier = Modifier.weight(1f),
        columns = GridCells.Fixed(5),
        verticalArrangement = Arrangement.spacedBy(19.dp),
        horizontalArrangement = Arrangement.spacedBy(19.dp),
    ) {
        if (showCreateProfile) {
            item {
                CreateProfileCard(
                    value = newProfileName,
                    error = createError,
                    onValueChange = onNewProfileNameChange,
                    onCreate = onCreateProfile,
                )
            }
        }
        profiles.forEach { profile ->
            item {
                ProfileCard(
                    profile = profile,
                    onOpen = { onOpen(profile) },
                    onFavorite = { onFavorite(profile) },
                    onRename = { newName -> onRename(profile, newName) },
                    onIconChange = { icon -> onIconChange(profile, icon) },
                    onDelete = { onDelete(profile) },
                )
            }
        }
    }
}

@Composable
private fun CreateProfileCard(
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onCreate: () -> Boolean,
) {
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val theme = LocalTheme.current
    val shape = theme.modCardShape
    var creating by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        if (creating || isHovered) Accent else theme.borderColor
    )
    val iconBackground by animateColorAsState(
        if (creating || isHovered) Accent.copy(alpha = 0.18f) else theme.textColor.copy(alpha = 0.06f)
    )
    val iconColor by animateColorAsState(
        if (creating || isHovered) Accent else theme.textColorSecondary
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ProfileCardHeight)
            .background(theme.modCardBackground.copy(alpha = 0.72f), shape)
            .border(1.dp, borderColor, shape)
            .onClick(interactionSource) {
                if (!creating) creating = true
            }
            .clip(shape)
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        if (creating) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 13.dp, end = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionIcon(
                    icon = "tick",
                    tint = theme.textColorSecondary.copy(0.45f),
                    hoveredTint = Accent,
                ) {
                    if (onCreate()) creating = false
                }
                ActionIcon("close", tint = theme.textColorSecondary) {
                    onValueChange("")
                    creating = false
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(iconBackground, theme.circleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon("plus", modifier = Modifier.size(30.dp), color = iconColor)
            }
            Spacer(Modifier.height(24.dp))
            if (creating) {
                ProfileTextField(
                    value = value,
                    placeholder = error ?: "Profile name",
                    isError = error != null,
                    width = 150.dp,
                    onValueChange = onValueChange,
                )
            } else {
                Text(
                    "New profile",
                    color = theme.textColor.copy(alpha = 0.86f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: UiProfile,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onRename: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val interactionSource = rememberInteractionSource()
    val theme = LocalTheme.current
    val shape = theme.modCardShape
    var editing by remember(profile.id) { mutableStateOf(false) }
    var editName by remember(profile.id) { mutableStateOf(profile.name) }

    val selectionBorderColor by animateColorAsState(
        if (profile.active) Accent else Color.Transparent
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ProfileCardHeight)
            .background(theme.modCardBackground, shape)
            .border(3.dp, selectionBorderColor, shape)
            .border(
                1.dp, Brush.verticalGradient(
                    listOf(theme.borderColor, theme.borderColor.copy(0f))
                ), shape
            )
            .onClick(interactionSource) {
                if (!editing) onOpen()
            }
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

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 13.dp, end = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (profile.editable) {
                if (editing) {
                    ActionIcon("tick", tint = Accent) {
                        onRename(editName)
                        editing = false
                    }
                    ActionIcon("close", tint = theme.textColorSecondary) {
                        editName = profile.name
                        editing = false
                    }
                } else {
                    ActionIcon("spanner", tint = theme.textColorSecondary) {
                        editing = true
                    }
                    ActionIcon("trash", tint = theme.textColorSecondary) {
                        onDelete()
                    }
                }
            }
            ActionIcon(
                icon = "star",
                tint = if (profile.favorite) Color(0xFFFFD700) else theme.textColor.copy(0.5f),
                onClick = onFavorite,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(profile.icon, modifier = Modifier.size(if (editing) 42.dp else 64.dp), color = theme.textColor)
            Spacer(Modifier.height(if (editing) 14.dp else 24.dp))
            if (editing) {
                ProfileTextField(
                    value = editName,
                    placeholder = "Profile name",
                    width = 150.dp,
                    onValueChange = { editName = it },
                )
                Spacer(Modifier.height(10.dp))
                ProfileIconPicker(
                    selectedIcon = profile.icon,
                    onIconChange = onIconChange,
                )
            } else {
                Text(
                    profile.name,
                    color = theme.textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ProfileIconPicker(
    selectedIcon: String,
    onIconChange: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProfileIconOptions.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { icon ->
                    ProfileIconChoice(
                        icon = icon,
                        selected = icon == selectedIcon,
                        onClick = { onIconChange(icon) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileIconChoice(
    icon: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = rememberInteractionSource()
    val theme = LocalTheme.current
    val isHovered by interactionSource.collectIsHoveredAsState()
    val backgroundColor by animateColorAsState(
        if (selected) Accent.copy(alpha = 0.22f)
        else if (isHovered) theme.textColor.copy(alpha = 0.08f)
        else Color.Transparent
    )
    val borderColor by animateColorAsState(
        if (selected) Accent else theme.borderColor.copy(alpha = 0f)
    )
    val iconColor by animateColorAsState(
        if (selected || isHovered) theme.textColor else theme.textColorSecondary
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .background(backgroundColor, theme.sideBarNavigationEntryShape)
            .border(1.dp, borderColor, theme.sideBarNavigationEntryShape)
            .onClick(interactionSource, onClick)
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, modifier = Modifier.size(14.dp), color = iconColor)
    }
}

@Composable
private fun ActionIcon(
    icon: String,
    enabled: Boolean = true,
    tint: Color,
    hoveredTint: Color = tint,
    onClick: () -> Unit,
) {
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val iconColor by animateColorAsState(
        if (enabled && isHovered) hoveredTint else tint
    )

    Box(
        modifier = Modifier
            .size(22.dp)
            .then(
                if (enabled) Modifier
                    .onClick(interactionSource) { onClick() }
                    .pointerHoverIcon(PointerIcon.Hand)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, color = iconColor, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    placeholder: String,
    width: androidx.compose.ui.unit.Dp,
    isError: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = theme.sideBarNavigationEntryShape
    val borderColor by animateColorAsState(
        if (isError) Color(0xFFE35B5B)
        else if (isFocused) Accent
        else theme.borderColor
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            color = theme.textColor,
            fontSize = 14.sp,
            fontFamily = theme.typography.family,
        ),
        interactionSource = interactionSource,
        cursorBrush = SolidColor(theme.textColor),
        modifier = Modifier
            .width(width)
            .background(theme.modCardBackground, shape)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = if (isError) Color(0xFFE35B5B) else theme.textColorSecondary,
                        fontSize = 14.sp,
                    )
                }
                innerTextField()
            }
        },
    )
}
