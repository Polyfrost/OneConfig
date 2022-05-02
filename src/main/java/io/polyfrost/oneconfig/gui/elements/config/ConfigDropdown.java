package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.interfaces.BasicOption;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.lwjgl.image.Images;
import io.polyfrost.oneconfig.utils.ColorUtils;
import io.polyfrost.oneconfig.utils.InputUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.Arrays;

public class ConfigDropdown extends BasicOption { // TODO: chose where dividers are somehow idfk please send help
    private final String[] options;
    private int backgroundColor = OneConfigConfig.GRAY_500;
    private boolean opened = false;
    private int[] dividers;

    public ConfigDropdown(Field field, String name, int size, String[] options, int [] dividers) {
        super(field, name, size);
        this.options = options;
        this.dividers = dividers;
    }

    @Override
    public void draw(long vg, int x, int y) {
        RenderManager.drawString(vg, name, x, y + 16, OneConfigConfig.WHITE_90, 14f, Fonts.INTER_MEDIUM);

        boolean hovered;
        if (size == 1) hovered = InputUtils.isAreaHovered(x + 224, y, 256, 32);
        else hovered = InputUtils.isAreaHovered(x + 352, y, 640, 32);

        if (hovered && InputUtils.isClicked() || opened && InputUtils.isClicked() &&
                (size == 1 && !InputUtils.isAreaHovered(x + 224, y + 40, 256, options.length * 32 + 4) ||
                        size == 2 && !InputUtils.isAreaHovered(x + 352, y + 40, 640, options.length * 32 + 4)))
            opened = !opened;
        if (opened) return;

        backgroundColor = ColorUtils.smoothColor(backgroundColor, OneConfigConfig.GRAY_500, OneConfigConfig.GRAY_400, hovered, 100);
        int selected = 0;
        try {
            selected = (int) get();
        } catch (IllegalAccessException ignored) {
        }

        if (hovered && Mouse.isButtonDown(0)) NanoVG.nvgGlobalAlpha(vg, 0.8f);
        if (size == 1) {
            RenderManager.drawRoundedRect(vg, x + 224, y, 256, 32, backgroundColor, 12);
            RenderManager.drawString(vg, options[selected], x + 236, y + 16, OneConfigConfig.WHITE_80, 14f, Fonts.INTER_MEDIUM);
            RenderManager.drawRoundedRect(vg, x + 452, y + 4, 24, 24, OneConfigConfig.BLUE_600, 8);
            RenderManager.drawImage(vg, Images.DROPDOWN_ARROW, x + 459, y + 8, 10, 6);
            NanoVG.nvgTranslate(vg, x + 469, y + 24);
        } else {
            RenderManager.drawRoundedRect(vg, x + 352, y, 640, 32, backgroundColor, 12);
            RenderManager.drawString(vg, options[selected], x + 364, y + 16, OneConfigConfig.WHITE_80, 14f, Fonts.INTER_MEDIUM);
            RenderManager.drawRoundedRect(vg, x + 964, y + 4, 24, 24, OneConfigConfig.BLUE_600, 8);
            RenderManager.drawImage(vg, Images.DROPDOWN_ARROW, x + 971, y + 8, 10, 6);
            NanoVG.nvgTranslate(vg, x + 981, y + 24);
        }
        NanoVG.nvgRotate(vg, (float) Math.toRadians(180));
        RenderManager.drawImage(vg, Images.DROPDOWN_ARROW, 0, 0, 10, 6);
        NanoVG.nvgResetTransform(vg);
        NanoVG.nvgGlobalAlpha(vg, 1f);
    }

    @Override
    public void drawLast(long vg, int x, int y) {
        if (!opened) return;

        boolean hovered;
        if (size == 1) hovered = InputUtils.isAreaHovered(x + 224, y, 256, 32);
        else hovered = InputUtils.isAreaHovered(x + 352, y, 640, 32);

        backgroundColor = ColorUtils.smoothColor(backgroundColor, OneConfigConfig.BLUE_800, OneConfigConfig.BLUE_700, hovered, 100);
        int selected = 0;
        try {
            selected = (int) get();
        } catch (IllegalAccessException ignored) {
        }

        if (hovered && Mouse.isButtonDown(0)) NanoVG.nvgGlobalAlpha(vg, 0.8f);
        if (size == 1) {
            RenderManager.drawRoundedRect(vg, x + 224, y, 256, 32, backgroundColor, 12);
            RenderManager.drawString(vg, options[selected], x + 236, y + 16, OneConfigConfig.WHITE_80, 14f, Fonts.INTER_MEDIUM);

            NanoVG.nvgGlobalAlpha(vg, 1f);
            RenderManager.drawRoundedRect(vg, x + 224, y + 40, 256, options.length * 32 + 4, OneConfigConfig.GRAY_700, 12);
            RenderManager.drawHollowRoundRect(vg, x + 224, y + 40, 256, options.length * 32 + 4, new Color(204, 204, 204, 77).getRGB(), 8, 1);
            int optionY = y + 56;
            for (String option : options) {
                int color = OneConfigConfig.WHITE_80;
                boolean optionHovered = InputUtils.isAreaHovered(x + 224, optionY - 16, 252, 32);
                if (optionHovered && Mouse.isButtonDown(0)) {
                    RenderManager.drawRoundedRect(vg, x + 228, optionY - 12, 248, 28, OneConfigConfig.BLUE_700_80, 8);
                } else if (optionHovered) {
                    RenderManager.drawRoundedRect(vg, x + 228, optionY - 12, 248, 28, OneConfigConfig.BLUE_700, 8);
                    color = OneConfigConfig.WHITE;
                }
                if (optionHovered && InputUtils.isClicked()) {
                    try {
                        set(Arrays.asList(options).indexOf(option));
                    } catch (IllegalAccessException ignored) {
                    }
                    opened = false;
                }

                RenderManager.drawString(vg, option, x + 240, optionY + 4, color, 14, Fonts.INTER_MEDIUM);
                if (!options[options.length - 1].equals(option))
                    RenderManager.drawLine(vg, x + 232, optionY + 18, x + 472, optionY + 18, 1, new Color(204, 204, 204, 77).getRGB());
                optionY += 32;
            }

            if (hovered && Mouse.isButtonDown(0)) NanoVG.nvgGlobalAlpha(vg, 0.8f);
            RenderManager.drawRoundedRect(vg, x + 452, y + 4, 24, 24, OneConfigConfig.BLUE_600, 8);
            RenderManager.drawImage(vg, Images.DROPDOWN_ARROW, x + 459, y + 8, 10, 6);
            NanoVG.nvgTranslate(vg, x + 469, y + 24);
        } else {
            RenderManager.drawRoundedRect(vg, x + 352, y, 640, 32, backgroundColor, 12);
            RenderManager.drawString(vg, options[selected], x + 364, y + 16, OneConfigConfig.WHITE_80, 14f, Fonts.INTER_MEDIUM);

            RenderManager.drawRoundedRect(vg, x + 352, y + 40, 640, options.length * 32 + 4, OneConfigConfig.GRAY_700, 12);
            RenderManager.drawHollowRoundRect(vg, x + 352, y + 40, 640, options.length * 32 + 4, new Color(204, 204, 204, 77).getRGB(), 8, 1);
            int optionY = y + 56;
            for (String option : options) {
                int color = OneConfigConfig.WHITE_80;
                boolean optionHovered = InputUtils.isAreaHovered(x + 352, optionY - 16, 640, 32);
                if (optionHovered && Mouse.isButtonDown(0)) {
                    RenderManager.drawRoundedRect(vg, x + 356, optionY - 12, 632, 28, OneConfigConfig.BLUE_700_80, 8);
                } else if (optionHovered) {
                    RenderManager.drawRoundedRect(vg, x + 356, optionY - 12, 632, 28, OneConfigConfig.BLUE_700, 8);
                    color = OneConfigConfig.WHITE;
                }

                RenderManager.drawString(vg, option, x + 368, optionY + 4, color, 14, Fonts.INTER_MEDIUM);
                if (!options[options.length - 1].equals(option))
                    RenderManager.drawLine(vg, x + 360, optionY + 18, x + 984, optionY + 18, 1, new Color(204, 204, 204, 77).getRGB());

                if (optionHovered && InputUtils.isClicked()) {
                    try {
                        set(Arrays.asList(options).indexOf(option));
                    } catch (IllegalAccessException ignored) {
                    }
                    opened = false;
                }
                optionY += 32;
            }

            if (hovered && Mouse.isButtonDown(0)) NanoVG.nvgGlobalAlpha(vg, 0.8f);
            RenderManager.drawRoundedRect(vg, x + 964, y + 4, 24, 24, OneConfigConfig.BLUE_600, 8);
            RenderManager.drawImage(vg, Images.DROPDOWN_ARROW, x + 971, y + 8, 10, 6);
            NanoVG.nvgTranslate(vg, x + 981, y + 24);
        }
        NanoVG.nvgRotate(vg, (float) Math.toRadians(180));
        RenderManager.drawImage(vg, Images.DROPDOWN_ARROW, 0, 0, 10, 6);
        NanoVG.nvgResetTransform(vg);
        NanoVG.nvgGlobalAlpha(vg, 1f);
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
