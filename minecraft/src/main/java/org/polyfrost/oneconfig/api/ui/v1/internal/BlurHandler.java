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

package org.polyfrost.oneconfig.api.ui.v1.internal;

import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler;

/**
 * An adapted and optimized implementation of the BlurMC mod by tterrag1098, later modified by boomboompower.
 * <p>
 * For the original source see <a href="https://github.com/tterrag1098/Blur/blob/1.8.9/src/main/java/com/tterrag/blur/Blur.java">here.</a>
 * taken the <a href="https://github.com/tterrag1098/Blur/blob/1.8.9/LICENSE">MIT Licence.</a>
 * <p>
 * Modifications based on source from ToggleChat. See <a href="https://github.com/boomboompower/ToggleChat/blob/master/LICENSE">here</a> for that licence.
 *
 * @author tterrag1098, boomboompower, nextday
 */
public final class BlurHandler {
    public static final BlurHandler INSTANCE = new BlurHandler();
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Blur");
    private final Identifier blurShader = Identifier.parse("shaders/post/fade_in_blur.json");

    private BlurHandler() {
        EventHandler.ofRemoving(ScreenOpenEvent.class, e -> reloadBlur(e.getScreen())).register();
    }

    public static void init() {
        // will call <clinit>
    }

    public static boolean isBlurring() {
        return false;
    }

    /**
     * Activates/deactivates the blur in the current world if
     * one of many conditions are met, such as no current other shader
     * is being used, we actually have the blur setting enabled
     */
    private boolean reloadBlur(Object gui) {
        return false; // TODO: Fix shader usage in 1.21.1+
    }

    private void tryStop() {
        // no-op: shader API not available in 1.21.1+
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isShaderActive() {
        return getShaderGroup() != null;
    }

    private Object getShaderGroup() {
        return null;
    }
}
