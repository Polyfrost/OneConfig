package io.polyfrost.oneconfig.config.core;

import io.polyfrost.oneconfig.config.data.Mod;
import io.polyfrost.oneconfig.hud.HudCore;

import java.util.ArrayList;

public class ConfigCore {
    public static ArrayList<Mod> OneConfigMods = new ArrayList<>();

    public static void saveAll() {
        for (Mod modData : OneConfigMods) {
            modData.config.save();
        }
    }

    public static void reInitAll() {
        ArrayList<Mod> data = new ArrayList<>(OneConfigMods);
        OneConfigMods.clear();
        HudCore.huds.clear();
        for (Mod modData : data) {
            modData.config.init(modData);
        }
    }
}
