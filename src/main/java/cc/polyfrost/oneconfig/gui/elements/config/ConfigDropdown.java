package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import cc.polyfrost.oneconfig.utils.ColorUtils;
import cc.polyfrost.oneconfig.utils.InputUtils;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.Arrays;

public class ConfigDropdown extends BasicOption { // TODO: remove dividers and fix corners
    private final String[] options;
    private int backgroundColor = OneConfigConfig.GRAY_500;
    private boolean opened = false;

    public ConfigDropdown(Field field, Object parent, String name, int size, String[] options) {
        super(field, parent, name, size);
        this.options = options;
    }

    @Override
    public void draw(long vg, int x, int y) {
        if (!isEnabled()) RenderManager.setAlpha(vg, 0.5f);
        RenderManager.drawString(vg, name, x, y + 16, OneConfigConfig.WHITE_90, 14f, Fonts.MEDIUM);

        boolean hovered;
        if (size == 1) hovered = InputUtils.isAreaHovered(x + 224, y, 256, 32) && isEnabled();
        else hovered = InputUtils.isAreaHovered(x + 352, y, 640, 32) && isEnabled();

        if (hovered && InputUtils.isClicked() || opened && InputUtils.isClicked(true) &&
                (size == 1 && !InputUtils.isAreaHovered(x + 224, y + 40, 256, options.length * 32) ||
                        size == 2 && !InputUtils.isAreaHovered(x + 352, y + 40, 640, options.length * 32))) {
            opened = !opened;
            InputUtils.blockClicks(opened);
        }
        if (opened) return;

        backgroundColor = ColorUtils.smoothColor(backgroundColor, OneConfigConfig.GRAY_500, OneConfigConfig.GRAY_400, hovered, 100);
        int selected = 0;
        try {
            selected = (int) get();
        } catch (IllegalAccessException ignored) {
        }

        if (hovered && Mouse.isButtonDown(0)) RenderManager.setAlpha(vg, 0.8f);
        if (size == 1) {
            RenderManager.drawRoundedRect(vg, x + 224, y, 256, 32, backgroundColor, 12);
            RenderManager.drawString(vg, options[selected], x + 236, y + 16, OneConfigConfig.WHITE_80, 14f, Fonts.MEDIUM);
            RenderManager.drawRoundedRect(vg, x + 452, y + 4, 24, 24, OneConfigConfig.PRIMARY_600, 8);
            RenderManager.drawSvg(vg, SVGs.DROPDOWN_LIST, x + 452, y + 4, 24, 24);
        } else {
            RenderManager.drawRoundedRect(vg, x + 352, y, 640, 32, backgroundColor, 12);
            RenderManager.drawString(vg, options[selected], x + 364, y + 16, OneConfigConfig.WHITE_80, 14f, Fonts.MEDIUM);
            RenderManager.drawRoundedRect(vg, x + 964, y + 4, 24, 24, OneConfigConfig.PRIMARY_600, 8);
            RenderManager.drawSvg(vg, SVGs.DROPDOWN_LIST, x + 964, y + 4, 24, 24);
        }
        RenderManager.setAlpha(vg, 1f);
    }

    @Override
    public void drawLast(long vg, int x, int y) {
        if (!opened) return;

        boolean hovered;
        if (size == 1) hovered = InputUtils.isAreaHovered(x + 224, y, 256, 32);
        else hovered = InputUtils.isAreaHovered(x + 352, y, 640, 32);

        backgroundColor = ColorUtils.smoothColor(backgroundColor, OneConfigConfig.PRIMARY_800, OneConfigConfig.PRIMARY_700, hovered, 100);
        int selected = 0;
        try {
            selected = (int) get();
        } catch (IllegalAccessException ignored) {
        }

        if (hovered && Mouse.isButtonDown(0)) RenderManager.setAlpha(vg, 0.8f);
        if (size == 1) {
            RenderManager.drawRoundedRect(vg, x + 224, y, 256, 32, backgroundColor, 12);
            RenderManager.drawString(vg, options[selected], x + 236, y + 16, OneConfigConfig.WHITE_80, 14f, Fonts.MEDIUM);
            if (hovered && Mouse.isButtonDown(0)) RenderManager.setAlpha(vg, 0.8f);
            RenderManager.drawRoundedRect(vg, x + 452, y + 4, 24, 24, OneConfigConfig.PRIMARY_600, 8);
            RenderManager.drawSvg(vg, SVGs.DROPDOWN_LIST, x + 452, y + 4, 24, 24);

            RenderManager.setAlpha(vg, 1f);
            RenderManager.drawRoundedRect(vg, x + 224, y + 48, 256, options.length * 32 + 8, OneConfigConfig.GRAY_700, 12);
            RenderManager.drawHollowRoundRect(vg, x + 223, y + 47, 258, options.length * 32 + 10, new Color(204, 204, 204, 77).getRGB(), 12, 1);
            int optionY = y + 52;
            for (String option : options) {
                int color = OneConfigConfig.WHITE_80;
                boolean optionHovered = InputUtils.isAreaHovered(x + 224, optionY, 252, 32);
                if (optionHovered && Mouse.isButtonDown(0)) {
                    RenderManager.drawRoundedRect(vg, x + 228, optionY + 2, 248, 28, OneConfigConfig.PRIMARY_700_80, 8);
                } else if (optionHovered) {
                    RenderManager.drawRoundedRect(vg, x + 228, optionY + 2, 248, 28, OneConfigConfig.PRIMARY_700, 8);
                    color = OneConfigConfig.WHITE;
                }
                if (optionHovered && InputUtils.isClicked(true)) {
                    try {
                        set(Arrays.asList(options).indexOf(option));
                    } catch (IllegalAccessException ignored) {
                    }
                    opened = false;
                    InputUtils.blockClicks(false);
                }

                RenderManager.drawString(vg, option, x + 240, optionY + 18, color, 14, Fonts.MEDIUM);
                optionY += 32;
            }
        } else {
            RenderManager.drawRoundedRect(vg, x + 352, y, 640, 32, backgroundColor, 12);
            RenderManager.drawString(vg, options[selected], x + 364, y + 16, OneConfigConfig.WHITE_80, 14f, Fonts.MEDIUM);
            if (hovered && Mouse.isButtonDown(0)) RenderManager.setAlpha(vg, 0.8f);
            RenderManager.drawRoundedRect(vg, x + 964, y + 4, 24, 24, OneConfigConfig.PRIMARY_600, 8);
            RenderManager.drawSvg(vg, SVGs.DROPDOWN_LIST, x + 964, y + 4, 24, 24);

            RenderManager.setAlpha(vg, 1f);
            RenderManager.drawRoundedRect(vg, x + 352, y + 48, 640, options.length * 32 + 8, OneConfigConfig.GRAY_700, 12);
            RenderManager.drawHollowRoundRect(vg, x + 351, y + 47, 642, options.length * 32 + 10, new Color(204, 204, 204, 77).getRGB(), 12, 1);
            int optionY = y + 52;
            for (String option : options) {
                int color = OneConfigConfig.WHITE_80;
                boolean optionHovered = InputUtils.isAreaHovered(x + 352, optionY, 640, 36);
                if (optionHovered && Mouse.isButtonDown(0)) {
                    RenderManager.drawRoundedRect(vg, x + 356, optionY + 2, 632, 28, OneConfigConfig.PRIMARY_700_80, 8);
                } else if (optionHovered) {
                    RenderManager.drawRoundedRect(vg, x + 356, optionY + 2, 632, 28, OneConfigConfig.PRIMARY_700, 8);
                    color = OneConfigConfig.WHITE;
                }

                RenderManager.drawString(vg, option, x + 368, optionY + 18, color, 14, Fonts.MEDIUM);

                if (optionHovered && InputUtils.isClicked(true)) {
                    try {
                        set(Arrays.asList(options).indexOf(option));
                    } catch (IllegalAccessException ignored) {
                    }
                    opened = false;
                    InputUtils.blockClicks(false);
                }
                optionY += 32;
            }
        }
        RenderManager.setAlpha(vg, 1f);
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
