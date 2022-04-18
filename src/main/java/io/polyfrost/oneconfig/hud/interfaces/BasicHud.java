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

    public void drawAll(float x, float y, float scale) {
        drawBackground(x, y, getWidth(scale), getHeight(scale), scale);
        draw((int) (x + paddingX * scale / 2f), (int) (y + paddingY * scale / 2f), scale);
    }

    public void drawExampleAll(float x, float y, float scale) {
        drawBackground(x, y, getExampleWidth(scale), getExampleHeight(scale), scale);
        drawExample((int) (x + paddingX * scale / 2f), (int) (y + paddingY * scale / 2f), scale);
    }

    public void drawExample(int x, int y, float scale) {
        draw(x, y, scale);
    }

    private void drawBackground(float x, float y, float width, float height, float scale) {
        RenderManager.setupAndDraw((vg) -> RenderManager.drawRoundedRect(vg, x, y, width + paddingX * scale,
                height + paddingY * scale, new Color(0, 0, 0, 120).getRGB(), 2 * scale));
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
        return 0;
    }
}