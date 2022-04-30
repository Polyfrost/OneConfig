package io.polyfrost.oneconfig.config.core;

import io.polyfrost.oneconfig.config.data.Mod;
import io.polyfrost.oneconfig.hud.HudCore;

import java.util.ArrayList;

/**
 * The Core Config class.
 */
public class ConfigCore {
    /**
     * The Array of all registered OneConfig Mods.
     */
    public static ArrayList<Mod> oneConfigMods = new ArrayList<>();

    /**
     * Saves all registered OneConfig Mods.
     */
    public static void saveAll() {
        for (Mod modData : oneConfigMods) {
            modData.config.save();
        }
    }

    /**
     * Initialize all registered OneConfig Mods.
     */
    public static void reInitAll() {
        ArrayList<Mod> data = new ArrayList<>(oneConfigMods);
        oneConfigMods.clear();
        HudCore.huds.clear();
        for (Mod modData : data) {
            modData.config.init(modData);
        }
    }
}
