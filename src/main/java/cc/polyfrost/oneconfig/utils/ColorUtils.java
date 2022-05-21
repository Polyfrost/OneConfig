package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.config.OneConfigConfig;

import java.awt.*;

public class ColorUtils {

    public static int getColor(int currentColor, int colorPalette, boolean hover, boolean click, float deltaTime) {
        float[] color = splitColor(currentColor);
        if (click) {
            switch (colorPalette) {
                case -2:
                    return new Color(0.9f, 0.9f, 0.9f, 0.2f).getRGB();
                case -1:
                    return OneConfigConfig.GRAY_500_80;
                default:
                case 2:
                case 0:
                    return OneConfigConfig.GRAY_400_80;
                case 1:
                    return OneConfigConfig.BLUE_600_80;
            }
        }

        switch (colorPalette) {
            case -2:
                return getColorComponents(color, splitColor(OneConfigConfig.TRANSPARENT), new float[]{0.9f, 0.9f, 0.9f, 0.3f}, hover, 20f, deltaTime);
            case -1:
                return getColorComponents(color, splitColor(OneConfigConfig.TRANSPARENT), splitColor(OneConfigConfig.GRAY_500), hover, 10f, deltaTime);
            default:
            case 0:
                return getColorComponents(color, splitColor(OneConfigConfig.GRAY_600), splitColor(OneConfigConfig.GRAY_300), hover, 25f, deltaTime);
            case 1:
                return getColorComponents(color, splitColor(OneConfigConfig.BLUE_600), splitColor(OneConfigConfig.BLUE_500), hover, 150f, deltaTime);
            case 2:
                return getColorComponents(color, splitColor(OneConfigConfig.GRAY_500), splitColor(OneConfigConfig.GRAY_300), hover, 50f, deltaTime);
            case 3:
                return getColorComponents(color, splitColor(OneConfigConfig.GRAY_500), splitColor(OneConfigConfig.GRAY_300), hover, 25f, deltaTime);
        }

    }

    /**
     * Smooths the transition of a color between two colors.
     *
     * @param currentColor the current color (also the one you want to change)
     * @param direction    false to move towards initColor, true to move towards finalColor
     * @param speed        speed of the transition
     * @return currentColor but with the new color
     */
    public static int smoothColor(int currentColor, int initColor, int finalColor, boolean direction, float speed, float deltaTime) {
        float[] init = splitColor(initColor);
        float[] finalC = splitColor(finalColor);
        float[] current = splitColor(currentColor);
        return getColorComponents(current, init, finalC, direction, speed, deltaTime);
    }

    private static float[] splitColor(int color) {
        return new float[]{(color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, (color >> 24 & 255) / 255f};
    }

    private static int getColorComponents(float[] currentColor, float[] initColor, float[] finalColor, boolean hover, float speed, float deltaTime) {
        currentColor[0] = smooth(currentColor[0], initColor[0], finalColor[0], hover, speed, deltaTime);
        currentColor[1] = smooth(currentColor[1], initColor[1], finalColor[1], hover, speed, deltaTime);
        currentColor[2] = smooth(currentColor[2], initColor[2], finalColor[2], hover, speed, deltaTime);
        currentColor[3] = smooth(currentColor[3], initColor[3], finalColor[3], hover, speed, deltaTime);

        return new Color(currentColor[0], currentColor[1], currentColor[2], currentColor[3]).getRGB();

    }

    private static float smooth(float current, float min, float max, boolean moveToFinal, float speed, float deltaTime) {
        current = MathUtils.easeOut(current, moveToFinal ? 1f : 0f, speed, deltaTime);
        if (current <= min) {
            current = min;
        }

        if (current >= max) {
            current = max;
        }
        return current;
    }

    public static int setAlpha(int color, int alpha) {
        return  ( alpha << 24 ) | ( color & 0x00ffffff );
    }
}
