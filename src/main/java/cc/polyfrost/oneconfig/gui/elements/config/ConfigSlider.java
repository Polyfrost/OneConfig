package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.elements.text.NumberInputField;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.MathUtils;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

public class ConfigSlider extends BasicOption {
    private final NumberInputField inputField;
    private final float min, max;
    private final int step;
    private boolean isFloat = true;
    private boolean dragging = false;
    private boolean mouseWasDown = false;

    public ConfigSlider(Field field, Object parent, String name, String category, String subcategory, float min, float max, int step) {
        super(field, parent, name,category, subcategory, 2);
        this.min = min;
        this.max = max;
        this.step = step;
        inputField = new NumberInputField(84, 32, 0, min, max, step == 0 ? 1 : step);
    }

    public static ConfigSlider create(Field field, Object parent) {
        Slider slider = field.getAnnotation(Slider.class);
        return new ConfigSlider(field, parent, slider.name(), slider.category(), slider.subcategory(), slider.min(), slider.max(), slider.step());
    }

    @Override
    public void draw(long vg, int x, int y) {
        int xCoordinate = 0;
        float value = 0;
        boolean hovered = InputUtils.isAreaHovered(x + 352, y, 512, 32) && isEnabled();
        inputField.disable(!isEnabled());
        if (!isEnabled()) RenderManager.setAlpha(vg, 0.5f);
        boolean isMouseDown = Mouse.isButtonDown(0);
        if (hovered && isMouseDown && !mouseWasDown) dragging = true;
        mouseWasDown = isMouseDown;
        if (dragging) {
            xCoordinate = (int) MathUtils.clamp(InputUtils.mouseX(), x + 352, x + 864);
            if (step > 0) xCoordinate = getStepCoordinate(xCoordinate, x);
            value = MathUtils.map(xCoordinate, x + 352, x + 864, min, max);
        } else if (inputField.isToggled() || inputField.arrowsClicked()) {
            value = inputField.getCurrentValue();
            xCoordinate = (int) MathUtils.clamp(MathUtils.map(value, min, max, x + 352, x + 864), x + 352, x + 864);
        }
        if (dragging && InputUtils.isClicked() || inputField.isToggled() || inputField.arrowsClicked()) {
            dragging = false;
            if (step > 0) {
                xCoordinate = getStepCoordinate(xCoordinate, x);
                value = MathUtils.map(xCoordinate, x + 352, x + 864, min, max);
            }
            setValue(value);
        }

        if (!dragging && !inputField.isToggled()) {
            try {
                Object object = get();
                if (object instanceof Integer)
                    isFloat = false;
                if (isFloat) value = (float) object;
                else value = (int) object;
                xCoordinate = (int) MathUtils.clamp(MathUtils.map(value, min, max, x + 352, x + 864), x + 352, x + 864);
            } catch (IllegalAccessException ignored) {
            }
        }
        if (!inputField.isToggled()) inputField.setCurrentValue(value);

        RenderManager.drawText(vg, name, x, y + 17, Colors.WHITE_90, 14f, Fonts.MEDIUM);
        RenderManager.drawRoundedRect(vg, x + 352, y + 13, 512, 6, Colors.GRAY_300, 4f);
        RenderManager.drawRoundedRect(vg, x + 352, y + 13, xCoordinate - x - 352, 6, Colors.PRIMARY_500, 4f);
        if (step > 0) {
            for (float i = x + 352; i <= x + 864; i += 512 / ((max - min) / step)) {
                int color = xCoordinate > i - 2 ? Colors.PRIMARY_500 : Colors.GRAY_300;
                RenderManager.drawRoundedRect(vg, i - 2, y + 9, 4, 14, color, 2f);
            }
        }
        if (step == 0) RenderManager.drawRoundedRect(vg, xCoordinate - 12, y + 4, 24, 24, Colors.WHITE, 12f);
        else RenderManager.drawRoundedRect(vg, xCoordinate - 4, y + 4, 8, 24, Colors.WHITE, 4f);
        inputField.draw(vg, x + 892, y);
        RenderManager.setAlpha(vg, 1f);
    }

    private int getStepCoordinate(int xCoordinate, int x) {
        Integer nearest = null;
        for (float i = x + 352; i <= x + 864; i += 512 / ((max - min) / step)) {
            if (nearest == null || Math.abs(xCoordinate - i) < Math.abs(xCoordinate - nearest)) nearest = (int) i;
        }
        return nearest == null ? 0 : nearest;
    }

    private void setValue(float value) {
        try {
            if (isFloat) set(value);
            else set(Math.round(value));
        } catch (IllegalAccessException ignored) {
        }
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        inputField.keyTyped(key, keyCode);
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
