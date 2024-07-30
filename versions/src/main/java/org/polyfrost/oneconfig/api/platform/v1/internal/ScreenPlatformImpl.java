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

package org.polyfrost.oneconfig.api.platform.v1.internal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.event.v1.EventDelay;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.platform.v1.ScreenPlatform;

public class ScreenPlatformImpl implements ScreenPlatform {
    //#if MC<11300
    private static final net.minecraft.client.gui.GuiButton btn = new net.minecraft.client.gui.GuiButton(-1, -1, -1, "");
    //#elseif MC>=11904
    //#if FABRIC
    //$$ private static final net.minecraft.client.gui.widget.ButtonWidget btn = new net.minecraft.client.gui.widget.ButtonWidget.Builder(net.minecraft.text.Text.empty(), (b) -> {}).build();
    //#else
    //$$ private static final net.minecraft.client.gui.components.Button btn = new net.minecraft.client.gui.components.Button.Builder(net.minecraft.network.chat.Component.empty(), (b) -> {}).build();
    //#endif
    //#else
    //$$ private static final net.minecraft.client.gui.widget.button.Button btn = new net.minecraft.client.gui.widget.button.Button(-1, -1, -1, -1, null, (b) -> {});
    //#endif

    @Override
    public boolean isInChat() {
        return Platform.screen().current() instanceof GuiChat;
    }

    @Override
    public boolean isInDebug() {
        //@formatter:off
        return Minecraft.getMinecraft()
                //#if MC<12000
                .gameSettings.showDebugInfo;
                //#elseif FABRIC
                //$$ .getDebugHud().shouldShowDebugHud();
                //#else
                //$$ .getDebugOverlay().showDebugScreen();
                //#endif
        //@formatter:on
    }

    @Override
    public void playClickSound() {
        btn.playPressSound(Minecraft.getMinecraft().getSoundHandler());
    }

    @Override
    public int viewportWidth() {
        //#if MC>=11502
        //$$ return Minecraft.getInstance().getMainWindow().getFramebufferWidth();
        //#else
        return Minecraft.getMinecraft().displayWidth;
        //#endif
    }

    @Override
    public int viewportHeight() {
        //#if MC>=11502
        //$$ return Minecraft.getInstance().getMainWindow().getFramebufferHeight();
        //#else
        return Minecraft.getMinecraft().displayHeight;
        //#endif
    }

    @Override
    public int windowWidth() {
        //#if MC>=11502
        //$$ return Minecraft.getInstance().getMainWindow().getWidth();
        //#else
        return Minecraft.getMinecraft().displayWidth;
        //#endif
    }

    @Override
    public int windowHeight() {
        //#if MC>=11502
        //$$ return Minecraft.getInstance().getMainWindow().getHeight();
        //#else
        return Minecraft.getMinecraft().displayHeight;
        //#endif
    }

    @Override
    public void display(@Nullable Object screen, int ticks) {
        if (ticks < 1) Minecraft.getMinecraft().displayGuiScreen((GuiScreen) screen);
        else EventDelay.tick(ticks, () -> Minecraft.getMinecraft().displayGuiScreen((GuiScreen) screen));
    }

    @Override
    @SuppressWarnings("unchecked" /*, reason = "reduces friction between versions" */)
    public <T> @Nullable T current() {
        return (T) Minecraft.getMinecraft().currentScreen;
    }
}
