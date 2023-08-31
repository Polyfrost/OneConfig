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

package org.polyfrost.oneconfig.utils.gui;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.libs.universal.UKeyboard;
import org.polyfrost.oneconfig.libs.universal.UMatrixStack;
import org.polyfrost.oneconfig.libs.universal.UResolution;
import org.polyfrost.oneconfig.libs.universal.UScreen;
import org.polyfrost.oneconfig.renderer.LwjglManager;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.color.Colors;
import org.polyfrost.polyui.component.Drawable;
import org.polyfrost.polyui.input.Keys;
import org.polyfrost.polyui.input.Modifiers;
import org.polyfrost.polyui.property.Settings;
import org.polyfrost.polyui.renderer.Renderer;
import org.polyfrost.polyui.renderer.data.Cursor;
import org.polyfrost.polyui.renderer.impl.MCWindow;

import java.util.function.Consumer;

@SuppressWarnings("unused")
public class PolyUIScreen extends UScreen {
    private PolyUI polyUI;
    private Drawable[] drawables;
    private final Consumer<PolyUI> func;
    private final Colors colors;
    //#if MC<=11300
    private float mx, my;
    //#endif

    public PolyUIScreen(Colors colors, Consumer<PolyUI> initFunction, Drawable... drawables) {
        super(true);
        this.colors = colors;
        this.drawables = drawables;
        this.func = initFunction;
    }

    public PolyUIScreen(Colors colors, Drawable... drawables) {
        this(colors, null, drawables);
    }

    public PolyUIScreen(Drawable... drawables) {
        this(null, null, drawables);
    }

    @Override
    public final void initScreen(int w, int h) {
        if (polyUI != null) return;
        Settings settings = new Settings();
        settings.setCleanupAfterInit(false);
        settings.setFramebuffersEnabled(false);
        settings.setRenderPausingEnabled(false);
        settings.setDebug(false);
        Renderer renderer = LwjglManager.INSTANCE.getRenderer(UResolution.getWindowWidth(), UResolution.getWindowHeight());
        //#if MC>=11300
        //$$ polyUI = new PolyUI("", renderer, settings, colors, drawables);
        //#else
        renderer.setWidth(UResolution.getWindowWidth());
        renderer.setHeight(UResolution.getWindowHeight());
        polyUI = new PolyUI("", renderer, settings, colors, drawables);
        //#endif
        if (useMinecraftUIScaling())
            polyUI.getRenderer().setPixelRatio$polyui((float) UResolution.getScaleFactor());
        drawables = null;
        polyUI.window = new MCWindow(Minecraft.getMinecraft());
        if (func != null) func.accept(polyUI);
        init(polyUI);
    }

    protected void init(PolyUI polyUI) {
    }

    public boolean useMinecraftUIScaling() {
        return false;
    }


    @Override
    public final void onDrawScreen(@NotNull UMatrixStack matrices, int mouseX, int mouseY, float delta) {
        //#if MC>=11300
        //$$ com.mojang.blaze3d.systems.RenderSystem.disableCull();
        //todo what's blend in 1.13+
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
        //$$ com.mojang.blaze3d.systems.RenderSystem.enableCull();
        //#else
        net.minecraft.client.renderer.GlStateManager.enableCull();
        //#endif
        super.onDrawScreen(matrices, mouseX, mouseY, delta);
    }

    @Override
    //#if MC>=11300
    //$$ public final void resize
    //#else
    public final void onResize
        //#endif
    (Minecraft client, int width, int height) {
        polyUI.resize(UResolution.getWindowWidth(), UResolution.getWindowHeight(), useMinecraftUIScaling() ? (float) UResolution.getScaleFactor() : 1f, false);
    }

    @Override
    public boolean uKeyPressed(int keyCode, int scanCode, @Nullable UKeyboard.Modifiers modifiers) {
        pressInternal(keyCode, (char) 0, true);
        return true;
    }

    @Override
    public boolean uKeyReleased(int keyCode, int scanCode, @Nullable UKeyboard.Modifiers modifiers) {
        pressInternal(keyCode, (char) 0, false);
        return true;
    }

    @Override
    public boolean uCharTyped(char c, @Nullable UKeyboard.Modifiers modifiers) {
        pressInternal(0, c, true);
        return true;
    }

    //#if MC>=11300
    //$$ @Override
    //#endif
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    //#if MC>=11300
    //$$ public boolean isPauseScreen()
    //#else
    public boolean doesGuiPauseGame()
    //#endif
    {
        return false;
    }

    protected final void pressInternal(int keyCode, char typedChar, boolean down) {
        if (typedChar != 0) {
            polyUI.getEventManager().keyTyped(typedChar);
            return;
        }
        if (keyCode == UKeyboard.KEY_ESCAPE && shouldCloseOnEsc()) {
            polyUI.window.close();
            return;
        }
        if (keyCode == UKeyboard.KEY_LSHIFT) mod(Modifiers.LSHIFT.getValue(), down);
        else if (keyCode == UKeyboard.KEY_RSHIFT) mod(Modifiers.RSHIFT.getValue(), down);
        else if (keyCode == UKeyboard.KEY_LCONTROL) mod(Modifiers.LCONTROL.getValue(), down);
        else if (keyCode == UKeyboard.KEY_RCONTROL) mod(Modifiers.RCONTROL.getValue(), down);
        else if (keyCode == UKeyboard.KEY_LMENU) mod(Modifiers.LALT.getValue(), down);
        else if (keyCode == UKeyboard.KEY_RMENU) mod(Modifiers.RALT.getValue(), down);
        else if (keyCode == UKeyboard.KEY_LMETA) mod(Modifiers.LMETA.getValue(), down);
        else if (keyCode == UKeyboard.KEY_RMETA) mod(Modifiers.RMETA.getValue(), down);
        else {
            Keys k;
            // you can't switch because of the stupid noInline stuff
            if (keyCode == UKeyboard.KEY_F1) k = Keys.F1;
            else if (keyCode == UKeyboard.KEY_F2) k = Keys.F2;
            else if (keyCode == UKeyboard.KEY_F3) k = Keys.F3;
            else if (keyCode == UKeyboard.KEY_F4) k = Keys.F4;
            else if (keyCode == UKeyboard.KEY_F5) k = Keys.F5;
            else if (keyCode == UKeyboard.KEY_F6) k = Keys.F6;
            else if (keyCode == UKeyboard.KEY_F7) k = Keys.F7;
            else if (keyCode == UKeyboard.KEY_F8) k = Keys.F8;
            else if (keyCode == UKeyboard.KEY_F9) k = Keys.F9;
            else if (keyCode == UKeyboard.KEY_F10) k = Keys.F10;
            else if (keyCode == UKeyboard.KEY_F11) k = Keys.F11;
            else if (keyCode == UKeyboard.KEY_F12) k = Keys.F12;
            else if (keyCode == UKeyboard.KEY_ESCAPE) k = Keys.ESCAPE;
            else if (keyCode == UKeyboard.KEY_ENTER) k = Keys.ENTER;
            else if (keyCode == UKeyboard.KEY_BACKSPACE) k = Keys.BACKSPACE;
            else if (keyCode == UKeyboard.KEY_TAB) k = Keys.TAB;
//            else if (keyCode == UKeyboard.KEY_PRIOR) k = Keys.PAGE_UP;
//            else if (keyCode == UKeyboard.KEY_NEXT) k = Keys.PAGE_DOWN;
            else if (keyCode == UKeyboard.KEY_END) k = Keys.END;
            else if (keyCode == UKeyboard.KEY_HOME) k = Keys.HOME;
            else if (keyCode == UKeyboard.KEY_LEFT) k = Keys.LEFT;
            else if (keyCode == UKeyboard.KEY_UP) k = Keys.UP;
            else if (keyCode == UKeyboard.KEY_RIGHT) k = Keys.RIGHT;
            else if (keyCode == UKeyboard.KEY_DOWN) k = Keys.DOWN;
//            else if (keyCode == UKeyboard.KEY_INSERT) k = Keys.INSERT;
            else k = Keys.UNKNOWN;

            if (k == Keys.UNKNOWN) {
                if (down) {
                    polyUI.getEventManager().keyDown(keyCode);
                } else {
                    polyUI.getEventManager().keyUp(keyCode);
                }
            } else if (down) {
                polyUI.getEventManager().keyDown(k);
            } else {
                polyUI.getEventManager().keyUp(k);
            }
        }
    }

    private void mod(short v, boolean down) {
        if (down) {
            polyUI.getEventManager().addModifier(v);
        } else {
            polyUI.getEventManager().removeModifier(v);
        }
    }

    @Override
    public boolean uMouseClicked(double mouseX, double mouseY, int mouseButton) {
        polyUI.getEventManager().mousePressed(mouseButton);
        return true;
    }

    @Override
    public boolean uMouseReleased(double mouseX, double mouseY, int mouseButton) {
        polyUI.getEventManager().mouseReleased(mouseButton);
        return true;
    }

    @Override
    public boolean uMouseScrolled(double delta) {
        polyUI.getEventManager().mouseScrolled(0, (int) delta);
        return true;
    }


    //#if MC>=11300
    //$$ @Override
    //#endif
    public final void mouseMoved(double mouseX, double mouseY) {
        if (useMinecraftUIScaling()) {
            polyUI.getEventManager().mouseMoved((float) mouseX, (float) mouseY);
            return;
        }
        float mx = (float) (mouseX * UResolution.getScaleFactor());
        float my = (float) (mouseY * UResolution.getScaleFactor());
        polyUI.getEventManager().mouseMoved(mx, my);
    }

    @Override
    public void onScreenClose() {
        this.polyUI.getWindow().setCursor(Cursor.Pointer);
//        this.polyUI.cleanup(); // todo: UScreen hooks at a time where it actually isn't the last frame, so this segfaults (lol)
    }

    public final PolyUI get() {
        return polyUI;
    }

    public final PolyUI polyUI() {
        return polyUI;
    }

    public final PolyUI getInstance() {
        return polyUI;
    }

    public final PolyUI getPolyUI() {
        return polyUI;
    }
}