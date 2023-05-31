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

package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.gui.animations.ColorAnimation;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import org.jetbrains.annotations.NotNull;

public class BasicElement {
    protected int width, height;
    /**
     * The color palette used for this element.
     */
    protected ColorPalette colorPalette;
    /**
     * hitBoxX and hitBoxY are integer variables to determine (in pixels) how far past the boundaries of this button it is still able to be interacted with.
     */
    protected int hitBoxX, hitBoxY;
    protected boolean hoverFx;
    /**
     * Whether the element is currently being hovered over
     */
    protected boolean hovered = false;
    /**
     * Whether the mouse is actively being held down on the element.
     */
    protected boolean pressed = false;
    /**
     * Whether the element is clicked.
     */
    protected boolean clicked = false;
    /**
     * The toggle state of the button. Its false, then if it is clicked, it becomes true, and if clicked again, it becomes false.
     */
    protected boolean toggled = false;
    /**
     * Whether the element is currently disabled.
     */
    protected boolean disabled = false;
    /**
     * The ARGB color of this element.
     */
    public int currentColor;
    protected final float radius;
    /**
     * Boolean to determine if this element is allowed to be clicked when {@link InputHandler#isBlockingInput()} is true.
     */
    private boolean block = false;
    /**
     * The color animation used by this element.
     */
    protected ColorAnimation colorAnimation;

    public BasicElement(int width, int height, @NotNull ColorPalette colorPalette, boolean hoverFx) {
        this(width, height, colorPalette, hoverFx, 12f);
    }

    public BasicElement(int width, int height, @NotNull ColorPalette colorPalette, boolean hoverFx, float radius) {
        this.height = height;
        this.width = width;
        this.colorPalette = colorPalette;
        this.hoverFx = hoverFx;
        this.radius = radius;
        this.colorAnimation = new ColorAnimation(colorPalette);
    }

    public BasicElement(int width, int height, boolean hoverFx) {
        this(width, height, ColorPalette.TRANSPARENT, hoverFx, 12f);
    }

    /**
     * Draw script for the element.
     * <br> <b>Make sure to call {@link #update(float, float, InputHandler)} to update the elements states!</b>
     *
     * @param vg NanoVG context (see {@link NanoVGHelper})
     * @param x  x position of the element
     * @param y  y position of the element
     */
    public void draw(long vg, float x, float y, InputHandler inputHandler) {
        this.update(x, y, inputHandler);
        NanoVGHelper.INSTANCE.drawRoundedRect(vg, x, y, width, height, currentColor, radius);
    }

    /**
     * Update this element's clicked, hovered, toggled, and pressed states, invoke any necessary methods, and update the color animation.
     */
    public void update(float x, float y, InputHandler inputHandler) {
        if (disabled) {
            hovered = false;
            pressed = false;
            clicked = false;
        } else {
            hovered = inputHandler.isAreaHovered(x - hitBoxX, y - hitBoxY, width + hitBoxX, height + hitBoxY);
            pressed = hovered && Platform.getMousePlatform().isButtonDown(0);
            clicked = inputHandler.isClicked(block) && hovered;

            if (clicked) {
                toggled = !toggled;
                onClick();
            }
        }

        if (hoverFx) currentColor = colorAnimation.getColor(hovered, pressed);
        else currentColor = colorAnimation.getColor(false, false);
    }

    public void ignoreBlockedTouches(boolean state) {
        block = state;
    }


    public void onClick() {

    }

    public void setCustomHitbox(int x, int y) {
        hitBoxX = x;
        hitBoxY = y;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setColorPalette(ColorPalette colorPalette) {
        if (this.colorPalette.equals(ColorPalette.TERTIARY) || this.colorPalette.equals(ColorPalette.TERTIARY_DESTRUCTIVE))
            this.colorAnimation.setColors(colorPalette.getNormalColorf());
        this.colorPalette = colorPalette;
        this.colorAnimation.setPalette(colorPalette);
    }

    public ColorPalette getColorPalette() {
        return colorPalette;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean isPressed() {
        return pressed;
    }

    public boolean isClicked() {
        return clicked;
    }

    public boolean isToggled() {
        return toggled;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void disable(boolean state) {
        disabled = state;
    }
}
