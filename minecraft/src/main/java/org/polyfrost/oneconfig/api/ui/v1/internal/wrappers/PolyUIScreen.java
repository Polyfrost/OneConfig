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
import dev.deftu.textile.Text;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.ui.v1.Notifications;
import org.polyfrost.oneconfig.api.ui.v1.UIManager;
import org.polyfrost.oneconfig.api.ui.v1.screen.BlurScreen;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.component.Drawable;
import org.polyfrost.polyui.data.Cursor;

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
    private final int[] viewport = new int[4];

    //#if MC < 1.13
    private int mx, my;
    //#endif

    public PolyUIScreen(@NotNull PolyUI polyUI, float designedWidth, float designedHeight, boolean pauses, boolean blurs, Consumer<PolyUI> onClose) {
        super(Text.empty(), true);

        this.polyUI = polyUI;
        this.designedWidth = designedWidth;
        this.designedHeight = designedHeight;
        this.initialWidth = polyUI.getMaster().getWidth();
        this.initialHeight = polyUI.getMaster().getHeight();
        this.blurs = false; //todo fix blur
        this.pauses = pauses;
        this.close = onClose;
        //#if MC <= 1.12.2
        // todo temp fix
        //$$ this.mc = Minecraft.getMinecraft();
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
        float scale = Platform.screen().mcToScreenScale();
        adjustResolution(width * scale, height * scale, false);
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
            // asm: we need to bind the main framebuffer on 1.21.5+ because for some reason mc unbinds it right before... whatever
            //#if MC >= 1.21.5
            //$$ kotlin.jvm.functions.Function0<kotlin.Unit> unbind = dev.deftu.omnicore.api.client.framebuffer.OmniFramebuffers.getMain().bind();
            //#endif
            Platform.gl().glViewport(viewport);
            float factor = Platform.screen().pixelRatio();
            int w = (int) (polyUI.getMaster().getWidth() * factor);
            int h = (int) (polyUI.getMaster().getHeight() * factor);
            int x = Platform.screen().viewportWidth() / 2 - w / 2;
            int y = Platform.screen().viewportHeight() / 2 - h / 2;
            glViewport(x, y, w, h);
            float[] v = polyUI.getWindow().getViewport();
            v[0] = (float) x;
            v[1] = (float) y;
            v[2] = (float) w;
            v[3] = (float) h;
            polyUI.render();
            glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            //#if MC >= 1.21.5
            //$$ unbind.invoke();
            //#endif
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
            // we scroll by 15 pixels per notch, so we'll convert the scroll amount accordingly
            float clampedAmount = (float)
                    //#if MC >= 1.16.5
                    //$$ amount * 15f; // amount = 1 up and -1 down
                    //#else
                    amount / 8f; // amount = 120 up and -120 down
                    //#endif
            polyUI.getInputManager().mouseScrolled((float) horizontalAmount, -clampedAmount);
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
        return false;
    }


    //#if MC >= 1.13
    //$$ @Override
    //#endif
    @MustBeInvokedByOverriders
    public void mouseMoved(double mouseX, double mouseY) {
        Drawable master = polyUI.getMaster();
        // guys it's not that deep
        float ox = Platform.screen().windowWidth() / 2f - master.getVisibleWidth() / 2f;
        float oy = Platform.screen().windowHeight() / 2f - master.getVisibleHeight() / 2f;

        float mx, my;
        //#if MC >= 1.13
         mx = (float) Minecraft.getInstance().mouseHandler.xpos();
         my = (float) Minecraft.getInstance().mouseHandler.ypos();
        //#else
        //$$ mx = org.lwjgl.input.Mouse.getX();
        //$$ my = (Platform.screen().windowHeight() - org.lwjgl.input.Mouse.getY() - 1);
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
            float newW = initialWidth * sx;
            float newH = initialHeight * sy;
            polyUI.resize(newW, newH, force);
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
