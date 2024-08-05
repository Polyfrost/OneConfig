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

package org.polyfrost.oneconfig.api.ui.v1;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.event.v1.events.HudRenderEvent;
import org.polyfrost.oneconfig.api.event.v1.events.ResizeEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.component.Component;
import org.polyfrost.polyui.Settings;
import org.polyfrost.polyui.renderer.Renderer;
import org.polyfrost.polyui.renderer.Window;

import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * Abstraction over the LWJGL3 implementation and loading.
 */
public interface UIManager {
    UIManager INSTANCE = ServiceLoader.load(
            UIManager.class,
            UIManager.class.getClassLoader()
    ).iterator().next();

    /**
     * Return the renderer instance. This interface specifies operations for rendering UI components. See PolyUI for more information.
     */
    Renderer getRenderer();

    /**
     * Return the TinyFD implementation instance. This interface specifies operations for opening native
     * file dialogs, and showing notifications.
     */
    TinyFD getTinyFD();

    /**
     * Create a new window that is backed by this Minecraft instance. Returns accurate sizing and has cursor support on MC 1.13+.
     */
    Window createWindow();

    /**
     * Wrap this PolyUI instance in a Minecraft screen object, ready to be displayed to the user. {@link org.polyfrost.oneconfig.api.platform.v1.ScreenPlatform#display(Object) Platform.screen().display(this)}
     *
     * @param polyUI             the PolyUI instance to use
     * @param desiredScreenWidth the resolution that this PolyUI instance was designed to use
     * @param pauses             weather to pause the game when the screen is opened
     * @param blurs              if true blur will be used on the background
     * @param onClose            callback to run when the screen is closed
     * @return a Minecraft screen object. Will be a GuiScreen or Screen depending on the Minecraft version.
     */
    Object createPolyUIScreen(@NotNull PolyUI polyUI, float desiredScreenWidth, float desiredScreenHeight, boolean pauses, boolean blurs, @Nullable Consumer<PolyUI> onClose);

    /**
     * return a PolyUI instance that is mounted to the entire screen. It is used internally for displaying and managing HUD components and notifications.
     */
    @NotNull
    PolyUI getDefaultInstance();

    /**
     * <h1>don't use this method!!</h1>
     */
    @ApiStatus.Internal
    default PolyUI createDefault() {
        Settings settings = new Settings();
        settings.enableDebugMode(false);
        settings.enableInitCleanup(false);
        PolyUI p = new PolyUI(new Component[0], getRenderer(), settings, 1920f, 1080f);
        p.getMaster().setRawResize(true);
        p.setWindow(createWindow());
        p.resize(Platform.screen().windowWidth(), Platform.screen().windowHeight(), false);
        EventHandler.of(HudRenderEvent.class, ev -> {
            ev.matrices.push();
            p.render();
            ev.matrices.pop();
        }).register();
        EventHandler.of(ResizeEvent.class, ev -> p.resize(ev.newWidth, ev.newHeight, false)).register();
        return p;
    }
}
