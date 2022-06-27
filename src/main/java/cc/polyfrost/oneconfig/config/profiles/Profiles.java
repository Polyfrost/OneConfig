package cc.polyfrost.oneconfig.config.profiles;

import cc.polyfrost.oneconfig.internal.OneConfig;
import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.internal.config.core.ConfigCore;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Profiles {
    public static final File nonProfileSpecificDir = new File("OneConfig/config");
    public static final File profileDir = new File("OneConfig/profiles");
    public static ArrayList<String> profiles;

    public static String getCurrentProfile() {
        if (!profileDir.exists() && !profileDir.mkdir()) {
            OneConfig.LOGGER.fatal("Could not create profiles folder");
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
            OneConfig.LOGGER.fatal("Could not create profile folder");
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

    public static File getNonProfileSpecificDir(String file) {
        return new File(nonProfileSpecificDir, file);
    }

    public static void loadProfile(String profile) {
        ConfigCore.saveAll();
        OneConfigConfig.currentProfile = profile;
        OneConfig.config.save();
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
            OneConfig.LOGGER.error("Failed to rename profile");
        }
    }

    public static void deleteProfile(String name) {
        if (name.equals(getCurrentProfile())) {
            if (profiles.size() == 1) {
                OneConfig.LOGGER.error("Cannot delete only profile!");
                return;
            }
            loadProfile(profiles.stream().filter(entry -> !entry.equals(name)).findFirst().get());
        }
        try {
            FileUtils.deleteDirectory(getProfileDir(name));
            profiles.remove(name);
        } catch (IOException e) {
            OneConfig.LOGGER.error("Failed to delete profile");
        }
    }
}
