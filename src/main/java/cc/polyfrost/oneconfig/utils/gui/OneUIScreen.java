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

package cc.polyfrost.oneconfig.utils.gui;

import cc.polyfrost.oneconfig.gui.GuiPause;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.libs.universal.UScreen;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.utils.InputHandler;
import org.jetbrains.annotations.NotNull;

/**
 * <h1>OneUIScreen</h1>
 * OneUIScreen is a GUI that can be used to render things on the client's screen.
 * It contains many handy methods for rendering, including {@link #draw(long, float, InputHandler)} for drawing using OneConfig's {@link NanoVGHelper}.
 * <p> It also contains methods for mouse input. (see {@link InputHandler} for more utils).
 * <p></p>
 * Use GuiUtils to display a screen; and GuiUtils.closeScreen to close it.
 */
public abstract class OneUIScreen extends UScreen implements GuiPause, BlurScreen {
    private final boolean useMinecraftScale;
    private final InputHandler inputHandler = new InputHandler();

    /**
     * Create a new OneUIScreen.
     *
     * @param useMinecraftScale wether to use Minecraft scale
     * @param restoreGuiOnClose use this to declare weather or not to open the Gui that was open before it when this screen is closed.
     */
    public OneUIScreen(boolean useMinecraftScale, boolean restoreGuiOnClose) {
        super(restoreGuiOnClose);
        this.useMinecraftScale = useMinecraftScale;
    }

    /**
     * Create a new OneUIScreen.
     *
     * @param useMinecraftScale wether to use Minecraft scale
     */
    public OneUIScreen(boolean useMinecraftScale) {
        this(useMinecraftScale, false);
    }

    /**
     * Create a new OneUIScreen.
     */
    public OneUIScreen() {
        this(false, false);
    }

    @Override
    public final void onDrawScreen(@NotNull UMatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.onDrawScreen(matrixStack, mouseX, mouseY, partialTicks);
        if (useMinecraftScale) inputHandler.scale(UResolution.getScaleFactor(), UResolution.getScaleFactor());
        NanoVGHelper.INSTANCE.setupAndDraw(useMinecraftScale, vg -> draw(vg, partialTicks, inputHandler));
    }

    /**
     * Use this method to draw things on the screen. It is called every render tick, and has a handy <code>vg</code> (NanoVG context) that can be used with the {@link NanoVGHelper} to draw things.
     * <p></p>
     * For example: <d> <code>{@link NanoVGHelper#drawRoundedRect(long, float, float, float, float, int, float)} </code>
     *
     * @param vg           The NanoVG context you can use to render things with
     * @param partialTicks The time between ticks (You can use this as a deltaTime equivalent)
     * @param inputHandler The input handler
     */
    public abstract void draw(long vg, float partialTicks, InputHandler inputHandler);

    /**
     * @return If this gui has background blur
     */
    @Override
    public boolean hasBackgroundBlur() {
        return false;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
