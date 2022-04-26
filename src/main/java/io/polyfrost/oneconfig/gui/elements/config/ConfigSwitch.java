package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.interfaces.BasicOption;

import java.lang.reflect.Field;

public class ConfigSwitch extends BasicOption {

    public ConfigSwitch(Field field, String name, String description, int size) {
        super(field, name, description, size);
    }


    @Override
    public void draw(long vg, int x, int y, int mouseX, int mouseY) {

    }

    @Override
    public int getHeight() {
        return 0;
    }
}
