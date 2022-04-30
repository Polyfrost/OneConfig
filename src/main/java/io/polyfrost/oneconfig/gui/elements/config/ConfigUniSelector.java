package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.interfaces.BasicOption;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.utils.InputUtils;
import org.lwjgl.nanovg.NanoVG;

import java.lang.reflect.Field;

public class ConfigUniSelector extends BasicOption {
    String[] options;

    public ConfigUniSelector(Field field, String name, int size, String[] options) {
        super(field, name, size);
        this.options = options;
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public void draw(long vg, int x, int y) {
        int selected = 0;
        try {
            selected = (int) get();
        } catch (IllegalAccessException ignored) {
        }
        String option = options[selected] + " " + (selected + 1) + "/" + options.length;
        RenderManager.drawString(vg, name, x, y + 16, OneConfigConfig.WHITE_90, 18f, Fonts.INTER_MEDIUM);
        RenderManager.drawString(vg, option, x + 352 - RenderManager.getTextWidth(vg, option, 14f, Fonts.INTER_MEDIUM) / 2f, y + 15, OneConfigConfig.WHITE_90, 14f, Fonts.INTER_MEDIUM);

        // actual coordinates: 240, 7
        NanoVG.nvgTranslate(vg, x + 248, y + 21);
        NanoVG.nvgRotate(vg, (float) Math.toRadians(180));
        RenderManager.drawImage(vg, "/assets/oneconfig/textures/arrow.png", 0, 0, 8, 14, OneConfigConfig.BLUE_400);
        NanoVG.nvgResetTransform(vg);
        RenderManager.drawImage(vg, "/assets/oneconfig/textures/arrow.png", x + 456, y + 7, 8, 14, OneConfigConfig.BLUE_400);

        if (InputUtils.isAreaClicked(x + 240, y + 7, 8, 14)) {
            if (selected > 0) selected -= 1;
            else selected = options.length - 1;
            try {
                set(selected);
            } catch (IllegalAccessException ignored) {
            }
        } else if (InputUtils.isAreaClicked(x + 456, y + 7, 8, 14)) {
            if (selected < options.length - 1) selected += 1;
            else selected = 0;
            try {
                set(selected);
            } catch (IllegalAccessException ignored) {
            }
        }
    }
}
