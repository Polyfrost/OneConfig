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

package org.polyfrost.oneconfig.internal;

import org.polyfrost.oneconfig.api.commands.CommandManager;
import org.polyfrost.oneconfig.api.events.EventManager;
import org.polyfrost.oneconfig.internal.command.OneConfigCommand;
import org.polyfrost.oneconfig.internal.ui.BlurHandler;
import org.polyfrost.oneconfig.ui.KeybindManager;
import org.polyfrost.oneconfig.ui.LwjglManager;
import org.polyfrost.oneconfig.ui.OneConfigUI;
import org.polyfrost.oneconfig.utils.GuiUtils;
import org.polyfrost.oneconfig.utils.hypixel.HypixelUtils;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.component.impl.Button;
import org.polyfrost.polyui.input.KeyBinder;
import org.polyfrost.polyui.input.Keys;
import org.polyfrost.polyui.input.Modifiers;
import org.polyfrost.polyui.layout.impl.FlexLayout;
import org.polyfrost.polyui.property.impl.BlockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class of OneConfig.
 */
//#if FORGE==1
//#if MC<=11202
@net.minecraftforge.fml.common.Mod(modid = "@ID@", name = "@NAME@", version = "@VER@")
//#else
//$$ @net.minecraftforge.fml.common.Mod("@ID@")
//#endif
//#endif
public class OneConfig {
    public static final Logger LOGGER = LoggerFactory.getLogger("@NAME@");
    public static final OneConfig INSTANCE = new OneConfig();
    private static boolean initialized = false;

    public OneConfig() {
    }

    /**
     * Called after mods are loaded.
     * <p><b>SHOULD NOT BE CALLED!</b></p>
     */
    public void init() {
        if (initialized) return;
        try {
            EventManager.INSTANCE.register(BlurHandler.INSTANCE);
        } catch (Exception e) {
            LOGGER.error("Failed to register blur handler", e);
        }
        preload();
        CommandManager.registerCommand(new OneConfigCommand());
        KeybindManager.registerKeybind(new KeyBinder.Bind((int[]) null, null, (int[]) null, Modifiers.RSHIFT.getValue(), 0L, () -> {
            GuiUtils.displayScreen(OneConfigUI.create());
            return true;
        }));
        HypixelUtils.INSTANCE.initialize();

        initialized = true;
        LOGGER.info("OneConfig initialized!");
    }

    /**
     * Ensure that key PolyUI classes are loaded to prevent lag-spikes when loading PolyUI for the first time.
     */
    private void preload() {
        long t1 = System.nanoTime();
        try {
            Class.forName(PolyUI.class.getName());
            Class.forName(Button.class.getName());
            Class.forName(FlexLayout.class.getName());
            Class.forName(BlockProperties.class.getName());
            LwjglManager.INSTANCE.getRenderer(100f, 100f);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to preload necessary PolyUI classes", e);
        }
        LOGGER.info("PolyUI preload took {}ms", (System.nanoTime() - t1) / 1_000_000.0);
    }
}
