package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.interfaces.Option;

import java.lang.reflect.Field;

public class OConfigText extends Option {
    private final String placeholder;
    private final boolean hideText;

    public OConfigText(Field field, String name, String description, String placeholder, boolean hideText, int size) {
        super(field, name, description, size);
        this.placeholder = placeholder;
        this.hideText = hideText;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void draw(long vg, int x, int y, int mouseX, int mouseY) {

    }
}
