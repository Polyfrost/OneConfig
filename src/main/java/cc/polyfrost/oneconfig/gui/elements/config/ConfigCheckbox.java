package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.gui.Colors;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.ColorAnimation;
import cc.polyfrost.oneconfig.gui.animations.DummyAnimation;
import cc.polyfrost.oneconfig.gui.animations.EaseInOutQuad;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.renderer.image.SVGs;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import cc.polyfrost.oneconfig.utils.color.ColorUtils;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.lang.reflect.Field;

public class ConfigCheckbox extends BasicOption {
    private final ColorAnimation color = new ColorAnimation(ColorPalette.SECONDARY);
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
            if (animation == null) animation = new DummyAnimation(toggled ? 1 : 0);
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
        float percentOn = animation.get();

        RenderManager.drawText(vg, name, x + 32, y + 17, Colors.WHITE_90, 14f, Fonts.MEDIUM);

        RenderManager.drawRoundedRect(vg, x, y + 4, 24, 24, color.getColor(hover, hover && Mouse.isButtonDown(0)), 6f);
        RenderManager.drawHollowRoundRect(vg, x, y + 4, 23.5f, 23.5f, Colors.GRAY_300, 6f, 1f);        // the 0.5f is to make it look better ok

        RenderManager.drawRoundedRect(vg, x, y + 4, 24, 24, ColorUtils.setAlpha(Colors.PRIMARY_500, (int) (percentOn * 255)), 6f);
        RenderManager.drawSvg(vg, SVGs.CHECKBOX_TICK, x, y + 4, 24, 24, new Color(1f, 1f, 1f, percentOn).getRGB());

        if (toggled && hover)
            RenderManager.drawHollowRoundRect(vg, x - 1, y + 3, 24, 24, Colors.PRIMARY_600, 6f, 2f);
        RenderManager.setAlpha(vg, 1f);
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
