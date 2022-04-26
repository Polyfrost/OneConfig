package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.data.OptionPage;
import io.polyfrost.oneconfig.config.interfaces.BasicOption;

import java.lang.reflect.Field;

public class ConfigPage extends BasicOption {
    public final OptionPage page;

    public ConfigPage(Field field, String name, String description, int size, OptionPage page) {
        super(field, name, description, size);
        this.page = page;
    }


    @Override
    public void draw(long vg, int x, int y, int mouseX, int mouseY) {

    }

    @Override
    public int getHeight() {
        return 0;
    }
}
