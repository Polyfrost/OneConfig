package io.polyfrost.oneconfig.hud.interfaces;

import io.polyfrost.oneconfig.lwjgl.RenderManager;

import java.awt.*;

public abstract class BasicHud {
    public double xUnscaled = 0;
    public double yUnscaled = 0;
    public float scale = 1;
    public int paddingX = 5;
    public int paddingY = 5;
    public boolean background = true;
    public boolean rounded = false;
    public BasicHud parent;
    public BasicHud childRight;
    public BasicHud childBottom;

    public abstract int getWidth(float scale);

    public abstract int getHeight(float scale);

    public abstract void draw(int x, int y, float scale);

    public int getExampleWidth(float scale) {
        return getWidth(scale);
    }

    public int getExampleHeight(float scale) {
        return getHeight(scale);
    }

    public void drawAll(float x, float y, float scale, boolean background) {
        if (background) drawBackground(x, y, getTotalWidth(scale), getTotalHeight(scale), scale);
        draw((int) (x + paddingX * scale / 2f), (int) (y + paddingY * scale / 2f), scale);
        if (childRight != null)
            childRight.drawAll((int) x + paddingX * scale / 2f + getWidth(scale), (int) y, childRight.scale, false);
        if (childBottom != null)
            childBottom.drawAll((int) x, (int) y + paddingY * scale / 2f + getHeight(scale), childBottom.scale, false);
    }

    public void drawExampleAll(float x, float y, float scale, boolean background) {
        if (background) drawBackground(x, y, getTotalExampleWidth(scale), getTotalExampleHeight(scale), scale);
        drawExample((int) (x + paddingX * scale / 2f), (int) (y + paddingY * scale / 2f), scale);
        if (childRight != null)
            childRight.drawExampleAll((int) x + paddingX * scale / 2f + getWidth(scale), (int) y, childRight.scale, false);
        if (childBottom != null)
            childBottom.drawExampleAll((int) x, (int) y + paddingY * scale / 2f + getHeight(scale), childBottom.scale, false);
    }

    public void drawExample(int x, int y, float scale) {
        draw(x, y, scale);
    }

    private void drawBackground(float x, float y, float width, float height, float scale) {
        RenderManager.setupAndDraw(true, (vg) -> RenderManager.drawRoundedRect(vg, x, y, (width + paddingX * scale),
                (height + paddingY * scale), new Color(0, 0, 0, 120).getRGB(), 2 * scale));
    }

    public float getXScaled(int screenWidth) {
        if (xUnscaled <= 0.5) {
            return (int) (screenWidth * xUnscaled);
        }
        return (float) (screenWidth - (1d - xUnscaled) * screenWidth - (getWidth(scale) + paddingX * scale));
    }

    public float getYScaled(int screenHeight) {
        if (yUnscaled <= 0.5) {
            return (int) (screenHeight * yUnscaled);
        }
        return (float) (screenHeight - (1d - yUnscaled) * screenHeight - (getHeight(scale) + paddingY * scale));
    }

    public float getTotalWidth(float scale) {
        float width = getWidth(scale);
        if (childRight != null) width += childRight.getTotalWidth(childRight.scale) + paddingY * scale / 2f;
        if (childBottom != null) width = Math.max(childBottom.getTotalWidth(childBottom.scale), width);
        return width;
    }

    public float getTotalHeight(float scale) {
        float height = getHeight(scale);
        if (childBottom != null) height += childBottom.getTotalHeight(childBottom.scale) + paddingY * scale / 2f;
        if (childRight != null) height = Math.max(childRight.getTotalHeight(childRight.scale), height);
        return height;
    }

    public float getTotalExampleWidth(float scale) {
        float width = getExampleWidth(scale);
        if (childRight != null) width += childRight.getTotalExampleWidth(childRight.scale) + paddingX * scale / 2f;
        if (childBottom != null) width = Math.max(childBottom.getTotalExampleWidth(childBottom.scale), width);
        return width;
    }

    public float getTotalExampleHeight(float scale) {
        float height = getExampleHeight(scale);
        if (childBottom != null) height += childBottom.getTotalExampleHeight(childBottom.scale) + paddingY * scale / 2f;
        if (childRight != null) height = Math.max(childRight.getTotalExampleHeight(childRight.scale), height);
        return height;
    }
}