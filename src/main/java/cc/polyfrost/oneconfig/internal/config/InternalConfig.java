package cc.polyfrost.oneconfig.internal.config;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.utils.JsonUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class InternalConfig extends Config {
    /**
     * @param title      title that is displayed
     * @param configFile file where config is stored
     */
    public InternalConfig(String title, String configFile) {
        super(new Mod(title, null), configFile);
    }

    @Override
    public void init(Mod mod) {
        if (new File("OneConfig/" + configFile).exists()) load();
        else save();
        generateOptionList(this, mod.defaultPage, mod, false);
        mod.config = this;
        this.mod = mod;
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
