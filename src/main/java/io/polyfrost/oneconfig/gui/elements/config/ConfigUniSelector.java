package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.interfaces.BasicOption;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.Scissor;
import io.polyfrost.oneconfig.lwjgl.ScissorManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.lwjgl.image.Images;
import io.polyfrost.oneconfig.utils.InputUtils;
import io.polyfrost.oneconfig.utils.MathUtils;
import org.lwjgl.nanovg.NanoVG;

import java.lang.reflect.Field;

public class ConfigUniSelector extends BasicOption {
    private final String[] options;
    private float percentMove = 1f;
    private int previous = -1;

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
        RenderManager.drawString(vg, name, x, y + 16, OneConfigConfig.WHITE_90, 14f, Fonts.INTER_MEDIUM);

        Scissor scissor = ScissorManager.scissor(vg, x + 256, y, 192, 32);
        if (previous == -1) {
            RenderManager.drawString(vg, option, x + 352 - RenderManager.getTextWidth(vg, option, 12f, Fonts.INTER_MEDIUM) / 2f, y + 15, OneConfigConfig.WHITE_90, 12f, Fonts.INTER_MEDIUM);
        } else {
            String prevOption = options[previous] + " " + (previous + 1) + "/" + options.length;
            RenderManager.drawString(vg, selected < previous ? prevOption : option, x + 352 - RenderManager.getTextWidth(vg, selected < previous ? prevOption : option, 12f, Fonts.INTER_MEDIUM) / 2f + 192 * percentMove, y + 15, OneConfigConfig.WHITE_90, 12f, Fonts.INTER_MEDIUM);
            RenderManager.drawString(vg, selected < previous ? option : prevOption, x + 352 - RenderManager.getTextWidth(vg, selected < previous ? option : prevOption, 12f, Fonts.INTER_MEDIUM) / 2f - 192 * (1 - percentMove), y + 15, OneConfigConfig.WHITE_90, 12f, Fonts.INTER_MEDIUM);
        }
        ScissorManager.resetScissor(vg, scissor);

        // actual coordinates: 240, 7
        NanoVG.nvgTranslate(vg, x + 248, y + 21);
        NanoVG.nvgRotate(vg, (float) Math.toRadians(180));
        RenderManager.drawImage(vg, Images.CHEVRON_ARROW, 0, 0, 8, 14, OneConfigConfig.BLUE_400);
        NanoVG.nvgResetTransform(vg);
        RenderManager.drawImage(vg, Images.CHEVRON_ARROW, x + 456, y + 7, 8, 14, OneConfigConfig.BLUE_400);

        if (InputUtils.isAreaClicked(x + 235, y + 5, 18, 18) && selected > 0) {
            previous = selected;
            selected -= 1;
            try {
                set(selected);
            } catch (IllegalAccessException ignored) {
            }
            percentMove = selected < previous ? 0f : 1f;
        } else if (InputUtils.isAreaClicked(x + 451, y + 5, 18, 18) && selected < options.length - 1) {
            previous = selected;
            selected += 1;
            try {
                set(selected);
            } catch (IllegalAccessException ignored) {
            }
            percentMove = selected < previous ? 0f : 1f;
        }
        if (previous != -1) percentMove = MathUtils.easeOut(percentMove, selected < previous ? 1f : 0f, 10);
        if ((selected < previous ? 1f : 0f) == percentMove) previous = -1;
    }
}
