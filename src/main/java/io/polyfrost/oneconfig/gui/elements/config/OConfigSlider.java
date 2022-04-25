package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.interfaces.Option;

import java.lang.reflect.Field;

public class OConfigSlider extends Option {
    private final float min;
    private final float max;
    private final float precision;

    public OConfigSlider(Field field, String name, String description, float min, float max, float precision, int size) {
        super(field, name, description, size);
        this.min = min;
        this.max = max;
        this.precision = precision;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void draw(long vg, int x, int y, int mouseX, int mouseY) {

    }
}
