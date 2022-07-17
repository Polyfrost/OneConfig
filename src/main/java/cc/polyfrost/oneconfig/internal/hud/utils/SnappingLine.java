package cc.polyfrost.oneconfig.internal.hud.utils;

import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.renderer.RenderManager;

import java.awt.*;

public class SnappingLine {
    private static final int COLOR =  new Color(138, 43, 226).getRGB();
    private final float line;
    private final float distance;
    private final float position;

    public SnappingLine(float line, float left, float size) {
        this.line = line;
        float center = left + size / 2f;
        float right = left + size;
        float leftDistance = Math.abs(line - left);
        float centerDistance = Math.abs(line - center);
        float rightDistance = Math.abs(line - right);
        if (leftDistance <= centerDistance && leftDistance <= rightDistance) {
            distance = leftDistance;
            position = line;
        } else if (centerDistance <= rightDistance && centerDistance <= leftDistance) {
            distance = rightDistance;
            position = line - size / 2f;
        } else {
            distance = rightDistance;
            position = line - size;
        }
    }

    public void drawLine(long vg, float lineWidth, boolean isX) {
        float pos = (float) (line * UResolution.getScaleFactor() - lineWidth / 2f);
        if (isX) {
            RenderManager.drawLine(vg, pos, 0, pos, UResolution.getWindowHeight(), lineWidth, COLOR);
        } else {
            RenderManager.drawLine(vg, 0, pos, UResolution.getWindowWidth(), pos, lineWidth, COLOR);
        }
    }

    public float getPosition() {
        return position;
    }

    public float getDistance() {
        return distance;
    }
}
