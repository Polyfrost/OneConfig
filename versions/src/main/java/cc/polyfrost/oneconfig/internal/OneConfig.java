/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
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

package cc.polyfrost.oneconfig.internal;

import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.ShutdownEvent;
import cc.polyfrost.oneconfig.internal.command.OneConfigCommand;
import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.internal.config.Preferences;
import cc.polyfrost.oneconfig.internal.config.compatibility.forge.ForgeCompat;
import cc.polyfrost.oneconfig.internal.config.core.ConfigCore;
import cc.polyfrost.oneconfig.internal.config.core.KeyBindHandler;
import cc.polyfrost.oneconfig.internal.gui.BlurHandler;
import cc.polyfrost.oneconfig.internal.hud.HudCore;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.utils.commands.CommandManager;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;
import cc.polyfrost.oneconfig.utils.hypixel.HypixelUtils;
import cc.polyfrost.oneconfig.utils.Notifications;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//#if FORGE==1
import net.minecraftforge.fml.common.ModContainer;
//#endif

//#if MC<=11202
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.common.Loader;
//#endif

import java.util.Map;

/**
 * The main class of OneConfig.
 */
//#if MC<=11202
@net.minecraftforge.fml.common.Mod(modid = "@ID@", name = "@NAME@", version = "@VER@")
//#else
//#if FORGE==1
//$$ @net.minecraftforge.fml.common.Mod("@ID@")
//#endif
//#endif
public class OneConfig {

    public OneConfig() {
        EventManager.INSTANCE.register(this);
    }

    public static final Logger LOGGER = LogManager.getLogger("@NAME@");
    private static boolean initialized = false;

    /**
     * Called after mods are loaded.
     * <p><b>SHOULD NOT BE CALLED!</b></p>
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void init() {
        if (initialized) return;
        if (OneConfigConfig.getInstance() == null) {
            OneConfigConfig.getInstance();
        }
        if (Preferences.getInstance() == null) {
            Preferences.getInstance();
        }
        //#if FORGE==1
            //#if MC<=11202
            for (ModContainer mod : Loader.instance().getActiveModList()) {

                IModGuiFactory factory = FMLClientHandler.instance().getGuiFactoryFor(mod);
                //#if MC<=10809
                if (factory == null || factory.mainConfigGuiClass() == null) continue;
                //#else
                //$$ if (factory == null || !factory.hasConfigGui()) continue;
                //#endif
                ForgeCompat.compatMods.put(new ForgeCompat.ForgeCompatMod(mod.getName(), ModType.THIRD_PARTY), () -> {
                    try {
                        GuiUtils.displayScreen(
                                //#if MC<=10809
                                factory.mainConfigGuiClass().getConstructor(GuiScreen.class).newInstance(Minecraft.getMinecraft().currentScreen)
                                //#else
                                //$$ factory.createConfigGui(Minecraft.getMinecraft().currentScreen)
                                //#endif
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            //#else
            //$$ try {
            //$$     java.lang.reflect.Field mods = net.minecraftforge.fml.ModList.class.getDeclaredField("mods");
            //$$     mods.setAccessible(true);
            //$$     for (ModContainer container : ((java.util.List<ModContainer>) mods.get(net.minecraftforge.fml.ModList.get()))) {
            //$$         try {
            //$$             java.util.Optional<java.util.function.BiFunction<Minecraft, net.minecraft.client.gui.screen.Screen, net.minecraft.client.gui.screen.Screen>> gui = container.getCustomExtension(net.minecraftforge.fml.ExtensionPoint.CONFIGGUIFACTORY);
            //$$             gui.ifPresent(minecraftScreenScreenBiFunction -> ForgeCompat.compatMods.put(new ForgeCompat.ForgeCompatMod(container.getModId(), ModType.THIRD_PARTY), () -> {
            //$$                 net.minecraft.client.gui.screen.Screen screen = minecraftScreenScreenBiFunction.apply(Minecraft.getInstance(), Minecraft.getInstance().currentScreen);
            //$$                 if (screen != null) {
            //$$                     GuiUtils.displayScreen(screen);
            //$$                 }
            //$$             }));
            //$$         } catch (Exception e) {
            //$$             e.printStackTrace();
            //$$         }
            //$$     }
            //$$ } catch (Exception e) {
            //$$     e.printStackTrace();
            //$$ }
            //#endif

        for (Map.Entry<Mod, Runnable> entry : ForgeCompat.compatMods.entrySet()) {
            ConfigCore.mods.add(entry.getKey());
        }
        //#endif
        GuiUtils.getDeltaTime(); // called to make sure static initializer is called
        try {
            EventManager.INSTANCE.register(BlurHandler.INSTANCE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        CommandManager.INSTANCE.registerCommand(new OneConfigCommand());
        EventManager.INSTANCE.register(new HudCore());
        HypixelUtils.INSTANCE.initialize();
        EventManager.INSTANCE.register(KeyBindHandler.INSTANCE);
        EventManager.INSTANCE.register(Notifications.INSTANCE);
        ConfigCore.sortMods();

        initialized = true;
    }

    @Subscribe
    private void onShutdown(ShutdownEvent event) {
        ConfigCore.saveAll();
    }
}
