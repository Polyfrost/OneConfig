package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.interfaces.BasicOption;

import java.lang.reflect.Field;

public class ConfigTextBox extends BasicOption {
    private final String placeholder;
    private final boolean secure;
    private final boolean multiLine;

    public ConfigTextBox(Field field, String name, int size, String placeholder, boolean secure, boolean multiLine) {
        super(field, name, size);
        this.placeholder = placeholder;
        this.secure = secure;
        this.multiLine = multiLine;
    }

    @Override
    public void draw(long vg, int x, int y) {

    }

    @Override
    public int getHeight() {
        return 0;
    }
}
