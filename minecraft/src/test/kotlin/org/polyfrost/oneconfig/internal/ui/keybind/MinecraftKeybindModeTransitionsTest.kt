package org.polyfrost.oneconfig.internal.ui.keybind

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MinecraftKeybindModeTransitionsTest {
    @Test
    fun reenablingSeparateControlsRestoresTheSelectedProfile() {
        val controls = FakeControls(
            live = "profile-a",
            profiles = mutableMapOf("A" to "profile-a", "B" to "default"),
        )
        controls.transitions.initialize("A", true)

        controls.transitions.change(false)
        assertEquals("profile-a", controls.shared)

        controls.preSaveLiveOwner()
        controls.transitions.profileChanged("B")
        assertEquals("profile-a", controls.live)
        controls.transitions.change(true)

        assertEquals("default", controls.live)
        assertEquals("profile-a", controls.profiles["A"])
        assertEquals("default", controls.profiles["B"])
    }

    @Test
    fun editsMadeWhileSharedAreKeptForTheNextSharedSession() {
        val controls = FakeControls(
            live = "profile-a",
            profiles = mutableMapOf("A" to "profile-a", "B" to "profile-b"),
        )
        controls.transitions.initialize("A", true)

        controls.transitions.change(false)
        controls.live = "edited-shared"
        controls.preSaveLiveOwner()
        controls.transitions.profileChanged("B")
        controls.transitions.change(true)

        assertEquals("profile-b", controls.live)
        assertEquals("edited-shared", controls.shared)

        controls.transitions.change(false)
        assertEquals("edited-shared", controls.live)
    }

    @Test
    fun disablingSeparateControlsRestoresAnExistingSharedMapping() {
        val controls = FakeControls(
            live = "profile-a",
            profiles = mutableMapOf("A" to "profile-a"),
        )
        controls.transitions.initialize("A", true)
        controls.shared = "shared"

        controls.transitions.change(false)

        assertEquals("shared", controls.live)
        assertEquals("profile-a", controls.profiles["A"])
        assertEquals("shared", controls.shared)
    }

    @Test
    fun profileCreatedWhileSharedUsesCurrentMappingsOnFirstEnable() {
        val controls = FakeControls(live = "shared", profiles = mutableMapOf())
        controls.transitions.initialize("old", false)

        controls.transitions.profileCreated("new")
        controls.transitions.change(true)

        assertEquals("shared", controls.live)
        assertEquals("shared", controls.profiles["new"])
    }

    @Test
    fun initializationPublishesOwnerOnlyAfterInitialSave() {
        val separate = FakeControls(
            live = "options",
            profiles = mutableMapOf("A" to "saved-profile"),
        )
        separate.transitions.initialize("A", true)

        assertEquals("options", separate.live)
        assertEquals("options", separate.profiles["A"])
        assertNull(separate.shared)
        assertEquals(listOf(null), separate.ownersSeenDuringProfileSave)
        assertEquals(MinecraftKeybindLiveOwner("A", true), separate.transitions.liveOwner())

        val shared = FakeControls(
            live = "saved-options",
            profiles = mutableMapOf("A" to "saved-profile"),
        )
        shared.transitions.initialize("A", false)
        assertEquals("saved-options", shared.shared)
        assertEquals("saved-options", shared.live)
        assertEquals(MinecraftKeybindLiveOwner("A", false), shared.transitions.liveOwner())
    }

    @Test
    fun switchingSeparateProfilesOnlyActivatesTheAlreadyPresavedTarget() {
        val controls = FakeControls(
            live = "profile-a",
            profiles = mutableMapOf("A" to "profile-a", "B" to "profile-b"),
        )
        controls.transitions.initialize("A", true)
        controls.profileSaveCalls.clear()

        controls.preSaveLiveOwner()
        controls.profileSaveCalls.clear()
        controls.transitions.profileChanged("B")

        assertEquals("profile-b", controls.live)
        assertEquals(listOf("B"), controls.profileSaveCalls)
        assertEquals(MinecraftKeybindLiveOwner("B", true), controls.transitions.liveOwner())
    }

    @Test
    fun earlyOptionsSaveUsesOldModeUntilModeTransitionCompletes() {
        val controls = FakeControls(
            live = "shared",
            profiles = mutableMapOf("B" to "default"),
        )
        controls.transitions.initialize("B", false)

        // ConfigManager has committed ON, but an earlier listener queued Options.save before this
        // controller receives its mode callback. Its own owner must remain shared until change().
        controls.optionsSaved()
        assertEquals(listOf(MinecraftKeybindLiveOwner("B", false)), controls.capturedOptionsOwners)
        assertEquals("default", controls.profiles["B"])

        controls.transitions.change(true)

        assertEquals("default", controls.live)
        assertEquals("shared", controls.shared)
        assertEquals("default", controls.profiles["B"])
        assertEquals(MinecraftKeybindLiveOwner("B", true), controls.transitions.liveOwner())
    }

    @Test
    fun optionsSaveTriggeredByRestoreIsSuppressedUntilNewOwnerIsPublished() {
        val controls = FakeControls(
            live = "shared",
            profiles = mutableMapOf("B" to "profile-b"),
        )
        controls.transitions.initialize("B", false)
        controls.capturedOptionsOwners.clear()

        controls.transitions.change(true)

        // Fake restore invokes optionsSaved(), matching apply() -> Options.save() in production.
        // The transition explicitly saves B afterwards; the hook must not rewrite shared meanwhile.
        assertEquals(emptyList<MinecraftKeybindLiveOwner>(), controls.capturedOptionsOwners)
        assertEquals("shared", controls.shared)
        assertEquals("profile-b", controls.profiles["B"])
        assertEquals("profile-b", controls.live)
    }

    @Test
    fun queuedOptionsSaveBeforeProfileSwitchStillBelongsToOldProfile() {
        val controls = FakeControls(
            live = "edited-a",
            profiles = mutableMapOf("A" to "old-a", "B" to "profile-b"),
        )
        controls.transitions.initialize("A", true)
        controls.live = "queued-save-a"
        controls.capturedOptionsOwners.clear()

        controls.optionsSaved()
        controls.preSaveLiveOwner()
        controls.transitions.profileChanged("B")

        assertEquals(listOf(MinecraftKeybindLiveOwner("A", true)), controls.capturedOptionsOwners)
        assertEquals("queued-save-a", controls.profiles["A"])
        assertEquals("profile-b", controls.profiles["B"])
        assertEquals("profile-b", controls.live)
    }

    @Test
    fun failedEnableCompensatesWithoutSavingTheBrokenOwnerAgain() {
        val controls = FakeControls(
            live = "shared",
            profiles = mutableMapOf("B" to "profile-b"),
        )
        controls.transitions.initialize("B", false)
        controls.failProfileSave = "B"

        assertThrows(IllegalStateException::class.java) {
            controls.transitions.change(true)
        }

        assertEquals("shared", controls.live)
        assertEquals(MinecraftKeybindLiveOwner("B", false), controls.transitions.liveOwner())

        // This matches ConfigManager rolling the preference back after the listener failure.
        controls.transitions.change(false)
        assertEquals("shared", controls.live)
        assertEquals(MinecraftKeybindLiveOwner("B", false), controls.transitions.liveOwner())
    }

    @Test
    fun failedDisableCompensatesWithoutSavingTheBrokenSharedSlotAgain() {
        val controls = FakeControls(
            live = "profile-a",
            profiles = mutableMapOf("A" to "profile-a"),
        )
        controls.transitions.initialize("A", true)
        controls.shared = "shared"
        controls.failSharedSave = true

        assertThrows(IllegalStateException::class.java) {
            controls.transitions.change(false)
        }

        assertEquals("profile-a", controls.live)
        assertEquals(MinecraftKeybindLiveOwner("A", true), controls.transitions.liveOwner())
        controls.transitions.change(true)
        assertEquals("profile-a", controls.live)
    }

    @Test
    fun failedPostCommitProfileRestoreStillAdoptsTheTargetOwner() {
        val controls = FakeControls(
            live = "profile-a",
            profiles = mutableMapOf("A" to "profile-a", "B" to "profile-b"),
        )
        controls.transitions.initialize("A", true)
        controls.failProfileRestore = "B"

        assertThrows(IllegalStateException::class.java) {
            controls.transitions.profileChanged("B")
        }

        assertEquals("profile-a", controls.live)
        assertEquals(MinecraftKeybindLiveOwner("B", true), controls.transitions.liveOwner())
    }

    @Test
    fun failedRootRestoreAfterDeleteStillAdoptsTheRootOwner() {
        val controls = FakeControls(
            live = "profile-a",
            profiles = mutableMapOf("A" to "profile-a", "" to "root"),
        )
        controls.transitions.initialize("A", true)
        controls.failProfileRestore = ""

        assertThrows(IllegalStateException::class.java) {
            controls.transitions.profileDeleted("A")
        }

        assertEquals("profile-a", controls.live)
        assertEquals(MinecraftKeybindLiveOwner("", true), controls.transitions.liveOwner())
    }

    private class FakeControls(
        var live: String,
        val profiles: MutableMap<String, String>,
    ) {
        var shared: String? = null
        val capturedOptionsOwners = mutableListOf<MinecraftKeybindLiveOwner>()
        val ownersSeenDuringProfileSave = mutableListOf<MinecraftKeybindLiveOwner?>()
        val profileSaveCalls = mutableListOf<String>()
        var failProfileSave: String? = null
        var failProfileRestore: String? = null
        var failSharedSave = false

        val transitions: MinecraftKeybindModeTransitions

        init {
            lateinit var initializedTransitions: MinecraftKeybindModeTransitions
            initializedTransitions = MinecraftKeybindModeTransitions(
                saveProfile = { profile ->
                    profileSaveCalls += profile
                    ownersSeenDuringProfileSave += initializedTransitions.liveOwner()
                    if (profile == failProfileSave) throw IllegalStateException("profile save failed")
                    profiles[profile] = live
                },
                restoreProfile = { profile ->
                    if (profile == failProfileRestore) throw IllegalStateException("profile restore failed")
                    val saved = profiles[profile]
                    if (saved != null) {
                        live = saved
                        optionsSaved(initializedTransitions)
                    }
                },
                saveShared = {
                    if (failSharedSave) throw IllegalStateException("shared save failed")
                    shared = live
                },
                restoreShared = {
                    val saved = shared
                    if (saved != null) {
                        live = saved
                        optionsSaved(initializedTransitions)
                    }
                },
            )
            transitions = initializedTransitions
        }

        fun optionsSaved() = optionsSaved(transitions)

        fun preSaveLiveOwner() {
            val owner = transitions.liveOwner() ?: return
            if (owner.separateControls) profiles[owner.profile] = live
            else shared = live
        }

        private fun optionsSaved(transitions: MinecraftKeybindModeTransitions) {
            transitions.captureSavedOptions { owner ->
                capturedOptionsOwners += owner
                if (owner.separateControls) profiles[owner.profile] = live
                else shared = live
            }
        }
    }
}
