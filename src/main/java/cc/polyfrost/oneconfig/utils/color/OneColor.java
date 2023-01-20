/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.utils.color;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * OneColor is a class for storing colors in HSBA format. This format is used to allow the color selectors to work correctly.
 * <p>
 * <code>
 * short[0] = hue (0-360)
 * short[1] = saturation (0-100)
 * short[2] = brightness (0-100)
 * short[3] = alpha (0-255)
 * </code>
 */
@SuppressWarnings("unused")
public class OneColor {
    transient private Integer argb = null;
    private short[] hsba;
    private int dataBit = -1;

    // hex constructor

    /**
     * Create a OneColor from the given hex.
     */
    public OneColor(String hex) {
        hsba = new short[]{0, 0, 0, 0};
        if (hex.length() > 7) {
            hsba[3] = (short) Integer.parseInt(hex.substring(6, 8), 16);
        }
        setColorFromHex(hex);
    }

    // rgb constructors

    /**
     * Create a new OneColor, converting the RGBA color to HSBA.
     */
    public OneColor(int argb) {
        this.argb = argb;
        this.hsba = ColorUtils.ARGBtoHSBA(this.argb);
    }

    /**
     * Create a new OneColor from the given RGBA values.
     */
    public OneColor(int r, int g, int b, int a) {
        this.argb = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF));
        this.hsba = ColorUtils.ARGBtoHSBA(this.argb);
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
        this.hsba = new short[]{(short) hue, (short) saturation, (short) brightness, (short) alpha};
        this.argb = ColorUtils.HSBAtoARGB(this.hsba[0], this.hsba[1], this.hsba[2], this.hsba[3]);

    }

    /**
     * Create a new OneColor from the given HSB values. (alpha is set to max)
     */
    public OneColor(float hue, float saturation, float brightness) {
        this(hue, saturation, brightness, 1.0f);
    }

    // chroma constructors

    /**
     * Create a new Chroma OneColor. The speed should be a max of 30s and a min of 1s.
     */
    public OneColor(int saturation, int brightness, int alpha, float chromaSpeed) {
        this(System.currentTimeMillis() % (int) chromaSpeed / chromaSpeed, saturation, brightness, alpha);
        if (chromaSpeed < 1) chromaSpeed = 1;
        if (chromaSpeed > 30) chromaSpeed = 30;
        this.dataBit = (int) chromaSpeed;
    }

    // internal constructor
    public OneColor(int hue, int saturation, int brightness, int alpha, int chromaSpeed) {
        if (chromaSpeed == -1) {
            this.hsba = new short[]{(short) hue, (short) saturation, (short) brightness, (short) alpha};
            this.argb = ColorUtils.HSBAtoARGB(this.hsba[0], this.hsba[1], this.hsba[2], this.hsba[3]);
        } else {
            this.dataBit = chromaSpeed;
            this.hsba = new short[]{(short) hue, (short) saturation, (short) brightness, (short) alpha};
        }
    }

    /**
     * Get the red value of the color (0-255).
     */
    public int getRed() {
        return ColorUtils.getRed(getRGB());
    }

    /**
     * Get the green value of the color (0-255).
     */
    public int getGreen() {
        return ColorUtils.getGreen(getRGB());
    }

    /**
     * Get the blue value of the color (0-255).
     */
    public int getBlue() {
        return ColorUtils.getBlue(getRGB());
    }

    /**
     * Get the hue value of the color (0-360).
     */
    public int getHue() {
        return hsba[0];
    }

    /**
     * Get the saturation value of the color (0-100).
     */
    public int getSaturation() {
        return hsba[1];
    }

    /**
     * Get the brightness value of the color (0-100).
     */
    public int getBrightness() {
        return hsba[2];
    }

    /**
     * Get the alpha value of the color (0-255).
     */
    public int getAlpha() {
        return hsba[3];
    }

    public void setAlpha(int alpha) {
        this.hsba[3] = (short) alpha;
        argb = ColorUtils.HSBAtoARGB(this.hsba[0], this.hsba[1], this.hsba[2], this.hsba[3]);
    }

    /**
     * Get the chroma speed of the color (1s-30s).
     */
    public int getDataBit() {
        return dataBit == -1 ? -1 : dataBit / 1000;
    }

    /**
     * Set the current chroma speed of the color. -1 to disable.
     */
    public void setChromaSpeed(int speed) {
        if (speed == -1) {
            this.dataBit = -1;
            return;
        }
        if (speed < 1) speed = 1;
        if (speed > 30) speed = 30;
        this.dataBit = speed * 1000;
    }

    /**
     * Set the HSBA values of the color.
     */
    public void setHSBA(int hue, int saturation, int brightness, int alpha) {
        this.hsba[0] = (short) hue;
        this.hsba[1] = (short) saturation;
        this.hsba[2] = (short) brightness;
        this.hsba[3] = (short) alpha;
        this.argb = ColorUtils.HSBAtoARGB(this.hsba[0], this.hsba[1], this.hsba[2], this.hsba[3]);
    }

    public void setFromOneColor(OneColor color) {
        setHSBA(color.hsba[0], color.hsba[1], color.hsba[2], color.hsba[3]);
    }

    /**
     * Return the current color in ARGB format. This is the format used by LWJGL and Minecraft.
     * This method WILL return the color as a chroma, at the specified speed, if it is set.
     * Otherwise, it will just return the current color.
     *
     * @return the current color in RGBA format (equivalent to getRGB of java.awt.Color)
     */
    public int getRGB() {
        if (dataBit == 0) dataBit = -1;
        if (dataBit == -1) {
            // fix for when rgba is not set because of deserializing not calling constructor
            if (argb == null) argb = ColorUtils.HSBAtoARGB(this.hsba[0], this.hsba[1], this.hsba[2], this.hsba[3]);
            return argb;
        } else {
            int temp = Color.HSBtoRGB(System.currentTimeMillis() % dataBit / (float) dataBit, hsba[1] / 100f, hsba[2] / 100f);
            hsba[0] = (short) ((System.currentTimeMillis() % dataBit / (float) dataBit) * 360);
            return ((temp & 0x00ffffff) | (hsba[3] << 24));
        }
    }

    /**
     * return the current color without its alpha. Internal method.
     */
    public int getRGBNoAlpha() {
        return 0xff000000 | getRGB();
    }

    /**
     * Return the color as if it had maximum saturation and brightness. Internal method.
     */
    public int getRGBMax(boolean maxBrightness) {
        if (dataBit == 0) dataBit = -1;
        if (dataBit == -1) {
            return ColorUtils.HSBAtoARGB(this.hsba[0], 100, maxBrightness ? 100 : 0, this.hsba[3]);
        } else {
            int temp = Color.HSBtoRGB(System.currentTimeMillis() % dataBit / (float) dataBit, 1, maxBrightness ? 1 : 0);
            hsba[0] = (short) ((System.currentTimeMillis() % dataBit / (float) dataBit) * 360);
            return ((temp & 0x00ffffff) | (hsba[3] << 24));
        }
    }

    public String getHex() {
        return Integer.toHexString(0xff000000 | getRGB()).toUpperCase().substring(2);
    }

    public void setColorFromHex(String hex) {
        hex = hex.replace("#", "");
        if (hex.length() == 8) {
            hsba[3] = (short) (Integer.parseInt(hex.substring(6, 8), 16));
        }
        if (hex.length() > 6) {
            hex = hex.substring(0, 6);
        }
        if (hex.length() == 3) {
            hex = charsToString(hex.charAt(0), hex.charAt(0), hex.charAt(1), hex.charAt(1), hex.charAt(2), hex.charAt(2));
        }
        if (hex.length() == 1) {
            hex = charsToString(hex.charAt(0), hex.charAt(0), hex.charAt(0), hex.charAt(0), hex.charAt(0), hex.charAt(0));
        }
        if (hex.length() == 2 && hex.charAt(1) == hex.charAt(0)) {
            hex = charsToString(hex.charAt(0), hex.charAt(0), hex.charAt(0), hex.charAt(0), hex.charAt(0), hex.charAt(0));
        }
        StringBuilder hexBuilder = new StringBuilder(hex);
        while (hexBuilder.length() < 6) {
            hexBuilder.append("0");
        }
        hex = hexBuilder.toString();
        int r = Integer.valueOf(hex.substring(0, 2), 16);
        int g = Integer.valueOf(hex.substring(2, 4), 16);
        int b = Integer.valueOf(hex.substring(4, 6), 16);
        this.argb = ((getAlpha() & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF));
        hsba = ColorUtils.ARGBtoHSBA(argb);
    }

    private String charsToString(char... chars) {
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }

    public Color toJavaColor() {
        return new Color(getRGB(), true);
    }

    @Override
    public String toString() {
        return "OneColor{rgba=[r=" + getRed() + ", g=" + getGreen() + ", b=" + getBlue() + ", a=" + getAlpha() + "], hsba=[h=" + getHue() + ", s=" + getSaturation() + ", b=" + getBrightness() + ", a=" + getAlpha() + "], hex=" + getHex() + "}";
    }
}
