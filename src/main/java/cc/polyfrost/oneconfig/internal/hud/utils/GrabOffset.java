package cc.polyfrost.oneconfig.internal.hud.utils;

public class GrabOffset {
    private float offsetX;
    private float offsetY;

    public GrabOffset(float offsetX, float offsetY) {
        setOffset(offsetX, offsetY);
    }

    public GrabOffset() {
        this(0, 0);
    }

    public void setOffset(float offsetX, float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public float getX() {
        return offsetX;
    }

    public float getY() {
        return offsetY;
    }
}
