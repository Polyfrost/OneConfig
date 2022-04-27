package io.polyfrost.oneconfig.config.core;

import io.polyfrost.oneconfig.config.data.Mod;
import io.polyfrost.oneconfig.hud.HudCore;

import java.util.ArrayList;

public class ConfigCore {
    public static ArrayList<Mod> oneConfigMods = new ArrayList<>();

    public static void saveAll() {
        for (Mod modData : oneConfigMods) {
            modData.config.save();
        }
    }

    public static void reInitAll() {
        ArrayList<Mod> data = new ArrayList<>(oneConfigMods);
        oneConfigMods.clear();
        HudCore.huds.clear();
        for (Mod modData : data) {
            modData.config.init(modData);
        }
    }
}
