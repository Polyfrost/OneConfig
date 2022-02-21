package io.polyfrost.oneconfig.config;

import com.google.gson.JsonParser;
import io.polyfrost.oneconfig.config.data.ModData;
import io.polyfrost.oneconfig.config.interfaces.Config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class OneConfigConfig extends Config {
    public OneConfigConfig() {
        super(null, "OneConfig.json");
    }

    @Override
    public void init(ModData modData) {
        if (new File("OneConfig/" + configFile).exists())
            load();
        else
            save();
    }

    @Override
    public void save() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("OneConfig/" + configFile), StandardCharsets.UTF_8))) {
            writer.write(gson.toJson(this.getClass()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void load() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("OneConfig/" + configFile), StandardCharsets.UTF_8))) {
            deserializePart(new JsonParser().parse(reader).getAsJsonObject(), this.getClass());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String, String> profiles = new HashMap<>();
    public static String currentProfile;
}
