package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.animations.ColorAnimation;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.renderer.scissor.Scissor;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.Arrays;

public class ConfigDropdown extends BasicOption {
    private final String[] options;
    private final ColorAnimation backgroundColor = new ColorAnimation(ColorPalette.SECONDARY);
    private final ColorAnimation atomColor = new ColorAnimation(new ColorPalette(Colors.PRIMARY_600, Colors.PRIMARY_500, Colors.PRIMARY_500));
    private boolean opened = false;
    private Scissor inputScissor = null;

    public ConfigDropdown(Field field, Object parent, String name, String category, String subcategory, int size, String[] options) {
        super(field, parent, name, category, subcategory, size);
        this.options = options;
    }

    public static ConfigDropdown create(Field field, Object parent) {
        Dropdown dropdown = field.getAnnotation(Dropdown.class);
        return new ConfigDropdown(field, parent, dropdown.name(), dropdown.category(), dropdown.subcategory(), dropdown.size(), dropdown.options());
    }

    @Override
    public void draw(long vg, int x, int y) {
        if (!isEnabled()) RenderManager.setAlpha(vg, 0.5f);
        RenderManager.drawText(vg, name, x, y + 16, Colors.WHITE_90, 14f, Fonts.MEDIUM);

        boolean hovered;
        if (size == 1) hovered = InputUtils.isAreaHovered(x + 224, y, 256, 32) && isEnabled();
        else hovered = InputUtils.isAreaHovered(x + 352, y, 640, 32) && isEnabled();

        if (hovered && InputUtils.isClicked() || opened && InputUtils.isClicked(true) &&
                (size == 1 && !InputUtils.isAreaHovered(x + 224, y + 40, 256, options.length * 32, true) ||
                        size == 2 && !InputUtils.isAreaHovered(x + 352, y + 40, 640, options.length * 32, true))) {
            opened = !opened;
            backgroundColor.setPalette(opened ? ColorPalette.PRIMARY : ColorPalette.SECONDARY);
            if (opened) inputScissor = InputUtils.blockAllInput();
            else if (inputScissor != null) InputUtils.stopBlock(inputScissor);
        }
        if (opened) return;

        int selected = 0;
        try {
            selected = (int) get();
        } catch (IllegalAccessException ignored) {
        }

        if (hovered && Platform.getMousePlatform().isButtonDown(0)) RenderManager.setAlpha(vg, 0.8f);
        if (size == 1) {
            RenderManager.drawRoundedRect(vg, x + 224, y, 256, 32, backgroundColor.getColor(hovered, hovered && Platform.getMousePlatform().isButtonDown(0)), 12);
            RenderManager.drawText(vg, options[selected], x + 236, y + 16, Colors.WHITE_80, 14f, Fonts.MEDIUM);
            RenderManager.drawRoundedRect(vg, x + 452, y + 4, 24, 24, atomColor.getColor(hovered, false), 8);
            RenderManager.drawSvg(vg, SVGs.DROPDOWN_LIST, x + 452, y + 4, 24, 24);
        } else {
            RenderManager.drawRoundedRect(vg, x + 352, y, 640, 32, backgroundColor.getColor(hovered, hovered && Platform.getMousePlatform().isButtonDown(0)), 12);
            RenderManager.drawText(vg, options[selected], x + 364, y + 16, Colors.WHITE_80, 14f, Fonts.MEDIUM);
            RenderManager.drawRoundedRect(vg, x + 964, y + 4, 24, 24, atomColor.getColor(hovered, false), 8);
            RenderManager.drawSvg(vg, SVGs.DROPDOWN_LIST, x + 964, y + 4, 24, 24);
        }
        RenderManager.setAlpha(vg, 1f);
    }

    @Override
    public void drawLast(long vg, int x, int y) {
        if (!opened) return;

        boolean hovered;
        if (size == 1) hovered = InputUtils.isAreaHovered(x + 224, y, 256, 32, true);
        else hovered = InputUtils.isAreaHovered(x + 352, y, 640, 32, true);

        int selected = 0;
        try {
            selected = (int) get();
        } catch (IllegalAccessException ignored) {
        }

        if (hovered && Platform.getMousePlatform().isButtonDown(0)) RenderManager.setAlpha(vg, 0.8f);
        if (size == 1) {
            RenderManager.drawRoundedRect(vg, x + 224, y, 256, 32, backgroundColor.getColor(hovered, hovered && Platform.getMousePlatform().isButtonDown(0)), 12);
            RenderManager.drawText(vg, options[selected], x + 236, y + 16, Colors.WHITE_80, 14f, Fonts.MEDIUM);
            if (hovered && Platform.getMousePlatform().isButtonDown(0)) RenderManager.setAlpha(vg, 0.8f);
            RenderManager.drawRoundedRect(vg, x + 452, y + 4, 24, 24, atomColor.getColor(hovered, false), 8);
            RenderManager.drawSvg(vg, SVGs.DROPDOWN_LIST, x + 452, y + 4, 24, 24);

            RenderManager.setAlpha(vg, 1f);
            RenderManager.drawRoundedRect(vg, x + 224, y + 40, 256, options.length * 32 + 8, Colors.GRAY_700, 12);
            RenderManager.drawHollowRoundRect(vg, x + 223, y + 39, 258, options.length * 32 + 10, new Color(204, 204, 204, 77).getRGB(), 12, 1);
            int optionY = y + 44;
            for (String option : options) {
                int color = Colors.WHITE_80;
                boolean optionHovered = InputUtils.isAreaHovered(x + 224, optionY, 252, 32, true);
                if (optionHovered && Platform.getMousePlatform().isButtonDown(0)) {
                    RenderManager.drawRoundedRect(vg, x + 228, optionY + 2, 248, 28, Colors.PRIMARY_700_80, 8);
                } else if (optionHovered) {
                    RenderManager.drawRoundedRect(vg, x + 228, optionY + 2, 248, 28, Colors.PRIMARY_700, 8);
                    color = Colors.WHITE;
                }
                if (optionHovered && InputUtils.isClicked(true)) {
                    try {
                        set(Arrays.asList(options).indexOf(option));
                    } catch (IllegalAccessException ignored) {
                    }
                    opened = false;
                    backgroundColor.setPalette(ColorPalette.SECONDARY);
                    if (inputScissor != null) InputUtils.stopBlock(inputScissor);
                }

                RenderManager.drawText(vg, option, x + 240, optionY + 18, color, 14, Fonts.MEDIUM);
                optionY += 32;
            }
        } else {
            RenderManager.drawRoundedRect(vg, x + 352, y, 640, 32, backgroundColor.getColor(hovered, hovered && Platform.getMousePlatform().isButtonDown(0)), 12);
            RenderManager.drawText(vg, options[selected], x + 364, y + 16, Colors.WHITE_80, 14f, Fonts.MEDIUM);
            if (hovered && Platform.getMousePlatform().isButtonDown(0)) RenderManager.setAlpha(vg, 0.8f);
            RenderManager.drawRoundedRect(vg, x + 964, y + 4, 24, 24, atomColor.getColor(hovered, false), 8);
            RenderManager.drawSvg(vg, SVGs.DROPDOWN_LIST, x + 964, y + 4, 24, 24);

            RenderManager.setAlpha(vg, 1f);
            RenderManager.drawRoundedRect(vg, x + 352, y + 40, 640, options.length * 32 + 8, Colors.GRAY_700, 12);
            RenderManager.drawHollowRoundRect(vg, x + 351, y + 39, 642, options.length * 32 + 10, new Color(204, 204, 204, 77).getRGB(), 12, 1);
            int optionY = y + 44;
            for (String option : options) {
                int color = Colors.WHITE_80;
                boolean optionHovered = InputUtils.isAreaHovered(x + 352, optionY, 640, 36, true);
                if (optionHovered && Platform.getMousePlatform().isButtonDown(0)) {
                    RenderManager.drawRoundedRect(vg, x + 356, optionY + 2, 632, 28, Colors.PRIMARY_700_80, 8);
                } else if (optionHovered) {
                    RenderManager.drawRoundedRect(vg, x + 356, optionY + 2, 632, 28, Colors.PRIMARY_700, 8);
                    color = Colors.WHITE;
                }

                RenderManager.drawText(vg, option, x + 368, optionY + 18, color, 14, Fonts.MEDIUM);

                if (optionHovered && InputUtils.isClicked(true)) {
                    try {
                        set(Arrays.asList(options).indexOf(option));
                    } catch (IllegalAccessException ignored) {
                    }
                    opened = false;
                    backgroundColor.setPalette(ColorPalette.SECONDARY);
                    if (inputScissor != null) InputUtils.stopBlock(inputScissor);
                }
                optionY += 32;
            }
        }
        RenderManager.setAlpha(vg, 1f);
    }

    @Override
    public float getHeight() {
        return 32;
    }
}
