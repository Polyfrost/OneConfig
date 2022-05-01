package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.interfaces.BasicOption;
import io.polyfrost.oneconfig.gui.elements.BasicElement;
import io.polyfrost.oneconfig.gui.elements.TextInputField;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.utils.ColorUtils;
import io.polyfrost.oneconfig.utils.InputUtils;
import io.polyfrost.oneconfig.utils.MathUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.nanovg.NanoVG;

import java.lang.reflect.Field;

public class ConfigSlider extends BasicOption {
    private final BasicElement slideYBoi = new BasicElement(24, 24, false);
    private final TextInputField inputField = new TextInputField(84, 24, "", false, false);
    private final BasicElement upArrow = new BasicElement(12, 14, false);
    private final BasicElement downArrow = new BasicElement(12, 14, false);
    private final float min, max;
    private final int step;
    private float current;
    private int colorTop, colorBottom;

    public ConfigSlider(Field field, String name, int size, float min, float max, int step) {
        super(field, name, size);
        this.min = min;
        this.max = max;
        this.step = step - 1;       // it adds one more step than actual
        slideYBoi.setCustomHitbox(28, 8);
        inputField.onlyAcceptNumbers(true);
        inputField.setCentered(true);
        try {
            inputField.setInput(String.valueOf(get()));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public void draw(long vg, int x, int y) {
        RenderManager.drawString(vg, name, x, y + 15, OneConfigConfig.WHITE_90, 18f, Fonts.INTER_MEDIUM);
        RenderManager.drawRoundedRect(vg, x + 352, y + 13, 512, 6, OneConfigConfig.GRAY_300, 4f);
        slideYBoi.update(x + 340 + (int) (current * 512), y + 4);
        if(step != 0) {
            for(float i = 0; i <= 1.005f; i += 1f / step) {         // sometimes it's just more than 1, so we add a little
                int color = current > i ? OneConfigConfig.BLUE_500 : OneConfigConfig.GRAY_300;
                RenderManager.drawRoundedRect(vg, x + 351 + (int) (i * 512), y + 9, 4, 14, color, 2f);
            }
        }
        RenderManager.drawRoundedRect(vg, x + 352, y + 13, (int) (current * 512), 6, OneConfigConfig.BLUE_500, 4f);
        if(step == 0) RenderManager.drawRoundedRect(vg, x + 340 + (int) (current * 512), y + 4, 24, 24, OneConfigConfig.WHITE, 12f);
        else RenderManager.drawRoundedRect(vg, x + 346 + (int) (current * 512), y + 4, 8, 24, OneConfigConfig.WHITE, 4f);


        int mouseX = InputUtils.mouseX() - (x + 352);
        if(InputUtils.isAreaClicked(x + 332, y + 9, 542, 10) && !slideYBoi.isHovered()) {
            if(step == 0) {
                current = MathUtils.clamp(mouseX / 512f);
            } else current = MathUtils.clamp(toNearestStep(mouseX / 512f));
        }
        if(slideYBoi.isHovered() && Mouse.isButtonDown(0)) {
            if(step == 0) {
                current = MathUtils.clamp(mouseX / 512f);
            } else current = MathUtils.clamp(toNearestStep(mouseX / 512f));
        }
        float currentAsNum = current * (max - min) + min;

        if(!inputField.isToggled()) inputField.setInput(String.format("%.01f", currentAsNum));
        inputField.setErrored(false);
        if(inputField.isToggled()) {
            try {
                float input = Float.parseFloat(inputField.getInput());
                if(input < min) {
                    inputField.setErrored(true);
                    input = min;
                }
                if(input > max) {
                    inputField.setErrored(true);
                    input = max;
                }
                if(step == 0) {
                    current = MathUtils.clamp((input - min) / (max - min));
                } else {
                    current = toNearestStep(MathUtils.clamp((input - min) / (max - min)));
                }
            } catch (NumberFormatException ignored) {
                inputField.setErrored(true);
            }
        }
        inputField.draw(vg, x + 892, y);

        RenderManager.drawRoundedRect(vg, x + 980, y, 12, 28, OneConfigConfig.GRAY_500, 6f);
        upArrow.update(x + 980, y);
        downArrow.update(x + 980, y + 14);
        if(current == 1f) colorTop = OneConfigConfig.GRAY_500_80;
        if(current == 0f) colorBottom = OneConfigConfig.GRAY_500_80;
        colorTop = ColorUtils.getColor(colorTop, 2, upArrow.isHovered(), upArrow.isClicked());
        colorBottom = ColorUtils.getColor(colorBottom, 2, downArrow.isHovered(), downArrow.isClicked());
        if(upArrow.isClicked()) {
            if(step == 0) {
                currentAsNum += 1;
                current = MathUtils.clamp((currentAsNum - min) / (max - min));
            } else {
                for(float i1 = 0f; i1 <= 1f; i1 += 1f / step) {
                    if(i1 > current) {
                        current = i1;
                        break;
                    }
                }
            }
        }
        if(downArrow.isClicked()) {
            if(step == 0) {
                currentAsNum -= 1;
                current = MathUtils.clamp((currentAsNum - min) / (max - min));
            } else {
                for(float i1 = 1f; i1 >= 0f; i1 -= 1f / step) {
                    if(i1 < current) {
                        current = i1;
                        break;
                    }
                }
            }
        }
        if(current == 1f) NanoVG.nvgGlobalAlpha(vg, 0.3f);
        RenderManager.drawRoundedRectVaried(vg, x + 980, y, 12, 14, colorTop,6f, 6f, 0f, 0f);
        RenderManager.drawImage(vg, "/assets/oneconfig/textures/smallUpArrow.png", x + 981, y + 2, 10, 10);
        if(current == 1f) NanoVG.nvgGlobalAlpha(vg, 1f);

        if(current == 0f) NanoVG.nvgGlobalAlpha(vg, 0.3f);
        RenderManager.drawRoundedRectVaried(vg, x + 980, y + 14, 12, 14, colorBottom,0f, 0f, 6f, 6f);
        NanoVG.nvgTranslate(vg, x + 991, y + 25);
        NanoVG.nvgRotate(vg, (float) Math.toRadians(180));
        RenderManager.drawImage(vg, "/assets/oneconfig/textures/smallUpArrow.png", 0, 0, 10, 10);
        NanoVG.nvgResetTransform(vg);
        NanoVG.nvgGlobalAlpha(vg, 1f);
    }

    private float toNearestStep(float input) {
        float stepF = 1f / step;
        float stepAbove = 1f, stepBelow = 0f;
        for(float a = 0f; a <= 1f; a += stepF) {
            if(a > input) {
                stepAbove = a;
                break;
            }
        }
        for(float a = 1f; a >= 0f; a -= stepF) {
            if(a <= input) {
                stepBelow = a;
                break;
            }
        }
        if(stepAbove - input > input - stepBelow) {
            return stepBelow;
        } else {
            return stepAbove;
        }
    }

    @Override
    public boolean hasHalfSize() {
        return false;
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        inputField.keyTyped(key, keyCode);
    }
}
