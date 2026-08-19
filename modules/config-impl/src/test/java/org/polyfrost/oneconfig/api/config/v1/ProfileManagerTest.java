/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.config.v1;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;
import org.polyfrost.oneconfig.api.config.v1.backend.Backend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ProfileManagerTest {
    private static final String PROFILE_A = "oc_test_profile_a";
    private static final String PROFILE_B = "oc_test_profile_b";
    private static final String PROFILE_C = "oc_test_profile_c";
    private static final String BLANK_CONFIG = "profile_blank_test.json";
    private static final String CLONE_CONFIG = "profile_clone_test.json";
    private static final String ROOT_NESTED_CONFIG = "oc_profile_root/nested.json";
    private static final String ROOT_PERSISTED_NESTED_CONFIG = "oc_profile_persisted/nested.json";
    private static final String ROOT_ORPHAN_HUD = "huds/oc_profile_orphan_hud.json";
    private static final String DIRECT_TREE_CONFIG = "profile_direct_tree_test.json";
    private static final String GLOBAL_UI_TREE_CONFIG = "profile_global_ui_tree_test.json";
    private static final String SEEDED_CONFIG = "profile_seeded_test.json";

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        ConfigManager.active();
        ConfigManager.openProfile("");
        cleanupProfiles();
    }

    @AfterEach
    void tearDown() throws IOException {
        ConfigManager.openProfile("");
        cleanupProfiles();
    }

    @Test
    void listsRootAndLocalProfiles() {
        ConfigManager.createProfile(PROFILE_A);
        ConfigManager.openProfile("");

        assertTrue(ConfigManager.profiles().contains(""));
        assertTrue(ConfigManager.profiles().contains(PROFILE_A));
    }

    @Test
    void createsAndOpensProfile() {
        ConfigManager.createProfile(PROFILE_A);

        assertEquals(PROFILE_A, ConfigManager.activeProfile());
        assertTrue(Files.isDirectory(ConfigManager.PROFILES_DIR.resolve(PROFILE_A)));
    }

    @Test
    void createsBlankProfile() {
        ProfileTestConfig config = new ProfileTestConfig(BLANK_CONFIG);
        config.initialize(false);
        config.enabled = true;
        config.save();

        ConfigManager.createProfile(PROFILE_A);

        assertFalse(config.enabled);
    }

    @Test
    void noListenerIsEverNotifiedWhileTheConfigManagerMonitorIsHeld() {
        List<String> violations = new ArrayList<>();
        ConfigManager.ProfileChangeListener listener = new ConfigManager.ProfileChangeListener() {
            private void check(String callback) {
                if (Thread.holdsLock(ConfigManager.class)) violations.add(callback);
            }

            @Override
            public void onProfileChanged(String newProfile) {
                check("onProfileChanged");
            }

            @Override
            public void onProfileSaving(String profile) {
                check("onProfileSaving");
            }

            @Override
            public void onProfileCreated(String profile) {
                check("onProfileCreated");
            }

            @Override
            public void onProfileRenamed(String oldProfile, String newProfile) {
                check("onProfileRenamed");
            }

            @Override
            public void onProfileDeleted(String profile) {
                check("onProfileDeleted");
            }

            @Override
            public void onProfileSpecificControlsChanged(boolean enabled) {
                check("onProfileSpecificControlsChanged");
            }
        };
        ConfigManager.addProfileChangeListener(listener);
        boolean controls = ConfigManager.profileSpecificControls();
        try {
            ConfigManager.createProfile(PROFILE_A);
            ConfigManager.cloneProfile(PROFILE_A, PROFILE_B);
            ConfigManager.renameProfile(PROFILE_B, PROFILE_C);
            ConfigManager.setProfileSpecificControls(!controls);
            ConfigManager.openProfile("");
            ConfigManager.deleteProfile(PROFILE_C);
            ConfigManager.deleteProfile(PROFILE_A);
        } finally {
            ConfigManager.setProfileSpecificControls(controls);
            ConfigManager.removeProfileChangeListener(listener);
        }

        assertEquals(List.of(), violations, "notified while holding the ConfigManager monitor");
    }

    @Test
    void onProfileCreatedCanSeedTheNewProfileAndTheValuesArePersisted() throws IOException {
        ProfileTestConfig config = new ProfileTestConfig(SEEDED_CONFIG);
        config.initialize(false);
        ConfigManager.ProfileChangeListener listener = new ConfigManager.ProfileChangeListener() {
            @Override
            public void onProfileChanged(String newProfile) {
            }

            @Override
            public void onProfileCreated(String profile) {
                assertFalse(config.enabled, "the seam must run after configs are reset to defaults");
                config.enabled = true;
            }
        };
        ConfigManager.addProfileChangeListener(listener);
        try {
            ConfigManager.createProfile(PROFILE_A);
            assertTrue(config.enabled);

            Path file = ConfigManager.profileDir(PROFILE_A).resolve(SEEDED_CONFIG);
            assertTrue(Files.isRegularFile(file));
            assertTrue(new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8)
                            .replace(" ", "").contains("\"enabled\":true"),
                    "seeded values must be on disk before the profile-changed listeners run");
        } finally {
            ConfigManager.removeProfileChangeListener(listener);
        }
    }

    @Test
    void createsBlankProfileForDirectlyRegisteredTrees() {
        Tree direct = ConfigManager.active().register(
                Tree.tree(DIRECT_TREE_CONFIG).put(Properties.simple("enabled", "Enabled", "", false))
        ).get();
        direct.getProp("enabled").setAs(true);
        ConfigManager.active().save(DIRECT_TREE_CONFIG);

        ConfigManager.createProfile(PROFILE_A);

        assertEquals(Boolean.FALSE, ConfigManager.active().get(DIRECT_TREE_CONFIG).getProp("enabled").getAs());
    }

    @Test
    void creatingBlankProfileDoesNotResetGlobalUiTrees() {
        Tree global = Tree.tree(GLOBAL_UI_TREE_CONFIG)
                .put(Properties.simple("enabled", "Enabled", "", false));
        global.addMetadata(Backend.UI_ONLY_METADATA, Boolean.TRUE);
        global = ConfigManager.active().register(global).get();
        global.getProp("enabled").setAs(true);

        ConfigManager.createProfile(PROFILE_A);

        assertEquals(Boolean.TRUE, ConfigManager.active().get(GLOBAL_UI_TREE_CONFIG).getProp("enabled").getAs());
    }

    @Test
    void clonesProfileContents() {
        ProfileTestConfig config = new ProfileTestConfig(CLONE_CONFIG);
        config.initialize(false);
        config.enabled = true;
        config.save();

        ConfigManager.cloneProfile("", PROFILE_A);

        assertEquals(PROFILE_A, ConfigManager.activeProfile());
        assertTrue(config.enabled);
    }

    @Test
    void cloneDoesNotMergeWithOrDeleteADirectoryCreatedWhileSaving() throws IOException {
        Path target = ConfigManager.profileDir(PROFILE_A);
        Path marker = target.resolve("belongs-to-someone-else.txt");
        ConfigManager.ProfileChangeListener listener = new ConfigManager.ProfileChangeListener() {
            @Override
            public void onProfileChanged(String newProfile) {
            }

            @Override
            public void onProfileSaving(String profile) {
                try {
                    Files.createDirectories(target);
                    Files.writeString(marker, "keep");
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        ConfigManager.addProfileChangeListener(listener);
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> ConfigManager.cloneProfile("", PROFILE_A));
            assertEquals("keep", Files.readString(marker));
        } finally {
            ConfigManager.removeProfileChangeListener(listener);
        }
    }

    @Test
    void profileListenersCannotStartANestedLifecycleOperation() {
        AtomicReference<Throwable> nestedFailure = new AtomicReference<>();
        ConfigManager.ProfileChangeListener listener = new ConfigManager.ProfileChangeListener() {
            @Override
            public void onProfileChanged(String newProfile) {
            }

            @Override
            public void onProfileCreated(String profile) {
                try {
                    ConfigManager.deleteProfile(profile);
                } catch (Throwable failure) {
                    nestedFailure.set(failure);
                }
            }
        };
        ConfigManager.addProfileChangeListener(listener);
        try {
            ConfigManager.createProfile(PROFILE_A);

            assertInstanceOf(IllegalStateException.class, nestedFailure.get());
            assertEquals(PROFILE_A, ConfigManager.activeProfile());
            assertTrue(Files.isDirectory(ConfigManager.profileDir(PROFILE_A)));
        } finally {
            ConfigManager.removeProfileChangeListener(listener);
        }
    }

    @Test
    void persistsFavorites() {
        ConfigManager.createProfile(PROFILE_A);
        ConfigManager.setFavoriteProfile(PROFILE_A, true);

        assertTrue(ConfigManager.isFavoriteProfile(PROFILE_A));
        assertTrue(ConfigManager.favoriteProfiles().contains(PROFILE_A));

        ConfigManager.setFavoriteProfile(PROFILE_A, false);
        assertFalse(ConfigManager.isFavoriteProfile(PROFILE_A));
    }

    @Test
    void persistsDefaultProfileFavorite() {
        ConfigManager.setFavoriteProfile("", true);

        assertTrue(ConfigManager.isFavoriteProfile(""));
        assertTrue(ConfigManager.favoriteProfiles().contains(""));

        ConfigManager.setFavoriteProfile("", false);
        assertFalse(ConfigManager.isFavoriteProfile(""));
        assertFalse(ConfigManager.favoriteProfiles().contains(""));
    }

    @Test
    void persistsProfileSpecificControlsPreference() {
        AtomicReference<Boolean> notified = new AtomicReference<>();
        ConfigManager.ProfileChangeListener listener = new ConfigManager.ProfileChangeListener() {
            @Override
            public void onProfileChanged(String newProfile) {
            }

            @Override
            public void onProfileSpecificControlsChanged(boolean enabled) {
                notified.set(enabled);
            }
        };
        ConfigManager.addProfileChangeListener(listener);
        try {
            ConfigManager.setProfileSpecificControls(false);
            assertFalse(ConfigManager.profileSpecificControls());
            assertEquals(Boolean.FALSE, notified.get());

            ConfigManager.setProfileSpecificControls(true);
            assertTrue(ConfigManager.profileSpecificControls());
            assertEquals(Boolean.TRUE, notified.get());
        } finally {
            ConfigManager.removeProfileChangeListener(listener);
        }
    }

    @Test
    void failedProfileSpecificControlsTransitionRollsBackThePreferenceAndListeners() {
        List<Boolean> observed = new ArrayList<>();
        ConfigManager.ProfileChangeListener observer = new ConfigManager.ProfileChangeListener() {
            @Override
            public void onProfileChanged(String newProfile) {
            }

            @Override
            public void onProfileSpecificControlsChanged(boolean enabled) {
                observed.add(enabled);
            }
        };
        ConfigManager.ProfileChangeListener failing = new ConfigManager.ProfileChangeListener() {
            @Override
            public void onProfileChanged(String newProfile) {
            }

            @Override
            public void onProfileSpecificControlsChanged(boolean enabled) {
                if (!enabled) throw new IllegalStateException("transition failed");
            }
        };
        ConfigManager.addProfileChangeListener(observer);
        ConfigManager.addProfileChangeListener(failing);
        try {
            assertThrows(IllegalStateException.class,
                    () -> ConfigManager.setProfileSpecificControls(false));

            assertTrue(ConfigManager.profileSpecificControls());
            assertEquals(List.of(false, true), observed);
        } finally {
            ConfigManager.removeProfileChangeListener(failing);
            ConfigManager.removeProfileChangeListener(observer);
        }
    }

    @Test
    void exportsProfileAsZip() throws IOException {
        ConfigManager.createProfile(PROFILE_A);
        Files.writeString(ConfigManager.profileDir(PROFILE_A).resolve("marker.txt"), "profile data");
        Path archive = tempDir.resolve("profile.zip");

        ConfigManager.exportProfile(PROFILE_A, archive);

        assertTrue(Files.isRegularFile(archive));
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            assertNotNull(zip.getEntry("marker.txt"));
        }
    }

    @Test
    void importsAZipDroppedIntoTheProfilesDirectory() throws IOException {
        Path archive = ConfigManager.PROFILES_DIR.resolve(PROFILE_A + ".zip");
        Files.createDirectories(ConfigManager.PROFILES_DIR);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry(PROFILE_A + "/marker.txt"));
            zip.write("profile data".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("__MACOSX/._" + PROFILE_A));
            zip.closeEntry();
        }

        assertTrue(ConfigManager.profiles().contains(PROFILE_A));
        assertEquals("profile data", Files.readString(ConfigManager.profileDir(PROFILE_A).resolve("marker.txt")));
        assertFalse(Files.exists(archive));
    }

    @Test
    void doesNotImportAZipOverAnExistingProfile() throws IOException {
        ConfigManager.createProfile(PROFILE_A);
        ConfigManager.openProfile("");
        Path archive = ConfigManager.PROFILES_DIR.resolve(PROFILE_A + ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("marker.txt"));
            zip.closeEntry();
        }

        assertTrue(ConfigManager.profiles().contains(PROFILE_A));
        assertFalse(Files.exists(ConfigManager.profileDir(PROFILE_A).resolve("marker.txt")));
        assertTrue(Files.exists(archive));
        Files.delete(archive);
    }

    @Test
    void failedExportDoesNotDeleteTheExistingDestination() throws IOException {
        ConfigManager.createProfile(PROFILE_A);
        Path destination = Files.createDirectory(tempDir.resolve("existing-destination"));
        Path marker = Files.writeString(destination.resolve("keep.txt"), "keep");

        assertThrows(IllegalStateException.class, () -> ConfigManager.exportProfile(PROFILE_A, destination));

        assertEquals("keep", Files.readString(marker));
    }

    @Test
    void rejectsExportThroughASymlinkBackIntoTheProfile() throws IOException {
        Path linkedParent = tempDir.resolve("profile-link");
        try {
            Files.createSymbolicLink(linkedParent, ConfigManager.profileDir("").toAbsolutePath());
        } catch (UnsupportedOperationException | IOException ignored) {
            return;
        }

        assertThrows(IllegalArgumentException.class,
                () -> ConfigManager.exportProfile("", linkedParent.resolve("recursive.zip")));
        assertFalse(Files.exists(ConfigManager.profileDir("").resolve("recursive.zip")));
    }

    @Test
    void clonesAndExportsRootOwnedSubdirectoriesWhileAnotherProfileIsActive() throws IOException {
        Tree rootOnly = Tree.tree(ROOT_NESTED_CONFIG).put(
                Properties.simple("enabled", "Enabled", "", true)
        );
        rootOnly.addMetadata(ConfigManager.PROFILE_LOCAL_METADATA, true);
        ConfigManager.active().register(rootOnly);
        ConfigManager.active().save(ROOT_NESTED_CONFIG);
        Path orphanHud = ConfigManager.profileDir("").resolve(ROOT_ORPHAN_HUD);
        Files.createDirectories(orphanHud.getParent());
        Files.writeString(orphanHud, "orphan HUD data");
        ConfigManager.createProfile(PROFILE_A);

        Path archive = tempDir.resolve("root-profile.zip");
        ConfigManager.exportProfile("", archive);
        ConfigManager.cloneProfile("", PROFILE_B);

        assertTrue(Files.isRegularFile(ConfigManager.profileDir(PROFILE_B).resolve(ROOT_NESTED_CONFIG)));
        assertEquals("orphan HUD data",
                Files.readString(ConfigManager.profileDir(PROFILE_B).resolve(ROOT_ORPHAN_HUD)));
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            assertNotNull(zip.getEntry(ROOT_NESTED_CONFIG));
            assertNotNull(zip.getEntry(ROOT_ORPHAN_HUD));
        }
    }

    @Test
    void remembersRootOwnedSubdirectoriesAfterTheirModIsNoLongerLoaded() throws IOException {
        Property<?> ownedSubdirs = ConfigManager.internal().get("profiles.json").getProp("ownedProfileSubdirs");
        Object previousOwnedSubdirs = ownedSubdirs.get();
        Tree rootOnly = Tree.tree(ROOT_PERSISTED_NESTED_CONFIG).put(
                Properties.simple("enabled", "Enabled", "", true)
        );
        rootOnly.addMetadata(ConfigManager.PROFILE_LOCAL_METADATA, true);
        try {
            ConfigManager.active().register(rootOnly);
            ConfigManager.active().save(ROOT_PERSISTED_NESTED_CONFIG);
            ConfigManager.active().unregister(ROOT_PERSISTED_NESTED_CONFIG);

            ConfigManager.createProfile(PROFILE_A);
            Path archive = tempDir.resolve("persisted-root-profile.zip");
            ConfigManager.exportProfile("", archive);
            ConfigManager.cloneProfile("", PROFILE_B);

            assertTrue(Files.isRegularFile(
                    ConfigManager.profileDir(PROFILE_B).resolve(ROOT_PERSISTED_NESTED_CONFIG)));
            try (ZipFile zip = new ZipFile(archive.toFile())) {
                assertNotNull(zip.getEntry(ROOT_PERSISTED_NESTED_CONFIG));
            }
        } finally {
            ownedSubdirs.setAs(previousOwnedSubdirs);
            ConfigManager.internal().save("profiles.json");
        }
    }

    @Test
    void persistsProfileIcons() {
        ConfigManager.createProfile(PROFILE_A);

        ConfigManager.setProfileIcon(PROFILE_A, "star");

        assertEquals("star", ConfigManager.profileIcon(PROFILE_A));
        assertEquals("star", ConfigManager.profileIcons().get(PROFILE_A));

        ConfigManager.setProfileIcon(PROFILE_A, "profiles");
        assertEquals("profiles", ConfigManager.profileIcon(PROFILE_A));
        assertFalse(ConfigManager.profileIcons().containsKey(PROFILE_A));
    }

    @Test
    void renamesActiveProfileAndKeepsItActive() {
        ConfigManager.createProfile(PROFILE_A);
        ConfigManager.setProfileIcon(PROFILE_A, "star");
        ConfigManager.setFavoriteProfile(PROFILE_A, true);

        ConfigManager.renameProfile(PROFILE_A, PROFILE_B);

        assertEquals(PROFILE_B, ConfigManager.activeProfile());
        assertFalse(ConfigManager.profiles().contains(PROFILE_A));
        assertTrue(ConfigManager.profiles().contains(PROFILE_B));
        assertEquals("star", ConfigManager.profileIcon(PROFILE_B));
        assertFalse(ConfigManager.profileIcons().containsKey(PROFILE_A));
        assertTrue(ConfigManager.isFavoriteProfile(PROFILE_B));
        assertFalse(ConfigManager.favoriteProfiles().contains(PROFILE_A));
    }

    @Test
    void reusingARenamedProfileNameDoesNotInheritItsFavoriteOrIcon() {
        ConfigManager.createProfile(PROFILE_A);
        ConfigManager.setProfileIcon(PROFILE_A, "star");
        ConfigManager.setFavoriteProfile(PROFILE_A, true);
        ConfigManager.renameProfile(PROFILE_A, PROFILE_B);

        ConfigManager.createProfile(PROFILE_A);

        assertFalse(ConfigManager.isFavoriteProfile(PROFILE_A));
        assertEquals("profiles", ConfigManager.profileIcon(PROFILE_A));
    }

    @Test
    void delayedSnapshotWritesDoNotResurrectRenamedOrDeletedProfiles() {
        CompatSnapshotStore delayedStore = new CompatSnapshotStore("delayed-profile-state.json");
        ConfigManager.createProfile(PROFILE_A);
        delayedStore.putValue(PROFILE_A, "test", "value", "latest");
        delayedStore.flush(PROFILE_A);

        ConfigManager.renameProfile(PROFILE_A, PROFILE_B);
        delayedStore.flush(PROFILE_A);
        ConfigManager.cloneProfile(PROFILE_B, PROFILE_C);

        assertEquals(3, ConfigManager.profiles().size());
        assertFalse(Files.exists(ConfigManager.profileDir(PROFILE_A)));
        assertTrue(Files.isDirectory(ConfigManager.profileDir(PROFILE_B)));
        assertTrue(Files.isDirectory(ConfigManager.profileDir(PROFILE_C)));

        ConfigManager.deleteProfile(PROFILE_C);
        delayedStore.putValue(PROFILE_C, "test", "value", "too late");
        delayedStore.flush(PROFILE_C);

        assertFalse(Files.exists(ConfigManager.profileDir(PROFILE_A)));
        assertFalse(Files.exists(ConfigManager.profileDir(PROFILE_C)));
        assertEquals(List.of("", PROFILE_B), ConfigManager.profiles());
        delayedStore.deleteProfile(PROFILE_A);
        delayedStore.deleteProfile(PROFILE_C);
    }

    @Test
    void movesSnapshotCacheWithRenamedProfileWithoutLosingNewIdentityUpdates() throws java.io.IOException {
        CompatSnapshotStore store = new CompatSnapshotStore("renamed-profile-state.json");
        ConfigManager.createProfile(PROFILE_A);
        store.putValue(PROFILE_A, "test", "value", "latest");
        store.flush(PROFILE_A);

        ConfigManager.renameProfile(PROFILE_A, PROFILE_B);
        store.putValue(PROFILE_B, "test", "value", "new identity update");
        store.renameProfile(PROFILE_A, PROFILE_B);

        assertEquals("new identity update", store.getValue(PROFILE_B, "test", "value"));
        assertFalse(Files.exists(ConfigManager.profileDir(PROFILE_A)));
        assertTrue(Files.isRegularFile(ConfigManager.profileDir(PROFILE_B).resolve("renamed-profile-state.json")));
    }

    @Test
    void reportsInactiveRenameWithoutChangingActiveProfile() {
        ConfigManager.createProfile(PROFILE_A);
        ConfigManager.openProfile("");
        AtomicReference<String> changed = new AtomicReference<>();
        AtomicReference<String> renamed = new AtomicReference<>();
        ConfigManager.ProfileChangeListener listener = new ConfigManager.ProfileChangeListener() {
            @Override
            public void onProfileChanged(String newProfile) {
                changed.set(newProfile);
            }

            @Override
            public void onProfileRenamed(String oldProfile, String newProfile) {
                renamed.set(oldProfile + "->" + newProfile);
            }
        };
        ConfigManager.addProfileChangeListener(listener);
        try {
            ConfigManager.renameProfile(PROFILE_A, PROFILE_B);

            assertEquals("", ConfigManager.activeProfile());
            assertEquals(PROFILE_A + "->" + PROFILE_B, renamed.get());
            assertNull(changed.get());
        } finally {
            ConfigManager.removeProfileChangeListener(listener);
        }
    }

    @Test
    void savesListenerStateBeforeCloneAndExport() throws IOException {
        ConfigManager.createProfile(PROFILE_A);
        ConfigManager.ProfileChangeListener listener = new ConfigManager.ProfileChangeListener() {
            @Override
            public void onProfileChanged(String newProfile) {
            }

            @Override
            public void onProfileSaving(String profile) {
                try {
                    Files.writeString(ConfigManager.profileDir(profile).resolve("listener-state.txt"), "fresh:" + profile);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        ConfigManager.addProfileChangeListener(listener);
        try {
            ConfigManager.cloneProfile(PROFILE_A, PROFILE_B);
            assertEquals("fresh:" + PROFILE_A,
                    Files.readString(ConfigManager.profileDir(PROFILE_B).resolve("listener-state.txt")));

            Path archive = tempDir.resolve("listener-state.zip");
            ConfigManager.exportProfile(PROFILE_B, archive);
            try (ZipFile zip = new ZipFile(archive.toFile())) {
                assertNotNull(zip.getEntry("listener-state.txt"));
            }
        } finally {
            ConfigManager.removeProfileChangeListener(listener);
        }
    }

    @Test
    void failedListenerSavePreventsProfileSwitch() {
        ConfigManager.createProfile(PROFILE_A);
        ConfigManager.ProfileChangeListener listener = new ConfigManager.ProfileChangeListener() {
            @Override
            public void onProfileChanged(String newProfile) {
            }

            @Override
            public void onProfileSaving(String profile) {
                if (PROFILE_A.equals(profile)) throw new IllegalStateException("save failed");
            }
        };
        ConfigManager.addProfileChangeListener(listener);
        try {
            assertThrows(IllegalStateException.class, () -> ConfigManager.openProfile(""));
            assertEquals(PROFILE_A, ConfigManager.activeProfile());
        } finally {
            ConfigManager.removeProfileChangeListener(listener);
        }
    }

    @Test
    void cloningAnInactiveProfileSavesTheCurrentProfileBeforeSwitching() {
        ConfigManager.createProfile(PROFILE_A);
        ConfigManager.createProfile(PROFILE_B);
        List<String> saved = new ArrayList<>();
        ConfigManager.ProfileChangeListener listener = new ConfigManager.ProfileChangeListener() {
            @Override
            public void onProfileChanged(String newProfile) {
            }

            @Override
            public void onProfileSaving(String profile) {
                saved.add(profile);
            }
        };
        ConfigManager.addProfileChangeListener(listener);
        try {
            ConfigManager.cloneProfile(PROFILE_A, PROFILE_C);

            assertEquals(List.of(PROFILE_A, PROFILE_B), saved);
            assertEquals(PROFILE_C, ConfigManager.activeProfile());
        } finally {
            ConfigManager.removeProfileChangeListener(listener);
        }
    }

    @Test
    void failedCurrentSaveDoesNotLeaveAPartialInactiveClone() {
        ConfigManager.createProfile(PROFILE_A);
        ConfigManager.createProfile(PROFILE_B);
        ConfigManager.ProfileChangeListener listener = new ConfigManager.ProfileChangeListener() {
            @Override
            public void onProfileChanged(String newProfile) {
            }

            @Override
            public void onProfileSaving(String profile) {
                if (PROFILE_B.equals(profile)) throw new IllegalStateException("current save failed");
            }
        };
        ConfigManager.addProfileChangeListener(listener);
        try {
            assertThrows(IllegalStateException.class,
                    () -> ConfigManager.cloneProfile(PROFILE_A, PROFILE_C));
            assertEquals(PROFILE_B, ConfigManager.activeProfile());
            assertFalse(Files.exists(ConfigManager.profileDir(PROFILE_C)));
        } finally {
            ConfigManager.removeProfileChangeListener(listener);
        }
    }

    @Test
    void deleteActiveProfileFallsBackToRoot() {
        ConfigManager.createProfile(PROFILE_A);

        ConfigManager.deleteProfile(PROFILE_A);

        assertEquals("", ConfigManager.activeProfile());
        assertFalse(ConfigManager.profiles().contains(PROFILE_A));
        assertFalse(ConfigManager.profileIcons().containsKey(PROFILE_A));
    }

    @Test
    void deletesSnapshotStateBeforeReportingTheFallbackProfile() {
        ConfigManager.createProfile(PROFILE_A);
        List<String> events = new ArrayList<>();
        ConfigManager.ProfileChangeListener listener = new ConfigManager.ProfileChangeListener() {
            @Override
            public void onProfileChanged(String newProfile) {
                events.add("changed:" + newProfile);
            }

            @Override
            public void onProfileDeleted(String profile) {
                events.add("deleted:" + profile);
            }
        };
        ConfigManager.addProfileChangeListener(listener);
        try {
            ConfigManager.deleteProfile(PROFILE_A);

            assertEquals(List.of("deleted:" + PROFILE_A, "changed:"), events);
        } finally {
            ConfigManager.removeProfileChangeListener(listener);
        }
    }

    @Test
    void rejectsInvalidProfileNames() {
        for (String name : new String[]{"", "   ", "../bad", "bad/name", "$", "cash$money"}) {
            assertThrows(IllegalArgumentException.class, () -> ConfigManager.createProfile(name));
            assertThrows(IllegalArgumentException.class, () -> ConfigManager.cloneProfile("", name));
        }
        assertTrue(ConfigManager.profiles().stream().noneMatch(profile -> profile.contains("$")));

        ConfigManager.createProfile(PROFILE_A);
        assertThrows(IllegalArgumentException.class, () -> ConfigManager.renameProfile(PROFILE_A, "cash$money"));
        assertThrows(IllegalArgumentException.class, () -> ConfigManager.renameProfile(PROFILE_A, "  "));
        assertTrue(Files.isDirectory(ConfigManager.PROFILES_DIR.resolve(PROFILE_A)));
    }

    @Test
    void switchingProfilesRebindsInitializedConfigs() {
        ProfileTestConfig config = new ProfileTestConfig();
        config.initialize(false);
        config.enabled = true;
        config.save();

        ConfigManager.createProfile(PROFILE_C);
        config.enabled = false;
        config.save();

        ConfigManager.openProfile("");
        assertTrue(config.enabled);

        ConfigManager.openProfile(PROFILE_C);
        assertFalse(config.enabled);
    }

    private static void cleanupProfiles() throws IOException {
        ConfigManager.setProfileSpecificControls(true);
        ConfigManager.setFavoriteProfile("", false);
        ConfigManager.setFavoriteProfile(PROFILE_A, false);
        ConfigManager.setFavoriteProfile(PROFILE_B, false);
        ConfigManager.setFavoriteProfile(PROFILE_C, false);
        if (ConfigManager.profiles().contains(PROFILE_A)) ConfigManager.setProfileIcon(PROFILE_A, null);
        if (ConfigManager.profiles().contains(PROFILE_B)) ConfigManager.setProfileIcon(PROFILE_B, null);
        if (ConfigManager.profiles().contains(PROFILE_C)) ConfigManager.setProfileIcon(PROFILE_C, null);
        Files.deleteIfExists(ConfigManager.PROFILES_DIR.resolve(PROFILE_A + ".zip"));
        deleteProfileDirectory(PROFILE_A);
        deleteProfileDirectory(PROFILE_B);
        deleteProfileDirectory(PROFILE_C);
        Files.deleteIfExists(ConfigManager.profileDir("").resolve(BLANK_CONFIG));
        Files.deleteIfExists(ConfigManager.profileDir("").resolve(CLONE_CONFIG));
        Files.deleteIfExists(ConfigManager.profileDir("").resolve(ROOT_ORPHAN_HUD));
        ConfigManager.active().delete(DIRECT_TREE_CONFIG);
        ConfigManager.active().delete(GLOBAL_UI_TREE_CONFIG);
        Files.deleteIfExists(ConfigManager.profileDir("").resolve(DIRECT_TREE_CONFIG));
        deletePath(ConfigManager.profileDir("").resolve("oc_profile_root"));
        deletePath(ConfigManager.profileDir("").resolve("oc_profile_persisted"));
    }

    private static void deleteProfileDirectory(String profile) throws IOException {
        deletePath(ConfigManager.PROFILES_DIR.resolve(profile));
    }

    private static void deletePath(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> stream = Files.walk(path)) {
            for (Path entry : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(entry);
            }
        }
    }

    private static final class ProfileTestConfig extends Config {
        @Switch(title = "Enabled")
        boolean enabled = false;

        private ProfileTestConfig() {
            this("profile_test.json");
        }

        private ProfileTestConfig(String id) {
            super(id, "Profile Test", Category.OTHER);
        }
    }
}
