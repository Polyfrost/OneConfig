package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.interfaces.Option;

import java.lang.reflect.Field;

public class OConfigSwitch extends Option {

    public OConfigSwitch(Field field, String name, String description, int size) {
        super(field, name, description, size);
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void draw(long vg, int x, int y, int mouseX, int mouseY) {

    }
}
