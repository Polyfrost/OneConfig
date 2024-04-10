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

package org.polyfrost.oneconfig.ui.screen;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.libs.universal.UKeyboard;
import org.polyfrost.oneconfig.libs.universal.UMatrixStack;
import org.polyfrost.oneconfig.libs.universal.UMinecraft;
import org.polyfrost.oneconfig.libs.universal.UMouse;
import org.polyfrost.oneconfig.libs.universal.UResolution;
import org.polyfrost.oneconfig.libs.universal.UScreen;
import org.polyfrost.oneconfig.ui.LwjglManager;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.color.Colors;
import org.polyfrost.polyui.color.DarkTheme;
import org.polyfrost.polyui.color.PolyColor;
import org.polyfrost.polyui.component.Drawable;
import org.polyfrost.polyui.event.InputManager;
import org.polyfrost.polyui.input.Translator;
import org.polyfrost.polyui.property.Settings;
import org.polyfrost.polyui.renderer.data.Cursor;
import org.polyfrost.polyui.unit.Align;
import org.polyfrost.polyui.unit.Vec2;

import static org.lwjgl.opengl.GL11.glViewport;
import static org.polyfrost.oneconfig.ui.KeybindManager.translateKey;

@SuppressWarnings("unused")
public class PolyUIScreen extends UScreen implements UIPause, BlurScreen {
    @Nullable
    public final PolyUI polyUI;

    @NotNull
    public final InputManager inputManager;

    @Nullable
    public final Vec2 desiredResolution;

    public boolean pauses, blurs;

    private final MCWindow window;

    //#if MC<=11300
    private float mx, my;
    //#endif

    private float ox, oy;


    @Contract("_, null, _, _, _, _, _, _, null -> fail")
    public PolyUIScreen(@Nullable Settings settings,
                        @Nullable InputManager inputManager,
                        @Nullable Translator translator,
                        @Nullable Align alignment,
                        @Nullable Colors colors,
                        @Nullable PolyColor backgroundColor,
                        @Nullable Vec2 desiredResolution,
                        @Nullable Vec2 size,
                        Drawable... drawables) {
        super(true);

        Settings s = settings == null ? new Settings() : settings;
        s.enableInitCleanup(false);
        if (drawables == null || drawables.length == 0) {
            if (inputManager == null) throw new IllegalArgumentException("Must be created with an inputManager or drawables");
            this.inputManager = inputManager;
            this.polyUI = null;
            this.desiredResolution = null;
            this.window = null;
        } else {
            Colors c = colors == null ? new DarkTheme() : colors;
            Align a = alignment == null ? new Align(Align.Main.Start, Align.Cross.Start, Align.Mode.Horizontal, Vec2.ZERO, 50) : alignment;
            this.polyUI = new PolyUI(LwjglManager.INSTANCE.getRenderer(), s, inputManager, translator, backgroundColor, a, c, size, drawables);
            this.window = new MCWindow(UMinecraft.getMinecraft());
            this.window.setPixelRatio(scale());
            this.polyUI.setWindow(window);
            this.inputManager = this.polyUI.getInputManager();
            this.desiredResolution = desiredResolution;
            adjustResolution();
        }
    }

    public PolyUIScreen(Drawable... drawables) {
        this(null, null, null, null, null, null, null, null, drawables);
    }

    public PolyUIScreen(@Nullable Align alignment, Vec2 size, Drawable... drawables) {
        this(null, null, null, alignment, null, null, null, size, drawables);
    }

    public PolyUIScreen(@NotNull InputManager inputManager) {
        this(null, inputManager, null, null, null, null, null, null);
    }

    @ApiStatus.Internal
    public PolyUIScreen(@NotNull PolyUI polyUI) {
        super(true);
        this.polyUI = polyUI;
        this.inputManager = polyUI.getInputManager();
        desiredResolution = null;
        window = new MCWindow(UMinecraft.getMinecraft());
        polyUI.setWindow(window);
    }

    private void adjustResolution() {
        // asm: normally, a polyui instance is as big as its window and that is it.
        // however, inside minecraft, the actual content is smaller than the window size, so resizing it directly would just fuck it up.
        // so instead, the developer specifies a resolution that their UI was designed for, and we resize accordingly.
        if (polyUI == null || desiredResolution == null) return;
        float sx = width() / desiredResolution.getX();
        float sy = height() / desiredResolution.getY();
        if (sx == 1f && sy == 1f) return;
        Vec2 size = polyUI.getMaster().getSize();
        polyUI.resize(size.getX() * sx, size.getY() * sy, false);
    }


    public boolean useMinecraftUIScaling() {
        return false;
    }


    @Override
    public void onDrawScreen(@NotNull UMatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (polyUI == null) return;

        Vec2 size = polyUI.getMaster().getSize();
        ox = width() / 2f - size.getX() / 2f;
        oy = height() / 2f - size.getY() / 2f;
        glViewport((int) ox, (int) oy, (int) size.getX(), (int) size.getY());


        //#if MC>=11300
        //$$ com.mojang.blaze3d.systems.RenderSystem.disableCull();
        //$$ com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        //#else
        net.minecraft.client.renderer.GlStateManager.disableCull();
        net.minecraft.client.renderer.GlStateManager.enableBlend();

        if (mouseX != mx || mouseY != my) {
            mx = mouseX;
            my = mouseY;
            this.mouseMoved(mx, my);
        }
        //#endif

        polyUI.render();

        //#if MC>=11300
        //$$ com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        //$$ com.mojang.blaze3d.systems.RenderSystem.enableCull();
        //#else
        net.minecraft.client.renderer.GlStateManager.disableBlend();
        net.minecraft.client.renderer.GlStateManager.enableCull();
        //#endif

        glViewport(0,0, (int) width(), (int) height());
    }

    @Override
    //#if MC>=11300
    //$$ public final void resize
    //#else
    public final void onResize
    //#endif
    (Minecraft client, int width, int height) {
        if (polyUI == null) return;
        polyUI.resize(width(), height(), false);
    }

    @Override
    public boolean uKeyPressed(int keyCode, int scanCode, @Nullable UKeyboard.Modifiers modifiers) {
        if (keyCode == UKeyboard.KEY_ESCAPE && shouldCloseOnEsc()) {
            UScreen.displayScreen(null);
            return true;
        }
        translateKey(inputManager, keyCode, (char) 0, true);
        return true;
    }

    @Override
    public boolean uKeyReleased(int keyCode, int scanCode, @Nullable UKeyboard.Modifiers modifiers) {
        translateKey(inputManager, keyCode, (char) 0, false);
        return true;
    }

    @Override
    public boolean uCharTyped(char c, @Nullable UKeyboard.Modifiers modifiers) {
        translateKey(inputManager, 0, c, true);
        return true;
    }

    @Override
    public boolean uMouseClicked(double mouseX, double mouseY, int mouseButton) {
        inputManager.mousePressed(mouseButton);
        return true;
    }

    @Override
    public boolean uMouseReleased(double mouseX, double mouseY, int mouseButton) {
        inputManager.mouseReleased(mouseButton);
        return true;
    }

    @Override
    public boolean uMouseScrolled(double delta) {
        inputManager.mouseScrolled(0f, delta > 0.0 ? -1f : 1f);
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
        return doesUIPauseGame();
    }

    @Override
    public boolean doesUIPauseGame() {
        return pauses;
    }

    @Override
    public boolean hasBackgroundBlur() {
        return blurs;
    }


    //#if MC>=11300
    //$$ @Override
    //#endif
    public void mouseMoved(double mouseX, double mouseY) {
        if (useMinecraftUIScaling()) {
            inputManager.mouseMoved((float) mouseX - ox, (float) mouseY - oy);
            return;
        }
        float mx = (float) UMouse.Raw.getX() - ox;
        float my = (float) UMouse.Raw.getY() - oy;
        inputManager.mouseMoved(mx, my);
    }

    @Override
    @MustBeInvokedByOverriders
    public void onScreenClose() {
        if (polyUI == null) return;
        // noinspection DataFlowIssue
        this.polyUI.getWindow().setCursor(Cursor.Pointer);
    }

    public Drawable getMaster() {
        if (polyUI == null) throw new IllegalArgumentException("no drawables attached this way");
        return polyUI.getMaster();
    }

    public float width() {
        return useMinecraftUIScaling() ? (float) (UResolution.getViewportWidth() * UResolution.getScaleFactor()) : UResolution.getViewportWidth();
    }

    public float height() {
        return useMinecraftUIScaling() ? (float) (UResolution.getViewportHeight() * UResolution.getScaleFactor()) : UResolution.getViewportHeight();
    }

    public float scale() {
        float s = useMinecraftUIScaling() ? (float) UResolution.getScaleFactor() : 1f;
        return PolyUI.isOnMac ? s * 2f : s;
    }
}