package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.notifications.v1.Notifications
import org.polyfrost.oneconfig.api.platform.v1.DesktopHelper
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.api.TinyFdApi
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.search.searchMatches
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric
import java.nio.file.Files

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

private data class ProfileActionResult(
    val profiles: List<UiProfile>?,
    val failure: Throwable?,
)

private val ProfileCardHeight = 180.dp
private enum class ProfileEditor { Rename, Clone, Icon }
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
    var profiles by remember { mutableStateOf(emptyList<UiProfile>()) }
    var newProfileName by remember { mutableStateOf("") }
    var createError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var createTick by remember { mutableIntStateOf(0) }
    var profileSpecificControls by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            loadProfiles() to ConfigManager.profileSpecificControls()
        }
        Platform.screen().runOnUiThread {
            profiles = loaded.first
            profileSpecificControls = loaded.second
        }
    }

    fun runProfileAction(
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = { Notifications.error("Profile action failed", it) },
        action: () -> Unit,
    ) {
        Platform.screen().runOnUiThread {
            if (busy) return@runOnUiThread
            busy = true
            // A profile operation must finish refreshing the global registry even if this page is
            // closed while its blocking IO is still running. Enter the non-cancellable section
            // before the first suspension so disposal of the composition cannot strand the UI on
            // the previous profile.
            val ownerJob = scope.coroutineContext[Job]!!
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                withContext(NonCancellable) {
                    val result = withContext(Dispatchers.IO) {
                        try {
                            action()
                            ProfileActionResult(loadProfiles(), null)
                        } catch (failure: Throwable) {
                            val refreshed = try {
                                loadProfiles()
                            } catch (refreshFailure: Throwable) {
                                failure.addSuppressed(refreshFailure)
                                null
                            }
                            ProfileActionResult(refreshed, failure)
                        }
                    }
                    Platform.screen().runOnUiThread {
                        if (ownerJob.isCancelled) {
                            result.failure?.let { failure ->
                                Notifications.error(
                                    "Profile action failed",
                                    failure.message ?: failure::class.java.simpleName,
                                )
                            }
                            return@runOnUiThread
                        }
                        try {
                            result.profiles?.let { profiles = it }
                            val failure = result.failure
                            if (failure == null) onSuccess()
                            else onError(failure.message ?: failure::class.java.simpleName)
                        } catch (failure: Throwable) {
                            Notifications.error(
                                "Profile action failed",
                                failure.message ?: failure::class.java.simpleName,
                            )
                        } finally {
                            busy = false
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        ShellState.title = "Profiles"
        onDispose { }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(19.dp)
    ) {
        val localSearchQuery = if (ShellState.globalSearchActive) "" else ShellState.searchQuery.trim()
        val categorizedProfiles = when (activeCategory) {
            ProfileCategory.All -> profiles
            ProfileCategory.Favorited -> profiles.filter { it.favorite }
        }
        val visibleProfiles = remember(categorizedProfiles, localSearchQuery) {
            if (localSearchQuery.isBlank()) categorizedProfiles
            else categorizedProfiles.filter { it.matchesSearch(localSearchQuery) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(
                    label = "Separate controls",
                    selected = profileSpecificControls,
                    icon = "keyboard",
                    onClick = {
                        val enabled = !profileSpecificControls
                        runProfileAction(onSuccess = { profileSpecificControls = enabled }) {
                            ConfigManager.setProfileSpecificControls(enabled)
                        }
                    },
                )
                Chip(
                    label = "Open folder",
                    selected = false,
                    icon = "folder",
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            Files.createDirectories(ConfigManager.PROFILES_DIR)
                            DesktopHelper.open(ConfigManager.PROFILES_DIR.toFile())
                        }
                    },
                )
            }
        }

        ProfilesGrid(
            profiles = visibleProfiles,
            existingProfileIds = profiles.mapTo(HashSet()) { it.id.lowercase() },
            showCreateProfile = activeCategory == ProfileCategory.All && localSearchQuery.isBlank(),
            emptyMessage = if (localSearchQuery.isBlank()) "No favorite profiles."
                else "No profiles match \"$localSearchQuery\"",
            newProfileName = newProfileName,
            createError = createError,
            busy = busy,
            createTick = createTick,
            onNewProfileNameChange = {
                newProfileName = it
                createError = null
            },
            onCreateProfile = {
                val profileName = newProfileName
                runProfileAction(onSuccess = {
                    newProfileName = ""
                    createError = null
                    createTick++
                }, onError = { createError = it }) {
                    ConfigManager.createProfile(profileName)
                }
            },
            onOpen = { profile ->
                if (!profile.active) {
                    runProfileAction { ConfigManager.openProfile(profile.id) }
                }
            },
            onFavorite = { profile ->
                runProfileAction { ConfigManager.setFavoriteProfile(profile.id, !profile.favorite) }
            },
            onRename = { profile, newName, onSuccess, onError ->
                runProfileAction(onSuccess, onError) {
                    ConfigManager.renameProfile(profile.id, newName)
                }
            },
            onClone = { profile, newName, onSuccess, onError ->
                runProfileAction(onSuccess, onError) {
                    ConfigManager.cloneProfile(profile.id, newName)
                }
            },
            onIconChange = { profile, icon, onSuccess, onError ->
                runProfileAction(onSuccess, onError) { ConfigManager.setProfileIcon(profile.id, icon) }
            },
            onDelete = { profile ->
                if (!busy) {
                    scope.launch {
                        val confirmed = withContext(Dispatchers.IO) {
                            TinyFdApi.getInstance().showMessageBox(
                                "Delete profile",
                                "Delete ${profile.name}? This cannot be undone.",
                                TinyFdApi.YES_NO_DIALOG,
                                TinyFdApi.WARNING_ICON,
                                false,
                            )
                        }
                        if (confirmed) {
                            runProfileAction { ConfigManager.deleteProfile(profile.id) }
                        }
                    }
                }
            },
            onExport = { profile ->
                if (!busy) {
                    scope.launch {
                        val defaultName = profile.name.replace(Regex("[^a-zA-Z0-9._-]"), "_") + ".zip"
                        val destination = withContext(Dispatchers.IO) {
                            TinyFdApi.getInstance().openSaveSelector(
                                "Export profile",
                                defaultName,
                                arrayOf("*.zip"),
                                "Zip archive",
                            )
                        } ?: return@launch
                        val archive = if (destination.fileName.toString().endsWith(".zip", ignoreCase = true)) {
                            destination
                        } else {
                            destination.resolveSibling(destination.fileName.toString() + ".zip")
                        }
                        runProfileAction {
                            ConfigManager.exportProfile(profile.id, archive)
                        }
                    }
                }
            }
        )
    }
}

private fun UiProfile.matchesSearch(query: String): Boolean {
    val q = query.lowercase()
    return listOf(name, id, icon)
        .any { searchMatches(it, q) }
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
    existingProfileIds: Set<String>,
    showCreateProfile: Boolean,
    emptyMessage: String,
    newProfileName: String,
    createError: String?,
    busy: Boolean,
    createTick: Int,
    onNewProfileNameChange: (String) -> Unit,
    onCreateProfile: () -> Unit,
    onOpen: (UiProfile) -> Unit,
    onFavorite: (UiProfile) -> Unit,
    onRename: (UiProfile, String, () -> Unit, (String) -> Unit) -> Unit,
    onClone: (UiProfile, String, () -> Unit, (String) -> Unit) -> Unit,
    onIconChange: (UiProfile, String, () -> Unit, (String) -> Unit) -> Unit,
    onDelete: (UiProfile) -> Unit,
    onExport: (UiProfile) -> Unit,
) {
    var editingProfileId by remember { mutableStateOf<String?>(null) }
    var activeEditor by remember { mutableStateOf<ProfileEditor?>(null) }
    var menuProfileId by remember { mutableStateOf<String?>(null) }
    var creatingProfile by remember { mutableStateOf(false) }
    val visibleProfileIds = profiles.mapTo(HashSet()) { it.id }

    LaunchedEffect(visibleProfileIds, showCreateProfile) {
        if (editingProfileId != null && editingProfileId !in visibleProfileIds) {
            editingProfileId = null
            activeEditor = null
        }
        if (menuProfileId != null && menuProfileId !in visibleProfileIds) menuProfileId = null
        if (!showCreateProfile) creatingProfile = false
    }

    if (profiles.isEmpty() && !showCreateProfile) {
        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyMessage, color = LocalTheme.current.textColorSecondary)
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
            item(key = "create-profile") {
                CreateProfileCard(
                    value = newProfileName,
                    error = createError,
                    busy = busy,
                    creating = creatingProfile,
                    createTick = createTick,
                    onValueChange = onNewProfileNameChange,
                    onCreate = onCreateProfile,
                    onCreatingChange = { creating ->
                        creatingProfile = creating
                        if (creating) {
                            editingProfileId = null
                            activeEditor = null
                            menuProfileId = null
                        }
                    },
                )
            }
        }
        items(profiles, key = { "profile:${it.id}" }) { profile ->
            ProfileCard(
                profile = profile,
                busy = busy,
                editor = activeEditor.takeIf { editingProfileId == profile.id },
                menuOpen = menuProfileId == profile.id,
                suggestedCloneName = nextCloneName(profile.name, existingProfileIds),
                onEditorChange = { editor ->
                    if (editor == null) {
                        if (editingProfileId == profile.id) {
                            editingProfileId = null
                            activeEditor = null
                        }
                    } else {
                        creatingProfile = false
                        editingProfileId = profile.id
                        activeEditor = editor
                        menuProfileId = null
                    }
                },
                onMenuOpenChange = { open ->
                    if (open) {
                        creatingProfile = false
                        menuProfileId = profile.id
                        editingProfileId = null
                        activeEditor = null
                    } else if (menuProfileId == profile.id) {
                        menuProfileId = null
                    }
                },
                onOpen = { onOpen(profile) },
                onFavorite = { onFavorite(profile) },
                onRename = { newName, onSuccess, onError ->
                    onRename(profile, newName, onSuccess, onError)
                },
                onClone = { newName, onSuccess, onError ->
                    onClone(profile, newName, onSuccess, onError)
                },
                onIconChange = { icon, onSuccess, onError ->
                    onIconChange(profile, icon, onSuccess, onError)
                },
                onDelete = { onDelete(profile) },
                onExport = { onExport(profile) },
            )
        }
    }
}

private fun nextCloneName(profileName: String, profileIds: Set<String>): String {
    val base = "$profileName copy"
    if (base.lowercase() !in profileIds) return base
    var suffix = 2
    while ("$base $suffix".lowercase() in profileIds) suffix++
    return "$base $suffix"
}

@Composable
private fun CreateProfileCard(
    value: String,
    error: String?,
    busy: Boolean,
    creating: Boolean,
    createTick: Int,
    onValueChange: (String) -> Unit,
    onCreate: () -> Unit,
    onCreatingChange: (Boolean) -> Unit,
) {
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val theme = LocalTheme.current
    val shape = theme.modCardShape
    LaunchedEffect(createTick) {
        if (createTick > 0) onCreatingChange(false)
    }

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
            .background(theme.modCardBackground, shape)
            .border(
                1.dp, Brush.verticalGradient(
                    listOf(borderColor, borderColor.copy(0f))
                ), shape
            )
            .onClick(interactionSource) {
                if (!creating) onCreatingChange(true)
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
                    enabled = !busy,
                    tint = theme.textColorSecondary.copy(0.45f),
                    hoveredTint = Accent,
                ) {
                    onCreate()
                }
                ActionIcon("close", enabled = !busy, tint = theme.textColorSecondary) {
                    onValueChange("")
                    onCreatingChange(false)
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
                    placeholder = "Profile name",
                    error = error,
                    enabled = !busy,
                    width = 150.dp,
                    onValueChange = onValueChange,
                    onSubmit = { if (!busy) onCreate() },
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
    busy: Boolean,
    editor: ProfileEditor?,
    menuOpen: Boolean,
    suggestedCloneName: String,
    onEditorChange: (ProfileEditor?) -> Unit,
    onMenuOpenChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onRename: (String, () -> Unit, (String) -> Unit) -> Unit,
    onClone: (String, () -> Unit, (String) -> Unit) -> Unit,
    onIconChange: (String, () -> Unit, (String) -> Unit) -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
) {
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val theme = LocalTheme.current
    val shape = theme.modCardShape
    var editName by remember(profile.id) { mutableStateOf(profile.name) }
    var editError by remember(profile.id) { mutableStateOf<String?>(null) }

    fun closeEditor() {
        onEditorChange(null)
        editName = profile.name
        editError = null
    }

    fun submitName() {
        if (busy) return
        editError = null
        val onSuccess = {
            onEditorChange(null)
            editError = null
        }
        val onError = { message: String -> editError = message }
        when (editor) {
            ProfileEditor.Rename -> onRename(editName, onSuccess, onError)
            ProfileEditor.Clone -> onClone(editName, onSuccess, onError)
            else -> Unit
        }
    }

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
                if (editor == null && !menuOpen) onOpen()
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
            if (editor != null) {
                if (editor == ProfileEditor.Rename || editor == ProfileEditor.Clone) {
                    ActionIcon("tick", enabled = !busy, tint = Accent, onClick = ::submitName)
                }
                ActionIcon("close", enabled = !busy, tint = theme.textColorSecondary, onClick = ::closeEditor)
            } else if (profile.active || isHovered || menuOpen) {
                ActionIcon("settings", enabled = !busy, tint = theme.textColorSecondary, hoveredTint = Accent) {
                    onMenuOpenChange(true)
                }
            }
        }

        ProfileActionsMenu(
            profile = profile,
            expanded = menuOpen,
            enabled = !busy,
            onDismiss = { onMenuOpenChange(false) },
            onClone = {
                editName = suggestedCloneName
                editError = null
                onEditorChange(ProfileEditor.Clone)
            },
            onRename = {
                editName = profile.name
                editError = null
                onEditorChange(ProfileEditor.Rename)
            },
            onIconChange = {
                editError = null
                onEditorChange(ProfileEditor.Icon)
            },
            onFavorite = onFavorite,
            onExport = onExport,
            onDelete = onDelete,
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(profile.icon, modifier = Modifier.size(if (editor != null) 42.dp else 64.dp), color = theme.textColor)
            Spacer(Modifier.height(if (editor != null) 14.dp else 24.dp))
            when (editor) {
                ProfileEditor.Rename, ProfileEditor.Clone -> ProfileTextField(
                    value = editName,
                    placeholder = "Profile name",
                    width = 150.dp,
                    error = editError,
                    enabled = !busy,
                    onValueChange = {
                        editName = it
                        editError = null
                    },
                    onSubmit = ::submitName,
                )
                ProfileEditor.Icon -> ProfileIconPicker(
                    selectedIcon = profile.icon,
                    enabled = !busy,
                    onIconChange = {
                        editError = null
                        onIconChange(
                            it,
                            {
                                onEditorChange(null)
                                editError = null
                            },
                            { message -> editError = message },
                        )
                    },
                )
                null -> {
                    BasicText(
                        profile.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        style = TextStyle(
                            color = theme.textColor,
                            fontSize = 18.sp,
                            fontFamily = theme.typography.family,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (editor == ProfileEditor.Icon && editError != null) {
                ProfileError(editError!!, 150.dp)
            }
        }
    }
}

@Composable
private fun ProfileActionsMenu(
    profile: UiProfile,
    expanded: Boolean,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onClone: () -> Unit,
    onRename: () -> Unit,
    onIconChange: () -> Unit,
    onFavorite: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    if (!expanded) return
    val theme = LocalTheme.current
    Popup(
        alignment = Alignment.TopEnd,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .background(theme.popupBackground, theme.popupShape)
                .border(1.dp, theme.borderColor, theme.popupShape)
                .padding(4.dp),
        ) {
            ProfileMenuItem("copy", "Clone", enabled = enabled) {
                onDismiss()
                onClone()
            }
            if (profile.editable) {
                ProfileMenuItem("text-input", "Rename", enabled = enabled) {
                    onDismiss()
                    onRename()
                }
                ProfileMenuItem("paintbrush", "Change icon", enabled = enabled) {
                    onDismiss()
                    onIconChange()
                }
            }
            ProfileMenuItem(
                if (profile.favorite) "star-filled" else "star",
                if (profile.favorite) "Remove favorite" else "Favorite",
                enabled = enabled,
            ) {
                onDismiss()
                onFavorite()
            }
            ProfileMenuItem("cloud", "Export / share", enabled = enabled) {
                onDismiss()
                onExport()
            }
            if (profile.editable) {
                Spacer(Modifier.height(4.dp))
                ProfileMenuItem("trash", "Delete", danger = true, enabled = enabled) {
                    onDismiss()
                    onDelete()
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: String,
    label: String,
    danger: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val baseColor = if (danger) Color(0xFFE35B5B) else theme.textColor
    val color = if (enabled) baseColor else theme.textColorSecondary
    val shape = theme.popupShape.concentric(4.dp)
    val hoverBackground by animateColorAsState(
        targetValue = baseColor.copy(alpha = if (enabled && isHovered) 0.10f else 0f),
        animationSpec = tween(120),
        label = "profileMenuItemHover",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(hoverBackground, shape)
            .then(
                if (enabled) Modifier
                    .onClick(interactionSource, onClick)
                    .hoverable(interactionSource)
                    .pointerHoverIcon(PointerIcon.Hand)
                else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, color = color, modifier = Modifier.size(14.dp))
        Text(label, color = color, fontSize = 13.sp)
    }
}

@Composable
private fun ProfileIconPicker(
    selectedIcon: String,
    enabled: Boolean,
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
                        enabled = enabled,
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
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = rememberInteractionSource()
    val theme = LocalTheme.current
    val isHovered by interactionSource.collectIsHoveredAsState()
    val backgroundColor by animateColorAsState(
        if (selected) Accent.copy(alpha = if (enabled) 0.22f else 0.10f)
        else if (enabled && isHovered) theme.textColor.copy(alpha = 0.08f)
        else Color.Transparent
    )
    val borderColor by animateColorAsState(
        if (selected && enabled) Accent else theme.borderColor.copy(alpha = 0f)
    )
    val iconColor by animateColorAsState(
        if (enabled && (selected || isHovered)) theme.textColor else theme.textColorSecondary
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .background(backgroundColor, theme.sideBarNavigationEntryShape)
            .border(1.dp, borderColor, theme.sideBarNavigationEntryShape)
            .then(
                if (enabled) Modifier
                    .onClick(interactionSource, onClick)
                    .pointerHoverIcon(PointerIcon.Hand)
                else Modifier
            ),
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
    error: String? = null,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val focusRequester = remember { FocusRequester() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = theme.sideBarNavigationEntryShape
    val borderColor by animateColorAsState(
        if (error != null) Color(0xFFE35B5B)
        else if (isFocused) Accent
        else theme.borderColor
    )

    Column(
        modifier = Modifier.width(width),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (enabled) onSubmit() }),
            textStyle = TextStyle(
                color = theme.textColor,
                fontSize = 14.sp,
                fontFamily = theme.typography.family,
            ),
            interactionSource = interactionSource,
            cursorBrush = SolidColor(theme.textColor),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (enabled && event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Enter || event.key == Key.NumPadEnter)
                    ) {
                        onSubmit()
                        true
                    } else {
                        false
                    }
                }
                .background(theme.modCardBackground, shape)
                .border(1.dp, borderColor, shape)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = theme.textColorSecondary,
                            fontSize = 14.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (error != null) ProfileError(error, width)
    }

    LaunchedEffect(enabled) {
        if (enabled) focusRequester.requestFocus()
    }
}

@Composable
private fun ProfileError(message: String, width: androidx.compose.ui.unit.Dp) {
    BasicText(
        text = message,
        modifier = Modifier.width(width).padding(top = 4.dp),
        style = TextStyle(
            color = Color(0xFFE35B5B),
            fontSize = 10.sp,
            fontFamily = LocalTheme.current.typography.family,
            textAlign = TextAlign.Center,
        ),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}
