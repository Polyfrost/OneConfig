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
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ProfileManagerTest {
    private static final String PROFILE_A = "oc_test_profile_a";
    private static final String PROFILE_B = "oc_test_profile_b";
    private static final String PROFILE_C = "oc_test_profile_c";

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
    void persistsFavorites() {
        ConfigManager.createProfile(PROFILE_A);
        ConfigManager.setFavoriteProfile(PROFILE_A, true);

        assertTrue(ConfigManager.isFavoriteProfile(PROFILE_A));
        assertTrue(ConfigManager.favoriteProfiles().contains(PROFILE_A));

        ConfigManager.setFavoriteProfile(PROFILE_A, false);
        assertFalse(ConfigManager.isFavoriteProfile(PROFILE_A));
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

        ConfigManager.renameProfile(PROFILE_A, PROFILE_B);

        assertEquals(PROFILE_B, ConfigManager.activeProfile());
        assertFalse(ConfigManager.profiles().contains(PROFILE_A));
        assertTrue(ConfigManager.profiles().contains(PROFILE_B));
        assertEquals("star", ConfigManager.profileIcon(PROFILE_B));
        assertFalse(ConfigManager.profileIcons().containsKey(PROFILE_A));
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
    void rejectsInvalidProfileNames() {
        assertThrows(IllegalArgumentException.class, () -> ConfigManager.createProfile(""));
        assertThrows(IllegalArgumentException.class, () -> ConfigManager.createProfile("../bad"));
        assertThrows(IllegalArgumentException.class, () -> ConfigManager.createProfile("bad/name"));
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
        ConfigManager.setFavoriteProfile(PROFILE_A, false);
        ConfigManager.setFavoriteProfile(PROFILE_B, false);
        ConfigManager.setFavoriteProfile(PROFILE_C, false);
        if (ConfigManager.profiles().contains(PROFILE_A)) ConfigManager.setProfileIcon(PROFILE_A, null);
        if (ConfigManager.profiles().contains(PROFILE_B)) ConfigManager.setProfileIcon(PROFILE_B, null);
        if (ConfigManager.profiles().contains(PROFILE_C)) ConfigManager.setProfileIcon(PROFILE_C, null);
        deleteProfileDirectory(PROFILE_A);
        deleteProfileDirectory(PROFILE_B);
        deleteProfileDirectory(PROFILE_C);
    }

    private static void deleteProfileDirectory(String profile) throws IOException {
        Path path = ConfigManager.PROFILES_DIR.resolve(profile);
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
            super("profile_test.json", "Profile Test", Category.OTHER);
        }
    }
}
