package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import cc.polyfrost.oneconfig.lwjgl.scissor.Scissor;
import cc.polyfrost.oneconfig.lwjgl.scissor.ScissorManager;
import cc.polyfrost.oneconfig.utils.ColorUtils;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.MathUtils;

import java.lang.reflect.Field;

public class ConfigUniSelector extends BasicOption {
    private final String[] options;
    private float percentMove = 1f;
    private int previous = -1;
    private int colorLeft;
    private int colorRight;

    public ConfigUniSelector(Field field, Object parent, String name, int size, String[] options) {
        super(field, parent, name, size);
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
        if (!isEnabled()) RenderManager.withAlpha(vg, 0.5f);
        String option = options[selected] + " " + (selected + 1) + "/" + options.length;
        RenderManager.drawString(vg, name, x, y + 16, OneConfigConfig.WHITE_90, 14f, Fonts.MEDIUM);

        Scissor scissor = ScissorManager.scissor(vg, x + 256, y, 192, 32);
        if (previous == -1) {
            RenderManager.drawString(vg, option, x + 352 - RenderManager.getTextWidth(vg, option, 14f, Fonts.MEDIUM) / 2f, y + 15, OneConfigConfig.WHITE_90, 14f, Fonts.MEDIUM);
        } else {
            String prevOption = options[previous] + " " + (previous + 1) + "/" + options.length;
            RenderManager.drawString(vg, selected < previous ? prevOption : option, x + 352 - RenderManager.getTextWidth(vg, selected < previous ? prevOption : option, 14f, Fonts.MEDIUM) / 2f + 192 * percentMove, y + 15, OneConfigConfig.WHITE_90, 14f, Fonts.MEDIUM);
            RenderManager.drawString(vg, selected < previous ? option : prevOption, x + 352 - RenderManager.getTextWidth(vg, selected < previous ? option : prevOption, 14f, Fonts.MEDIUM) / 2f - 192 * (1 - percentMove), y + 15, OneConfigConfig.WHITE_90, 14f, Fonts.MEDIUM);
        }
        ScissorManager.resetScissor(vg, scissor);

        boolean hoveredLeft = InputUtils.isAreaHovered(x + 231, y + 7, 18, 18) && selected > 0 && isEnabled();
        boolean hoveredRight = InputUtils.isAreaHovered(x + 455, y + 7, 18, 18) && selected < options.length - 1 && isEnabled();
        colorLeft = ColorUtils.smoothColor(colorLeft, OneConfigConfig.BLUE_500, OneConfigConfig.BLUE_400, hoveredLeft, 40f, OneConfigGui.INSTANCE.getDeltaTime());
        colorRight = ColorUtils.smoothColor(colorRight, OneConfigConfig.BLUE_500, OneConfigConfig.BLUE_400, hoveredRight, 40f, OneConfigGui.INSTANCE.getDeltaTime());

        if (selected <= 0 && isEnabled()) RenderManager.withAlpha(vg, 0.5f);
        RenderManager.drawSvg(vg, SVGs.CHEVRON_LEFT, x + 231, y + 7, 18, 18, colorLeft);
        if (isEnabled()) RenderManager.withAlpha(vg, selected >= options.length - 1 ? 0.5f : 1f);
        RenderManager.drawSvg(vg, SVGs.CHEVRON_RIGHT, x + 455, y + 7, 18, 18, colorRight);
        if (isEnabled()) RenderManager.withAlpha(vg, 1f);

        if (hoveredLeft && InputUtils.isClicked()) {
            previous = selected;
            selected -= 1;
            try {
                set(selected);
            } catch (IllegalAccessException ignored) {
            }
            percentMove = selected < previous ? 0f : 1f;
        } else if (hoveredRight && InputUtils.isClicked()) {
            previous = selected;
            selected += 1;
            try {
                set(selected);
            } catch (IllegalAccessException ignored) {
            }
            percentMove = selected < previous ? 0f : 1f;
        }
        if (previous != -1) percentMove = MathUtils.easeOut(percentMove, selected < previous ? 1f : 0f, 75, OneConfigGui.INSTANCE.getDeltaTime());
        if ((selected < previous ? 1f : 0f) == percentMove) previous = -1;
        RenderManager.withAlpha(vg, 1f);
    }
}
