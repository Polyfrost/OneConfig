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

import dev.deftu.omnicore.api.client.input.KeyboardModifiers;
import dev.deftu.omnicore.api.client.input.OmniKey;
import dev.deftu.omnicore.api.client.input.OmniKeys;
import dev.deftu.omnicore.api.client.input.OmniMouseButton;
import dev.deftu.omnicore.api.client.render.OmniRenderingContext;
import dev.deftu.omnicore.api.client.screen.KeyPressEvent;
import dev.deftu.omnicore.api.client.screen.OmniScreen;
import dev.deftu.textile.minecraft.MCSimpleTextHolder;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.ui.v1.Notifications;
import org.polyfrost.oneconfig.api.ui.v1.UIManager;
import org.polyfrost.oneconfig.api.ui.v1.screen.BlurScreen;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.component.Drawable;
import org.polyfrost.polyui.data.Cursor;

import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;
import static org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager.translateKey;

@SuppressWarnings("unused")
public class PolyUIScreen extends OmniScreen implements BlurScreen {

    private static final Logger LOGGER = LogManager.getLogger("OneConfig/PolyUIScreen");

    @NotNull
    public final PolyUI polyUI;

    private final float designedWidth, designedHeight, initialWidth, initialHeight;
    private final boolean pauses, blurs;
    private final Consumer<PolyUI> close;
    private final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(
            //#if MC >= 1.13
            //$$ 4
            //#else
            16
            //#endif
    );

    //#if MC < 1.13
    private int mx, my;
    //#endif

    public PolyUIScreen(@NotNull PolyUI polyUI, float designedWidth, float designedHeight, boolean pauses, boolean blurs, Consumer<PolyUI> onClose) {
        super(new MCSimpleTextHolder(""), true);

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
    public void onInitialize(int width, int height) {
        super.onInitialize(width, height);
        float w = (float) Platform.screen().windowWidth();
        float h = (float) Platform.screen().windowHeight();
        adjustResolution(w, h, false);
    }

    @Override
    @MustBeInvokedByOverriders
    public final void onResize(int width, int height) {
        float w = (float) Platform.screen().windowWidth();
        float h = (float) Platform.screen().windowHeight();
        adjustResolution(w, h, false);
    }

    @Override
    @MustBeInvokedByOverriders
    public void onRender(@NotNull OmniRenderingContext ctx, int mouseX, int mouseY, float delta) {
        //#if MC < 1.13
        if (mouseX != mx || mouseY != my) {
            mx = mouseX;
            my = mouseY;
            this.mouseMoved(mx, my);
        }

        //#endif
        if (polyUI == UIManager.INSTANCE.getDefaultInstance()) {
            return;
        }

        try {
            ((Buffer) VIEWPORT).clear();
            //#if MC >= 1.13
            //$$ glGetIntegerv(GL_VIEWPORT, VIEWPORT);
            //#else
            glGetInteger(GL_VIEWPORT, VIEWPORT);
            //#endif
            int w = (int) polyUI.getMaster().getWidth();
            int h = (int) polyUI.getMaster().getHeight();
            int x = Platform.screen().windowWidth() / 2 - w / 2;
            int y = Platform.screen().windowHeight() / 2 - h / 2;
            glViewport(x, y, w, h);
            polyUI.render();
            glViewport(VIEWPORT.get(), VIEWPORT.get(), VIEWPORT.get(), VIEWPORT.get());
        } catch (Exception e) {
            polyUI.getRenderer().endFrame();
            death(e);
        }
    }

    @Override
    @MustBeInvokedByOverriders
    public boolean onKeyPress(@NotNull OmniKey key, int scanCode, char typedChar, @NotNull KeyboardModifiers modifiers, @NotNull KeyPressEvent event) {
        if (key == OmniKeys.KEY_ESCAPE && shouldCloseOnEsc()) {
            Platform.screen().close();
            return super.onKeyPress(key, scanCode, typedChar, modifiers, event);
        }

        try {
            translateKey(polyUI.getInputManager(), key.getCode(), typedChar, true);
        } catch (Exception e) {
            death(e);
        }

        return super.onKeyPress(key, scanCode, typedChar, modifiers, event);
    }

    @Override
    @MustBeInvokedByOverriders
    public boolean onKeyRelease(@NotNull OmniKey key, int scanCode, @NotNull KeyboardModifiers modifiers) {
        try {
            translateKey(polyUI.getInputManager(), key.getCode(), (char) 0, false);
        } catch (Exception e) {
            death(e);
        }

        return super.onKeyRelease(key, scanCode, modifiers);
    }

    @Override
    @MustBeInvokedByOverriders
    public boolean onMouseClick(@NotNull OmniMouseButton button, double x, double y, @NotNull KeyboardModifiers modifiers) {
        try {
            polyUI.getInputManager().mousePressed(button.getCode());
        } catch (Exception e) {
            death(e);
        }

        return super.onMouseClick(button, x, y, modifiers);
    }

    @Override
    @MustBeInvokedByOverriders
    public boolean onMouseRelease(@NotNull OmniMouseButton button, double mouseX, double mouseY, @NotNull KeyboardModifiers modifiers) {
        try {
            polyUI.getInputManager().mouseReleased(button.getCode());
        } catch (Exception e) {
            death(e);
        }

        return true;
    }

    @Override
    @MustBeInvokedByOverriders
    public boolean onMouseScroll(double x, double y, double amount, double horizontalAmount) {
        try {
            float clampedAmount = (float)
                    //#if MC < 1.13
                    //$$ amount / 8f;
                    //#else
                    amount;
                    //#endif
            polyUI.getInputManager().mouseScrolled((float) horizontalAmount, clampedAmount);
        } catch (Exception e) {
            death(e);
        }

        return super.onMouseScroll(x, y, amount, horizontalAmount);
    }

    //#if MC >= 1.13
    //$$ @Override
    //#endif
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPausingScreen() {
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
        // guys it's not that deep
        float ox = Platform.screen().windowWidth() / 2f - master.getWidth() / 2f;
        float oy = Platform.screen().windowHeight() / 2f - master.getHeight() / 2f;

        float mx, my;
        //#if MC >= 1.13
        //$$ mx = (float) Minecraft.getInstance().mouseHandler.xpos();
        //$$ my = (float) Minecraft.getInstance().mouseHandler.ypos();
        //#else
        mx = org.lwjgl.input.Mouse.getX();
        my = (Platform.screen().windowHeight() - org.lwjgl.input.Mouse.getY() - 1);
        //#endif

        try {
            polyUI.getInputManager().mouseMoved(mx - ox, my - oy);
        } catch (Exception e) {
            death(e);
        }
    }

    @Override
    @MustBeInvokedByOverriders
    public void onScreenClose() {
        polyUI.getInputManager().unfocus();
        if (close != null) close.accept(polyUI);
        // noinspection DataFlowIssue
        this.polyUI.getWindow().setCursor(Cursor.Pointer);
        super.onScreenClose();
    }

    protected final void adjustResolution(float w, float h, boolean force) {

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
            float ratio = Platform.screen().pixelRatio();
            // framebuffer should you know probably be the correct larger size because.. well yeah of course it does
            // didn't anyone think of that?
            polyUI.resize(initialWidth * sx, initialHeight * sy, force);
            polyUI.getWindow().setPixelRatio(ratio);
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
