package io.polyfrost.oneconfig.config.data;


import java.util.HashMap;

public class OptionPage {
    public final String name;
    public final Mod mod;
    /**
     * Depth 1 = categories
     * Depth 2 = subcategories
     * Depth 3 = list of options
     */
    public final HashMap<String, OptionCategory> categories = new HashMap<>();

    public OptionPage(String name, Mod mod) {
        this.name = name;
        this.mod = mod;
    }
}
