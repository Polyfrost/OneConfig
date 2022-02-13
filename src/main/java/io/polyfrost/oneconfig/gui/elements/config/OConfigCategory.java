package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.interfaces.Option;

import java.util.List;

public class OConfigCategory extends Option {
    public final List<Option> options;

    public OConfigCategory(String name, String description, List<Option> options) {
        super(null, name, description);
        this.options = options;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void draw(int x, int y, int width, int mouseX, int mouseY) {

    }
}
