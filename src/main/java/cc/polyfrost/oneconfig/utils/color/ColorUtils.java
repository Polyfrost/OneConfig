package cc.polyfrost.oneconfig.utils.color;

/**
 * A class to help with color manipulation.
 */
public final class ColorUtils {
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
