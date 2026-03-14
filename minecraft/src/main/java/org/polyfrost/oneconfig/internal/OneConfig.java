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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.deftu.clipboard.Clipboard;
import dev.deftu.omnicore.api.client.OmniClient;
import dev.deftu.omnicore.api.client.chat.OmniClientChat;
import dev.deftu.omnicore.api.client.commands.OmniClientCommandSource;
import dev.deftu.omnicore.api.client.commands.OmniClientCommands;
import dev.deftu.omnicore.api.client.input.OmniKeys;
import dev.deftu.omnicore.api.client.screen.OmniScreens;
import dev.deftu.omnicore.api.loader.ModInfo;
import dev.deftu.omnicore.api.loader.OmniLoader;
import dev.deftu.omnicore.internal.client.commands.ClientCommandInternals;
import dev.deftu.textile.Text;
import dev.deftu.textile.minecraft.MCTextStyle;
import dev.deftu.textile.minecraft.TextColors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.compose.render.RenderContext;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.HudRenderEvent;
import org.polyfrost.oneconfig.api.event.v1.events.InitializationEvent;
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent;
import org.polyfrost.oneconfig.api.event.v1.events.WindowFocusEvent;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import org.polyfrost.oneconfig.api.hypixel.v1.HypixelUtils;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.ui.v1.internal.BlurHandler;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindHelper;
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry;
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource;
import org.polyfrost.oneconfig.internal.ui.compose.McFontService;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen;
import org.polyfrost.oneconfig.test.TestMod_Test;

//#if MC > 1.16
//$$ import com.mojang.brigadier.arguments.ArgumentType;
//$$ import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
//$$ import net.minecraft.commands.synchronization.ArgumentSerializer;
//$$ // import org.polyfrost.oneconfig.internal.mixin.command.Mixin_ModernArgumentTypeEntryAccessor;
//$$ import org.polyfrost.oneconfig.internal.mixin.command.Mixin_ModernArgumentTypesAccessor;
//$$ import java.util.Map;
//#endif

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The main class of OneConfig.
 */
//#if FORGE-LIKE
//#if NEOFORGE
//$$ @net.neoforged.fml.common.Mod("oneconfigv1")
//#else
//$$ @net.minecraftforge.fml.common.Mod("oneconfigv1")
//#endif
//#endif
public class OneConfig
        //#if FABRIC
        implements net.fabricmc.api.ClientModInitializer
        //#endif
{
    public static final OneConfig INSTANCE = new OneConfig();
    private static final Logger LOGGER = LogManager.getLogger("OneConfig");
    private boolean initialized = false;

    //#if FORGE-LIKE
    //#if MC <= 1.12.2
    //$$@net.minecraftforge.fml.common.Mod.EventHandler
    //$$private void onInit(net.minecraftforge.fml.common.event.FMLPostInitializationEvent ev) {
    //$$    init();
    //$$}
    //#else
    //$$ static {
    //$$     INSTANCE.init();
    //$$ }
    //#endif
    //#else
    @Override
    public void onInitializeClient() {
        init();
    }
    //#endif


    private void init() {
        if (initialized) {
            LOGGER.error("Attempted to initialize oneconfig twice! this will be ignored");
            return;
        }

        // To enable RenderDoc, set the following JVM arguments:
        // -Drenderdoc.enabled=true
        // (Windows) -Drenderdoc.path="C:\Program Files\RenderDoc\renderdoc.dll" (or wherever you installed RenderDoc)
        // (Linux)   Ensure that librenderdoc.so is available in your LD_PRELOAD
        //#if MC >= 1.19.2
        //$$ RenderDoc.init();
        //#endif

//        if (Boolean.getBoolean("oneconfig.test")) {
            try {
                TestMod_Test.initialize();
            } catch (Throwable e) {
                e.printStackTrace();
            }
//        }
        
        long t1 = System.nanoTime();
        ModInfo self = OmniLoader.findModOrNull("oneconfig");
        String v = self == null ? "LOCAL" : self.getVersion();
        LOGGER.info("Loading OneConfig v{}", v);
        BlurHandler.init();
        McFontService.INSTANCE.init();

        preloadCopycat();

        new OneConfigConfig();
        registerCommands();
        registerKeybinds();
        registerEventHandlers();

        initialized = true;
        LOGGER.info("OneConfig initialization took {}ms", (System.nanoTime() - t1) / 1_000_000.0);
    }

    @SuppressWarnings({"unchecked", "rawtypes", "UnstableApiUsage"})
    private static void registerCommands() {
        ClientCommandInternals.initialize();
        // //#if MC > 1.16
        // //#if MC > 1.19
        // // todo still broken on 1.20+
        // //$$ net.minecraft.commands.CommandBuildContext cmdCtx = new net.minecraft.commands.CommandBuildContext(net.minecraft.core.RegistryAccess.BUILTIN.get());
        // //#endif
        // //$$ for (Map.Entry<Class<?>, Object> entry : Mixin_ModernArgumentTypesAccessor.getArgumentTypes().entrySet()) {
        // //$$     ArgumentSerializer serializer = ((Mixin_ModernArgumentTypeEntryAccessor) entry.getValue()).getSerializer();
        // //$$     if (serializer instanceof EmptyArgumentSerializer<?>) {
        // //$$         CommandManager.INSTANCE.registerArgumentType(
        // //$$                 (Class) entry.getKey(),
        // //$$                 ((EmptyArgumentSerializer<ArgumentType<?>>) serializer).deserializeFromNetwork(null)
        // //$$                                //#if MC > 1.19
        // //$$                                //$$ .instantiate(cmdCtx)
        // //$$                                //#endif
        // //$$         );
        // //$$     }
        // //$$ }
        // //#endif

        Command<OmniClientCommandSource> executor = (ctx) -> ctx.getSource().openScreen(new OneConfigUIScreen());

        LiteralCommandNode<OmniClientCommandSource> node = OmniClientCommands.literal("oneconfig")
                .executes(executor)
                .then(OmniClientCommands.literal("delete")
                        .executes((ctx) -> {
                            return ctx.getSource().replyChat("Deleted OneConfig UI. Please make a report if you were having issues!");
                        })
                )
                .then(OmniClientCommands.literal("hud")
                        .executes((ctx) -> { HudManager.INSTANCE.toggleEditor(); return Command.SINGLE_SUCCESS; })
                )
                .then(OmniClientCommands.literal("locraw")
                        .executes((ctx) -> ctx.getSource().replyChat(HypixelUtils.getLocation().toString()))
                ).build();
        OmniClientCommands.register(node);
        OmniClientCommands.register(OmniClientCommands.literal("ocfg").executes(executor).redirect(node));
    }

    private static void registerKeybinds() {
        KeybindHelper.builder()
            .inScreens()
            .key(OmniKeys.KEY_RIGHT_SHIFT)
            .action(() -> {
                if (OmniClient.getWorld() == null && !OmniLoader.isDevelopment()) return;
                if (OmniScreens.isInChatScreen()) return;
                try {
                    Platform.screen().display(new OneConfigUIScreen());
                } catch (Throwable t) {
                    OmniClientChat.displayChatMessage(Text.literal("Failed to open OneConfig UI: " + t.getMessage() + ". Please report this!").setStyle(MCTextStyle.color(TextColors.RED)));
                    throw t;
                }
            })
            .register();
    }

    private static void registerEventHandlers() {
        EventManager.register(InitializationEvent.class, e -> HudManager.INSTANCE.initialize());
        EventManager.register(HudRenderEvent.class, e -> {
            if (!SkiaCtx.INSTANCE.isReady()) return;
            SkiaCtx.INSTANCE.queueDraw((Runnable) () -> {
                var ctx = new RenderContext(SkiaCtx.INSTANCE.getCanvas());
                HudManager.INSTANCE.render(ctx, OmniClient.getWindow().getScreenWidth(), OmniClient.getWindow().getScreenHeight());
            });
        });
        EventManager.register(InitializationEvent.class, e -> {
            ConfigManager.initialize();
            ConfigRegistry.INSTANCE.loadFrom(ConfigManager.active(), ConfigSource.OC);
        });
//        //#if MC < 1.13
//        // this is cringe but is better than the alternative of checking every frame in a mixin (that's how vanilla does it lol)
//        AtomicBoolean active = new AtomicBoolean(false);
//        EventManager.register(TickEvent.End.class, e -> {
//            boolean current = org.lwjgl.opengl.Display.isActive();
//            if (current != active.get()) {
//                active.set(current);
//                if (current) EventManager.INSTANCE.post(WindowFocusEvent.Gained.INSTANCE);
//                else EventManager.INSTANCE.post(WindowFocusEvent.Lost.INSTANCE);
//            }
//        });
//        //#endif
    }

    private static void preloadCopycat() {
        long t1 = System.nanoTime();
        try {
            // Copycat
            Clipboard.getInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to preload necessary Copycat classes", e);
        }

        LOGGER.info("  -> Copycat preload took {}ms", (System.nanoTime() - t1) / 1_000_000.0);
    }
}
