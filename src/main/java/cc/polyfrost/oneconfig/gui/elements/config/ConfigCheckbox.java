package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import cc.polyfrost.oneconfig.utils.ColorUtils;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.MathUtils;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;
import java.lang.reflect.Field;

public class ConfigCheckbox extends BasicOption {
    private int color;
    private float percentOn = 0f;

    public ConfigCheckbox(Field field, Object parent, String name, int size) {
        super(field, parent, name, size);
    }

    @Override
    public void draw(long vg, int x, int y) {
        if (!isEnabled()) NanoVG.nvgGlobalAlpha(vg, 0.5f);
        boolean toggled = false;
        try {
            toggled = (boolean) get();
        } catch (IllegalAccessException ignored) {
        }
        boolean hover = InputUtils.isAreaHovered(x, y + 4, 24, 24);

        boolean clicked = InputUtils.isClicked() && hover;
        if (clicked && isEnabled()) {
            toggled = !toggled;
            try {
                set(toggled);
            } catch (IllegalAccessException e) {
                System.err.println("failed to write config value: class=" + this + " fieldWatching=" + field + " valueWrite=" + toggled);
                e.printStackTrace();
            }
        }
        if (percentOn != 1f) {       // performance
            RenderManager.drawRoundedRect(vg, x, y + 4, 24, 24, color, 6f);
            RenderManager.drawHollowRoundRect(vg, x, y + 4, 23.5f, 23.5f, OneConfigConfig.GRAY_300, 6f, 1f);        // the 0.5f is to make it look better ok
        }
        color = ColorUtils.smoothColor(color, OneConfigConfig.GRAY_600, OneConfigConfig.GRAY_400, hover, 40f);
        RenderManager.drawString(vg, name, x + 32, y + 17, OneConfigConfig.WHITE_90, 14f, Fonts.MEDIUM);
        percentOn = MathUtils.clamp(MathUtils.easeOut(percentOn, toggled ? 1f : 0f, 50f));
        if (percentOn != 0 && percentOn != 1f) {
            RenderManager.drawRoundedRect(vg, x, y + 4, 24, 24, ColorUtils.setAlpha(OneConfigConfig.BLUE_500, (int) (percentOn * 255)), 6f);
            RenderManager.drawSvg(vg, SVGs.CHECKBOX_TICK, x, y + 4, 24, 24, new Color(1f, 1f, 1f, percentOn).getRGB());
        } else if (percentOn != 0) {
            RenderManager.drawRoundedRect(vg, x, y + 4, 24, 24, OneConfigConfig.BLUE_500, 6f);
            RenderManager.drawSvg(vg, SVGs.CHECKBOX_TICK, x, y + 4, 24, 24);
        }
        NanoVG.nvgGlobalAlpha(vg, 1f);
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
