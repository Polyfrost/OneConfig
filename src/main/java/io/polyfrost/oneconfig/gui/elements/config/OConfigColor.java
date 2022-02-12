package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.interfaces.Option;

import java.lang.reflect.Field;

public class OConfigColor extends Option {
    private final boolean allowAlpha;

    public OConfigColor(Field field, String name, String description, boolean allowAlpha) {
        super(field, name, description);
        this.allowAlpha = allowAlpha;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void draw(int x, int y, int width, int mouseX, int mouseY) {

    }
}
