package io.polyfrost.oneconfig.gui.pages;

import io.polyfrost.oneconfig.config.data.Mod;

public class ModConfigPage extends Page {
    private final Mod modData;

    public ModConfigPage(Mod mod) {
        super("Mod: " + mod.name);
        this.modData = mod;
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
}
