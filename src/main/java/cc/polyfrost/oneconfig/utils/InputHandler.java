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

package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.scissor.Scissor;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;

import java.util.ArrayList;

/**
 * Various utility methods for input.
 * <p>
 * All values returned from this class are not scaled to Minecraft's GUI scale.
 * For scaled values, see {@link cc.polyfrost.oneconfig.libs.universal.UMouse}.
 * </p>
 */
public class InputHandler {
    private final ArrayList<Scissor> blockScissors = new ArrayList<>();
    private double scaleX = 1d;
    private double scaleY = 1d;

    private boolean blockDWheel = false;

    /**
     * Push a scale for the input utils to use
     *
     * @param scaleX X scale
     * @param scaleY Y scale
     */
    public void scale(double scaleX, double scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    /**
     * Reset the scale input utils uses
     */
    public void resetScale() {
        scaleX = 1d;
        scaleY = 1d;
    }

    public double getXScaleFactor() {
        return scaleX;
    }

    public double getYScaleFactor() {
        return scaleY;
    }


    /**
     * function to determine weather the mouse is currently over a specific region. Uses the current nvgScale to fix to any scale.
     *
     * @return true if mouse is over region, false if not.
     */
    public boolean isAreaHovered(float x, float y, float width, float height, boolean ignoreBlock) {
        float mouseX = mouseX();
        float mouseY = mouseY();
        return (ignoreBlock || blockScissors.size() == 0 || !shouldBlock(mouseX, mouseY)) && mouseX > x && mouseY > y && mouseX < x + width && mouseY < y + height;
    }

    /**
     * function to determine weather the mouse is currently over a specific region. Uses the current nvgScale to fix to any scale.
     *
     * @return true if mouse is over region, false if not.
     */
    public boolean isAreaHovered(float x, float y, float width, float height) {
        return isAreaHovered(x, y, width, height, false);
    }

    /**
     * Checks whether the mouse is currently over a specific region and clicked.
     *
     * @param x           the x position of the region
     * @param y           the y position of the region
     * @param width       the width of the region
     * @param height      the height of the region
     * @param ignoreBlock if true, will ignore
     * @return true if the mouse is clicked and is over the region, false if not
     * @see InputHandler#isAreaHovered(float, float, float, float)
     */
    public boolean isAreaClicked(float x, float y, float width, float height, boolean ignoreBlock) {
        return isAreaHovered(x, y, width, height, ignoreBlock) && isClicked(false);
    }

    /**
     * Checks whether the mouse is currently over a specific region and clicked.
     *
     * @param x      the x position of the region
     * @param y      the y position of the region
     * @param width  the width of the region
     * @param height the height of the region
     * @return true if the mouse is clicked and is over the region, false if not
     * @see InputHandler#isAreaClicked(float, float, float, float, boolean)
     */
    public boolean isAreaClicked(float x, float y, float width, float height) {
        return isAreaClicked(x, y, width, height, false);
    }

    /**
     * Checks whether the mouse is clicked or not.
     *
     * @param ignoreBlock if true, will ignore
     * @return true if the mouse is clicked, false if not
     */
    public boolean isClicked(boolean ignoreBlock) {
        return GuiUtils.wasMouseDown() && !Platform.getMousePlatform().isButtonDown(0) && (ignoreBlock || blockScissors.size() == 0 || !shouldBlock(mouseX(), mouseY()));
    }

    /**
     * Checks whether the mouse is clicked or not.
     *
     * @return true if the mouse is clicked, false if not
     * @see InputHandler#isClicked(boolean)
     */
    public boolean isClicked() {
        return isClicked(false);
    }

    /**
     * @param button The button
     * @return If the button is down
     */
    public boolean isMouseDown(int button) {
        return Platform.getMousePlatform().isButtonDown(button);
    }

    /**
     * @return If the left mouse button is down
     */
    public boolean isMouseDown() {
        return isMouseDown(0);
    }

    /**
     * Gets the current mouse X position.
     * <p>
     * All values returned from this class are not scaled to Minecraft's GUI scale.
     * For scaled values, see {@link cc.polyfrost.oneconfig.libs.universal.UMouse}.
     * </p>
     *
     * @return the current mouse X position
     */
    public float mouseX() {
        return (float) (Platform.getMousePlatform().getMouseX() / scaleX);
    }

    /**
     * Gets the current mouse Y position.
     * <p>
     * All values returned from this class are not scaled to Minecraft's GUI scale.
     * For scaled values, see {@link cc.polyfrost.oneconfig.libs.universal.UMouse}.
     * </p>
     *
     * @return the current mouse Y position
     */
    public float mouseY() {
        return (float) (Platform.getMousePlatform().getMouseY() / scaleY);
    }

    /**
     * Block all clicks outside an area
     *
     * @param x      X coordinate
     * @param y      Y coordinate
     * @param width  Width
     * @param height Height
     */
    public Scissor blockInputArea(float x, float y, float width, float height) {
        Scissor scissor = new Scissor(new Scissor(x, y, width, height));
        blockScissors.add(scissor);
        return scissor;
    }

    /**
     * THIS SHOULD ONLY BE USED WITH SCISSORS FROM {@link #blockInputArea(float, float, float, float)}
     * @param inputScissor The scissor area
     */
    public void blockInputArea(Scissor inputScissor) {
        blockScissors.add(inputScissor);
    }

    /**
     * Should be used if there is something above other components and you don't want it clicking trough
     */
    public Scissor blockAllInput() {
        return blockInputArea(0, 0, 1920, 1080);
    }

    /**
     * Stop blocking an area from being interacted with
     *
     * @param scissor The scissor area
     */
    public void stopBlock(Scissor scissor) {
        blockScissors.remove(scissor);
    }

    /**
     * Clears all blocking areas
     */
    public void stopBlockingInput() {
        blockScissors.clear();
    }

    /**
     * Whether clicks are blocked
     *
     * @return true if clicks are blocked, false if not
     */
    public boolean isBlockingInput() {
        return !blockScissors.isEmpty();
    }

    public double getDWheel(boolean ignoreBlock) {
        return ignoreBlock ? Platform.getMousePlatform().getDWheel() : (blockDWheel ? 0.0 : Platform.getMousePlatform().getDWheel());
    }

    public double getDWheel() {
        return getDWheel(false);
    }

    public void blockDWheel() {
        blockDWheel = true;
    }

    public void unblockDWheel() {
        blockDWheel = false;
    }

    private boolean shouldBlock(float x, float y) {
        for (Scissor block : blockScissors) {
            if (block.isInScissor(x, y)) return true;
        }
        return false;
    }
}
