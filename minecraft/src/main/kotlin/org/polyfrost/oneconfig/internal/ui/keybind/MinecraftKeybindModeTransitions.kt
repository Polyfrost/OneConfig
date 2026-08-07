package org.polyfrost.oneconfig.internal.ui.keybind

internal data class MinecraftKeybindLiveOwner(
    val profile: String,
    val separateControls: Boolean,
)

internal class MinecraftKeybindModeTransitions(
    private val saveProfile: (String) -> Unit,
    private val restoreProfile: (String) -> Unit,
    private val saveShared: () -> Unit,
    private val restoreShared: () -> Unit,
) {
    private data class PublishedState(
        val owner: MinecraftKeybindLiveOwner?,
        val captureOptionsSaves: Boolean,
    )

    private val stateLock = Any()
    private var publishedState = PublishedState(null, false)

    /**
     * Runs an Options.save capture against one stable owner. Transitions hold the same reentrant
     * lock and temporarily disable captures, so the save performed by restoreProfile/restoreShared
     * cannot publish the newly applied mappings into the previous owner.
     */
    fun captureSavedOptions(capture: (MinecraftKeybindLiveOwner) -> Unit) {
        synchronized(stateLock) {
            val state = publishedState
            if (!state.captureOptionsSaves) return
            val owner = state.owner ?: return
            capture(owner)
        }
    }

    fun liveOwner(): MinecraftKeybindLiveOwner? = synchronized(stateLock) { publishedState.owner }

    fun initialize(profile: String, separateControls: Boolean) {
        synchronized(stateLock) {
            check(publishedState.owner == null) { "Minecraft keybind profiles are already initialized" }
            if (separateControls) {
                // options.txt already contains the mappings that were live when this profile last ran.
                // It is also where vanilla persists edits made immediately before shutdown.
                saveProfile(profile)
            } else {
                // While controls are shared, options.txt is the live source of truth.
                saveShared()
            }
            publishedState = PublishedState(
                MinecraftKeybindLiveOwner(profile, separateControls),
                true,
            )
        }
    }

    fun profileChanged(newProfile: String) {
        val oldOwner = requireOwner()
        if (oldOwner.profile == newProfile) return
        val newOwner = oldOwner.copy(profile = newProfile)
        transitionTo(newOwner) { publish ->
            if (oldOwner.separateControls) {
                // ConfigManager saves the live owner before committing the active profile.
                try {
                    restoreProfile(newProfile)
                } finally {
                    // The config backend is already committed. Even a corrupt target snapshot must
                    // not leave later Options.save calls writing the inherited live keys into old.
                    publish()
                }
                saveProfile(newProfile)
            } else {
                // Shared mappings stay live; only their selected profile label changes.
                publish()
            }
        }
    }

    fun profileCreated(profile: String) {
        val oldOwner = requireOwner()
        val newOwner = oldOwner.copy(profile = profile)
        transitionTo(newOwner) { publish ->
            // The backend has already committed the new profile and its mappings stay live.
            publish()
            if (oldOwner.separateControls) {
                // A newly created profile intentionally inherits the mappings which are live now.
                saveProfile(profile)
            } else {
                saveShared()
            }
        }
    }

    fun profileRenamed(oldProfile: String, newProfile: String) {
        val oldOwner = requireOwner()
        if (oldOwner.profile != oldProfile) return
        transitionTo(oldOwner.copy(profile = newProfile)) { publish -> publish() }
    }

    fun profileDeleted(profile: String) {
        val oldOwner = requireOwner()
        if (oldOwner.profile != profile) return
        val newOwner = oldOwner.copy(profile = "")
        transitionTo(newOwner) { publish ->
            if (oldOwner.separateControls) {
                try {
                    restoreProfile("")
                } finally {
                    publish()
                }
                saveProfile("")
            } else {
                publish()
                saveShared()
            }
        }
    }

    fun change(separateControls: Boolean) {
        val oldOwner = requireOwner()
        if (oldOwner.separateControls == separateControls) return
        val newOwner = oldOwner.copy(separateControls = separateControls)
        transitionTo(
            newOwner,
            rollbackAfterPublish = {
                // ConfigManager rolls the persisted preference back when this listener fails. Put
                // the mappings back too, without first trying to save the owner which just failed.
                if (oldOwner.separateControls) restoreProfile(oldOwner.profile) else restoreShared()
            },
        ) { publish ->
            if (separateControls) {
                // Preserve edits made in shared mode, then restore the selected profile's mappings.
                saveShared()
                restoreProfile(oldOwner.profile)
                publish()
                saveProfile(oldOwner.profile)
            } else {
                // Preserve the selected profile before replacing its mappings with the shared set.
                saveProfile(oldOwner.profile)
                restoreShared()
                publish()
                saveShared()
            }
        }
    }

    private fun requireOwner(): MinecraftKeybindLiveOwner =
        synchronized(stateLock) { checkNotNull(publishedState.owner) { "Minecraft keybind profiles are not initialized" } }

    private fun transitionTo(
        newOwner: MinecraftKeybindLiveOwner,
        rollbackAfterPublish: (() -> Unit)? = null,
        action: (() -> Unit) -> Unit,
    ) {
        synchronized(stateLock) {
            val previous = publishedState
            checkNotNull(previous.owner) { "Minecraft keybind profiles are not initialized" }
            check(previous.captureOptionsSaves) { "Minecraft keybind profile transition is already running" }
            publishedState = previous.copy(captureOptionsSaves = false)
            var ownerPublished = false
            val publish = {
                check(!ownerPublished) { "Minecraft keybind live owner was already published" }
                publishedState = PublishedState(newOwner, false)
                ownerPublished = true
            }
            try {
                action(publish)
                check(ownerPublished) { "Minecraft keybind transition did not publish its live owner" }
                publishedState = PublishedState(newOwner, true)
            } catch (failure: Throwable) {
                var liveOwner = if (ownerPublished) newOwner else previous.owner
                if (ownerPublished && rollbackAfterPublish != null) {
                    try {
                        rollbackAfterPublish()
                        liveOwner = previous.owner
                    } catch (rollbackFailure: Throwable) {
                        failure.addSuppressed(rollbackFailure)
                    }
                }
                // If compensation fails, keep routing Options.save to the owner whose mappings are
                // most likely still live instead of silently writing them into the previous slot.
                publishedState = PublishedState(liveOwner, true)
                throw failure
            }
        }
    }
}
