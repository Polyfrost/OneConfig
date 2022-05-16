package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import cc.polyfrost.oneconfig.lwjgl.scissor.Scissor;
import cc.polyfrost.oneconfig.lwjgl.scissor.ScissorManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.MathUtils;
import org.lwjgl.nanovg.NanoVG;

import java.lang.reflect.Field;

public class ConfigUniSelector extends BasicOption {
    private final String[] options;
    private float percentMove = 1f;
    private int previous = -1;

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
        if (!isEnabled()) NanoVG.nvgGlobalAlpha(vg, 0.5f);
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

        RenderManager.drawSvg(vg, SVGs.CHEVRON_LEFT, x + 231, y + 7, 18, 18, OneConfigConfig.BLUE_400);
        RenderManager.drawSvg(vg, SVGs.CHEVRON_RIGHT, x + 455, y + 7, 18, 18, OneConfigConfig.BLUE_400);

        if (InputUtils.isAreaClicked(x + 231, y + 7, 18, 18) && selected > 0 && isEnabled()) {
            previous = selected;
            selected -= 1;
            try {
                set(selected);
            } catch (IllegalAccessException ignored) {
            }
            percentMove = selected < previous ? 0f : 1f;
        } else if (InputUtils.isAreaClicked(x + 455, y + 7, 18, 18) && selected < options.length - 1 && isEnabled()) {
            previous = selected;
            selected += 1;
            try {
                set(selected);
            } catch (IllegalAccessException ignored) {
            }
            percentMove = selected < previous ? 0f : 1f;
        }
        if (previous != -1) percentMove = MathUtils.easeOut(percentMove, selected < previous ? 1f : 0f, 75);
        if ((selected < previous ? 1f : 0f) == percentMove) previous = -1;
        NanoVG.nvgGlobalAlpha(vg, 1f);
    }
}
