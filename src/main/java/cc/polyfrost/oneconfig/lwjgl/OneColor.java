package cc.polyfrost.oneconfig.lwjgl;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Arrays;

/**
 * OneColor is a class for storing Colors in HSBA format. This format is used to allow the color selectors to work correctly.
 * <p>
 * <code>
 * byte[0] = hue (0-360)
 * byte[1] = saturation (0-100)
 * byte[2] = brightness (0-100)
 * byte[3] = alpha (0-100)
 * </code>
 */
@SuppressWarnings("unused")
public class OneColor {
    transient private int rgba;
    private final byte[] hsba;
    private int chroma = -1;

    // rgb constructors

    /**
     * Create a new OneColor, converting the RGBA color to HSBA.
     */
    public OneColor(int rgba) {
        this.rgba = rgba;
        this.hsba = RGBAtoHSBA(this.rgba);
    }

    /**
     * Create a new OneColor from the given RGBA values.
     */
    public OneColor(int r, int g, int b, int a) {
        this.rgba = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF));
        this.hsba = RGBAtoHSBA(this.rgba);
    }

    /**
     * Create a new OneColor, converting the RGB color to HSBA.
     */
    public OneColor(int r, int g, int b) {
        this(r, g, b, 255);
    }

    /**
     * Convert the java.awt.Color to an OneColor (HSBA format).
     */
    public OneColor(@NotNull Color c) {
        this(c.getRGB());
    }

    // hsb constructors

    /**
     * Create a new OneColor from the given HSBA values.
     */
    public OneColor(float hue, float saturation, float brightness, float alpha) {
        this.hsba = new byte[]{(byte) hue, (byte) saturation, (byte) brightness, (byte) alpha};
        this.rgba = HSBAtoRGBA(this.hsba[0], this.hsba[1], this.hsba[2], this.hsba[3]);

    }

    /**
     * Create a new OneColor from the given HSB values. (alpha is set to max)
     */
    public OneColor(float hue, float saturation, float brightness) {
        this(hue, saturation, brightness, 1.0f);
    }

    // chroma constructors
    public OneColor(int saturation, int brightness, int alpha, float chromaSpeed) {
        this(System.currentTimeMillis() % (int) chromaSpeed / chromaSpeed, saturation, brightness, alpha);
        this.chroma = (int) chromaSpeed;
    }

    // internal constructor
    public OneColor(int hue, int saturation, int brightness, int alpha, int chromaSpeed) {
        if (chromaSpeed == -1) {
            this.hsba = new byte[]{(byte) hue, (byte) saturation, (byte) brightness, (byte) alpha};
            this.rgba = HSBAtoRGBA(this.hsba[0], this.hsba[1], this.hsba[2], this.hsba[3]);
        } else {
            this.chroma = chromaSpeed;
            this.hsba = new byte[]{(byte) hue, (byte) saturation, (byte) brightness, (byte) alpha};
        }
    }


    // accessors
    public int getRed() {
        return rgba >> 16 & 255;
    }

    public int getGreen() {
        return rgba >> 8 & 255;
    }

    public int getBlue() {
        return rgba & 255;
    }

    public float getHue() {
        return hsba[0];
    }

    public float getSaturation() {
        return hsba[1];
    }

    public float getBrightness() {
        return hsba[2];
    }

    public int getAlpha() {
        return hsba[3];
    }

    /**
     * Return the current color in RGBA format. This is the format used by LWJGL and Minecraft.
     * This method WILL return the color as a chroma, at the specified speed, if it is set.
     * Otherwise, it will just return the current color.
     *
     * @return the current color in RGBA format (equivalent to getRGB of java.awt.Color)
     */
    public int getRGB() {
        if (chroma == -1) {
            return rgba;
        } else {
            return HSBAtoRGBA(System.currentTimeMillis() % chroma / (float) chroma, hsba[1], hsba[2], hsba[3]);
        }
    }

    /**
     * Get the RGBA color from the HSB color, and apply the alpha.
     */
    public int HSBAtoRGBA(float hue, float saturation, float brightness, float alpha) {
        int temp = Color.HSBtoRGB(hue / 360f, saturation / 100f, brightness / 100f);
        return temp | (int) (alpha * 255) << 24; // trusting copilot on this
    }

    /**
     * Get the HSBA color from the RGBA color.
     */
    public byte[] RGBAtoHSBA(int rgba) {
        byte[] hsb = new byte[4];
        float[] hsbArray = Color.RGBtoHSB((rgba >> 16 & 255), (rgba >> 8 & 255), (rgba & 255), null);
        hsb[0] = (byte) (hsbArray[0] * 360);
        hsb[1] = (byte) (hsbArray[1] * 100);
        hsb[2] = (byte) (hsbArray[2] * 100);
        hsb[3] = (byte) (rgba >> 24 & 255);
        return hsb;
    }
}
