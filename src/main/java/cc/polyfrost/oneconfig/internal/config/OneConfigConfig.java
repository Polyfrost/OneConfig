package cc.polyfrost.oneconfig.internal.config;

import cc.polyfrost.oneconfig.config.core.OneColor;

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

    private static OneConfigConfig INSTANCE;

    public OneConfigConfig() {
        super("", "OneConfig.json");
        initialize();
        INSTANCE = this;
    }

    public static OneConfigConfig getInstance() {
        return INSTANCE == null ? (INSTANCE = new OneConfigConfig()) : INSTANCE;
    }
}
