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

import org.polyfrost.universal.UMinecraft;
import org.polyfrost.universal.UScreen;
import org.polyfrost.oneconfig.api.platform.v1.GuiPlatform;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
//#if MC<=11202
import net.minecraft.client.gui.GuiButton;
//#elseif MC<=11605
//$$ import net.minecraft.client.gui.widget.button.Button;
//#endif

public class GuiPlatformImpl implements GuiPlatform {

    @Override
    public GuiScreen getCurrentScreen() {
        return UScreen.getCurrentScreen();
    }

    @Override
    public void setCurrentScreen(Object screen) {
        UScreen.displayScreen((GuiScreen) screen);
    }

    @Override
    public boolean isInChat() {
        return getCurrentScreen() instanceof GuiChat;
    }

    @Override
    public boolean isInDebug() {
        //#if MC<12000
        return UMinecraft.getSettings().showDebugInfo;
        //#else
        //$$ return UMinecraft.getMinecraft()
        //#if FABRIC
        //$$    .getDebugHud().shouldShowDebugHud();
        //#else
        //$$    .getDebugOverlay().showDebugScreen();
        //#endif
        //#endif
    }

    @Override
    public void playClickSound() {
        //#if MC<=11202
        new GuiButton(-1, -1, -1, "")
                .playPressSound(UMinecraft.getMinecraft().getSoundHandler());
        //#elseif MC<=11605
        //$$ new Button(-1, -1, -1, -1, null, (button) -> {})
        //$$         .playDownSound(UMinecraft.getMinecraft().getSoundHandler());
        //#endif
    }
}
