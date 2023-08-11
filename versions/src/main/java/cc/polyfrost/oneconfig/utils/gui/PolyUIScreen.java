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

import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.libs.universal.UScreen;
import cc.polyfrost.oneconfig.renderer.LwjglManager;
import cc.polyfrost.polyui.PolyUI;
import cc.polyfrost.polyui.color.Colors;
import cc.polyfrost.polyui.component.Drawable;
import cc.polyfrost.polyui.input.Keys;
import cc.polyfrost.polyui.input.Modifiers;
import cc.polyfrost.polyui.property.Settings;
import cc.polyfrost.polyui.renderer.Renderer;
import cc.polyfrost.polyui.renderer.data.Cursor;
import cc.polyfrost.polyui.renderer.impl.MCWindow;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

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

    @SuppressWarnings("ConstantConditions") // may produce NullPointerException
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

    //#if MC>=11300
    @Override
    //$$ public final void resize
    //#else
    public final void onResize
    //#endif
    (Minecraft client, int width, int height) {
        polyUI.onResize(UResolution.getWindowWidth(), UResolution.getWindowHeight(), useMinecraftUIScaling() ? (float) UResolution.getScaleFactor() : 1f, false);
    }

    @Override
    public final void onKeyPressed(int keyCode, char typedChar, UKeyboard.Modifiers modifiers) {
        pressInternal(keyCode, typedChar, true);
    }

    @Override
    public final void onKeyReleased(int keyCode, char typedChar, UKeyboard.Modifiers modifiers) {
        pressInternal(keyCode, typedChar, false);
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
        }
        if (keyCode == UKeyboard.KEY_ESCAPE && shouldCloseOnEsc()) {
            UScreen.displayScreen(null);
            return;
        }

        if (keyCode >= 255 && keyCode <= 348) {
            if (keyCode < 340) {
                Keys key;
                // insert, pg down, etc
                if (keyCode >= 256 && keyCode <= 261) {
                    key = Keys.fromValue(keyCode - 156);
                } else if (keyCode >= 266 && keyCode <= 269) { // end, home, etc
                    key = Keys.fromValue(keyCode - 160);
                } else if (keyCode >= 262 && keyCode <= 265) { // arrows
                    key = Keys.fromValue(keyCode - 62);
                } else if (keyCode >= 290 && keyCode <= 314) { // function keys
                    key = Keys.fromValue(keyCode - 289);
                } else {
                    key = Keys.UNKNOWN;
                }

                if (down) {
                    polyUI.getEventManager().keyDown(key);
                } else {
                    polyUI.getEventManager().keyUp(key);
                }
            } else if (keyCode < 348) {
                Modifiers key;
                if (keyCode == 340) {
                    key = Modifiers.LSHIFT;
                } else if (keyCode == 341) {
                    key = Modifiers.LCONTROL;
                } else if (keyCode == 342) {
                    key = Modifiers.LALT;
                } else if (keyCode == 343) {
                    key = Modifiers.LMETA;
                } else if (keyCode == 344) {
                    key = Modifiers.RSHIFT;
                } else if (keyCode == 345) {
                    key = Modifiers.RCONTROL;
                } else if (keyCode == 346) {
                    key = Modifiers.RALT;
                } else {
                    key = Modifiers.RMETA;
                }
                if (down) {
                    polyUI.getEventManager().addModifier(key.getValue());
                } else {
                    polyUI.getEventManager().removeModifier(key.getValue());
                }
            }
            return;
        }
        if (down) {
            polyUI.getEventManager().keyDown(keyCode);
        } else {
            polyUI.getEventManager().keyUp(keyCode);
        }
    }

    @Override
    public final void onMouseClicked(double mouseX, double mouseY, int button) {
        polyUI.getEventManager().onMousePressed(button);
    }

    @Override
    public final void onMouseReleased(double mouseX, double mouseY, int button) {
        polyUI.getEventManager().onMouseReleased(button);
    }

    @Override
    public final void onMouseScrolled(double amount) {
        polyUI.getEventManager().onMouseScrolled(0, (int) amount);
    }

    @Override
    public void onMouseDragged(double x, double y, int clickedButton, long timeSinceLastClick) {
        super.onMouseDragged(x, y, clickedButton, timeSinceLastClick);
    }

    @SuppressWarnings("ConstantConditions") // may produce NullPointerException
    //#if MC>=11300
    //$$ @Override
    //#endif
    public final void mouseMoved(double mouseX, double mouseY) {
        if (useMinecraftUIScaling()) {
            polyUI.getEventManager().setMousePosAndUpdate((float) mouseX, (float) mouseY);
            return;
        }
        float mx = (float) (mouseX * UResolution.getScaleFactor());
        float my = (float) (mouseY * UResolution.getScaleFactor());
        polyUI.getEventManager().setMousePosAndUpdate(mx, my);
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