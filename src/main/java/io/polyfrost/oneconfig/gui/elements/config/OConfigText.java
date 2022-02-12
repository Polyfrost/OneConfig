package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.interfaces.Option;

import java.lang.reflect.Field;

public class OConfigText extends Option {
    private final String placeholder;
    private final boolean hideText;

    public OConfigText(Field field, String name, String description, String placeholder, boolean hideText) {
        super(field, name, description);
        this.placeholder = placeholder;
        this.hideText = hideText;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void draw(int x, int y, int width, int mouseX, int mouseY) {

    }
}
