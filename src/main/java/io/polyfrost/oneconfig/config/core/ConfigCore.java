package io.polyfrost.oneconfig.config.core;

import io.polyfrost.oneconfig.config.data.ModData;
import io.polyfrost.oneconfig.config.interfaces.Option;

import java.util.ArrayList;
import java.util.HashMap;

public class ConfigCore {
    public static HashMap<ModData, ArrayList<Option>> settings = new HashMap<>();

    public static void saveAll () {
        for (ModData modData : settings.keySet()) {
            modData.config.save();
        }
    }

    public static void reInitAll () {
        ArrayList<ModData> data = new ArrayList<>(settings.keySet());
        settings.clear();
        for (ModData modData : data) {
            modData.config.init(modData);
        }
    }
}
