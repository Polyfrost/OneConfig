/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.internal.config.profiles;

import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.internal.config.core.ConfigCore;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Profiles {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig Profiles");
    public static final File nonProfileSpecificDir = new File("OneConfig/config");
    public static final File profileDir = new File("OneConfig/profiles");
    private static ArrayList<String> profiles;

    public static String getCurrentProfile() {
        if (OneConfigConfig.getInstance() == null) {
            OneConfigConfig.getInstance();
        }
        if (!profileDir.exists() && !profileDir.mkdir()) {
            LOGGER.fatal("Could not create profiles folder");
            return null;
        }
        if (profiles == null) {
            String[] profilesArray = profileDir.list((file, s) -> file.isDirectory());
            if (profilesArray != null) profiles = new ArrayList<>(Arrays.asList(profilesArray));
        }
        if (!getProfileDir(OneConfigConfig.currentProfile).exists()) {
            createProfile(OneConfigConfig.currentProfile);
        }
        return OneConfigConfig.currentProfile;
    }

    public static void createProfile(String name) {
        File folder = new File(profileDir, name);
        if (!folder.exists() && !folder.mkdir()) {
            LOGGER.fatal("Could not create profile folder");
            return;
        }
        profiles.add(name);
    }

    public static File getProfileDir() {
        return getProfileDir(getCurrentProfile());
    }

    public static File getProfileDir(String profile) {
        return new File(profileDir, profile);
    }

    public static File getProfileFile(String file) {
        return new File(getProfileDir(), file);
    }

    public static File getNonProfileSpecificFile(String file) {
        return new File(nonProfileSpecificDir, file);
    }

    public static List<String> getProfiles() {
        return new ArrayList<>(profiles);
    }

    public static boolean doesProfileExist(String profile) {
        return profiles.contains(profile);
    }

    public static void loadProfile(String profile) {
        ConfigCore.saveAll();
        OneConfigConfig.currentProfile = profile;
        OneConfigConfig.getInstance().save();
        ConfigCore.reInitAll();
    }

    public static void renameProfile(String name, String newName) {
        try {
            File newFile = new File(profileDir, newName);
            FileUtils.moveDirectory(getProfileDir(name), newFile);
            if (OneConfigConfig.currentProfile.equals(name)) OneConfigConfig.currentProfile = newName;
            profiles.remove(name);
            profiles.add(newName);
        } catch (IOException e) {
            LOGGER.error("Failed to rename profile");
        }
    }

    public static void deleteProfile(String name) {
        if (name.equals(getCurrentProfile())) {
            if (profiles.size() == 1) {
                LOGGER.error("Cannot delete only profile!");
                return;
            }
            loadProfile(profiles.stream().filter(entry -> !entry.equals(name)).findFirst().get());
        }
        try {
            FileUtils.deleteDirectory(getProfileDir(name));
            profiles.remove(name);
        } catch (IOException e) {
            LOGGER.error("Failed to delete profile");
        }
    }
}
