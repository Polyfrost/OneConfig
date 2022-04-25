package io.polyfrost.oneconfig.gui.pages;

import io.polyfrost.oneconfig.config.core.ConfigCore;
import io.polyfrost.oneconfig.config.data.ModData;
import io.polyfrost.oneconfig.config.interfaces.Option;
import io.polyfrost.oneconfig.gui.elements.config.OConfigCategory;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;

public class ModConfigPage extends Page {
    private final ModData modData;
    private final ArrayList<Option> options;

    public ModConfigPage(ModData mod) {
        super("Mod: " + mod.name);
        this.modData = mod;
        options = ConfigCore.settings.get(mod);
    }

    @Override
    public void draw(long vg, int x, int y) {
        for (Option option : options) {
            if (option instanceof OConfigCategory) {
                OConfigCategory category = (OConfigCategory) option;
                for (Option subOption : category.options) {
                    if (subOption.size == 0) {
                        subOption.draw(vg, x, y, Mouse.getX(), Mouse.getY());
                    }
                }
                for (Option subOption : category.options) {
                    if (subOption.size == 1) {
                        subOption.draw(vg, x, y, Mouse.getX(), Mouse.getY());
                    }
                }
            }
        }
    }

    @Override
    public void finishUpAndClose() {
        modData.config.save();      // TODO
    }

    public ModData getModData() {
        return modData;
    }

    protected ArrayList<Option> getOptions() {
        return options;
    }
}
