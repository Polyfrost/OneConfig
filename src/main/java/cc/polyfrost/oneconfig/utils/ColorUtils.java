package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.config.OneConfigConfig;

import java.awt.*;

/**
 * A class to help with color manipulation.
 */
public class ColorUtils {

    public static int getColor(int currentColor, int colorPalette, boolean hover, boolean click) {
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
                    return OneConfigConfig.PRIMARY_600_80;
            }
        }

        switch (colorPalette) {
            case -2:
                return getColorComponents(color, splitColor(OneConfigConfig.TRANSPARENT), new float[]{0.9f, 0.9f, 0.9f, 0.3f}, hover, 50f);
            case -1:
                return getColorComponents(color, splitColor(OneConfigConfig.TRANSPARENT), splitColor(OneConfigConfig.GRAY_500), hover, 50f);
            default:
            case 0:
                return getColorComponents(color, splitColor(OneConfigConfig.GRAY_600), splitColor(OneConfigConfig.GRAY_300), hover, 50f);
            case 1:
                return getColorComponents(color, splitColor(OneConfigConfig.PRIMARY_600), splitColor(OneConfigConfig.PRIMARY_500), hover, 150f);
            case 2:
                return getColorComponents(color, splitColor(OneConfigConfig.GRAY_500), splitColor(OneConfigConfig.GRAY_300), hover, 50f);
            case 3:
                return getColorComponents(color, splitColor(OneConfigConfig.GRAY_500), splitColor(OneConfigConfig.GRAY_300), hover, 150f);
        }

    }

    /**
     * Smooths the transition of a color between two colors.
     *
     * @param currentColor the current color (also the one you want to change)
     * @param direction    false to move towards initColor, true to move towards finalColor
     * @param speed        speed of the transition
     */
    public static int smoothColor(int currentColor, int initColor, int finalColor, boolean direction, float speed) {
        float[] init = splitColor(initColor);
        float[] finalC = splitColor(finalColor);
        float[] current = splitColor(currentColor);
        return getColorComponents(current, init, finalC, direction, speed + 100f);
    }

    private static float[] splitColor(int color) {
        return new float[]{getRed(color) / 255f, getGreen(color) / 255f, getBlue(color) / 255f, getAlpha(color) / 255f};
    }

    private static int getColorComponents(float[] currentColor, float[] initColor, float[] finalColor, boolean hover, float speed) {
        currentColor[0] = smooth(currentColor[0], initColor[0], finalColor[0], hover, speed);
        currentColor[1] = smooth(currentColor[1], initColor[1], finalColor[1], hover, speed);
        currentColor[2] = smooth(currentColor[2], initColor[2], finalColor[2], hover, speed);
        currentColor[3] = smooth(currentColor[3], initColor[3], finalColor[3], hover, speed);

        return ((int) (currentColor[3] * 255) << 24) |
                ((int) (currentColor[0] * 255) << 16) |
                ((int) (currentColor[1] * 255) << 8)  |
                ((int) (currentColor[2] * 255));

    }

    private static float smooth(float current, float min, float max, boolean moveToFinal, float speed) {
        current = MathUtils.easeOut(current, moveToFinal ? 1f : 0f, speed);
        if (current <= min) {
            current = min;
        }

        if (current >= max) {
            current = max;
        }
        return current;
    }

    /**
     * Get the red component of an RGB color.
     * @param color the color.
     * @return the red component.
     */
    public static int getRed(int color) {
        return (color >> 16) & 0xFF;
    }

    /**
     * Get the green component of an RGB color.
     * @param color the color.
     * @return the green component.
     */
    public static int getGreen(int color) {
        return (color >> 8) & 0xFF;
    }

    /**
     * Get the blue component of an RGB color.
     * @param color the color.
     * @return the blue component.
     */
    public static int getBlue(int color) {
        return color & 0xFF;
    }

    /**
     * Get the alpha component of an ARGB color.
     * @param color the color.
     * @return the alpha component.
     */
    public static int getAlpha(int color) {
        return (color >> 24) & 0xFF;
    }

    /**
     * Get the RGB color from the given color components.
     * @param red the red component.
     * @param green the green component.
     * @param blue the blue component.
     * @param alpha the alpha component.
     * @return the RGB color.
     */
    public static int getColor(float red, float green, float blue, float alpha) {
        return getColor((int) red * 255, (int) green * 255, (int) blue * 255, (int) alpha * 255);
    }

    /**
     * Get the RGB color from the given color components.
     * @param red the red component.
     * @param green the green component.
     * @param blue the blue component.
     * @return the RGB color.
     */
    public static int getColor(int red, int green, int blue) {
        return getColor(red, green, blue, 255);
    }

    /**
     * Get the RGB color from the given color components.
     * @param red the red component.
     * @param green the green component.
     * @param blue the blue component.
     * @param alpha the alpha component.
     * @return the RGB color.
     */
    public static int getColor(int red, int green, int blue, int alpha) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Return the color with the given red component.
     * @param color the color.
     * @param red the red component.
     * @return the color with the given red component.
     */
    public static int setRed(int color, int red) {
        return (color & 0x00FFFF) | (red << 16);
    }

    /**
     * Return the color with the given green component.
     * @param color the color.
     * @param green the green component.
     * @return the color with the given green component.
     */
    public static int setGreen(int color, int green) {
        return (color & 0xFF00FF) | (green << 8);
    }

    /**
     * Return the color with the given blue component.
     * @param color the color.
     * @param blue the blue component.
     * @return the color with the given blue component.
     */
    public static int setBlue(int color, int blue) {
        return (color & 0xFFFF00) | blue;
    }

    /**
     * Return the color with the given alpha component.
     * @param color the color.
     * @param alpha the alpha component.
     * @return the color with the given alpha component.
     */
    public static int setAlpha(int color, int alpha) {
        return (color & 0xFFFFFF) | (alpha << 24);
    }
}
