package io.polyfrost.oneconfig.gui.pages;

import io.polyfrost.oneconfig.config.core.ConfigCore;
import io.polyfrost.oneconfig.config.data.Mod;
import io.polyfrost.oneconfig.config.interfaces.BasicOption;

import java.util.ArrayList;

public class ModConfigPage extends Page {
    private final Mod modData;
    private final ArrayList<BasicOption> options;

    public ModConfigPage(Mod mod) {
        super("Mod: " + mod.name);
        this.modData = mod;
        options = ConfigCore.settings.get(mod);
    }

    @Override
    public void draw(long vg, int x, int y) {

    }

    @Override
    public void finishUpAndClose() {
        modData.config.save();      // TODO
    }

    public Mod getModData() {
        return modData;
    }

    protected ArrayList<BasicOption> getOptions() {
        return options;
    }
}
