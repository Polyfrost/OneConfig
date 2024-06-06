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

package org.polyfrost.oneconfig.internal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.commands.v1.CommandManager;
import org.polyfrost.oneconfig.api.commands.v1.factories.builder.CommandBuilder;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.InitializationEvent;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.ui.v1.UIManager;
import org.polyfrost.oneconfig.api.ui.v1.internal.BlurHandler;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindHelper;
import org.polyfrost.oneconfig.internal.ui.OneConfigUI;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.component.Drawable;
import org.polyfrost.polyui.input.KeyModifiers;
import org.polyfrost.polyui.input.Translator;

import static org.polyfrost.oneconfig.api.commands.v1.factories.builder.CommandBuilder.runs;

/**
 * The main class of OneConfig.
 */
//#if FORGE
//#if MC<=11202
@net.minecraftforge.fml.common.Mod(modid = "oneconfig")
//#else
//$$ @net.minecraftforge.fml.common.Mod("oneconfig")
//#endif
//#endif
public class OneConfig
        //#if FABRIC
        //$$ implements net.fabricmc.api.ClientModInitializer
        //#endif
{
    public static final OneConfig INSTANCE = new OneConfig();
    private static final Logger LOGGER = LogManager.getLogger("OneConfig");

    //#if FORGE
    //#if MC<=11202
    @net.minecraftforge.fml.common.Mod.EventHandler
    private void onInit(net.minecraftforge.fml.common.event.FMLPostInitializationEvent ev) {
        init();
    }
    //#else
    //$$ static {
    //$$     INSTANCE.init();
    //$$ }
    //#endif
    //#else
    //$$ @Override
    //$$ public void onInitializeClient() {
    //$$     init();
    //$$ }
    //#endif


    private void init() {
        BlurHandler.init();
        preload();
        CommandBuilder b = CommandManager.builder("oneconfig", "ocfg", "ocfgv1").description("OneConfig main command");
        b.then(runs().does(OneConfigUI.INSTANCE::open).description("Opens the OneConfig GUI"));
        b.then(runs("hud").does(() -> Platform.screen().display(HudManager.INSTANCE.getWithEditor())).description("Opens the OneConfig HUD editor"));
        CommandManager.registerCommand(b);
        KeybindHelper.builder().mods(KeyModifiers.RSHIFT).does(OneConfigUI.INSTANCE::open).register();
        EventManager.INSTANCE.post(InitializationEvent.INSTANCE);
        LOGGER.info("OneConfig initialized!");
    }

    /**
     * Ensure that key PolyUI classes are loaded to prevent lag-spikes when loading PolyUI for the first time.
     */
    private static void preload() {
        long t1 = System.nanoTime();
        try {
            Class.forName(PolyUI.class.getName());
            Class.forName(Drawable.class.getName());
            Class.forName(Translator.class.getName());
            UIManager.INSTANCE.getRenderer();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to preload necessary PolyUI classes", e);
        }
        LOGGER.info("PolyUI preload took {}ms", (System.nanoTime() - t1) / 1_000_000.0);
    }
}
