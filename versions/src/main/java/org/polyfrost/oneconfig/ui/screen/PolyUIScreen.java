/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

import kotlin.Pair;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.Display;
import org.polyfrost.oneconfig.libs.universal.UKeyboard;
import org.polyfrost.oneconfig.libs.universal.UMatrixStack;
import org.polyfrost.oneconfig.libs.universal.UMinecraft;
import org.polyfrost.oneconfig.libs.universal.UMouse;
import org.polyfrost.oneconfig.libs.universal.UResolution;
import org.polyfrost.oneconfig.libs.universal.UScreen;
import org.polyfrost.oneconfig.ui.LwjglManager;
import org.polyfrost.oneconfig.utils.GuiUtils;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.color.Colors;
import org.polyfrost.polyui.color.DarkTheme;
import org.polyfrost.polyui.component.Drawable;
import org.polyfrost.polyui.property.Settings;
import org.polyfrost.polyui.renderer.Renderer;
import org.polyfrost.polyui.renderer.data.Cursor;
import org.polyfrost.polyui.renderer.impl.MCWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;
import static org.polyfrost.oneconfig.ui.KeybindManager.translateKey;

@SuppressWarnings("unused")
public class PolyUIScreen extends UScreen {
    public final float width, height;
    private float ofsX, ofsY;
    public static final Logger LOGGER = LoggerFactory.getLogger("PolyUIScreen");
    private PolyUI polyUI;
    private Drawable[] drawables;
    private final Consumer<PolyUI> func;
    private final Colors colors;
    //#if MC<=11300
    private float mx, my;
    //#endif

    public PolyUIScreen(float width, float height, Colors colors, Consumer<PolyUI> initFunction, Drawable... drawables) {
        super(true);
        this.width = width;
        this.height = height;
        this.colors = colors == null ? new DarkTheme() : colors;
        this.drawables = drawables;
        this.func = initFunction;
    }

    public PolyUIScreen(float width, float height, Colors colors, Drawable... drawables) {
        this(width, height, colors, null, drawables);
    }

    public PolyUIScreen(float width, float height, Drawable... drawables) {
        this(width, height, null, null, drawables);
    }

    @Override
    public final void initScreen(int w, int h) {
        if (polyUI != null) return;
        try {
            LOGGER.info("Creating screen");
            Settings settings = new Settings();
            settings.enableInitCleanup(false);
            Renderer renderer = LwjglManager.INSTANCE.getRenderer(UResolution.getWindowWidth(), UResolution.getWindowHeight());
            polyUI = new PolyUI(renderer, settings, null, GuiUtils.translator, null, null, colors, drawables);
            ofsX = UResolution.getWindowWidth() / 2f - width / 2f;
            ofsY = UResolution.getWindowHeight() / 2f - height / 2f;
            System.out.println(Display.getPixelScaleFactor());
            polyUI.beforeRender(self -> {
                self.translate(ofsX, ofsY);
                return false;
            });
            //#if MC<=11300
            settings.setScrollMultiplier(new Pair<>(0.3f, 0.3f));
            //#endif
            if (useMinecraftUIScaling())
                polyUI.getRenderer().setPixelRatio$polyui((float) UResolution.getScaleFactor());
            drawables = null;
            polyUI.window = new MCWindow(UMinecraft.getMinecraft());
            if (func != null) func.accept(polyUI);
            init(polyUI);
        } catch (Exception e) {
            LOGGER.error("Failed to create screen!", e);
        }
    }

    protected void init(PolyUI polyUI) {
    }

    public boolean useMinecraftUIScaling() {
        return false;
    }


    @Override
    public final void onDrawScreen(@NotNull UMatrixStack matrices, int mouseX, int mouseY, float delta) {
        ofsX = UResolution.getWindowWidth() / 2f - width / 2f;
        ofsY = UResolution.getWindowHeight() / 2f - height / 2f;
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glDisable(GL_ALPHA_TEST);
        glPointSize(1f);

        //#if MC<=11300
        if (mouseX != mx || mouseY != my) {
            mx = mouseX;
            my = mouseY;
            this.mouseMoved(mx, my);
        }
        //#endif

        polyUI.render();

        glPopAttrib();


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
        if (keyCode == UKeyboard.KEY_ESCAPE && shouldCloseOnEsc()) {
            UScreen.displayScreen(null);
            return true;
        }
        translateKey(polyUI.getEventManager(), keyCode, (char) 0, true);
        return true;
    }

    @Override
    public boolean uKeyReleased(int keyCode, int scanCode, @Nullable UKeyboard.Modifiers modifiers) {
        translateKey(polyUI.getEventManager(), keyCode, (char) 0, false);
        return true;
    }

    @Override
    public boolean uCharTyped(char c, @Nullable UKeyboard.Modifiers modifiers) {
        translateKey(polyUI.getEventManager(), 0, c, true);
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
        return false;
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
            polyUI.getEventManager().mouseMoved((float) mouseX - ofsX, (float) mouseY - ofsY);
            return;
        }
        float mx = (float) UMouse.Raw.getX() - ofsX;
        float my = (float) UMouse.Raw.getY() - ofsY;
        polyUI.getEventManager().mouseMoved(mx, my);
    }

    @Override
    public void onScreenClose() {
        this.polyUI.getWindow().setCursor(Cursor.Pointer);
//        this.polyUI.cleanup(); // todo: UScreen hooks at a time where it actually isn't the last frame, so this segfaults (lol)
    }

    public final Drawable getMaster() {
        return polyUI.getMaster();
    }

    public final PolyUI get() {
        return polyUI;
    }

    public final PolyUI getPolyUI() {
        return polyUI;
    }
}