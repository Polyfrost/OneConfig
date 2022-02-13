package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.interfaces.Option;

import java.lang.reflect.Field;

public class OConfigSwitch extends Option {

    public OConfigSwitch(Field field, String name, String description) {
        super(field, name, description);
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void draw(int x, int y, int width, int mouseX, int mouseY) {

    }
}
