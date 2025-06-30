/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.ui.v1.internal.wrappers;

import dev.deftu.omnicore.client.OmniKeyboard;
import dev.deftu.omnicore.client.OmniScreen;
import dev.deftu.omnicore.client.render.OmniMatrixStack;
import dev.deftu.omnicore.client.render.OmniResolution;
import dev.deftu.omnicore.client.render.framebuffer.Framebuffer;
import dev.deftu.omnicore.client.render.framebuffer.ManagedFramebuffer;
import dev.deftu.omnicore.client.render.state.DepthFunction;
import dev.deftu.omnicore.client.render.state.OmniManagedDepthState;
import dev.deftu.omnicore.client.render.texture.GpuTexture;
import kotlin.Unit;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.ui.v1.Notifications;
import org.polyfrost.oneconfig.api.ui.v1.UIManager;
import org.polyfrost.oneconfig.api.ui.v1.internal.RendererImpl;
import org.polyfrost.oneconfig.api.ui.v1.screen.BlurScreen;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.component.Drawable;
import org.polyfrost.polyui.data.Cursor;

import java.awt.*;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.glViewport;
import static org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager.translateKey;

@SuppressWarnings("unused")
public class PolyUIScreen extends OmniScreen implements BlurScreen {

    private static final Logger LOGGER = LogManager.getLogger("OneConfig/PolyUIScreen");

    @NotNull
    public final PolyUI polyUI;

    private Framebuffer framebuffer;

    private final float designedWidth, designedHeight, initialWidth, initialHeight;
    private final boolean pauses, blurs;
    private final Consumer<PolyUI> close;

    //#if MC < 1.13
    private int mx, my;
    //#endif

    public PolyUIScreen(@NotNull PolyUI polyUI, float designedWidth, float designedHeight, boolean pauses, boolean blurs, Consumer<PolyUI> onClose) {
        super(true);

        this.polyUI = polyUI;
        this.designedWidth = designedWidth;
        this.designedHeight = designedHeight;
        this.initialWidth = polyUI.getMaster().getWidth();
        this.initialHeight = polyUI.getMaster().getHeight();
        this.blurs = blurs;
        this.pauses = pauses;
        this.close = onClose;
        //#if MC <= 1.12.2
        // todo temp fix
        this.mc = Minecraft.getMinecraft();
        //#endif
    }

    @Override
    public void handleInitialize(int width, int height) {
        float w = (float) Platform.screen().windowWidth();
        float h = (float) Platform.screen().windowHeight();
        adjustResolution(w, h, false);
    }

    @Override
    @MustBeInvokedByOverriders
    public final void handleResize(int width, int height) {
        float w = (float) Platform.screen().windowWidth();
        float h = (float) Platform.screen().windowHeight();
        adjustResolution(w, h, false);
    }

    @Override
    public void handleRender(@NotNull OmniMatrixStack matrices, int mouseX, int mouseY, float delta) {
        //#if MC < 1.13
        if (mouseX != mx || mouseY != my) {
            mx = mouseX;
            my = mouseY;
            this.mouseMoved(mx, my);
        }

        //#endif
        if (framebuffer == null || polyUI == UIManager.INSTANCE.getDefaultInstance()) {
            return;
        }

        int width = Platform.screen().windowWidth();
        int height = Platform.screen().windowHeight();
        Drawable master = polyUI.getMaster();

        try {
            framebuffer.clearColor(0f, 0f, 0f, 0f); // Clear to transparent black
            if (framebuffer instanceof ManagedFramebuffer) {
                ((ManagedFramebuffer) framebuffer).clearDepthStencil(1.0, 0);
            }

            framebuffer.usingToRender((matrixStack, w, h) -> {
                matrices.runReplacingGlobalState(polyUI::render);
                return Unit.INSTANCE;
            });
        } catch (Exception e) {
            polyUI.getRenderer().endFrame();
            death(e);
        }

        float scalingFactor = 1f / (float) OmniResolution.getScaleFactor();

        float scaledX = (Platform.screen().windowWidth() / 2f - master.getWidth() / 2f) * scalingFactor;
        float scaledY = (Platform.screen().windowHeight() / 2f - master.getHeight() / 2f) * scalingFactor;
        float scaledWidth = master.getWidth() * scalingFactor;
        float scaledHeight = master.getHeight() * scalingFactor;

        framebuffer.drawColorTexture(
                matrices,
                scaledX, scaledY,
                scaledWidth, scaledHeight,
                Color.WHITE.getRGB()
        );

        OmniManagedDepthState.enable(DepthFunction.LESS_OR_EQUAL);
    }

    @Override
    @MustBeInvokedByOverriders
    public boolean handleKeyPress(int keyCode, int scancode, char typedChar, OmniKeyboard.@NotNull KeyboardModifiers modifiers, OmniScreen.@NotNull KeyPressTrigger trigger) {
        if (keyCode == OmniKeyboard.KEY_ESCAPE && shouldCloseOnEsc()) {
            Platform.screen().close();
            return true;
        }

        try {
            translateKey(polyUI.getInputManager(), keyCode, typedChar, true);
        } catch (Exception e) {
            death(e);
        }

        return true;
    }

    @Override
    @MustBeInvokedByOverriders
    public boolean handleKeyRelease(int keyCode, int scancode, OmniKeyboard.KeyboardModifiers modifiers) {
        try {
            translateKey(polyUI.getInputManager(), keyCode, '\u0000', false);
        } catch (Exception e) {
            death(e);
        }

        return true;
    }

    @Override
    @MustBeInvokedByOverriders
    public boolean handleMouseClick(double mouseX, double mouseY, int mouseButton) {
        try {
            polyUI.getInputManager().mousePressed(mouseButton);
        } catch (Exception e) {
            death(e);
        }

        return true;
    }

    @Override
    @MustBeInvokedByOverriders
    public boolean handleMouseReleased(double mouseX, double mouseY, int mouseButton) {
        try {
            polyUI.getInputManager().mouseReleased(mouseButton);
        } catch (Exception e) {
            death(e);
        }

        return true;
    }

    @Override
    @MustBeInvokedByOverriders
    public boolean handleMouseScrolled(double delta) {
        try {
            float v = (float)
                    //#if MC < 1.13
                    //$$ delta / 8f;
                    //#else
                    delta;
                    //#endif
            polyUI.getInputManager().mouseScrolled(0f, v);
        } catch (Exception e) {
            death(e);
        }

        return true;
    }

    //#if MC >= 1.13
    //$$ @Override
    //#endif
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean doesPauseGame() {
        return pauses;
    }

    @Override
    public boolean hasBackgroundBlur() {
        return blurs;
    }


    //#if MC >= 1.13
    //$$ @Override
    //#endif
    @MustBeInvokedByOverriders
    public void mouseMoved(double mouseX, double mouseY) {
        Drawable master = polyUI.getMaster();
        float scalingFactor = 1f / (float) OmniResolution.getScaleFactor();
        float ox = ((float) Platform.screen().windowWidth() / 2f - master.getWidth() / 2f) * scalingFactor;
        float oy = ((float) Platform.screen().windowHeight() / 2f - master.getHeight() / 2f) * scalingFactor;

        float mx, my;
        //#if MC >= 1.13
        //$$ mx = (float) Minecraft.getInstance().mouseHandler.xpos();
        //$$ my = (float) Minecraft.getInstance().mouseHandler.ypos();
        //#else
        mx = org.lwjgl.input.Mouse.getX();
        my = Platform.screen().windowHeight() - org.lwjgl.input.Mouse.getY() - 1;
        //#endif

        try {
            polyUI.getInputManager().mouseMoved(mx - ox, my - oy);
        } catch (Exception e) {
            death(e);
        }
    }

    @Override
    @MustBeInvokedByOverriders
    public void handleClose() {
        polyUI.getInputManager().unfocus();
        if (close != null) close.accept(polyUI);
        // noinspection DataFlowIssue
        this.polyUI.getWindow().setCursor(Cursor.Pointer);
    }

    protected final void adjustResolution(float w, float h, boolean force) {
        if (this.framebuffer == null) {
            int width = Platform.screen().windowWidth();
            int height = Platform.screen().windowHeight();
            this.framebuffer = new ManagedFramebuffer(width, height, GpuTexture.TextureFormat.RGBA8, GpuTexture.TextureFormat.DEPTH24_STENCIL8);
        }

        // asm: normally, a polyui instance is as big as its window and that is it.
        // however, inside minecraft, the actual content is smaller than the window size, so resizing it directly would just fuck it up.
        // so instead, the developer specifies a resolution that their UI was designed for, and we resize accordingly.
        if (designedWidth == 0f || designedHeight == 0f) {
            return;
        }

        float sx = w / designedWidth;
        float sy = h / designedHeight;
        if (sx == 1f && sy == 1f) {
            return;
        }

        try {
            framebuffer.resize((int) (initialWidth * sx), (int) (initialHeight * sy));
            polyUI.resize(initialWidth * sx, initialHeight * sy, force);
        } catch (Exception e) {
            death(e);
        }
    }

    private void death(Exception e) {
        Platform.screen().close();
        LOGGER.error("Unexpected error", e);
        Notifications.enqueue(Notifications.Type.Error, "An unexpected error occurred with this screen.\nPlease report this to the developer!");
    }

}
