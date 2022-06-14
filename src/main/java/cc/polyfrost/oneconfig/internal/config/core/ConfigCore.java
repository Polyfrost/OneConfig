package cc.polyfrost.oneconfig.internal.config.core;

import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.internal.hud.HudCore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigCore {
    public static List<Mod> oneConfigMods = new ArrayList<>();

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
        sortMods();
    }

    public static void sortMods() {
        ArrayList<Mod> mods = new ArrayList<>(oneConfigMods);
        oneConfigMods = mods.stream().filter((mod -> OneConfigConfig.favoriteMods.contains(mod.name))).sorted().collect(Collectors.toList());
        mods.removeAll(oneConfigMods);
        oneConfigMods.addAll(mods.stream().filter(mod -> mod.modType != ModType.THIRD_PARTY).sorted().collect(Collectors.toList()));
        mods.removeAll(oneConfigMods);
        oneConfigMods.addAll(mods.stream().sorted().collect(Collectors.toList()));
    }
}
