package cc.polyfrost.oneconfig.internal.config.core;

import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.internal.hud.HudCore;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class ConfigCore {
    static {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                saveAll();
            }
        }, 30000, 30000);
    }
    public static List<Mod> mods = new ArrayList<>();

    public static void saveAll() {
        for (Mod modData : mods) {
            modData.config.save();
        }
    }

    public static void reInitAll() {
        ArrayList<Mod> data = new ArrayList<>(mods);
        mods.clear();
        HudCore.huds.clear();
        KeyBindHandler.INSTANCE.clearKeyBinds();
        for (Mod modData : data) {
            modData.config.initialize();
        }
        sortMods();
    }

    public static void sortMods() {

        ArrayList<Mod> mods = new ArrayList<>(ConfigCore.mods);
        ConfigCore.mods = mods.stream().filter((mod -> OneConfigConfig.favoriteMods.contains(mod.name))).sorted().collect(Collectors.toList());
        mods.removeAll(ConfigCore.mods);
        ConfigCore.mods.addAll(mods.stream().filter(mod -> mod.modType != ModType.THIRD_PARTY).sorted().collect(Collectors.toList()));
        mods.removeAll(ConfigCore.mods);
        ConfigCore.mods.addAll(mods.stream().sorted().collect(Collectors.toList()));
        OneConfigConfig.getInstance().save();
    }
}
