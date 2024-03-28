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

package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public class Position {
    public AnchorPosition anchor;
    private float x;
    private float y;
    @Exclude
    private float width;
    @Exclude
    private float height;
    @Exclude
    @Nullable
    private Hud hud;

    /**
     * Position object used for huds
     *
     * @param x            The X coordinate
     * @param y            The Y coordinate
     * @param width        The width of the HUD
     * @param height       The height of the HUD
     * @param screenWidth  The width of the screen to initialize the position width
     * @param screenHeight The height of the screen to initialize the position width
     */
    public Position(Hud hud, float x, float y, float width, float height, float screenWidth, float screenHeight) {
        this.hud = hud;
        setSize(width, height);
        setPosition(x, y, screenWidth, screenHeight);
    }

    /**
     * Position object used for huds
     *
     * @param x      The X coordinate
     * @param y      The Y coordinate
     * @param width  The width of the HUD
     * @param height The height of the HUD
     */
    public Position(Hud hud, float x, float y, float width, float height) {
        this(hud, x, y, width, height, 1920, 1080);
    }

    /**
     * Set the position
     *
     * @param x            The X coordinate
     * @param y            The Y coordinate
     * @param screenWidth  The screen width
     * @param screenHeight The screen height
     */
    public void setPosition(float x, float y, float screenWidth, float screenHeight) {
        float rightX = x + width;
        float bottomY = y + height;

        if (x <= screenWidth / 3f && y <= screenHeight / 3f)
            this.anchor = AnchorPosition.TOP_LEFT;
        else if (rightX >= screenWidth / 3f * 2f && y <= screenHeight / 3f)
            this.anchor = AnchorPosition.TOP_RIGHT;
        else if (x <= screenWidth / 3f && bottomY >= screenHeight / 3f * 2f)
            this.anchor = AnchorPosition.BOTTOM_LEFT;
        else if (rightX >= screenWidth / 3f * 2f && bottomY >= screenHeight / 3f * 2f)
            this.anchor = AnchorPosition.BOTTOM_RIGHT;
        else if (y <= screenHeight / 3f)
            this.anchor = AnchorPosition.TOP_CENTER;
        else if (x <= screenWidth / 3f)
            this.anchor = AnchorPosition.MIDDLE_LEFT;
        else if (rightX >= screenWidth / 3f * 2f)
            this.anchor = AnchorPosition.MIDDLE_RIGHT;
        else if (bottomY >= screenHeight / 3f * 2f)
            this.anchor = AnchorPosition.BOTTOM_CENTER;
        else
            this.anchor = AnchorPosition.MIDDLE_CENTER;

        if (hud != null) {
            switch (hud.positionAlignment) {
                case 1:
                    switch (anchor) {
                        case TOP_LEFT:
                        case TOP_RIGHT:
                        case TOP_CENTER:
                            anchor = AnchorPosition.TOP_LEFT;
                            break;
                        case MIDDLE_LEFT:
                        case MIDDLE_RIGHT:
                        case MIDDLE_CENTER:
                            anchor = AnchorPosition.MIDDLE_LEFT;
                            break;
                        case BOTTOM_LEFT:
                        case BOTTOM_RIGHT:
                        case BOTTOM_CENTER:
                            anchor = AnchorPosition.BOTTOM_LEFT;
                    }
                    break;
                case 2:
                    switch (anchor) {
                        case TOP_LEFT:
                        case TOP_RIGHT:
                        case TOP_CENTER:
                            anchor = AnchorPosition.TOP_CENTER;
                            break;
                        case MIDDLE_LEFT:
                        case MIDDLE_RIGHT:
                        case MIDDLE_CENTER:
                            anchor = AnchorPosition.MIDDLE_CENTER;
                            break;
                        case BOTTOM_LEFT:
                        case BOTTOM_RIGHT:
                        case BOTTOM_CENTER:
                            anchor = AnchorPosition.BOTTOM_CENTER;
                    }
                    break;
                case 3:
                    switch (anchor) {
                        case TOP_LEFT:
                        case TOP_RIGHT:
                        case TOP_CENTER:
                            anchor = AnchorPosition.TOP_RIGHT;
                            break;
                        case MIDDLE_LEFT:
                        case MIDDLE_RIGHT:
                        case MIDDLE_CENTER:
                            anchor = AnchorPosition.MIDDLE_RIGHT;
                            break;
                        case BOTTOM_LEFT:
                        case BOTTOM_RIGHT:
                        case BOTTOM_CENTER:
                            anchor = AnchorPosition.BOTTOM_RIGHT;
                    }
                    break;
            }
        }

        this.x = x - getAnchorX(screenWidth) + getAnchorX(width);
        this.y = y - getAnchorY(screenHeight) + getAnchorY(height);
    }

    /**
     * Set the position
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     */
    public void setPosition(float x, float y) {
        setPosition(x, y, UResolution.getScaledWidth(), UResolution.getScaledHeight());
    }

    /**
     * Set the size of the position
     *
     * @param width  The width
     * @param height The height
     */
    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public void setHud(Hud hud) {
        this.hud = hud;
    }

    /**
     * Update the position so the top left corner stays in the same spot
     *
     * @param width  The width
     * @param height The height
     */
    public void updateSizePosition(float width, float height) {
        float x = getX();
        float y = getY();
        setSize(width, height);
        setPosition(x, y);
    }

    /**
     * Get the X coordinate scaled to the size of the screen
     *
     * @param screenWidth The width of the screen
     * @return The X coordinate
     */
    public float getX(float screenWidth) {
        return x + getAnchorX(screenWidth) - getAnchorX(width);
    }

    /**
     * Get the X coordinate scaled to the size of the screen
     *
     * @return The X coordinate
     */
    public float getX() {
        return getX(UResolution.getScaledWidth());
    }

    /**
     * Set the X coordinate scaled to the size of the screen
     *
     * @param x The X coordinate
     */
    public void setX(float x) {
        this.x = x - getAnchorX(UResolution.getScaledWidth()) + getAnchorX(width);
    }

    /**
     * Get the Y coordinate scaled to the size of the screen
     *
     * @param screenHeight The height of the screen
     * @return The Y coordinate
     */
    public float getY(float screenHeight) {
        return y + getAnchorY(screenHeight) - getAnchorY(height);
    }

    /**
     * Get the Y coordinate scaled to the size of the screen
     *
     * @return The Y coordinate
     */
    public float getY() {
        return getY(UResolution.getScaledHeight());
    }

    /**
     * Set the Y coordinate scaled to the size of the screen
     *
     * @param y The Y coordinate
     */
    public void setY(float y) {
        this.y = y - getAnchorY(UResolution.getScaledHeight()) + getAnchorY(height);
    }

    /**
     * Get the X coordinate scaled to the size of the screen of the right corner
     *
     * @param screenWidth The width of the screen
     * @return The X coordinate of the right corner
     */
    public float getRightX(float screenWidth) {
        return getX(screenWidth) + width;
    }

    /**
     * Get the X coordinate scaled to the size of the screen of the right corner
     *
     * @return The X coordinate of the right corner
     */
    public float getRightX() {
        return getRightX(UResolution.getScaledWidth());
    }

    /**
     * Get the Y coordinate scaled to the size of the screen of the bottom corner
     *
     * @param screenHeight The width of the screen
     * @return The Y coordinate of the bottom corner
     */
    public float getBottomY(float screenHeight) {
        return getY(screenHeight) + height;
    }

    /**
     * Get the Y coordinate scaled to the size of the screen of the bottom corner
     *
     * @return The Y coordinate of the bottom corner
     */
    public float getBottomY() {
        return getBottomY(UResolution.getScaledHeight());
    }

    /**
     * Get the center X coordinate
     *
     * @param screenWidth The width of the screen
     * @return The center X coordinate
     */
    public float getCenterX(float screenWidth) {
        return getX(screenWidth) + width / 2f;
    }

    /**
     * Get the center X coordinate
     *
     * @return The center X coordinate
     */
    public float getCenterX() {
        return getCenterX(UResolution.getScaledWidth());
    }

    /**
     * Get the center Y coordinate
     *
     * @param screenHeight The width of the screen
     * @return The center Y coordinate
     */
    public float getCenterY(float screenHeight) {
        return getY(screenHeight) + height / 2f;
    }

    /**
     * Get the center Y coordinate
     *
     * @return The center Y coordinate
     */
    public float getCenterY() {
        return getCenterY(UResolution.getScaledHeight());
    }

    /**
     * @return The width of the position
     */
    public float getWidth() {
        return width;
    }

    /**
     * @return The height of the position
     */
    public float getHeight() {
        return height;
    }

    private float getAnchorX(float value) {
        return value * anchor.x;
    }

    private float getAnchorY(float value) {
        return value * anchor.y;
    }

    /**
     * Position of the anchors were the position is relative too
     */
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
