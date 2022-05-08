package cc.polyfrost.oneconfig.lwjgl;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

@SuppressWarnings("unused")
public class OneColor {
    private byte[] rgb = new byte[3];
    private byte alpha;
    private int rgba;
    private final float[] hsb;
    private int chroma = -1;


    // rgb constructors
    public OneColor(int rgba) {
        this.rgba = rgba;
        this.rgb = splitColor(rgba);
        this.alpha = (byte) (rgba >> 24 & 255);
        hsb = Color.RGBtoHSB(this.rgb[0], this.rgb[1], this.rgb[2], null);
    }

    public OneColor(int r, int g, int b, int a) {
        this.rgba = (a << 24) | (r << 16) | (g << 8) | b;       // trusting copilot on this
        this.rgb = splitColor(rgba);
        this.alpha = (byte) a;
        hsb = Color.RGBtoHSB(this.rgb[0], this.rgb[1], this.rgb[2], null);
    }

    public OneColor(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public OneColor(@NotNull Color c) {
        this(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }



    // hsb constructors
    public OneColor(float hue, float saturation, float brightness, float alpha) {
        this.hsb = new float[]{hue, saturation, brightness};
        this.alpha = (byte) (alpha * 255);
        this.rgba = getRGBAFromHSB(this.hsb[0], this.hsb[1], this.hsb[2]);

    }

    public OneColor(float hue, float saturation, float brightness) {
        this(hue, saturation, brightness, 1.0f);
    }

    // chroma constructors
    public OneColor(float saturation, float brightness, float alpha, int chromaSpeed) {
        this(System.currentTimeMillis() % chromaSpeed / (float) chromaSpeed, saturation, brightness, alpha);
        this.chroma = chromaSpeed;
    }


    // internal constructor
    public OneColor(float hue, float saturation, float brightness, float alpha, int chromaSpeed) {
        if(chromaSpeed == -1) {
            this.hsb = new float[]{hue, saturation, brightness};
            this.alpha = (byte) (alpha * 255);
            this.rgba = getRGBAFromHSB(this.hsb[0], this.hsb[1], this.hsb[2]);
        } else {
            this.chroma = chromaSpeed;
            this.hsb = new float[]{hue, saturation, brightness};
            this.alpha = (byte) (alpha * 255);
        }
    }





    // accessors
    public int getRed() {
        return rgb[0];
    }
    public int getGreen() {
        return rgb[1];
    }
    public int getBlue() {
        return rgb[2];
    }
    public int getAlpha() {
        return alpha;
    }

    public float getHue() {
        return hsb[0];
    }
    public float getSaturation() {
        return hsb[1];
    }
    public float getBrightness() {
        return hsb[2];
    }

    /**
     * Return the current color in RGBA format. This is the format used by LWJGL and Minecraft.
     * This method WILL return the color as a chroma, at the specified speed, if it is set.
     * Otherwise, it will just return the current color.
     */
    public int getRGB() {
        if(chroma == -1) {
            return rgba;
        } else {
            return getRGBAFromHSB(System.currentTimeMillis() % chroma / (float) chroma, hsb[1], hsb[2]);
        }
    }



    private byte[] splitColor(int rgb) {
        byte r = (byte) (rgb >> 16 & 255);
        byte g = (byte) (rgb >> 8 & 255);
        byte b = (byte) (rgb & 255);
        return new byte[]{r, g, b};
    }

    /** set the current chroma speed. Set to -1 to disable chroma. */
    public void setChromaSpeed(int speed) {
        this.chroma = speed;
    }

    public void setAlpha(int alpha) {
        this.alpha = (byte) alpha;
    }

    /** Get the RGBA color from the HSB color, and apply the alpha. */
    public int getRGBAFromHSB(float hue, float saturation, float brightness) {
        int temp = Color.HSBtoRGB(hue, saturation, brightness);
        this.rgba = (this.alpha << 24) | (temp >> 16 & 255) << 16 | (temp >> 8 & 255) << 8 | temp & 255;        // trusting copilot on this
        return rgba;
    }

}
