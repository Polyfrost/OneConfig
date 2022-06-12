package cc.polyfrost.oneconfig.internal.config.core;

import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.internal.hud.HudCore;

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
        KeyBindHandler.INSTANCE.clearKeyBinds();
        for (Mod modData : data) {
            modData.config.init(modData);
        }
    }
}
