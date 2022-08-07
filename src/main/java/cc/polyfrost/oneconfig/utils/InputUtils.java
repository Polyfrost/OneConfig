/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
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

import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.scissor.Scissor;

import java.util.ArrayList;

/**
 * Various utility methods for input.
 * <p>
 * All values returned from this class are not scaled to Minecraft's GUI scale.
 * For scaled values, see {@link cc.polyfrost.oneconfig.libs.universal.UMouse}.
 * </p>
 */
public final class InputUtils {
    private static final ArrayList<Scissor> blockScissors = new ArrayList<>();

    /**
     * function to determine weather the mouse is currently over a specific region. Uses the current nvgScale to fix to any scale.
     *
     * @return true if mouse is over region, false if not.
     */
    public static boolean isAreaHovered(float x, float y, float width, float height, boolean ignoreBlock) {
        float mouseX = mouseX();
        float mouseY = mouseY();
        return (ignoreBlock || blockScissors.size() == 0 || !shouldBlock(mouseX, mouseY)) && mouseX > x && mouseY > y && mouseX < x + width && mouseY < y + height;
    }

    /**
     * function to determine weather the mouse is currently over a specific region. Uses the current nvgScale to fix to any scale.
     *
     * @return true if mouse is over region, false if not.
     */
    public static boolean isAreaHovered(float x, float y, float width, float height) {
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
     * @see InputUtils#isAreaHovered(float, float, float, float)
     */
    public static boolean isAreaClicked(float x, float y, float width, float height, boolean ignoreBlock) {
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
     * @see InputUtils#isAreaClicked(float, float, float, float, boolean)
     */
    public static boolean isAreaClicked(float x, float y, float width, float height) {
        return isAreaClicked(x, y, width, height, false);
    }

    /**
     * Checks whether the mouse is clicked or not.
     *
     * @param ignoreBlock if true, will ignore
     * @return true if the mouse is clicked, false if not
     */
    public static boolean isClicked(boolean ignoreBlock) {
        return OneConfigGui.INSTANCE != null && OneConfigGui.INSTANCE.mouseDown && !Platform.getMousePlatform().isButtonDown(0) && (ignoreBlock || blockScissors.size() == 0 || !shouldBlock(mouseX(), mouseY()));
    }

    /**
     * Checks whether the mouse is clicked or not.
     *
     * @return true if the mouse is clicked, false if not
     * @see InputUtils#isClicked(boolean)
     */
    public static boolean isClicked() {
        return isClicked(false);
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
    public static float mouseX() {
        if (OneConfigGui.INSTANCE == null) return (float) Platform.getMousePlatform().getMouseX();
        return (float) (Platform.getMousePlatform().getMouseX() / OneConfigGui.INSTANCE.getScaleFactor());
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
    public static float mouseY() {
        if (OneConfigGui.INSTANCE == null) return (float) (UResolution.getWindowHeight() - Math.abs(Platform.getMousePlatform().getMouseY()));
        return (float) ((UResolution.getWindowHeight() - Math.abs(Platform.getMousePlatform().getMouseY())) / OneConfigGui.INSTANCE.getScaleFactor());
    }

    /**
     * Block all clicks outside an area
     *
     * @param x      X coordinate
     * @param y      Y coordinate
     * @param width  Width
     * @param height Height
     */
    public static Scissor blockInputArea(float x, float y, float width, float height) {
        Scissor scissor = new Scissor(new Scissor(x, y, width, height));
        blockScissors.add(scissor);
        return scissor;
    }

    /**
     * Should be used if there is something above other components and you don't want it clicking trough
     */
    public static Scissor blockAllInput() {
        return blockInputArea(0, 0, 1920, 1080);
    }

    /**
     * Stop blocking an area from being interacted with
     *
     * @param scissor The scissor area
     */
    public static void stopBlock(Scissor scissor) {
        blockScissors.remove(scissor);
    }

    /**
     * Clears all blocking areas
     */
    public static void stopBlockingInput() {
        blockScissors.clear();
    }

    /**
     * Whether clicks are blocked
     *
     * @return true if clicks are blocked, false if not
     */
    public static boolean isBlockingInput() {
        return blockScissors.size() > 0;
    }

    private static boolean shouldBlock(float x, float y) {
        for (Scissor block : blockScissors) {
            if (block.isInScissor(x, y)) return true;
        }
        return false;
    }
}
