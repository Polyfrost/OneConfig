package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import java.nio.file.Files
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.BoxScope
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.ColumnScope
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.Spacer
import org.polyfrost.compose.foundation.layout.align
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.rememberScrollState
import org.polyfrost.compose.foundation.text.BasicTextField
import org.polyfrost.compose.foundation.text.TextFieldState
import org.polyfrost.compose.foundation.verticalScroll
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.draw.scale
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.RadialGradient
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.key.KeyEvent
import org.polyfrost.compose.ui.input.key.KeyEventType
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.text.style.TextAlign
import org.polyfrost.compose.ui.text.style.TextOverflow
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.compose.ui.window.Popup
import org.polyfrost.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.notifications.v1.Notifications
import org.polyfrost.oneconfig.api.platform.v1.DesktopHelper
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.api.TinyFdApi
import org.polyfrost.oneconfig.internal.ui.api.settings.KeybindOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.KeybindOption
import org.polyfrost.oneconfig.internal.ui.keybind.ProfileKeybinds
import org.polyfrost.oneconfig.internal.ui.poly.GUTTER
import org.polyfrost.oneconfig.internal.ui.search.searchMatches
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric

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
private const val OverlayFrameTimeoutMs = 500L
private val DangerColor = Color(0xFFE35B5B)
private enum class ProfileEditor { Rename, Clone, Icon, Delete }
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
fun Profiles(query: String) {
    var activeCategory by remember { mutableStateOf(ProfileCategory.All) }
    var profiles by remember { mutableStateOf(emptyList<UiProfile>()) }
    val newProfileName = remember { TextFieldState() }
    var createError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var createTick by remember { mutableIntStateOf(0) }
    var profileSpecificControls by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            loadProfiles() to ConfigManager.profileSpecificControls()
        }
        profiles = loaded.first
        profileSpecificControls = loaded.second
    }

    fun runProfileAction(
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = { Notifications.error("Profile action failed", it) },
        holdsBusy: Boolean = false,
        action: () -> Unit,
    ) {
        Platform.screen().runOnUiThread {
            if (busy && !holdsBusy) return@runOnUiThread
            busy = true
            // A profile operation must finish refreshing the global registry even if this page is
            // closed while its blocking IO is still running. Enter the non-cancellable section
            // before the first suspension so disposal of the composition cannot strand the UI on
            // the previous profile.
            val ownerJob = scope.coroutineContext[Job]!!
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                withContext(NonCancellable) {
                    awaitOverlayDrawn()
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
                                    failure.messageOrType(),
                                )
                            }
                            return@runOnUiThread
                        }
                        try {
                            result.profiles?.let { profiles = it }
                            val failure = result.failure
                            if (failure == null) onSuccess()
                            else onError(failure.messageOrType())
                        } catch (failure: Throwable) {
                            Notifications.error(
                                "Profile action failed",
                                failure.messageOrType(),
                            )
                        } finally {
                            busy = false
                        }
                    }
                }
            }
        }
    }

    fun <T : Any> runProfileDialog(prompt: () -> T?, action: (T) -> Unit) {
        Platform.screen().runOnUiThread {
            if (busy) return@runOnUiThread
            busy = true
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                withContext(NonCancellable) {
                    awaitOverlayDrawn()
                    val choice = try {
                        withContext(Dispatchers.IO) { prompt() }
                    } catch (failure: Throwable) {
                        Platform.screen().runOnUiThread {
                            busy = false
                            Notifications.error("Profile action failed", failure.messageOrType())
                        }
                        return@withContext
                    }
                    if (choice == null) {
                        Platform.screen().runOnUiThread { busy = false }
                        return@withContext
                    }
                    runProfileAction(holdsBusy = true) { action(choice) }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = GUTTER.dp),
            verticalArrangement = Arrangement.spacedBy(19.dp)
        ) {
            val localSearchQuery = query.trim()
            val categorizedProfiles = when (activeCategory) {
                ProfileCategory.All -> profiles
                ProfileCategory.Favorited -> profiles.filter { it.favorite }
            }
            val visibleProfiles = remember(categorizedProfiles, localSearchQuery) {
                if (localSearchQuery.isBlank()) categorizedProfiles
                else categorizedProfiles.filter { it.matchesSearch(localSearchQuery) }
            }
            val existingProfileIds = remember(profiles) { profiles.mapTo(HashSet()) { it.id.lowercase() } }

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
                                runCatching {
                                    Files.createDirectories(ConfigManager.PROFILES_DIR)
                                    DesktopHelper.open(ConfigManager.PROFILES_DIR.toFile())
                                }.onFailure {
                                    Notifications.error("Could not open the profiles folder", it.messageOrType())
                                }
                            }
                        },
                    )
                }
            }

            ProfilesGrid(
                profiles = visibleProfiles,
                existingProfileIds = existingProfileIds,
                showCreateProfile = activeCategory == ProfileCategory.All && localSearchQuery.isBlank(),
                emptyMessage = if (localSearchQuery.isBlank()) "No favorite profiles."
                    else "No profiles match \"$localSearchQuery\"",
                newProfileName = newProfileName,
                createError = createError,
                busy = busy,
                createTick = createTick,
                onNewProfileNameChange = {
                    createError = null
                },
                onCreateProfile = {
                    val profileName = newProfileName.text
                    runProfileAction(onSuccess = {
                        newProfileName.replaceText("")
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
                    runProfileAction { ConfigManager.deleteProfile(profile.id) }
                },
                onExport = { profile ->
                    runProfileDialog(
                        prompt = {
                            val defaultName = profile.name.replace(Regex("[^a-zA-Z0-9._-]"), "_") + ".zip"
                            TinyFdApi.getInstance().openSaveSelector(
                                "Export profile",
                                defaultName,
                                arrayOf("*.zip"),
                                "Zip archive",
                            )
                        },
                        action = { destination ->
                            val archive = if (destination.fileName.toString().endsWith(".zip", ignoreCase = true)) {
                                destination
                            } else {
                                destination.resolveSibling(destination.fileName.toString() + ".zip")
                            }
                            ConfigManager.exportProfile(profile.id, archive)
                        },
                    )
                }
            )
        }
        if (busy) ProfileBusyOverlay()
    }
}

private suspend fun awaitOverlayDrawn() {
    withTimeoutOrNull(OverlayFrameTimeoutMs) {
        withFrameNanos { }
        withFrameNanos { }
    }
}

@Composable
private fun BoxScope.ProfileBusyOverlay() {
    val theme = LocalTheme.current
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).onPointerEvent { true },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .background(theme.modCardBackground, theme.modCardShape)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "Applying profile changes\u2026",
                color = theme.textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "The game will stop responding until it finishes.",
                color = theme.textColorSecondary,
                fontSize = 14.sp,
            )
        }
    }
}

private fun Throwable.messageOrType(): String = message ?: this::class.java.simpleName

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
    newProfileName: TextFieldState,
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
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(emptyMessage, color = LocalTheme.current.textColorSecondary)
        }
        return
    }

    ProfileGrid(
        modifier = Modifier.weight(1f),
        columns = 5,
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

private class ProfileGridScope {
    val cells = ArrayList<Pair<Any, @Composable () -> Unit>>()

    fun item(key: Any, content: @Composable () -> Unit) {
        cells += key to content
    }

    fun <T> items(items: List<T>, key: (T) -> Any, itemContent: @Composable (T) -> Unit) {
        for (item in items) {
            val content: @Composable () -> Unit = { itemContent(item) }
            cells += key(item) to content
        }
    }
}

@Composable
private fun ProfileGrid(
    modifier: Modifier,
    columns: Int,
    verticalArrangement: Arrangement.Vertical,
    horizontalArrangement: Arrangement.Horizontal,
    content: ProfileGridScope.() -> Unit,
) {
    val cells = ProfileGridScope().apply(content).cells
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = verticalArrangement,
    ) {
        for (row in cells.chunked(columns)) Row(Modifier.fillMaxWidth(), horizontalArrangement = horizontalArrangement) {
            for ((id, cell) in row) key(id) {
                Box(Modifier.weight(1f)) { cell() }
            }
            repeat(columns - row.size) { Box(Modifier.weight(1f)) }
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
    value: TextFieldState,
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
    val iconBackground = animateColorAsState(
        if (creating || isHovered) Accent.copy(alpha = 0.18f) else theme.textColor.copy(alpha = 0.06f)
    )
    val iconColor = animateColorAsState(
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
        Canvas(Modifier.fillMaxSize()) { x, y, w, h ->
            rectGradient(
                x, y, w, h,
                RadialGradient(
                    colors = listOf(
                        vignetteColor.copy(alpha = 0f),
                        vignetteColor.copy(alpha = 0.02f),
                        vignetteColor.copy(alpha = 0.05f)
                    ),
                    radius = 0.9f
                )
            )
        }

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
                    value.replaceText("")
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
    val editName = remember(profile.id) { TextFieldState(profile.name) }
    var editError by remember(profile.id) { mutableStateOf<String?>(null) }
    val keybindProp = remember(profile.id) { ProfileKeybinds.property(profile.id) }

    fun closeEditor() {
        onEditorChange(null)
        editName.replaceText(profile.name)
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
            ProfileEditor.Rename -> onRename(editName.text, onSuccess, onError)
            ProfileEditor.Clone -> onClone(editName.text, onSuccess, onError)
            else -> Unit
        }
    }

    val selectionBorderColor by animateColorAsState(
        if (profile.active) Accent else Accent.copy(alpha = 0f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ProfileCardHeight)
            .background(theme.modCardBackground, shape)
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
        Canvas(Modifier.fillMaxSize()) { x, y, w, h ->
            rectGradient(
                x, y, w, h,
                RadialGradient(
                    colors = listOf(
                        vignetteColor.copy(alpha = 0f),
                        vignetteColor.copy(alpha = 0.02f),
                        vignetteColor.copy(alpha = 0.05f)
                    ),
                    radius = 0.9f
                )
            )
        }
        Box(Modifier.fillMaxSize().border(3.dp, selectionBorderColor, shape))

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 13.dp, end = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (editor != null) {
                when (editor) {
                    ProfileEditor.Rename, ProfileEditor.Clone ->
                        ActionIcon("tick", enabled = !busy, tint = Accent, onClick = ::submitName)
                    ProfileEditor.Delete -> ActionIcon("tick", enabled = !busy, tint = DangerColor) {
                        onEditorChange(null)
                        onDelete()
                    }
                    ProfileEditor.Icon -> Unit
                }
                ActionIcon("close", enabled = !busy, tint = theme.textColorSecondary, onClick = ::closeEditor)
            } else {
                if (profile.favorite) {
                    ActionIcon("star-filled", enabled = !busy, tint = theme.favoriteColor) { onFavorite() }
                }
                if (profile.active || isHovered || menuOpen) {
                    ActionIcon("settings", enabled = !busy, tint = theme.textColorSecondary, hoveredTint = Accent) {
                        onMenuOpenChange(true)
                    }
                }
            }
        }

        ProfileActionsMenu(
            profile = profile,
            expanded = menuOpen,
            enabled = !busy,
            onDismiss = { onMenuOpenChange(false) },
            onClone = {
                editName.replaceText(suggestedCloneName)
                editError = null
                onEditorChange(ProfileEditor.Clone)
            },
            onRename = {
                editName.replaceText(profile.name)
                editError = null
                onEditorChange(ProfileEditor.Rename)
            },
            onIconChange = {
                editError = null
                onEditorChange(ProfileEditor.Icon)
            },
            onFavorite = onFavorite,
            onExport = onExport,
            onDelete = {
                editError = null
                onEditorChange(ProfileEditor.Delete)
            },
        )

        Column(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val compact = editor != null && editor != ProfileEditor.Delete
            Icon(profile.icon, modifier = Modifier.size(if (compact) 42.dp else 64.dp), color = theme.textColor)
            Spacer(Modifier.height(if (compact) 14.dp else 24.dp))
            when (editor) {
                ProfileEditor.Rename, ProfileEditor.Clone -> key(editor) {
                    ProfileTextField(
                        value = editName,
                        placeholder = "Profile name",
                        width = 150.dp,
                        error = editError,
                        enabled = !busy,
                        onValueChange = {
                            editError = null
                        },
                        onSubmit = ::submitName,
                    )
                }
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
                ProfileEditor.Delete, null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                    ) {
                        Text(
                            profile.name,
                            modifier = Modifier.align(Alignment.Center),
                            color = theme.textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (editor == ProfileEditor.Icon && editError != null) {
                ProfileError(editError!!, 150.dp)
            }
            if (editor == ProfileEditor.Delete) {
                Text("Delete this profile?", modifier = Modifier.padding(top = 4.dp), color = DangerColor, fontSize = 13.sp)
            }
        }

        if (editor == null && (profile.active || isHovered || menuOpen || keybindProp.get()?.isBound == true)) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)) {
                KeybindOption(KeybindOptionData(keybindProp))
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
    val labelFont = PolyFont(theme.typography.family.resolve(FontWeight.Normal), 13f)
    val labels = listOf("Clone", if (profile.favorite) "Remove favorite" else "Favorite", "Export / share") +
        if (profile.editable) listOf("Rename", "Change icon", "Delete") else emptyList()
    val menuWidth = labels.maxOf { labelFont.measureTextWidth(it) } + 14f + 8f + 20f + 8f
    Popup(
        alignment = Alignment.TopEnd,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .width(menuWidth.dp)
                .shellTransition()
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
            ProfileMenuItem("down", "Export / share", enabled = enabled) {
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
private fun Modifier.shellTransition(): Modifier {
    val t by animateFloatAsState(1f, initialValue = 0f)
    return alpha(t).scale(0.92f + 0.08f * t)
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
    val baseColor = if (danger) DangerColor else theme.textColor
    val color = if (enabled) baseColor else theme.textColorSecondary
    val shape = theme.popupShape.concentric(4.dp)
    val hoverBackground = animateColorAsState(
        baseColor.copy(alpha = if (enabled && isHovered) 0.10f else 0f)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
    val backgroundColor = animateColorAsState(
        if (selected) Accent.copy(alpha = if (enabled) 0.22f else 0.10f)
        else if (enabled && isHovered) theme.textColor.copy(alpha = 0.08f)
        else theme.textColor.copy(alpha = 0f)
    )
    val borderColor = animateColorAsState(
        if (selected && enabled) Accent else theme.borderColor.copy(alpha = 0f)
    )
    val iconColor = animateColorAsState(
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
    val iconColor = animateColorAsState(
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
    value: TextFieldState,
    placeholder: String,
    width: Dp,
    error: String? = null,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val theme = LocalTheme.current
    val clipboard = LocalUiClipboard.current
    val isFocused = value.focused
    val shape = theme.sideBarNavigationEntryShape
    val borderColor = animateColorAsState(
        if (error != null) DangerColor
        else if (isFocused) Accent
        else theme.borderColor
    )

    Column(
        modifier = Modifier.width(width),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.modCardBackground, shape)
                .border(1.dp, borderColor, shape)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            BasicTextField(
                value,
                PolyFont(theme.typography.family.resolve(FontWeight.Normal), 14f),
                color = theme.textColor,
                caretColor = theme.textColor,
                selectionColor = Accent.copy(alpha = 0.4f),
                readClipboard = clipboard.read,
                writeClipboard = clipboard.write,
                modifier = Modifier.fillMaxWidth(),
                placeholder = placeholder,
                placeholderColor = theme.textColorSecondary,
                onKey = { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == KeyEvent.KEY_ENTER) {
                        if (enabled) onSubmit()
                        true
                    } else if (!enabled) {
                        false
                    } else {
                        val before = value.text
                        value.onKey(event, clipboard.read, clipboard.write).also {
                            if (value.text != before) onValueChange(value.text)
                        }
                    }
                },
            )
        }
        if (error != null) ProfileError(error, width)
    }

    LaunchedEffect(Unit) {
        value.requestFocus()
    }
}

@Composable
private fun ProfileError(message: String, width: Dp) {
    Box(Modifier.width(width).padding(top = 4.dp)) {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            color = DangerColor,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
        )
    }
}
