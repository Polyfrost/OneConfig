package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.interfaces.Option;

import java.lang.reflect.Field;

public class OConfigButton extends Option {
    private final String text;

    public OConfigButton(Field field, String name, String description, String text) {
        super(field, name, description);
        this.text = text;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void draw(int x, int y, int width, int mouseX, int mouseY) {

    }
}
