package cc.polyfrost.oneconfig.config.elements;

import cc.polyfrost.oneconfig.config.Config;

import java.util.ArrayList;

public class OptionCategory {
    public final ArrayList<OptionSubcategory> subcategories = new ArrayList<>();

    public void reset(Config config) {
        for (OptionSubcategory subcategory : subcategories) {
            subcategory.reset(config);
        }
    }
}
