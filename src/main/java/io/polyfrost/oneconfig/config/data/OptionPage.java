package io.polyfrost.oneconfig.config.data;

import java.util.LinkedHashMap;

public class OptionPage {
    public final String name;
    public final Mod mod;
    /**
     * Depth 1 = categories
     * Depth 2 = subcategories
     * Depth 3 = list of options
     */
    public final LinkedHashMap<String, OptionCategory> categories = new LinkedHashMap<>();

    public OptionPage(String name, Mod mod) {
        this.name = name;
        this.mod = mod;
    }
}
