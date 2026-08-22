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
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Scoreboard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.compose.render.RenderContext;
import org.polyfrost.oneconfig.api.commands.v1.CommandManager;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.InitializationEvent;
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading;
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent;
import org.polyfrost.oneconfig.api.event.v1.events.ShutdownEvent;
import org.polyfrost.oneconfig.api.event.v1.events.WorldEvent;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import org.polyfrost.oneconfig.api.hud.v1.events.HudEditorToggleEvent;
import org.polyfrost.oneconfig.api.hypixel.v1.HypixelUtils;
import org.polyfrost.oneconfig.api.notifications.v1.Notification;
import org.polyfrost.oneconfig.api.notifications.v1.NotificationType;
import org.polyfrost.oneconfig.api.notifications.v1.Notifications;
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsRenderer;
import org.polyfrost.oneconfig.api.platform.v1.ModInfo;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
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
import org.polyfrost.oneconfig.internal.ui.keybind.MinecraftKeybindProfiles;
import org.polyfrost.oneconfig.internal.ui.keybind.RightShiftConflicts;
import org.polyfrost.oneconfig.internal.ui.search.SearchCorpus;
import org.polyfrost.oneconfig.test.TestMod_Test;

//? neoforge
//@net.neoforged.fml.common.Mod("oneconfigv1")
public class OneConfig
        //? fabric
        implements net.fabricmc.api.ClientModInitializer {
    public static final OneConfig INSTANCE = new OneConfig();
    private static final Logger LOGGER = LogManager.getLogger("OneConfig");
    private boolean initialized = false;

    private static void registerCommands() {
        Command<FabricClientCommandSource> executor = (c) -> {
            OneConfigUIScreen.openLastSession();
            return Command.SINGLE_SUCCESS;
        };

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

    private static void registerKeybinds() {
        // the action lives here rather than on the keybind because it references platform classes and
        // is lost when the keybind is deserialized
        OneConfigConfig.setOpenAction(pressed -> {
            if (!pressed) {
                return true;
            }
            // the screen may have closed itself on this very press (see notifyKeybindClosedGui) so
            // reopening here would make the keybind look like it does nothing
            if (OneConfigConfig.consumeKeybindClose()) {
                return true;
            }
            Object screen = Platform.screen().current();
            if (screen != null && !(Platform.compatibility().isDevelopment() && screen instanceof TitleScreen)) {
                return true;
            }
            if (Minecraft.getInstance().level == null && !Platform.compatibility().isDevelopment()) {
                return true;
            }
            try {
                OneConfigUIScreen.openLastSession();
            } catch (Throwable t) {
                //~ if >= 26.2 'gui.getChat' -> 'gui.hud.getChat'
                Minecraft.getInstance().gui.hud.getChat()
                        //~ if >= 26.1 'addMessage' -> 'addClientSystemMessage'
                        .addClientSystemMessage(Component.literal("Failed to open OneConfig UI: " + t.getMessage() + ". Please report this!")
                                .withStyle(
                                        ChatFormatting.RED));
                throw t;
            }
            return true;
        });
    }

    /**
     * Mirrors vanilla's tab list visibility check which is more than the keybind being held
     * <p>
     * In a single player world with no other listed players and no LIST scoreboard objective the
     * tab list stays hidden even while the key is down
     */
    private static boolean isTabListVisible() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.keyPlayerList.isDown()) return false;
        if (minecraft.level == null || minecraft.player == null) return false;
        if (!minecraft.isLocalServer()) return true;

        Scoreboard scoreboard = minecraft.level.getScoreboard();
        return minecraft.player.connection.getListedOnlinePlayers().size() > 1
                || scoreboard.getDisplayObjective(DisplaySlot.LIST) != null;
    }

    public static void render(GuiGraphicsExtractor graphics, float partial) {
        if (!SkiaCtx.INSTANCE.isReady()) {
            return;
        }

        float sw = Platform.screen().guiWidth();
        float sh = Platform.screen().guiHeight();
        // guiWidth()/guiHeight() are already GUI-scaled so do not divide by guiScale again
        HudManager.guiScreenWidth = sw;
        HudManager.guiScreenHeight = sh;

        //? if >= 26.2 {
        HudManager.isGuiHidden = Minecraft.getInstance().gui.hud.isHidden();
        //? } else {
        /*HudManager.isGuiHidden = Minecraft.getInstance().options.hideGui;
        *///? }
        HudManager.isDebugScreenVisible = Minecraft.getInstance().getDebugOverlay().showDebugScreen();
        HudManager.isTabListVisible = isTabListVisible();
        HudManager.isGuiScreenOpen = Platform.screen().current() != null;
        HudManager.isChatScreenOpen = Platform.screen().current() instanceof ChatScreen;
        HudManager.inWorld = true;
        HudManager.targetPixelWidth = Platform.screen().viewportWidth();
        HudManager.targetPixelHeight = Platform.screen().viewportHeight();

        //~ if < 1.21.8 '.suppressInGameHudRender' -> '.shouldSuppressInGameHudRender()'
        boolean hudRendersLive = !SkiaCtx.INSTANCE.suppressInGameHudRender;
        if (hudRendersLive || !org.polyfrost.oneconfig.internal.ui.hud.LegacyHudOffscreen.INSTANCE.render()) {
            LegacyHudRenderer.INSTANCE.renderLive(graphics);
        }
        // records the F3 overlay offscreen so Skia can put it above the Compose UI instead of below the
        // blur and it must run every frame regardless of the HUD dirty gate
        org.polyfrost.oneconfig.internal.ui.hud.DebugOverlayOffscreen.INSTANCE.render();
        if (HudManager.INSTANCE.beginFrame(sw, sh)) {
            SkiaCtx.INSTANCE.queueHudDraw(() -> {
                var ctx = new RenderContext(SkiaCtx.INSTANCE.getCanvas());
                HudManager.INSTANCE.render(ctx, sw, sh);
            });
            // renders into the offscreen TextureTarget which the mixin blits onto MC's render target
            SkiaCtx.INSTANCE.drawNow();
        }
    }

    private static void installNotificationRenderer() {
        SkiaCtx.INSTANCE.setNotifRenderer(() -> {
            float sw = Platform.screen().guiWidth();
            float sh = Platform.screen().guiHeight();
            if (sw <= 0f || sh <= 0f) return;
            var ctx = new RenderContext(SkiaCtx.INSTANCE.getCanvas());
            NotificationsRenderer.render(ctx, sw, sh);
        });
    }

    private static void registerEventHandlers() {
        EventManager.register(ShutdownEvent.class, e -> MinecraftKeybindProfiles.shutdown());
        EventManager.register(InitializationEvent.class, e -> {
            HudManager.INSTANCE.setProfileReloadDispatcher(r -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && !mc.isSameThread()) mc.execute(r);
                else r.run();
            });
            HudManager.INSTANCE.initialize();
        });
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
        // resets the editing flag if another screen replaces the HUD editor without going through
        // HudManager.closeEditor() and null opens are ignored because commands close chat first
        EventManager.register(
                ScreenOpenEvent.class, e -> {
                    if (HudManager.INSTANCE.isEditorOpen() && e.getScreen() != null && !(e.getScreen() instanceof HudEditorUIScreen)) {
                        HudManager.INSTANCE.closeEditor();
                    }
                });
        EventManager.register(
                InitializationEvent.class, e -> {
                    ConfigManager.initialize();
                    RightShiftConflicts.unbindMinecraftKeybinds();
                    org.polyfrost.oneconfig.api.config.v1.CompatSnapshots.setDispatcher(r -> {
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc != null && !mc.isSameThread()) mc.execute(r);
                        else r.run();
                    });
                    MinecraftKeybindProfiles.init();
                    ConfigRegistry.INSTANCE.loadFrom(ConfigManager.active(), ConfigSource.OC);
                    org.polyfrost.oneconfig.internal.ui.hud.BuiltinHudRegistrar.register();
                    org.polyfrost.oneconfig.internal.compat.FirmamentHudCompat.register();
                    //? if wwaypoints_compat
                    org.polyfrost.oneconfig.internal.compat.WWaypointsCompat.register();
                    org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry.INSTANCE.loadFromConfig();
                });
        EventManager.register(WorldEvent.Load.class, e -> showFirstLaunchNotification());
        // after loading finishes so translation keys are available
        EventManager.register(ResourceFinishedLoading.class, e -> SearchCorpus.INSTANCE.init());
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
                OneConfigUIScreen::openLastSession);
    }

    public static void dismissFirstLaunchToast() {
        if (firstLaunchToast != null) {
            Notifications.dismiss(firstLaunchToast);
            firstLaunchToast = null;
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

        SkikoDataPath.redirect();

        // to enable RenderDoc set these JVM arguments
        // -Drenderdoc.enabled=true
        // (Windows) -Drenderdoc.path="C:\Program Files\RenderDoc\renderdoc.dll" (or wherever you installed RenderDoc)
        // (Linux) ensure librenderdoc.so is available in your LD_PRELOAD (TODO)
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
        McFontService.INSTANCE.init();
        ThirdPartyModCategories.INSTANCE.init();
        // force class-load so the hello/disconnect handlers are armed before joining any server
        HypixelUtils.isHypixel();
        org.polyfrost.oneconfig.internal.ui.sound.ExternalSounds.INSTANCE.ensureDownloaded();

        KeybindProviderRegistry.INSTANCE.register(MinecraftKeybindProvider.INSTANCE);
        registerKeybinds();
        new OneConfigConfig();
        new ThemeConfig();
        registerCommands();
        registerEventHandlers();
        installNotificationRenderer();
        MainMenuFpsSampler.init();
        //? fabric
        org.polyfrost.oneconfig.internal.compat.ModMenuShimLoader.enable();
        org.polyfrost.oneconfig.internal.compat.KaleidoCompat.enable();

        initialized = true;
        LOGGER.info("OneConfig initialization took {}ms", (System.nanoTime() - t1) / 1_000_000.0);
    }

}
