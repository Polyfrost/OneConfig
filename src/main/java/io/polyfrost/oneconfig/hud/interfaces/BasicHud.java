package io.polyfrost.oneconfig.hud.interfaces;

import io.polyfrost.oneconfig.renderer.Renderer;

import java.awt.*;

public abstract class BasicHud {
    public int normalX;
    public int normalY;
    public int paddingX = 5;
    public int paddingY = 5;
    public boolean background = true;
    public boolean rounded = false;

    public abstract int getWidth(float scale);

    public abstract int getHeight(float scale);

    public abstract void draw(int x, int y, float scale);

    public int getExampleWidth(float scale) {
        return getWidth(scale);
    }

    public int getExampleHeight(float scale) {
        return getHeight(scale);
    }

    public void drawAll(int x, int y, float scale) {
        drawBackGround(x, y, scale);
        draw(x, y, scale);
    }

    public void drawExampleAll(int x, int y, float scale) {
        drawBackGround(x, y, scale);
        drawExample(x, y, scale);
    }

    public void drawExample(int x, int y, float scale) {
        draw(x, y, scale);
    }

    private void drawBackGround(int x, int y, float scale) {
        Renderer.drawRoundRect(x - paddingX * scale / 2f, y - paddingY * scale / 2f,
                getWidth(scale) + paddingX * scale, getHeight(scale) + paddingY * scale,
                (2 * scale), new Color(0, 0, 0, 100).getRGB());
    }
}
