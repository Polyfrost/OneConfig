package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.notifications.v1.Notifications
import org.polyfrost.oneconfig.api.platform.v1.DesktopHelper
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.api.TinyFdApi
import org.polyfrost.oneconfig.api.ui.v1.components.Confirm
import org.polyfrost.oneconfig.api.ui.v1.components.DropdownMenu
import org.polyfrost.oneconfig.api.ui.v1.components.EmptyState
import org.polyfrost.oneconfig.api.ui.v1.components.LoadState
import org.polyfrost.oneconfig.api.ui.v1.components.MenuAction
import org.polyfrost.oneconfig.api.ui.v1.components.MenuEntry
import org.polyfrost.oneconfig.api.ui.v1.components.MenuSeparator
import org.polyfrost.oneconfig.api.ui.v1.components.ModalDialog
import org.polyfrost.oneconfig.api.ui.v1.components.Placement
import org.polyfrost.oneconfig.api.ui.v1.components.ProgressBar
import org.polyfrost.oneconfig.api.ui.v1.components.Scrollbar
import org.polyfrost.oneconfig.api.ui.v1.components.Text
import org.polyfrost.oneconfig.api.ui.v1.components.TextField
import org.polyfrost.oneconfig.api.ui.v1.components.TextPromptDialog
import org.polyfrost.oneconfig.api.ui.v1.components.contextMenu
import org.polyfrost.oneconfig.api.ui.v1.components.smoothWheelScroll
import org.polyfrost.oneconfig.internal.ui.api.settings.KeybindOptionData
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.hoverGlow
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.KeybindOption
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.keybind.ProfileKeybinds
import org.polyfrost.oneconfig.internal.ui.search.searchMatches
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.shell.pageSearchQuery
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.animateAccentColorAsState
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

private data class ProfileActionResult(
    val profiles: List<UiProfile>?,
    val failure: Throwable?,
)

private val ProfileCardHeight = 180.dp
private const val BusyOverlayDelayMs = 400L
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
    var loadState by remember { mutableStateOf<LoadState>(LoadState.Loading) }
    var loadAttempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(loadAttempt) {
        loadState = LoadState.Loading
        try {
            val loaded = withContext(Dispatchers.IO) {
                loadProfiles() to ConfigManager.profileSpecificControls()
            }
            profiles = loaded.first
            profileSpecificControls = loaded.second
            loadState = LoadState.Idle
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            loadState = LoadState.Error(failure.messageOrType())
        }
    }

    fun runProfileAction(
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = { Notifications.error(actionFailedTitle(), it) },
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
                                    actionFailedTitle(),
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
                                actionFailedTitle(),
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
                    val choice = try {
                        withContext(Dispatchers.IO) { prompt() }
                    } catch (failure: Throwable) {
                        Platform.screen().runOnUiThread {
                            busy = false
                            Notifications.error(actionFailedTitle(), failure.messageOrType())
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

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = PageGutter)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(19.dp)
        ) {
            val localSearchQuery = pageSearchQuery().trim()
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
                            label = translated("oneconfig.profiles.category.${it.name.lowercase()}", it.title),
                            selected = activeCategory == it,
                            icon = it.icon,
                            onClick = { activeCategory = it }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(
                        label = translated("oneconfig.profiles.separate_controls", "Separate controls"),
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
                        label = translated("oneconfig.profiles.open_folder", "Open folder"),
                        selected = false,
                        icon = "folder",
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                runCatching {
                                    Files.createDirectories(ConfigManager.PROFILES_DIR)
                                    DesktopHelper.open(ConfigManager.PROFILES_DIR.toFile())
                                }.onFailure {
                                    Notifications.error(
                                        translated("oneconfig.profiles.open_folder.failed", "Could not open the profiles folder"),
                                        it.messageOrType(),
                                    )
                                }
                            }
                        },
                    )
                }
            }

            val state = loadState
            if (state != LoadState.Idle) {
                Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        if (state is LoadState.Error) translated("oneconfig.profiles.load_failed", "Couldn't load profiles")
                        else translated("oneconfig.profiles.loading", "Loading profiles"),
                        state = state,
                        onRetry = { loadAttempt++ },
                    )
                }
            } else ProfilesGrid(
                profiles = visibleProfiles,
                existingProfileIds = existingProfileIds,
                showCreateProfile = activeCategory == ProfileCategory.All && localSearchQuery.isBlank(),
                emptyMessage = if (localSearchQuery.isBlank()) translated("oneconfig.profiles.no_favorites", "No favorite profiles.")
                    else translated("oneconfig.profiles.nomatch", "No profiles match \"%s\"", localSearchQuery),
                emptyIcon = if (localSearchQuery.isBlank()) "star" else "search",
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
                    Confirm.ask(
                        translated("oneconfig.profiles.delete.title", "Delete profile?"),
                        translated("oneconfig.profiles.delete.text", "The profile \"%s\" and all its settings will be deleted.", profile.name),
                        translated("oneconfig.profiles.delete.confirm", "Delete profile"),
                        onConfirm = { runProfileAction { ConfigManager.deleteProfile(profile.id) } },
                        icon = "trash",
                    )
                },
                onExport = { profile ->
                    runProfileDialog(
                        prompt = {
                            val defaultName = profile.name.replace(Regex("[^a-zA-Z0-9._-]"), "_") + ".zip"
                            TinyFdApi.getInstance().openSaveSelector(
                                translated("oneconfig.profiles.export.title", "Export profile"),
                                defaultName,
                                arrayOf("*.zip"),
                                translated("oneconfig.profiles.export.filter", "Zip archive"),
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

@Composable
private fun ProfileBusyOverlay() {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(BusyOverlayDelayMs)
        shown = true
    }
    if (!shown) return
    val theme = LocalTheme.current
    ModalDialog(onDismiss = {}, dismissOnScrim = false, dismissOnEscape = false) {
        Text(
            translated("oneconfig.profiles.busy.title", "Applying profile changes\u2026"),
            Modifier.fillMaxWidth(),
            theme.textColor,
            theme.typography.title,
            textAlign = TextAlign.Center,
        )
        Text(
            translated("oneconfig.profiles.busy.text", "The game will stop responding until it finishes."),
            Modifier.fillMaxWidth(),
            theme.textColorSecondary,
            theme.typography.body,
            textAlign = TextAlign.Center,
        )
        ProgressBar(null, Modifier.fillMaxWidth().padding(top = theme.spacing.xs))
    }
}

private fun actionFailedTitle(): String = translated("oneconfig.profiles.action_failed", "Profile action failed")

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
            name = if (id.isEmpty()) translated("oneconfig.profiles.default", "Default") else id,
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
    emptyIcon: String,
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
            EmptyState(emptyMessage, icon = emptyIcon)
        }
        return
    }

    val gridState = rememberLazyGridState()
    Box(Modifier.weight(1f)) {
        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier.fillMaxSize().padding(end = LocalTheme.current.spacing.l).smoothWheelScroll(gridState),
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
        Scrollbar(gridState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
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

    val borderColor by animateAccentColorAsState(
        creating || isHovered,
        if (creating || isHovered) Accent else theme.borderColor
    )
    val iconBackground by animateAccentColorAsState(
        creating || isHovered,
        if (creating || isHovered) Accent.copy(alpha = 0.18f) else theme.textColor.copy(alpha = 0.06f)
    )
    val iconColor by animateAccentColorAsState(
        creating || isHovered,
        if (creating || isHovered) Accent else theme.textColorSecondary
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ProfileCardHeight)
            .hoverGlow(shape)
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
                    placeholder = translated("oneconfig.profiles.name_placeholder", "Profile name"),
                    error = error,
                    enabled = !busy,
                    width = 150.dp,
                    onValueChange = onValueChange,
                    onSubmit = { if (!busy) onCreate() },
                )
            } else {
                Text(
                    translated("oneconfig.profiles.new", "New profile"),
                    color = theme.textColor.copy(alpha = 0.86f),
                    style = theme.typography.title.copy(fontWeight = FontWeight.Medium),
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
    var iconError by remember(profile.id) { mutableStateOf<String?>(null) }
    var nameError by remember(profile.id) { mutableStateOf<Pair<String, String>?>(null) }
    val keybindProp = remember(profile.id) { ProfileKeybinds.property(profile.id) }

    fun openEditor(next: ProfileEditor) {
        iconError = null
        nameError = null
        onEditorChange(next)
    }

    fun closeEditor() {
        onEditorChange(null)
        iconError = null
        nameError = null
    }

    fun menuEntries(): List<MenuEntry> = buildList<MenuEntry> {
        add(MenuAction(translated("oneconfig.profiles.menu.apply", "Apply"), onOpen, icon = "tick", enabled = !busy && !profile.active))
        if (profile.editable) {
            add(MenuAction(translated("oneconfig.profiles.menu.rename", "Rename"), { openEditor(ProfileEditor.Rename) }, icon = "text-input", enabled = !busy))
        }
        add(MenuAction(translated("oneconfig.profiles.menu.clone", "Clone"), { openEditor(ProfileEditor.Clone) }, icon = "copy", enabled = !busy))
        if (profile.editable) {
            add(MenuAction(translated("oneconfig.profiles.menu.icon", "Change icon"), { openEditor(ProfileEditor.Icon) }, icon = "paintbrush", enabled = !busy))
        }
        add(
            MenuAction(
                if (profile.favorite) translated("oneconfig.profiles.menu.unfavorite", "Remove favorite")
                else translated("oneconfig.profiles.menu.favorite", "Favorite"),
                onFavorite,
                icon = if (profile.favorite) "star-filled" else "star",
                enabled = !busy,
            ),
        )
        add(MenuAction(translated("oneconfig.profiles.menu.export", "Export / share"), onExport, icon = "down", enabled = !busy))
        if (profile.editable) {
            add(MenuSeparator)
            add(MenuAction(translated("oneconfig.profiles.menu.delete", "Delete"), onDelete, icon = "trash", enabled = !busy, danger = true))
        }
    }

    val selectionBorderColor by animateAccentColorAsState(
        profile.active,
        if (profile.active) Accent else Color.Transparent
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ProfileCardHeight)
            .hoverGlow(shape)
            .background(theme.modCardBackground, shape)
            .border(3.dp, selectionBorderColor, shape)
            .border(
                1.dp, Brush.verticalGradient(
                    listOf(theme.borderColor, theme.borderColor.copy(0f))
                ), shape
            )
            .contextMenu(enabled = editor == null) { menuEntries() }
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
            if (editor == ProfileEditor.Icon) {
                ActionIcon("close", enabled = !busy, tint = theme.textColorSecondary, onClick = ::closeEditor)
            } else {
                if (profile.favorite) {
                    ActionIcon("star-filled", enabled = !busy, tint = theme.favoriteColor) { onFavorite() }
                }
                if (profile.active || isHovered || menuOpen) {
                    Box {
                        ActionIcon("settings", enabled = !busy, tint = theme.textColorSecondary, hoveredTint = Accent) {
                            onMenuOpenChange(true)
                        }
                        DropdownMenu(menuOpen, { onMenuOpenChange(false) }, menuEntries(), placement = Placement.BelowEnd)
                    }
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val compact = editor == ProfileEditor.Icon
            Icon(profile.icon, modifier = Modifier.size(if (compact) 42.dp else 64.dp), color = theme.textColor)
            Spacer(Modifier.height(if (compact) 14.dp else 24.dp))
            if (editor == ProfileEditor.Icon) {
                ProfileIconPicker(
                    selectedIcon = profile.icon,
                    enabled = !busy,
                    onIconChange = {
                        iconError = null
                        onIconChange(
                            it,
                            {
                                onEditorChange(null)
                                iconError = null
                            },
                            { message -> iconError = message },
                        )
                    },
                )
                iconError?.let { ProfileError(it, 150.dp) }
            } else {
                Text(
                    profile.name,
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    theme.textColor,
                    theme.typography.title.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (editor == null && (profile.active || isHovered || menuOpen || keybindProp.get()?.isBound == true)) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)) {
                KeybindOption(KeybindOptionData(keybindProp))
            }
        }
    }

    if (editor == ProfileEditor.Rename || editor == ProfileEditor.Clone) {
        val renaming = editor == ProfileEditor.Rename
        val initial = if (renaming) profile.name else suggestedCloneName
        val title = if (renaming) translated("oneconfig.profiles.rename.title", "Rename profile")
            else translated("oneconfig.profiles.clone.title", "Clone profile")
        key(editor) {
            TextPromptDialog(
                title = title,
                initial = initial,
                onConfirm = { name ->
                    if (!busy) {
                        val submit = if (renaming) onRename else onClone
                        submit(
                            name,
                            {
                                nameError = null
                                onEditorChange(null)
                            },
                            { message ->
                                nameError = name to message
                                if (name == initial) Notifications.error(title, message)
                            },
                        )
                    }
                },
                onDismiss = { if (!busy) closeEditor() },
                confirmLabel = if (renaming) translated("oneconfig.profiles.menu.rename", "Rename")
                    else translated("oneconfig.profiles.menu.clone", "Clone"),
                validate = { value ->
                    val failed = nameError
                    when {
                        value.isBlank() -> translated("oneconfig.profiles.name_required", "Enter a name")
                        failed != null && failed.first == value -> failed.second
                        else -> null
                    }
                },
            )
        }
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
    val backgroundColor by animateAccentColorAsState(
        listOf(selected, enabled, isHovered),
        if (selected) Accent.copy(alpha = if (enabled) 0.22f else 0.10f)
        else if (enabled && isHovered) theme.textColor.copy(alpha = 0.08f)
        else Color.Transparent
    )
    val borderColor by animateAccentColorAsState(
        selected && enabled,
        if (selected && enabled) Accent else theme.borderColor.copy(alpha = 0f)
    )
    val iconColor by Motion.animateColorAsState(
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
    val iconColor by Motion.animateColorAsState(
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
    width: Dp,
    error: String? = null,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier.width(width),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextField(
            value,
            onValueChange,
            Modifier.fillMaxWidth(),
            placeholder,
            null,
            Int.MAX_VALUE,
            error != null,
            enabled,
            true,
            true,
            14.sp,
            null,
            false,
            focusRequester,
            null,
            null,
            onSubmit,
        )
        if (error != null) ProfileError(error, width)
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }
}

@Composable
private fun ProfileError(message: String, width: Dp) {
    val theme = LocalTheme.current
    Text(
        message,
        Modifier.width(width).padding(top = 4.dp),
        theme.error,
        theme.typography.caption,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
}
