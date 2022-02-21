package io.polyfrost.oneconfig.themes;

import io.polyfrost.oneconfig.OneConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Themes {
    public static final int VERSION = 0;
    public static Theme activeTheme;
    public static final Logger themeLog = LogManager.getLogger("OneConfig Themes");

    /**
     * Return a list of all available themes in the directory.
     * @return list of themes
     */
    public static List<File> getThemes() {
        FilenameFilter filter = (dir, name) -> name.endsWith(".zip");
        return Arrays.asList(Objects.requireNonNull(OneConfig.themesDir.listFiles(filter)));
    }

    /**
     * Return the active theme instance.
     */
    public static Theme getActiveTheme() {
        return activeTheme;
    }

    /**
     * Open a new theme in the window, and restart the GUI.
     * @param theme Theme file to open
     */
    public static void openTheme(File theme) {
        try {
            activeTheme = new Theme(theme);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // TODO restart gui
    }


    public String toString() {
        return "OneConfig Theme {loaded=" + activeTheme.getLoadedTime() + ", name=" + activeTheme.getName() + ", desc=" + activeTheme.getDescription() + ", ready=" + activeTheme.isReady() + "}";
    }

}
