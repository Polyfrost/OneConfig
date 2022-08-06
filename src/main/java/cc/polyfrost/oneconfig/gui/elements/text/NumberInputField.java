package cc.polyfrost.oneconfig.gui.elements.text;

import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.gui.animations.ColorAnimation;
import cc.polyfrost.oneconfig.gui.elements.BasicElement;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;

public class NumberInputField extends TextInputField {
    private final BasicElement upArrow = new BasicElement(12, 14, false);
    private final BasicElement downArrow = new BasicElement(12, 14, false);
    private final ColorAnimation colorTop = new ColorAnimation(ColorPalette.SECONDARY);
    private final ColorAnimation colorBottom = new ColorAnimation(ColorPalette.SECONDARY);
    private float min;
    private float max;
    private float step;
    private float current;

    public NumberInputField(int width, int height, float defaultValue, float min, float max, float step) {
        super(width - 16, height, "", false, false, null);      // TODO
        //super.onlyNums = true;
        this.min = min;
        this.max = max;
        this.step = step;
        this.input = String.format("%.01f", defaultValue);
    }

    @Override
    public void draw(long vg, float x, float y) {
        super.errored = false;
        if(disabled) RenderManager.setAlpha(vg, 0.5f);
        RenderManager.drawRoundedRect(vg, x + width + 4, y, 12, 28, Colors.GRAY_500, 6f);
        upArrow.disable(disabled);
        downArrow.disable(disabled);
        upArrow.update(x + width + 4, y);
        downArrow.update(x + width + 4, y + 14);
        try {
            current = Float.parseFloat(input);
        } catch (NumberFormatException e) {
            super.errored = true;
        }

        if (current < min || current > max) {
            super.errored = true;
        } else {
            upArrow.disable(false);
            downArrow.disable(false);
        }

        if (upArrow.isClicked()) {
            current += step;
            if (current > max) current = max;
            setCurrentValue(current);
        }
        if (downArrow.isClicked()) {
            current -= step;
            if (current < min) current = min;
            setCurrentValue(current);
        }
        if (current >= max && !disabled) {
            RenderManager.setAlpha(vg, 0.3f);
            upArrow.disable(true);
        }
        RenderManager.drawRoundedRectVaried(vg, x + width + 4, y, 12, 14, colorTop.getColor(upArrow.isHovered(), upArrow.isPressed()), 6f, 6f, 0f, 0f);
        RenderManager.drawSvg(vg, SVGs.CHEVRON_UP, x + width + 5, y + 2, 10, 10);
        if (current >= max && !disabled) RenderManager.setAlpha(vg, 1f);

        if (current <= min && !disabled) {
            RenderManager.setAlpha(vg, 0.3f);
            downArrow.disable(true);
        }
        RenderManager.drawRoundedRectVaried(vg, x + width + 4, y + 14, 12, 14, colorBottom.getColor(downArrow.isHovered(), downArrow.isPressed()), 0f, 0f, 6f, 6f);
        RenderManager.drawSvg(vg, SVGs.CHEVRON_DOWN, x + width + 5, y + 15, 10, 10);
        if(!disabled) RenderManager.setAlpha(vg, 1f);

        try {
            super.draw(vg, x, y - 2);
        } catch (Exception e) {
            setCurrentValue(current);
            super.caretPos = 0;
            //super.prevCaret = 0;
        }
        if(disabled) RenderManager.setAlpha(vg, 1f);
    }


    public float getCurrentValue() {
        return current;
    }

    public void setCurrentValue(float value) {
        input = String.format("%.01f", value);
    }

    @Override
    public void close() {
        try {
            if (current < min) current = min;
            if (current > max) current = max;
            setCurrentValue(current);
        } catch (Exception ignored) {

        }
    }

    public void setStep(float step) {
        this.step = step;
    }

    public void setMax(float max) {
        this.max = max;
    }

    public void setMin(float min) {
        this.min = min;
    }

    public boolean arrowsClicked() {
        return upArrow.isClicked() || downArrow.isClicked();
    }
}
