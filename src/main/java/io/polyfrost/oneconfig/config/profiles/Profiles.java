package io.polyfrost.oneconfig.config.profiles;

import io.polyfrost.oneconfig.OneConfig;
import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.core.ConfigCore;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class Profiles {
    public static Map.Entry<String, String> getCurrentProfile() {
        if (OneConfigConfig.currentProfile == null) {
            OneConfigConfig.currentProfile = createProfile("Default Profile").getKey();
            OneConfig.config.save();
        }
        return getProfile(OneConfigConfig.currentProfile);
    }

    public static Map.Entry<String, String> createProfile(String name) {
        File profileDir = new File("OneConfig/profiles");
        if (!profileDir.exists() && !profileDir.mkdir()) {
            System.out.println("Could not create profiles folder");
            return null;
        }
        File folder = new File(profileDir, name);
        if (!folder.exists() && !folder.mkdir()) {
            System.out.println("Could not create profile folder");
            return null;
        }
        OneConfigConfig.profiles.put(name, folder.getAbsolutePath());
        OneConfig.config.save();
        return getProfile(name);
    }

    public static Map.Entry<String, String> getProfile(String name) {
        return OneConfigConfig.profiles.entrySet().stream().filter(entry -> entry.getKey().equals(name)).findFirst().get();
    }

    public static File getProfileFile(String file) {
        return new File(new File(getCurrentProfile().getValue()), file);
    }

    public static void loadProfile(String profile) {
        ConfigCore.saveAll();
        OneConfigConfig.currentProfile = profile;
        OneConfig.config.save();
        ConfigCore.reInitAll();
    }

    public static void renameProfile(String name, String newName) {
        try {
            Map.Entry<String, String> profile = getProfile(name);
            File newFile = new File(new File("OneConfig/profiles"), newName);
            FileUtils.moveDirectory(new File(profile.getValue()), newFile);
            if (OneConfigConfig.currentProfile.equals(name))
                OneConfigConfig.currentProfile = newName;
            OneConfigConfig.profiles.remove(name);
            OneConfigConfig.profiles.put(newName, newFile.getAbsolutePath());
            OneConfig.config.save();
        } catch (IOException e) {
            System.out.println("Failed to rename profile");
        }
    }

    public static void deleteProfile(String name) {
        Map.Entry<String, String> profile = getProfile(name);
        if (profile.equals(getCurrentProfile())) {
            if (OneConfigConfig.profiles.size() == 1) {
                System.out.println("Cannot delete only profile!");
                return;
            }
            loadProfile(OneConfigConfig.profiles.entrySet().stream().filter(entry -> !entry.getKey().equals(name)).findFirst().get().getKey());
        }
        try {
            FileUtils.deleteDirectory(new File(profile.getValue()));
            OneConfigConfig.profiles.remove(name);
            OneConfig.config.save();
        } catch (IOException e) {
            System.out.println("Failed to delete profile");
        }
    }
}
