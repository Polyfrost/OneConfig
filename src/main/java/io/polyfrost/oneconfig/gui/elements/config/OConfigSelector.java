package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.interfaces.Option;

import java.lang.reflect.Field;

public class OConfigSelector extends Option {
    private final String[] options;
    private final int defaultSelection;

    public OConfigSelector(Field field, String name, String description, String[] options, int defaultSelection, int size) {
        super(field, name, description, size);
        this.options = options;
        this.defaultSelection = defaultSelection;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void draw(long vg, int x, int y, int mouseX, int mouseY) {

    }
}
