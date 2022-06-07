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

public class OneConfigConfig extends Config {
    public static String currentProfile = "Default Profile";
    public static ArrayList<String> favoriteMods = new ArrayList<>();
    public static ArrayList<OneColor> favoriteColors = new ArrayList<>();
    public static ArrayList<OneColor> recentColors = new ArrayList<>();
    public static boolean allShowShortCut = false;
    public static boolean australia = false;

    public OneConfigConfig() {
        super(null, "OneConfig.json");
    }

    @Override
    public void init(Mod mod) {
        if (new File("OneConfig/" + configFile).exists()) load();
        else save();
    }

    @Override
    public void save() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(Paths.get("OneConfig/" + configFile)), StandardCharsets.UTF_8))) {
            writer.write(gson.toJson(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void load() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get("OneConfig/" + configFile)), StandardCharsets.UTF_8))) {
            deserializePart(JsonUtils.PARSER.parse(reader).getAsJsonObject(), this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
