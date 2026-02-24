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

package cc.polyfrost.oneconfig.internal;

import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.internal.command.OneConfigCommand;
import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.internal.config.Preferences;
import cc.polyfrost.oneconfig.internal.config.compatibility.forge.ForgeCompat;
import cc.polyfrost.oneconfig.internal.config.core.ConfigCore;
import cc.polyfrost.oneconfig.internal.config.core.KeyBindHandler;
import cc.polyfrost.oneconfig.internal.gui.BlurHandler;
import cc.polyfrost.oneconfig.internal.hud.HudCore;
import cc.polyfrost.oneconfig.libs.universal.ChatColor;
import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.libs.universal.UScreen;
import cc.polyfrost.oneconfig.libs.universal.wrappers.UPlayer;
import cc.polyfrost.oneconfig.utils.TickDelay;
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
import cc.polyfrost.oneconfig.utils.IgnoredGuiFactory;
//#endif

//#if MC<=11202
//#if FORGE==1
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModMetadata;
//#endif
//#endif

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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

    public static final OneConfig INSTANCE = new OneConfig();

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
    public void init() {
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
                if (mod == null) continue;
                try {
                    boolean shouldAttempt = true;
                    for (Mod configMod : ConfigCore.mods) {
                        final String configModName = configMod.name.toLowerCase(Locale.ENGLISH);
                        final String spacelessConfigModName = configModName.replace(" ", "");
                        if (Objects.equals(configModName, mod.getName().toLowerCase(Locale.ENGLISH)) || Objects.equals(configModName, mod.getModId()) ||
                                Objects.equals(spacelessConfigModName, mod.getName().toLowerCase(Locale.ENGLISH)) || Objects.equals(spacelessConfigModName, mod.getModId())) {
                            shouldAttempt = false;
                        }
                    }

                    for (ArtifactVersion dependency : mod.getDependencies()) {
                        if (Objects.equals("oneconfig", dependency.getLabel())) {
                            shouldAttempt = false;
                        }
                    }
                    if (shouldAttempt) {
                        if (!handleForgeGui(mod)) {
                            handleForgeCommand(mod);
                        }
                    }
                } catch (Throwable t) {
                    LOGGER.error("Failed to handle Forge compatibility for {}", mod.getModId(), t);
                }
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
        // called to make sure static initializer is called
        Notifications.INSTANCE.getClass();
        EventManager.INSTANCE.register(Notifications.INSTANCE);
        GuiUtils.getDeltaTime();
        try {
            EventManager.INSTANCE.register(BlurHandler.INSTANCE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        CommandManager.INSTANCE.registerCommand(new OneConfigCommand());
        EventManager.INSTANCE.register(new HudCore());
        HypixelUtils.INSTANCE.initialize();
        EventManager.INSTANCE.register(KeyBindHandler.INSTANCE);
        ConfigCore.sortMods();

        initialized = true;
    }

    //#if FORGE==1 && MC<=11202

    private void handleForgeCommand(ModContainer mod) {
        for (net.minecraft.command.ICommand command : net.minecraftforge.client.ClientCommandHandler.instance.getCommands().values()) {
            if (Objects.equals(command.getCommandName(), mod.getModId())) {
                for (String alias : command.getCommandAliases()) {
                    for (Mod configMod : ConfigCore.mods) {
                        if (Objects.equals(configMod.name.toLowerCase(Locale.ENGLISH).replace(" ", ""), alias.toLowerCase(Locale.ENGLISH))) {
                            return;
                        }
                    }
                }
                ForgeCompat.compatMods.put(new ForgeCompat.ForgeCompatMod(mod.getName(), ModType.THIRD_PARTY), () -> new TickDelay(() -> {
                    UScreen.displayScreen(null);
                    try {
                        //#if MC<=10809
                        command.processCommand(UPlayer.getPlayer(), new String[]{});
                        //#else
                        //$$ command.execute(net.minecraft.client.Minecraft.getMinecraft().getIntegratedServer(), UPlayer.getPlayer(), new String[]{});
                        //#endif
                    } catch (Exception e) {
                        UChat.chat(ChatColor.RED + "Forge command compat has failed! Please report this to Polyfrost at https://polyfrost.cc/discord");
                    }
                }, 1));
                return;
            }
        }
    }

    private boolean handleForgeGui(ModContainer mod) {
        if ("fml".equalsIgnoreCase(mod.getModId())) {
            return false;
        }

        IModGuiFactory factory = FMLClientHandler.instance().getGuiFactoryFor(mod);
        //#if MC<=10809
        if (factory == null || factory.mainConfigGuiClass() == null) return false;
        //#else
        //$$ if (factory == null || !factory.hasConfigGui()) return false;
        //#endif
        if (IgnoredGuiFactory.class.isAssignableFrom(factory.getClass())) return false;

        boolean isForgeContainer = "forge".equalsIgnoreCase(mod.getModId());
        ModMetadata metadata = mod.getMetadata();
        String modLogoFile =
                metadata.logoFile == null || metadata.logoFile.isEmpty()
                        ? null
                        : metadata.logoFile;
        if (modLogoFile != null && !isForgeContainer) {
            if (!modLogoFile.startsWith("assets/")) {
                modLogoFile = "/assets/" + modLogoFile;
            }
            //noinspection HttpUrlsUsage
            if (!modLogoFile.startsWith("/") && !modLogoFile.startsWith("http://") && !modLogoFile.startsWith("https://")) {
                modLogoFile = "/" + modLogoFile;
            }

            if (OneConfig.class.getResource(modLogoFile) == null) {
                LOGGER.warn("Mod '{}' has an invalid logo file: {}", mod.getName(), modLogoFile);
                modLogoFile = null;
            }
        }

        String icon = isForgeContainer
                ? "/assets/oneconfig/icons/forge_logo.png"
                : modLogoFile;

        ForgeCompat.compatMods.put(new ForgeCompat.ForgeCompatMod(mod.getName(), ModType.THIRD_PARTY, icon), () -> {
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
        return true;
    }
    //#endif
}
