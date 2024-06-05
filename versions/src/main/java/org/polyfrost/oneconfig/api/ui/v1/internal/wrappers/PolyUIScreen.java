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

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.ui.v1.screen.BlurScreen;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.renderer.data.Cursor;
import org.polyfrost.polyui.unit.Vec2;
import org.polyfrost.universal.UKeyboard;
import org.polyfrost.universal.UMatrixStack;
import org.polyfrost.universal.UScreen;

import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.glViewport;
import static org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager.translateKey;

@SuppressWarnings("unused")
public class PolyUIScreen extends UScreen implements BlurScreen {
    @NotNull
    public final PolyUI polyUI;

    @Nullable
    public final Vec2 desiredResolution;
    private final boolean pauses, blurs;
    private final Consumer<PolyUI> close;

    //#if MC<=11300
    private int mx, my;
    //#endif

    public PolyUIScreen(@NotNull PolyUI polyUI, @Nullable Vec2 desiredResolution, boolean pauses, boolean blurs, Consumer<PolyUI> onClose) {
        super(true);
        this.polyUI = polyUI;
        this.desiredResolution = desiredResolution;
        this.blurs = blurs;
        this.pauses = pauses;
        this.close = onClose;
        adjustResolution(Platform.screen().windowWidth(), Platform.screen().windowHeight(), false);
    }

    protected final void adjustResolution(float w, float h, boolean force) {
        // asm: normally, a polyui instance is as big as its window and that is it.
        // however, inside minecraft, the actual content is smaller than the window size, so resizing it directly would just fuck it up.
        // so instead, the developer specifies a resolution that their UI was designed for, and we resize accordingly.
        if (desiredResolution == null) return;
        float sx = w / desiredResolution.getX();
        float sy = h / desiredResolution.getY();
        if (sx == 1f && sy == 1f) return;
        Vec2 size = polyUI.getMaster().getSize();
        polyUI.resize(size.getX() * sx, size.getY() * sy, force);
    }

    @Override
    public void onDrawScreen(@NotNull UMatrixStack matrices, int mouseX, int mouseY, float delta) {
        Vec2 size = polyUI.getMaster().getSize();
        //noinspection DataFlowIssue
        float scale = polyUI.getWindow().getPixelRatio();
        float ox = (Platform.screen().windowWidth() / 2f - size.getX() / 2f) * scale;
        float oy = (Platform.screen().windowHeight() / 2f - size.getY() / 2f) * scale;
        glViewport((int) ox, (int) oy, (int) (size.getX() * scale), (int) (size.getY() * scale));

        //#if MC<11300
        if (mouseX != mx || mouseY != my) {
            mx = mouseX;
            my = mouseY;
            this.mouseMoved(mx, my);
        }
        //#endif

        matrices.runReplacingGlobalState(polyUI::render);

        glViewport(0, 0, Platform.screen().viewportWidth(), Platform.screen().viewportHeight());
    }

    @Override
    @MustBeInvokedByOverriders
    public final void onResize(Minecraft client, int width, int height) {
        float w = (float) Platform.screen().viewportWidth();
        float h = (float) Platform.screen().viewportHeight();
        adjustResolution(w, h, false);
    }

    @Override
    @MustBeInvokedByOverriders
    public boolean uKeyPressed(int keyCode, int scanCode, @Nullable UKeyboard.Modifiers modifiers) {
        if (keyCode == UKeyboard.KEY_ESCAPE && shouldCloseOnEsc()) {
            Platform.screen().close();
            return true;
        }
        translateKey(polyUI.getInputManager(), keyCode, (char) 0, true);
        return true;
    }

    @Override
    @MustBeInvokedByOverriders
    public boolean uKeyReleased(int keyCode, int scanCode, @Nullable UKeyboard.Modifiers modifiers) {
        translateKey(polyUI.getInputManager(), keyCode, (char) 0, false);
        return true;
    }

    @Override
    @MustBeInvokedByOverriders
    public boolean uCharTyped(char c, @Nullable UKeyboard.Modifiers modifiers) {
        translateKey(polyUI.getInputManager(), 0, c, true);
        return true;
    }

    @Override
    @MustBeInvokedByOverriders
    public boolean uMouseClicked(double mouseX, double mouseY, int mouseButton) {
        polyUI.getInputManager().mousePressed(mouseButton);
        return true;
    }

    @Override
    @MustBeInvokedByOverriders
    public boolean uMouseReleased(double mouseX, double mouseY, int mouseButton) {
        polyUI.getInputManager().mouseReleased(mouseButton);
        return true;
    }

    @Override
    @MustBeInvokedByOverriders
    public boolean uMouseScrolled(double delta) {
        polyUI.getInputManager().mouseScrolled(0f, (float) delta);
        return true;
    }

    //#if MC>=11300
    //$$ @Override
    //#endif
    public boolean shouldCloseOnEsc() {
        return true;
    }

    //#if MC<=11300
    @Override
    //#endif
    public boolean doesGuiPauseGame() {
        return pauses;
    }

    @Override
    public boolean hasBackgroundBlur() {
        return blurs;
    }


    //#if MC>=11300
    //$$ @Override
    //#endif
    @MustBeInvokedByOverriders
    public void mouseMoved(double mouseX, double mouseY) {
        Vec2 size = polyUI.getMaster().getSize();
        float ox = (float) Platform.screen().windowWidth() / 2f - size.getX() / 2f;
        float oy = (float) Platform.screen().windowHeight() / 2f - size.getY() / 2f;

        float mx, my;
        //#if MC>=11300
        //$$ mx = (float) Minecraft.getInstance().mouseHelper.getMouseX();
        //$$ my = (float) Minecraft.getInstance().mouseHelper.getMouseY();
        //#else
        mx = org.lwjgl.input.Mouse.getX();
        my = Platform.screen().windowHeight() - org.lwjgl.input.Mouse.getY() - 1;
        //#endif

        polyUI.getInputManager().mouseMoved(mx - ox, my - oy);
    }

    @Override
    @MustBeInvokedByOverriders
    public void onScreenClose() {
        if (close != null) close.accept(polyUI);
        // noinspection DataFlowIssue
        this.polyUI.getWindow().setCursor(Cursor.Pointer);
    }
}