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

//~ gui_graphics
package org.polyfrost.oneconfig.internal;

import com.mojang.brigadier.Command;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.polyfrost.compose.render.RenderContext;
import org.polyfrost.oneconfig.api.commands.v1.CommandManager;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.InitializationEvent;
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent;
import org.polyfrost.oneconfig.api.event.v1.events.WorldEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import org.polyfrost.oneconfig.api.hud.v1.events.HudEditorToggleEvent;
import org.polyfrost.oneconfig.api.notifications.v1.Notification;
import org.polyfrost.oneconfig.api.notifications.v1.NotificationType;
import org.polyfrost.oneconfig.api.notifications.v1.Notifications;
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsRenderer;
import org.polyfrost.oneconfig.api.platform.v1.ModInfo;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.ui.v1.internal.BlurHandler;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindHelper;
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry;
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource;
import org.polyfrost.oneconfig.internal.ui.api.ThirdPartyModCategories;
import org.polyfrost.oneconfig.internal.ui.compose.McFontService;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import org.polyfrost.oneconfig.internal.ui.compose.impls.HudEditorUIScreen;
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen;
import org.polyfrost.oneconfig.internal.ui.hud.LegacyHudRenderer;
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindProviderRegistry;
import org.polyfrost.oneconfig.internal.ui.keybind.MinecraftKeybindProvider;
import org.polyfrost.oneconfig.test.TestMod_Test;

/**
 * The main class of OneConfig.
 */
//? neoforge
//@net.neoforged.fml.common.Mod("oneconfigv1")
public class OneConfig
        //? fabric
        implements net.fabricmc.api.ClientModInitializer {
    public static final OneConfig INSTANCE = new OneConfig();
    private static final Logger LOGGER = LogManager.getLogger("OneConfig");
    private boolean initialized = false;

    private static void registerCommands() {
        Command<FabricClientCommandSource> executor = (c) -> OneConfig.INSTANCE.openScreen(new OneConfigUIScreen());

        var node = CommandManager.literal("oneconfig")
                .executes(executor)
                .then(CommandManager.literal("delete")
                        .executes((ctx) -> {
                            ctx.getSource()
                                    .sendFeedback(Component.literal(
                                            "Deleted OneConfig UI. Please make a report if you were having issues!"));
                            return 1;
                        })
                )
                .then(CommandManager.literal("hud")
                        .executes((ctx) -> {
                            HudManager.INSTANCE.openEditor();
                            return Command.SINGLE_SUCCESS;
                        })
                ).build();
        CommandManager.INSTANCE.register(node);
        CommandManager.INSTANCE.register(CommandManager.literal("ocfg").executes(executor).redirect(node));
    }

    public static boolean isInChatScreen() {
        //? if >= 26.2 {
        /*return Minecraft.getInstance().gui.screen() instanceof ChatScreen;
        *///?} else {
        return Minecraft.getInstance().screen instanceof ChatScreen;
        //?}
    }

    private static void registerKeybinds() {
        // Supply the open-GUI action to the config-backed OneConfig keybind. The action lives here (not on the
        // keybind itself) because it references platform classes and is lost when the keybind is deserialized.
        OneConfigConfig.setOpenAction(pressed -> {
            if (!pressed) {
                return true;
            }
            if (Platform.screen().current() != null) {
                return true;
            }
            if (Minecraft.getInstance().level == null && !Platform.compatibility().isDevelopment()) {
                return true;
            }
            try {
                Platform.screen().display(new OneConfigUIScreen());
            } catch (Throwable t) {
                //~ if >= 26.2 'gui.getChat' -> 'gui.hud.getChat'
                Minecraft.getInstance().gui.getChat()
                        //~ if >= 26.1 'addMessage' -> 'addClientSystemMessage'
                        .addClientSystemMessage(Component.literal("Failed to open OneConfig UI: " + t.getMessage() + ". Please report this!")
                                .withStyle(
                                        ChatFormatting.RED));
                throw t;
            }
            return true;
        });
    }

    private static void unbindRightShiftMinecraftKeybindsOnFirstRun() {
        if (!ConfigManager.isFirstRun()) return;

        Minecraft minecraft = Minecraft.getInstance();
        String rightShift = InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_RSHIFT).getName();
        int unbound = 0;

        for (KeyMapping keyMapping : minecraft.options.keyMappings) {
            if (rightShift.equals(keyMapping.saveString())) {
                keyMapping.setKey(InputConstants.UNKNOWN);
                unbound++;
            }
        }

        if (unbound > 0) {
            KeyMapping.resetMapping();
            minecraft.options.save();
            LOGGER.info("Unbound {} Minecraft keybind(s) using Right Shift on first launch", unbound);
        }
    }

    public static void render(GuiGraphicsExtractor graphics, float partial) {
        if (!SkiaCtx.INSTANCE.isReady()) {
            return;
        }

        float sw = graphics.guiWidth();
        float sh = graphics.guiHeight();
        // guiWidth()/guiHeight() are already GUI-scaled (== Screen dimensions), do not divide by guiScale again.
        HudManager.guiScreenWidth = sw;
        HudManager.guiScreenHeight = sh;

        // Update HUD visibility state for per-HUD filtering
        HudManager.isDebugScreenVisible = Minecraft.getInstance().getDebugOverlay().showDebugScreen();
        HudManager.isTabListVisible = Minecraft.getInstance().options.keyPlayerList.isDown();
        HudManager.isGuiScreenOpen = Platform.screen().current() != null;

        LegacyHudRenderer.INSTANCE.renderLive(graphics);
        SkiaCtx.INSTANCE.queueHudDraw(() -> {
            var ctx = new RenderContext(SkiaCtx.INSTANCE.getCanvas());
            HudManager.INSTANCE.render(ctx, sw, sh);
        });
        SkiaCtx.INSTANCE.queueNotifDraw(() -> {
            var ctx = new RenderContext(SkiaCtx.INSTANCE.getCanvas());
            NotificationsRenderer.render(ctx, sw, sh);
        });
        // Render Skia HUDs into the offscreen TextureTarget.
        // The mixin blits the texture onto MC's render target afterwards.
        SkiaCtx.INSTANCE.drawNow();
    }

    private static void registerEventHandlers() {
        EventManager.register(InitializationEvent.class, e -> HudManager.INSTANCE.initialize());
        EventManager.register(
                HudEditorToggleEvent.class, e -> {
                    if (e.open) {
                        if (!(Platform.screen().current() instanceof HudEditorUIScreen)) {
                            Platform.screen().display(new HudEditorUIScreen());
                        }
                    } else {
                        if (Platform.screen().current() instanceof HudEditorUIScreen) {
                            Platform.screen().display(null, 0);
                        }
                    }
                });
        // Safety: if another screen replaces the HUD editor without going through HudManager.closeEditor(),
        // reset the editing flag. Null screen opens are ignored because commands close chat before deferred screens open.
        EventManager.register(
                org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent.class, e -> {
                    if (HudManager.INSTANCE.isEditing() && e.getScreen() != null && !(e.getScreen() instanceof HudEditorUIScreen)) {
                        HudManager.INSTANCE.closeEditor();
                    }
                });
        EventManager.register(
                InitializationEvent.class, e -> {
                    ConfigManager.initialize();
                    unbindRightShiftMinecraftKeybindsOnFirstRun();
                    org.polyfrost.oneconfig.api.config.v1.CompatSnapshots.setDispatcher(r -> {
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc != null) mc.execute(r);
                        else r.run();
                    });
                    org.polyfrost.oneconfig.internal.ui.keybind.MinecraftKeybindProfiles.init();
                    ConfigRegistry.INSTANCE.loadFrom(ConfigManager.active(), ConfigSource.OC);
                    org.polyfrost.oneconfig.internal.ui.hud.BuiltinHudRegistrar.register();
                    org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry.INSTANCE.loadFromConfig();
                });
        EventManager.register(WorldEvent.Load.class, e -> showFirstLaunchNotification());
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

    private static Notification firstLaunchToast;
    private static EventHandler<ScreenOpenEvent> firstLaunchScreenWatcher;

    private static void showFirstLaunchNotification() {
        if (!OneConfigConfig.showFirstLaunchMessage) {
            return;
        }
        OneConfigConfig.markFirstLaunchShown();
        String keyName = OneConfigConfig.oneConfigKeybind.displayName();
        String title = Platform.i18n().translateString("oneconfig.notification.first_launch.title");
        String message = Platform.i18n().translateString("oneconfig.notification.first_launch.message", keyName);
        firstLaunchToast = Notifications.send(
                title,
                message,
                NotificationType.INFO,
                Notifications.icon("/assets/oneconfig/brand/oneconfig-icon.svg", 64),
                -1f,
                null,
                () -> Platform.screen().display(new OneConfigUIScreen()));
        firstLaunchScreenWatcher = EventManager.register(
                ScreenOpenEvent.class, e -> {
                    if (e.getScreen() instanceof OneConfigUIScreen) {
                        dismissFirstLaunchToast();
                    }
                });
    }

    private static void dismissFirstLaunchToast() {
        if (firstLaunchToast != null) {
            Notifications.dismiss(firstLaunchToast);
            firstLaunchToast = null;
        }
        if (firstLaunchScreenWatcher != null) {
            EventManager.INSTANCE.unregister(firstLaunchScreenWatcher);
            firstLaunchScreenWatcher = null;
        }
    }

    //? neoforge {
    //static {
    //    INSTANCE.init();
    //}
    //? }
    @Override
    public void onInitializeClient() {
        init();
    }

    private void init() {
        if (initialized) {
            LOGGER.error("Attempted to initialize oneconfig twice! this will be ignored");
            return;
        }

        // To enable RenderDoc, set the following JVM arguments:
        // -Drenderdoc.enabled=true
        // (Windows) -Drenderdoc.path="C:\Program Files\RenderDoc\renderdoc.dll" (or wherever you installed RenderDoc)
        // (Linux)   Ensure that librenderdoc.so is available in your LD_PRELOAD todo?
        //RenderDoc.init();

        if (Boolean.getBoolean("oneconfig.test")) {
            try {
                TestMod_Test.initialize();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        long t1 = System.nanoTime();
        ModInfo self = Platform.compatibility()
                .getMods()
                .stream()
                .filter(it -> "oneconfig".equals(it.getId()))
                .findFirst()
                .orElse(null);
        String v = self == null ? "LOCAL" : self.getVersion();
        LOGGER.info("Loading OneConfig v{}", v);
        BlurHandler.init();
        McFontService.INSTANCE.init();
        ThirdPartyModCategories.INSTANCE.init();
        org.polyfrost.oneconfig.internal.ui.sound.ExternalSounds.INSTANCE.ensureDownloaded();

        KeybindProviderRegistry.INSTANCE.register(MinecraftKeybindProvider.INSTANCE);
        registerKeybinds();
        new OneConfigConfig();
        new ThemeConfig();
        registerCommands();
        registerEventHandlers();
        //? fabric
        org.polyfrost.oneconfig.internal.compat.ModMenuShimLoader.enable();

        initialized = true;
        LOGGER.info("OneConfig initialization took {}ms", (System.nanoTime() - t1) / 1_000_000.0);
    }

    private int openScreen(Screen screen) {
        Platform.screen().display(screen);
        return Command.SINGLE_SUCCESS;
    }
}
