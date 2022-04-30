package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.interfaces.BasicOption;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.utils.ColorUtils;
import io.polyfrost.oneconfig.utils.InputUtils;
import io.polyfrost.oneconfig.utils.MathUtils;

import java.awt.*;
import java.lang.reflect.Field;

public class ConfigCheckbox extends BasicOption {
    private int color;
    private float percentOn = 0f;
    private boolean toggled = false;

    public ConfigCheckbox(Field field, String name, int size) {
        super(field, name, size);
    }

    @Override
    public void draw(long vg, int x, int y) {
        boolean hover = InputUtils.isAreaHovered(x, y, 24, 24);

        boolean clicked = InputUtils.isClicked() && hover;
        if(clicked) {
            toggled = !toggled;
        }
        if(percentOn != 1f) {       // performance
            RenderManager.drawRoundedRect(vg, x, y, 24, 24, color, 6f);
            RenderManager.drawHollowRoundRect(vg, x, y, 23.5f, 23.5f, OneConfigConfig.GRAY_300, 6f, 1f);        // the 0.5f is to make it look better ok
        }
        color = ColorUtils.smoothColor(color, OneConfigConfig.GRAY_600, OneConfigConfig.GRAY_400, hover, 40f);
        RenderManager.drawString(vg, name, x + 32, y + 14, OneConfigConfig.WHITE_90, 18f, Fonts.INTER_MEDIUM);
        percentOn = MathUtils.clamp(MathUtils.easeOut(percentOn, toggled ? 1f : 0f, 5f));
        if(percentOn == 0f) return;
        if(percentOn != 1f) {
            RenderManager.drawImage(vg, "/assets/oneconfig/textures/check.png", x, y, 24, 24, new Color(1f, 1f, 1f, percentOn).getRGB());
        } else {       // performance, that color could cause havoc am I right definitely
            RenderManager.drawImage(vg, "/assets/oneconfig/textures/check.png", x, y, 24, 24);
        }
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
