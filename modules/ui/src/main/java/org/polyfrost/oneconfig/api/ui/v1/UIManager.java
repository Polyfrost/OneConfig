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

import dev.deftu.omnicore.client.OmniChat;
import dev.deftu.omnicore.client.render.OmniMatrixStack;
import dev.deftu.omnicore.client.render.OmniResolution;
import dev.deftu.omnicore.client.render.framebuffer.ManagedFramebuffer;
import dev.deftu.omnicore.client.render.state.OmniManagedBlendState;
import dev.deftu.omnicore.client.render.state.OmniManagedDepthState;
import dev.deftu.omnicore.client.render.texture.GpuTexture;
import dev.deftu.textile.minecraft.MCSimpleTextHolder;
import dev.deftu.textile.minecraft.MCTextFormat;
import kotlin.Unit;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.event.v1.EventDelay;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.HudRenderEvent;
import org.polyfrost.oneconfig.api.event.v1.events.ResizeEvent;
import org.polyfrost.oneconfig.api.event.v1.events.WorldEvent;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.ui.v1.api.TinyFdApi;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.Settings;
import org.polyfrost.polyui.component.Component;
import org.polyfrost.polyui.component.Drawable;
import org.polyfrost.polyui.renderer.Renderer;
import org.polyfrost.polyui.renderer.Window;

import java.awt.*;
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
    TinyFdApi getTinyFD();

    /**
     * Create a new window that is backed by this Minecraft instance. Returns accurate sizing and has cursor support on MC 1.13+.
     */
    Window createWindow();

    /**
     * Wrap this PolyUI instance in a Minecraft screen object, ready to be displayed to the user. {@link org.polyfrost.oneconfig.api.platform.v1.ScreenPlatform#display(Object) Platform.screen().display(this)}
     *
     * @param polyUI        the PolyUI instance to use
     * @param designedWidth the resolution that this PolyUI instance was designed to use
     * @param pauses        weather to pause the game when the screen is opened
     * @param blurs         if true blur will be used on the background
     * @param onClose       callback to run when the screen is closed
     * @return a Minecraft screen object. Will be a GuiScreen or Screen depending on the Minecraft version.
     */
    Object createPolyUIScreen(@NotNull PolyUI polyUI, float designedWidth, float designedHeight, boolean pauses, boolean blurs, @Nullable Consumer<PolyUI> onClose);

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
        try {
            int width = Platform.screen().viewportWidth();
            int height = Platform.screen().viewportHeight();
            ManagedFramebuffer framebuffer = new ManagedFramebuffer(width, height, GpuTexture.TextureFormat.RGBA8, GpuTexture.TextureFormat.DEPTH24_STENCIL8);

            Settings settings = new Settings();
            settings.enableDebugMode(true);
            settings.enableInitCleanup(false);

            PolyUI polyUI = new PolyUI(new Component[0], getRenderer(), settings, 1920f, 1080f);
            polyUI.getMaster().setRawResize(true);
            polyUI.setWindow(createWindow());
            polyUI.resize(Platform.screen().windowWidth(), Platform.screen().windowHeight(), false);

            Drawable master = polyUI.getMaster();
            EventManager.register(HudRenderEvent.class, event -> {
                OmniMatrixStack matrices = event.matrices;
                Platform.screen().setSmuggledMatrixStack(matrices);

                framebuffer.clearColor(0f, 0f, 0f, 0f); // Clear to transparent black
                framebuffer.clearDepthStencil(1.0, 0);
                framebuffer.usingToRender((matrixStack, w, h) -> {
                    matrices.runReplacingGlobalState(polyUI::render);
                    return Unit.INSTANCE;
                });

                float ratio = Platform.screen().pixelRatio();
                float scalingFactor = 1f / (float) OmniResolution.getScaleFactor();
                float scaledWidth = master.getWidth() * scalingFactor * ratio;
                float scaledHeight = master.getHeight() * scalingFactor * ratio;
                framebuffer.drawColorTexture(
                        matrices,
                        0, 0,
                        scaledWidth, scaledHeight,
                        Color.WHITE.getRGB()
                );

                OmniManagedBlendState.disableBlend();
                OmniManagedDepthState.disableDepth();
            });

            EventManager.register(ResizeEvent.class, event -> {
                float ratio = Platform.screen().pixelRatio();
                framebuffer.resize((int) (event.newWidth * ratio), (int) (event.newHeight * ratio));
                polyUI.resize(event.newWidth, event.newHeight, false);
                polyUI.getWindow().setPixelRatio(ratio);
            });

            return polyUI;
        } catch (Throwable t) {
            LogManager.getLogger("OneConfig/UI").error("Failed to load renderer!", t);
            EventManager.register(WorldEvent.Load.class, () -> EventDelay.tick(20, () -> OmniChat.displayClientMessage(new MCSimpleTextHolder("Failed to load the renderer for OneConfig. This means the UI, HUD and Notifications will not work. Please report this to https://discord.gg/polyfrost and attach your log.").withFormatting(MCTextFormat.RED))));
            return null;
        }
    }
}
