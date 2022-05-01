package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.interfaces.BasicOption;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.utils.InputUtils;
import org.lwjgl.nanovg.NanoVG;

import java.lang.reflect.Field;

public class ConfigDropdown extends BasicOption {
    private final String[] options;
    public ConfigDropdown(Field field, String name, int size, String[] options) {
        super(field, name, size);
        this.options = options;
    }

    @Override
    public void draw(long vg, int x, int y) {

    }

    @Override
    public void drawLast(long vg, int x, int y) {
        boolean hovered;
        int selected = 0;
        try {
            selected = (int) get();
        } catch (IllegalAccessException ignored) {
        }

        RenderManager.drawString(vg, name, x, y + 16, OneConfigConfig.WHITE_90, 14f, Fonts.INTER_MEDIUM);

        if (size == 1) {
            RenderManager.drawRoundedRect(vg, x + 224, y, 256, 32, OneConfigConfig.GRAY_500, 12);
            RenderManager.drawString(vg, options[selected], x + 236, y + 16, OneConfigConfig.WHITE_80, 14f, Fonts.INTER_MEDIUM);
            RenderManager.drawRoundedRect(vg, x + 452, y + 4, 24, 24, OneConfigConfig.BLUE_600, 8);
            RenderManager.drawImage(vg, "/assets/oneconfig/textures/dropdown_arrow.png", x + 459, y + 8, 10, 6);
            NanoVG.nvgTranslate(vg, x + 469, y + 24);
        } else {
            RenderManager.drawRoundedRect(vg, x + 352, y, 640, 32, OneConfigConfig.GRAY_500, 12);
            RenderManager.drawString(vg, options[selected], x + 364, y + 16, OneConfigConfig.WHITE_80, 14f, Fonts.INTER_MEDIUM);
            RenderManager.drawRoundedRect(vg, x + 964, y + 4, 24, 24, OneConfigConfig.BLUE_600, 8);
            RenderManager.drawImage(vg, "/assets/oneconfig/textures/dropdown_arrow.png", x + 971, y + 8, 10, 6);
            NanoVG.nvgTranslate(vg, x + 981, y + 24);
        }
        NanoVG.nvgRotate(vg, (float) Math.toRadians(180));
        RenderManager.drawImage(vg, "/assets/oneconfig/textures/dropdown_arrow.png", 0, 0, 10, 6);
        NanoVG.nvgResetTransform(vg);
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
