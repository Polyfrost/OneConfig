package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.interfaces.Option;

import java.util.List;

public class OConfigCategory extends Option {
    public final List<Option> options;

    public OConfigCategory(String name, String description, List<Option> options, int size) {
        super(null, name, description, size);
        this.options = options;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void draw(long vg, int x, int y, int mouseX, int mouseY) {

    }
}
