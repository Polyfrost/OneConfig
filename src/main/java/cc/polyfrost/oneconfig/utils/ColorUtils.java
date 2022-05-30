package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.config.OneConfigConfig;

import java.awt.*;

/**
 * A class to help with color manipulation.
 */
public final class ColorUtils {
    /**
     * Always returns transparent.
     */
    public static final int TRANSPARENT = -10;
    /**
     * <h1>Primary Color Scheme</h1> Normal: Primary 600,<br> Hover: Primary 700,<br> Clicked: Primary 700 (80%)
     */
    public static final int PRIMARY = 1;
    /**
     * <h1>Secondary Color Scheme</h1> Normal: Gray 500,<br> Hover: Gray 400,<br> Clicked: Gray 400 (80%)
     */
    public static final int SECONDARY = 2;
    /**
     * <h1>Secondary (Transparent) Color Scheme</h1> Normal: Transparent,<br> Hover: Gray rgba(229, 229, 229, 77),<br> Clicked: Gray rgba(229, 229, 229, 51)
     */
    public static final int SECONDARY_TRANSPARENT = 0;
    /**
     * <h1>Tertiary Color Scheme</h1> Normal: Transparent (Text=White 90%),<br> Hover: Transparent (Text=White 100%),<br> Clicked: Transparent (Text=White 80%)
     * <h2>NOTICE this returns the text colors as it is always transparent.</h2>
     */
    public static final int TERTIARY = 3;
    /**
     * <h1>Primary Destructive Color Scheme</h1> Normal: Error 700,<br> Hover: Error 600,<br> Clicked: Error 600 (80%)
     */
    public static final int PRIMARY_DESTRUCTIVE = -1;
    /**
     * <h1>Secondary Destructive Color Scheme</h1> Normal: Gray 500,<br> Hover: Error 800,<br> Clicked: Error 800 (80%)
     */
    public static final int SECONDARY_DESTRUCTIVE = -2;
    /**
     * <h1>Tertiary Destructive Color Scheme</h1> Normal: Transparent (Text=White 90%),<br> Hover: Transparent (Text=Error 300),<br> Clicked: Transparent (Text=Error 300 80%)
     * <h2>NOTICE this returns the text colors as it is always transparent.</h2>
     */
    public static final int TERTIARY_DESTRUCTIVE = -3;


    public static int getColor(int currentColor, int colorPalette, boolean hover, boolean click) {
        float[] color = splitColor(currentColor);
        if (colorPalette == TRANSPARENT) {
            return OneConfigConfig.TRANSPARENT;
        }
        if (click) {
            switch (colorPalette) {
                case PRIMARY_DESTRUCTIVE:
                    return OneConfigConfig.ERROR_600_80;
                case SECONDARY_DESTRUCTIVE:
                    return OneConfigConfig.ERROR_800_80;
                case TERTIARY_DESTRUCTIVE:
                    return OneConfigConfig.ERROR_300_80;
                case TERTIARY:
                    return OneConfigConfig.WHITE_80;
                default:
                case SECONDARY:
                    return OneConfigConfig.GRAY_400_80;
                case SECONDARY_TRANSPARENT:
                    return new Color(0.9f, 0.9f, 0.9f, 0.2f).getRGB();
                case PRIMARY:
                    return OneConfigConfig.PRIMARY_700_80;
            }
        }

        switch (colorPalette) {
            case SECONDARY_TRANSPARENT:         // Formally -2
                return getColorComponents(color, new float[]{0f, 0f, 0f, 0f}, new float[]{0.9f, 0.9f, 0.9f, 0.3f}, hover, 50f);
            case PRIMARY:       // Formally 1
                return getColorComponents(color, splitColor(OneConfigConfig.PRIMARY_700), splitColor(OneConfigConfig.PRIMARY_600), hover, 100f);
            default:
            case SECONDARY:     // Formally 0
                return getColorComponents(color, splitColor(OneConfigConfig.GRAY_500), splitColor(OneConfigConfig.GRAY_400), hover, 100f);
            case TERTIARY:
                return getColorComponents(color, splitColor(OneConfigConfig.WHITE_90), splitColor(OneConfigConfig.WHITE), hover, 150f);
            case PRIMARY_DESTRUCTIVE:
                return getColorComponents(color, splitColor(OneConfigConfig.ERROR_700), splitColor(OneConfigConfig.ERROR_600), hover, 100f);
            case SECONDARY_DESTRUCTIVE:
                return getColorComponents(color, splitColor(OneConfigConfig.GRAY_500), splitColor(OneConfigConfig.ERROR_800), hover, 100f);
            case TERTIARY_DESTRUCTIVE:
                return getColorComponents(color, splitColor(OneConfigConfig.WHITE_90), splitColor(OneConfigConfig.ERROR_300), hover, 100f);
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
                ((int) (currentColor[1] * 255) << 8) |
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
     *
     * @param color the color.
     * @return the red component.
     */
    public static int getRed(int color) {
        return (color >> 16) & 0xFF;
    }

    /**
     * Get the green component of an RGB color.
     *
     * @param color the color.
     * @return the green component.
     */
    public static int getGreen(int color) {
        return (color >> 8) & 0xFF;
    }

    /**
     * Get the blue component of an RGB color.
     *
     * @param color the color.
     * @return the blue component.
     */
    public static int getBlue(int color) {
        return color & 0xFF;
    }

    /**
     * Get the alpha component of an ARGB color.
     *
     * @param color the color.
     * @return the alpha component.
     */
    public static int getAlpha(int color) {
        return (color >> 24) & 0xFF;
    }

    /**
     * Get the RGB color from the given color components.
     *
     * @param red   the red component.
     * @param green the green component.
     * @param blue  the blue component.
     * @param alpha the alpha component.
     * @return the RGB color.
     */
    public static int getColor(float red, float green, float blue, float alpha) {
        return getColor((int) red * 255, (int) green * 255, (int) blue * 255, (int) alpha * 255);
    }

    /**
     * Get the RGB color from the given color components.
     *
     * @param red   the red component.
     * @param green the green component.
     * @param blue  the blue component.
     * @return the RGB color.
     */
    public static int getColor(int red, int green, int blue) {
        return getColor(red, green, blue, 255);
    }

    /**
     * Get the RGB color from the given color components.
     *
     * @param red   the red component.
     * @param green the green component.
     * @param blue  the blue component.
     * @param alpha the alpha component.
     * @return the RGB color.
     */
    public static int getColor(int red, int green, int blue, int alpha) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Return the color with the given red component.
     *
     * @param color the color.
     * @param red   the red component.
     * @return the color with the given red component.
     */
    public static int setRed(int color, int red) {
        return (color & 0x00FFFF) | (red << 16);
    }

    /**
     * Return the color with the given green component.
     *
     * @param color the color.
     * @param green the green component.
     * @return the color with the given green component.
     */
    public static int setGreen(int color, int green) {
        return (color & 0xFF00FF) | (green << 8);
    }

    /**
     * Return the color with the given blue component.
     *
     * @param color the color.
     * @param blue  the blue component.
     * @return the color with the given blue component.
     */
    public static int setBlue(int color, int blue) {
        return (color & 0xFFFF00) | blue;
    }

    /**
     * Return the color with the given alpha component.
     *
     * @param color the color.
     * @param alpha the alpha component.
     * @return the color with the given alpha component.
     */
    public static int setAlpha(int color, int alpha) {
        return (color & 0xFFFFFF) | (alpha << 24);
    }
}
