package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.libs.universal.UResolution;
import com.google.gson.annotations.SerializedName;

public class Position {
    private AnchorPosition anchorPosition;
    private float x;
    private float y;
    private float width;
    private float height;

    public Position(float x, float y, float width, float height, float screenWidth, float screenHeight) {
        this.width = width;
        this.height = height;
        setPosition(x, y, screenWidth, screenHeight);
    }

    public void setPosition(float x, float y, float screenWidth, float screenHeight) {
        float widthX = x + width;
        float heightY = y + height;

        if (x <= screenWidth / 3f && y <= screenHeight / 3f)
            this.anchorPosition = AnchorPosition.TOP_LEFT;
        else if (widthX >= screenWidth / 3f * 2f && y <= screenHeight / 3f)
            this.anchorPosition = AnchorPosition.TOP_RIGHT;
        else if (x <= screenWidth / 3f && heightY >= screenHeight / 3f * 2f)
            this.anchorPosition = AnchorPosition.BOTTOM_LEFT;
        else if (widthX >= screenWidth / 3f * 2f && heightY >= screenHeight / 3f * 2f)
            this.anchorPosition = AnchorPosition.BOTTOM_RIGHT;
        else if (y <= screenHeight / 3f)
            this.anchorPosition = AnchorPosition.TOP_CENTER;
        else if (x <= screenWidth / 3f)
            this.anchorPosition = AnchorPosition.MIDDLE_LEFT;
        else if (widthX >= screenWidth / 3f * 2f)
            this.anchorPosition = AnchorPosition.MIDDLE_RIGHT;
        else if (heightY >= screenHeight / 3f * 2f)
            this.anchorPosition = AnchorPosition.BOTTOM_CENTER;
        else
            this.anchorPosition = AnchorPosition.MIDDLE_CENTER;

        // todo: use alignment to right and bottom and stuff to prevent it clipping or moving if hud size changes
        this.x = x - getAnchorX(screenWidth);
        this.y = y - getAnchorY(screenHeight);
    }

    public void setPosition(float x, float y) {
        setPosition(x, y, UResolution.getScaledWidth(), UResolution.getScaledHeight());
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public float getX(float screenWidth) {
        return x + getAnchorX(screenWidth);
    }

    public float getX() {
        return getX(UResolution.getScaledWidth());
    }

    public float getY(float screenHeight) {
        return y + getAnchorY(screenHeight);
    }

    public float getY() {
        return getY(UResolution.getScaledHeight());
    }

    private float getAnchorX(float screenWidth) {
        switch (anchorPosition) {
            case TOP_LEFT:
            case MIDDLE_LEFT:
            case BOTTOM_LEFT:
                return 0;
            case TOP_CENTER:
            case MIDDLE_CENTER:
            case BOTTOM_CENTER:
                return screenWidth / 2f;
            case TOP_RIGHT:
            case MIDDLE_RIGHT:
            case BOTTOM_RIGHT:
                return screenWidth;
        }
        return 0;
    }

    private float getAnchorY(float screenHeight) {
        switch (anchorPosition) {
            case TOP_LEFT:
            case TOP_RIGHT:
            case TOP_CENTER:
                return 0;
            case MIDDLE_LEFT:
            case MIDDLE_CENTER:
            case MIDDLE_RIGHT:
                return screenHeight / 2f;
            case BOTTOM_LEFT:
            case BOTTOM_CENTER:
            case BOTTOM_RIGHT:
                return screenHeight;
        }
        return 0;
    }

    public enum AnchorPosition {
        @SerializedName("0")
        TOP_LEFT(0f, 0f),
        @SerializedName("1")
        TOP_CENTER(0.5f, 0f),
        @SerializedName("2")
        TOP_RIGHT(1f, 0f),
        @SerializedName("3")
        MIDDLE_LEFT(0f, 0.5f),
        @SerializedName("4")
        MIDDLE_CENTER(0.5f, 0.5f),
        @SerializedName("5")
        MIDDLE_RIGHT(1f, 0.5f),
        @SerializedName("6")
        BOTTOM_LEFT(0f, 1f),
        @SerializedName("7")
        BOTTOM_CENTER(0.5f, 1f),
        @SerializedName("8")
        BOTTOM_RIGHT(1f, 1f);

        public final float x;
        public final float y;

        AnchorPosition(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
