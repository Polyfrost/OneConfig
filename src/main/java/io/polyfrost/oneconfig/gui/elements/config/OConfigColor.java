package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.interfaces.Option;

import java.lang.reflect.Field;

public class OConfigColor extends Option {
    private final boolean allowAlpha;

    public OConfigColor(Field field, String name, String description, boolean allowAlpha, int size) {
        super(field, name, description, size);
        this.allowAlpha = allowAlpha;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void draw(long vg, int x, int y, int mouseX, int mouseY) {

    }
}
