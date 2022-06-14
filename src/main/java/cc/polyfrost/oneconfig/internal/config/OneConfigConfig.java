package cc.polyfrost.oneconfig.internal.config;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.utils.JsonUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class OneConfigConfig extends InternalConfig {
    public static String currentProfile = "Default Profile";
    public static boolean autoUpdate = true;
    /**
     * 0 = Releases
     * 1 = Pre-Releases
     */
    public static int updateChannel = 0;
    public static ArrayList<String> favoriteMods = new ArrayList<>();
    public static ArrayList<OneColor> favoriteColors = new ArrayList<>();
    public static ArrayList<OneColor> recentColors = new ArrayList<>();
    public static boolean australia = false;

    public OneConfigConfig() {
        super("", "OneConfig.json");
    }
}
