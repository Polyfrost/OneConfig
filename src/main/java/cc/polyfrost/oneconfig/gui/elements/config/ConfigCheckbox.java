package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.DummyAnimation;
import cc.polyfrost.oneconfig.gui.animations.EaseInOutQuad;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import cc.polyfrost.oneconfig.utils.color.ColorUtils;

import java.awt.*;
import java.lang.reflect.Field;

public class ConfigCheckbox extends BasicOption {
    private int color;
    private Animation animation;

    public ConfigCheckbox(Field field, Object parent, String name, int size) {
        super(field, parent, name, size);
    }

    @Override
    public void draw(long vg, int x, int y) {
        if (!isEnabled()) RenderManager.setAlpha(vg, 0.5f);
        boolean toggled = false;
        try {
            toggled = (boolean) get();
            if (animation == null) animation = new DummyAnimation(toggled ? 0 : 1);
        } catch (IllegalAccessException ignored) {
        }
        boolean hover = InputUtils.isAreaHovered(x, y + 4, 24, 24);

        boolean clicked = InputUtils.isClicked() && hover;
        if (clicked && isEnabled()) {
            toggled = !toggled;
            animation = new EaseInOutQuad(100, 0, 1, !toggled);
            try {
                set(toggled);
            } catch (IllegalAccessException e) {
                System.err.println("failed to write config value: class=" + this + " fieldWatching=" + field + " valueWrite=" + toggled);
                e.printStackTrace();
            }
        }
        color = ColorUtils.getColor(color, ColorPalette.SECONDARY, hover, false);
        float percentOn = animation.get();
        if (percentOn != 1f) {
            RenderManager.drawRoundedRect(vg, x, y + 4, 24, 24, color, 6f);
            RenderManager.drawHollowRoundRect(vg, x, y + 4, 23.5f, 23.5f, OneConfigConfig.GRAY_300, 6f, 1f);        // the 0.5f is to make it look better ok
        }
        RenderManager.drawText(vg, name, x + 32, y + 17, OneConfigConfig.WHITE_90, 14f, Fonts.MEDIUM);
        if (percentOn != 0 && percentOn != 1f) {
            RenderManager.drawRoundedRect(vg, x, y + 4, 24, 24, ColorUtils.setAlpha(OneConfigConfig.PRIMARY_500, (int) (percentOn * 255)), 6f);
            RenderManager.drawSvg(vg, SVGs.CHECKBOX_TICK, x, y + 4, 24, 24, new Color(1f, 1f, 1f, percentOn).getRGB());
        } else if (percentOn != 0) {
            RenderManager.drawRoundedRect(vg, x, y + 4, 24, 24, OneConfigConfig.PRIMARY_500, 6f);
            RenderManager.drawSvg(vg, SVGs.CHECKBOX_TICK, x, y + 4, 24, 24);
        }
        if (toggled && hover)
            RenderManager.drawHollowRoundRect(vg, x - 1, y + 3, 24, 24, OneConfigConfig.PRIMARY_600, 6f, 2f);
        RenderManager.setAlpha(vg, 1f);
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
