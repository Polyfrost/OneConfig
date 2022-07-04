package cc.polyfrost.oneconfig.config.elements;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.data.Mod;

import java.util.LinkedHashMap;

public class OptionPage {
    public final String name;
    public final Mod mod;
    public final LinkedHashMap<String, OptionCategory> categories = new LinkedHashMap<>();

    public OptionPage(String name, Mod mod) {
        this.name = name;
        this.mod = mod;
    }

    public void reset(Config config) {
        for (OptionCategory subcategory : categories.values()) {
            subcategory.reset(config);
        }
    }
}
