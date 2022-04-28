package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.interfaces.BasicOption;

import java.lang.reflect.Field;

public class ConfigCheckbox extends BasicOption {
    public ConfigCheckbox(Field field, String name, int size) {
        super(field, name, size);
    }

    @Override
    public void draw(long vg, int x, int y) {

    }

    @Override
    public int getHeight() {
        return 0;
    }
}
