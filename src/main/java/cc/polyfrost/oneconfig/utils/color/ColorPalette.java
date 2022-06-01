package cc.polyfrost.oneconfig.utils.color;


import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.core.OneColor;

import java.awt.*;

import static cc.polyfrost.oneconfig.config.OneConfigConfig.*;

public class ColorPalette {
    /**
     * Always returns transparent.
     */
    public static final ColorPalette TRANSPARENT = new ColorPalette(OneConfigConfig.TRANSPARENT, OneConfigConfig.TRANSPARENT, OneConfigConfig.TRANSPARENT);
    /**
     * <h1>Primary Color Scheme</h1> Normal: Primary 600,<br> Hover: Primary 700,<br> Clicked: Primary 700 (80%)
     */
    public static final ColorPalette PRIMARY = new ColorPalette(PRIMARY_600, PRIMARY_700, PRIMARY_700_80);
    /**
     * <h1>Secondary Color Scheme</h1> Normal: Gray 500,<br> Hover: Gray 400,<br> Clicked: Gray 400 (80%)
     */
    public static final ColorPalette SECONDARY = new ColorPalette(OneConfigConfig.GRAY_500, OneConfigConfig.GRAY_400, OneConfigConfig.GRAY_400_80);
    /**
     * <h1>Secondary (Transparent) Color Scheme</h1> Normal: Transparent,<br> Hover: Gray rgba(229, 229, 229, 77),<br> Clicked: Gray rgba(229, 229, 229, 51)
     */
    public static final ColorPalette SECONDARY_TRANSPARENT = new ColorPalette(OneConfigConfig.TRANSPARENT, new Color(229, 229, 229, 77).getRGB(), new Color(229, 229, 229, 51).getRGB());
    /**
     * <h1>Tertiary Color Scheme</h1> Normal: Transparent (Text=White 90%),<br> Hover: Transparent (Text=White 100%),<br> Clicked: Transparent (Text=White 80%)
     * <h2>NOTICE this returns the text colors as it is always transparent.</h2>
     */
    public static final ColorPalette TERTIARY = new ColorPalette(WHITE_90, WHITE, WHITE_80);
    /**
     * <h1>Primary Destructive Color Scheme</h1> Normal: Error 700,<br> Hover: Error 600,<br> Clicked: Error 600 (80%)
     */
    public static final ColorPalette PRIMARY_DESTRUCTIVE = new ColorPalette(ERROR_700, ERROR_600, ERROR_600_80);
    /**
     * <h1>Secondary Destructive Color Scheme</h1> Normal: Gray 500,<br> Hover: Error 800,<br> Clicked: Error 800 (80%)
     */
    public static final ColorPalette SECONDARY_DESTRUCTIVE = new ColorPalette(OneConfigConfig.GRAY_500, ERROR_800, ERROR_800_80);
    /**
     * <h1>Tertiary Destructive Color Scheme</h1> Normal: Transparent (Text=White 90%),<br> Hover: Transparent (Text=Error 300),<br> Clicked: Transparent (Text=Error 300 80%)
     * <h2>NOTICE this returns the text colors as it is always transparent.</h2>
     */
    public static final ColorPalette TERTIARY_DESTRUCTIVE = new ColorPalette(WHITE_90, ERROR_300, ERROR_300_80);




    private final int colorNormal;
    private final int colorHovered;
    private final int colorPressed;
    private final float[] colorNormalf;
    private final float[] colorHoveredf;
    private final float[] colorPressedf;

    /** <h1>Create a new ColorPalette.</h1>
     * This color palette is used with animations, and the elements like BasicButton, BasicElement, and more.
     * <br> This method takes integers in ARGB format, like many other classes, such as {@link OneColor} and {@link Color}.
     * @param colorNormal the color of the element when it is not hovered or pressed.
     * @param colorHovered the color of the element when it is hovered.
     * @param colorPressed the color of the element when it is pressed.
     */
    public ColorPalette(int colorNormal, int colorHovered, int colorPressed) {
        this.colorNormal = colorNormal;
        this.colorHovered = colorHovered;
        this.colorPressed = colorPressed;
        this.colorNormalf = new float[]{ColorUtils.getRed(colorNormal) / 255f, ColorUtils.getGreen(colorNormal) / 255f, ColorUtils.getBlue(colorNormal) / 255f, ColorUtils.getAlpha(colorNormal) / 255f};
        this.colorHoveredf = new float[]{ColorUtils.getRed(colorHovered) / 255f, ColorUtils.getGreen(colorHovered) / 255f, ColorUtils.getBlue(colorHovered) / 255f, ColorUtils.getAlpha(colorHovered) / 255f};
        this.colorPressedf = new float[]{ColorUtils.getRed(colorPressed) / 255f, ColorUtils.getGreen(colorPressed) / 255f, ColorUtils.getBlue(colorPressed) / 255f, ColorUtils.getAlpha(colorPressed) / 255f};
    }
    /** <h1>Create a new ColorPalette.</h1>
     * This color palette is used with animations, and the elements like BasicButton, BasicElement, and more.
     * <br> This method takes {@link OneColor} in ARGB format.
     * @param colorNormal the color of the element when it is not hovered or pressed.
     * @param colorHovered the color of the element when it is hovered.
     * @param colorPressed the color of the element when it is pressed.
     */
    public ColorPalette(OneColor colorNormal, OneColor colorHovered, OneColor colorPressed) {
        this(colorNormal.getRGB(), colorHovered.getRGB(), colorPressed.getRGB());
    }

    /** <h1>Create a new ColorPalette.</h1>
     * This color palette is used with animations, and the elements like BasicButton, BasicElement, and more.
     * <br> This method takes {@link Color} in ARGB format.
     * @param colorNormal the color of the element when it is not hovered or pressed.
     * @param colorHovered the color of the element when it is hovered.
     * @param colorPressed the color of the element when it is pressed.
     */
    public ColorPalette(Color colorNormal, Color colorHovered, Color colorPressed) {
        this(colorNormal.getRGB(), colorHovered.getRGB(), colorPressed.getRGB());
    }

    /** <h1>Create a new ColorPalette.</h1>
     * This color palette is used with animations, and the elements like BasicButton, BasicElement, and more.
     * <br> This method takes float arrays of the color between 0f and 1f, in [R, G, B, A] format.
     * @param colorNormal the color of the element when it is not hovered or pressed.
     * @param colorHovered the color of the element when it is hovered.
     * @param colorPressed the color of the element when it is pressed.
     */
    public ColorPalette(float[] colorNormal, float[] colorHovered, float[] colorPressed) {
        this.colorNormalf = colorNormal;
        this.colorHoveredf = colorHovered;
        this.colorPressedf = colorPressed;
        this.colorNormal = ColorUtils.getColor(colorNormal[0], colorNormal[1], colorNormal[2], colorNormal[3]);
        this.colorHovered = ColorUtils.getColor(colorHovered[0], colorHovered[1], colorHovered[2], colorHovered[3]);
        this.colorPressed = ColorUtils.getColor(colorPressed[0], colorPressed[1], colorPressed[2], colorPressed[3]);
    }

    /** Return the color of the element when it is not hovered or pressed in ARGB format. */
    public int getNormalColor() {
        return colorNormal;
    }

    /** Return the color of the element when it is hovered in ARGB format. */
    public int getHoveredColor() {
        return colorHovered;
    }

    /** Return the color of the element when it is pressed in ARGB format. */
    public int getPressedColor() {
        return colorPressed;
    }

    /** Return the color of the element when it is not hovered or pressed in a float array (r,g,b,a). */
    public float[] getNormalColorf() {
        return colorNormalf;
    }

    /** Return the color of the element when it is hovered in a float array (r,g,b,a). */
    public float[] getHoveredColorf() {
        return colorHoveredf;
    }

    /** Return the color of the element when it is pressed in a float array (r,g,b,a). */
    public float[] getPressedColorf() {
        return colorPressedf;
    }

}
