package cc.polyfrost.oneconfig.gui.animations;

import cc.polyfrost.oneconfig.utils.color.ColorPalette;

public class ColorAnimation {
    private ColorPalette palette;
    /**
     * 0 = nothing
     * 1 = hovered
     * 2 = pressed
     * 3 = color palette changed
     */
    private int prevState = 0;
    private Animation redAnimation;
    private Animation greenAnimation;
    private Animation blueAnimation;
    private Animation alphaAnimation;

    public ColorAnimation(ColorPalette palette) {
        this.palette = palette;
        redAnimation = new DummyAnimation(palette.getNormalColorf()[0]);
        greenAnimation = new DummyAnimation(palette.getNormalColorf()[1]);
        blueAnimation = new DummyAnimation(palette.getNormalColorf()[2]);
        alphaAnimation = new DummyAnimation(palette.getNormalColorf()[3]);
    }

    public int getColor(boolean hovered, boolean pressed) {
        int state = pressed ? 2 : hovered ? 1 : 0;
        if (state != prevState) {
            float[] newColors = pressed ? palette.getPressedColorf() : hovered ? palette.getHoveredColorf() : palette.getNormalColorf();
            redAnimation = new EaseInOutQuad(100, redAnimation.get(), newColors[0], false);
            greenAnimation = new EaseInOutQuad(100, greenAnimation.get(), newColors[1], false);
            blueAnimation = new EaseInOutQuad(100, blueAnimation.get(), newColors[2], false);
            alphaAnimation = new EaseInOutQuad(100, alphaAnimation.get(), newColors[3], false);
            prevState = state;
            return ((int) (alphaAnimation.get(0) * 255) << 24) | ((int) (redAnimation.get(0) * 255) << 16) | ((int) (greenAnimation.get(0) * 255) << 8) | ((int) (blueAnimation.get(0) * 255));
        }
        return ((int) (alphaAnimation.get() * 255) << 24) | ((int) (redAnimation.get() * 255) << 16) | ((int) (greenAnimation.get() * 255) << 8) | ((int) (blueAnimation.get() * 255));
    }

    public ColorPalette getPalette() {
        return palette;
    }

    public void setPalette(ColorPalette palette) {
        this.palette = palette;
        prevState = 3;
    }

    public void setColors(float[] colors) {
        redAnimation = new DummyAnimation(colors[0]);
        greenAnimation = new DummyAnimation(colors[1]);
        blueAnimation = new DummyAnimation(colors[2]);
        alphaAnimation = new DummyAnimation(colors[3]);
    }
}
