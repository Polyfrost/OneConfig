package cc.polyfrost.oneconfig.config.profiles;

import cc.polyfrost.oneconfig.internal.OneConfig;
import cc.polyfrost.oneconfig.internal.config.core.ConfigCore;
import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Profiles {
    private static final File profileDir = new File("OneConfig/profiles");
    public static ArrayList<String> profiles;

    public static String getCurrentProfile() {
        if (!profileDir.exists() && !profileDir.mkdir()) {
            System.out.println("Could not create profiles folder");
            return null;
        }
        if (profiles == null) {
            String[] profilesArray = new File("OneConfig/profiles").list((file, s) -> file.isDirectory());
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
            System.out.println("Could not create profile folder");
            return;
        }
        profiles.add(name);
    }

    public static File getProfileDir() {
        return getProfileDir(getCurrentProfile());
    }

    public static File getProfileDir(String profile) {
        return new File(new File("OneConfig/profiles"), profile);
    }

    public static File getProfileFile(String file) {
        return new File(getProfileDir(), file);
    }

    public static void loadProfile(String profile) {
        ConfigCore.saveAll();
        OneConfigConfig.currentProfile = profile;
        OneConfig.config.save();
        ConfigCore.reInitAll();
    }

    public static void renameProfile(String name, String newName) {
        try {
            File newFile = new File(new File("OneConfig/profiles"), newName);
            FileUtils.moveDirectory(getProfileDir(name), newFile);
            if (OneConfigConfig.currentProfile.equals(name)) OneConfigConfig.currentProfile = newName;
            profiles.remove(name);
            profiles.add(newName);
        } catch (IOException e) {
            System.out.println("Failed to rename profile");
        }
    }

    public static void deleteProfile(String name) {
        if (name.equals(getCurrentProfile())) {
            if (profiles.size() == 1) {
                System.out.println("Cannot delete only profile!");
                return;
            }
            loadProfile(profiles.stream().filter(entry -> !entry.equals(name)).findFirst().get());
        }
        try {
            FileUtils.deleteDirectory(getProfileDir(name));
            profiles.remove(name);
        } catch (IOException e) {
            System.out.println("Failed to delete profile");
        }
    }
}
