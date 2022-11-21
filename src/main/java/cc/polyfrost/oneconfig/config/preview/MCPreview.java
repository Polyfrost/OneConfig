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

package cc.polyfrost.oneconfig.config.preview;

import cc.polyfrost.oneconfig.internal.config.preview.MCPreviewManager;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.utils.InputHandler;

/**
 * Minecraft-specific rendering preview class.
 */
public abstract class MCPreview extends BasicPreview {

    private float x;
    private float y;
    private float width;
    private float height;
    private InputHandler inputHandler;
    private boolean render = false;

    public MCPreview() {
        MCPreviewManager.INSTANCE.previews.add(this);
    }

    @Override
    public final void setupCallDraw(long vg, float x, float y, float width, float height, InputHandler inputHandler) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.inputHandler = inputHandler;
        this.render = true;
    }

    // this actually does stuff
    @SuppressWarnings("unused")
    private void setupDraw(UMatrixStack matrixStack) {
        if (render) {
            matrixStack.push();
            float scale = (float) (1f / UResolution.getScaleFactor());
            matrixStack.scale(scale, scale, 1);
            matrixStack.translate(-(x *scale), -(y / scale), 1);
            matrixStack.push();
            matrixStack.translate(x * scale, y * scale, 1);
            matrixStack.applyToGlobalState();
            draw(matrixStack, width, height, inputHandler);
            matrixStack.pop();
            matrixStack.pop();
            matrixStack.applyToGlobalState();
            render = false;
        }
    }

    /**
     * Draws the preview.
     * <p>
     * <b>OneConfig removes Minecraft GUI scaling, thus InputHandler MUST be used to handle input.</b>
     *
     * @param matrices The matrix stack used to draw the preview. The X and Y coordinates have already been translated on this stack.
     */
    protected abstract void draw(UMatrixStack matrices, float width, float height, InputHandler inputHandler);
}