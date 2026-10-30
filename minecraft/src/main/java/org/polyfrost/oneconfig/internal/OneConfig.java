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
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Scoreboard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.commands.v1.CommandManager;
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots;
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
import org.polyfrost.oneconfig.api.notifications.v1.Notifications;
import org.polyfrost.oneconfig.api.platform.v1.ModInfo;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.internal.compat.CompatLoader;
import org.polyfrost.oneconfig.internal.compat.ConfigEntrypoints;
import org.polyfrost.oneconfig.internal.compat.KaleidoCompat;
import org.polyfrost.oneconfig.internal.compat.toggle.ModGates;
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry;
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource;
import org.polyfrost.oneconfig.internal.ui.api.ThirdPartyModCategories;
import org.polyfrost.oneconfig.internal.ui.components.item.ItemCatalog;
import org.polyfrost.oneconfig.internal.ui.compose.McFontService;
//? if >= 1.21.11
import org.polyfrost.oneconfig.internal.ui.compose.BlazeHud;
import org.polyfrost.oneconfig.internal.ui.compose.GlHost;
import org.polyfrost.oneconfig.internal.ui.compose.impls.HudEditorUIScreen;
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen;
import org.polyfrost.oneconfig.internal.ui.hud.BuiltinHudRegistrar;
import org.polyfrost.oneconfig.internal.ui.hud.LegacyHudRenderer;
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindConflicts;
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindProviderRegistry;
import org.polyfrost.oneconfig.internal.ui.keybind.MinecraftKeybindProfiles;
import org.polyfrost.oneconfig.internal.ui.keybind.ProfileKeybinds;
import org.polyfrost.oneconfig.internal.ui.keybind.MinecraftKeybindProvider;
import org.polyfrost.oneconfig.internal.ui.navigation.BuiltinPages;
import org.polyfrost.oneconfig.internal.ui.navigation.graph.CreditsGraph;
import org.polyfrost.oneconfig.internal.ui.navigation.graph.KeybindsGraph;
import org.polyfrost.oneconfig.internal.ui.search.SearchCorpus;
import org.polyfrost.oneconfig.internal.ui.sound.ExternalSounds;
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry;
import org.polyfrost.oneconfig.test.TestMod_Test;

//? if > 1.8.9 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.scores.DisplaySlot;
import org.polyfrost.oneconfig.internal.compat.ArmorHudCompat;
import org.polyfrost.oneconfig.internal.compat.FirmamentHudCompat;
//?}

//? if fabric {
import org.polyfrost.oneconfig.internal.compat.ModMenuShimLoader;
//?}

//? if < 1.21.11 {
/*import kotlin.Unit;
*///?}

//? if wwaypoints_compat {
import org.polyfrost.oneconfig.internal.compat.WWaypointsCompat;
//?}

//? neoforge
//@net.neoforged.fml.common.Mod("oneconfigv1")
public class OneConfig
        //? fabric || ornithe
        implements ClientModInitializer {
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
                )
                .then(CommandManager.literal("credits")
                        .executes((ctx) -> {
                            Platform.screen().display(OneConfigUIScreen.forRoute(CreditsGraph.INSTANCE));
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(CommandManager.literal("keybinds")
                        .executes((ctx) -> {
                            Platform.screen().display(OneConfigUIScreen.forRoute(KeybindsGraph.INSTANCE));
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .build();
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
    private static boolean isHudEditor(Object screen) {
        return screen instanceof HudEditorUIScreen;
    }

    private static boolean isTabListVisible() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.keyPlayerList.isDown()) return false;
        if (minecraft.level == null || minecraft.player == null) return false;
        if (!minecraft.isLocalServer()) return true;

        Scoreboard scoreboard = minecraft.level.getScoreboard();
        //~ if = 1.8.9 'player.connection.getListedOnlinePlayers' -> 'getNetworkHandler().getOnlinePlayers'
        return minecraft.player.connection.getListedOnlinePlayers().size() > 1
                //~ if = 1.8.9 'DisplaySlot.LIST' -> '0'
                || scoreboard.getDisplayObjective(DisplaySlot.LIST) != null;
    }

    public static boolean isDebugScreenVisible() {
        Minecraft minecraft = Minecraft.getInstance();
        //? if >= 1.21.9 {
        //~ if < 1.21.11 'isOverlayVisible' -> 'isF3Visible'
        if (!minecraft.debugEntries.isOverlayVisible()) return false;
        //?}
        return minecraft.getDebugOverlay().showDebugScreen();
    }


    //~ if = 1.8.9 '(GuiGraphicsExtractor graphics)' -> '()'
    public static void render(GuiGraphicsExtractor graphics) {
        prepareHud();
        //~ if = 1.8.9 '(graphics)' -> '()'
        submitHud(graphics);
    }

    /**
     * Runs the offscreen passes before vanilla extracts the HUD to maintain compatibility with Gnetum
     */
    public static void prepareHud() {
        if (GlHost.INSTANCE.unavailableReason() != null) return;

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
        HudManager.isDebugScreenVisible = isDebugScreenVisible();
        HudManager.isTabListVisible = isTabListVisible();
        HudManager.isGuiScreenOpen = Platform.screen().current() != null;
        HudManager.isChatScreenOpen = Platform.screen().current() instanceof ChatScreen;
        HudManager.inWorld = true;
        HudManager.targetPixelWidth = Platform.screen().viewportWidth();
        HudManager.targetPixelHeight = Platform.screen().viewportHeight();

        if (HudManager.isConfigUiOpen) {
            HudManager.INSTANCE.prepare(sw, sh);
            ItemCatalog.INSTANCE.renderHudIcons();
            HudManager.invalidate();
            return;
        }
        //? if >= 1.21.11 {
        if (HudManager.INSTANCE.beginFrame(sw, sh, ItemCatalog.INSTANCE::renderHudIcons, true, true)) BlazeHud.markDirty();
        //?} else {
        /*if (HudManager.INSTANCE.beginFrame(sw, sh, ItemCatalog.INSTANCE::renderHudIcons, false)) {
            GlHost.INSTANCE.drawHudGl((ctx, volatileCtx) -> {
                HudManager.INSTANCE.render(ctx, sw, sh, volatileCtx);
                return Unit.INSTANCE;
            });
        }
        *///?}
    }

    /** Submits the legacy HUDs (unless the OneConfig menu draws them) and the GlHost HUD blit into the vanilla HUD */
    //~ if = 1.8.9 '(GuiGraphicsExtractor graphics)' -> '()'
    public static void submitHud(GuiGraphicsExtractor graphics) {
        if (!HudManager.isConfigUiOpen || !GlHost.INSTANCE.getDrawsGuiOverScreen()) {
            //~ if = 1.8.9 '(graphics)' -> '()'
            LegacyHudRenderer.INSTANCE.renderLive(graphics);
        }
        if (GlHost.INSTANCE.unavailableReason() != null || HudManager.isConfigUiOpen) return;
        //? if >= 1.21.11 {
        BlazeHud.render(graphics);
        //?} else {
        /*//~ if = 1.8.9 '(graphics)' -> '()'
        GlHost.INSTANCE.blitHud(graphics);
        *///?}
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
                        if (!isHudEditor(Platform.screen().current())) {
                            Platform.screen().display(HudEditorUIScreen.open(Platform.screen().current() instanceof OneConfigUIScreen));
                        }
                    } else if (!e.screenAlreadyGone) {
                        if (isHudEditor(Platform.screen().current())) {
                            Platform.screen().display(null, 0);
                        }
                    }
                });
        // resets the editing flag if another screen replaces the HUD editor without going through
        // HudManager.closeEditor() and null opens are ignored because commands close chat first
        // closeEditor() would re-enter setScreen(null) mid-switch, which 26.2+ rejects during disconnect
        EventManager.register(
                ScreenOpenEvent.class, e -> {
                    if (HudManager.INSTANCE.isEditorOpen() && e.getScreen() != null && !isHudEditor(e.getScreen())) {
                        HudManager.INSTANCE.onEditorScreenRemoved();
                    }
                });
        EventManager.register(
                InitializationEvent.class, e -> {
                    try {
                        ConfigManager.setProfileSwitchDispatcher(Platform.screen()::runOnUiThread);
                        try {
                            ConfigEntrypoints.registerAll();
                        } catch (Throwable t) {
                            LOGGER.error("Failed to run the OneConfig config entrypoints", t);
                        }
                        ConfigManager.initialize();
                    } finally {
                        // compat stays queued until this runs
                        ConfigEntrypoints.finishRegistration();
                    }
                    KeybindConflicts.unbindMinecraftKeybinds();
                    CompatSnapshots.setDispatcher(r -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null && !mc.isSameThread()) mc.execute(r);
                        else r.run();
                    });
                    MinecraftKeybindProfiles.init();
                    ProfileKeybinds.init();
                    ConfigRegistry.INSTANCE.loadFrom(ConfigManager.active(), ConfigSource.OC);
                    BuiltinHudRegistrar.register();
                    ModGates.register();
                    //? if > 1.8.9 {
                    FirmamentHudCompat.register();
                    ArmorHudCompat.register();
                    //?}
                    //? if wwaypoints_compat
                    WWaypointsCompat.register();
                    ThemeRegistry.INSTANCE.loadFromConfig();
                });
        EventManager.register(WorldEvent.Load.class, e -> {
            showFirstLaunchNotification();
            ItemCatalog.INSTANCE.markIconsAvailable();
        });
        // after loading finishes so translation keys are available
        EventManager.register(ResourceFinishedLoading.class, e -> SearchCorpus.INSTANCE.init());
        EventManager.register(ResourceFinishedLoading.class, e -> CompatLoader.INSTANCE.onResourcesLoaded());
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
        firstLaunchToast = Notifications.builder(title, message)
                .icon(Notifications.icon("/assets/oneconfig/brand/oneconfig-icon.svg", 64))
                .persistent()
                .onClick(OneConfigUIScreen::openLastSession)
                .send();
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
        BuiltinPages.register();
        // force class-load so the hello/disconnect handlers are armed before joining any server
        HypixelUtils.isHypixel();
        ExternalSounds.INSTANCE.ensureDownloaded();

        KeybindProviderRegistry.INSTANCE.register(MinecraftKeybindProvider.INSTANCE);
        registerKeybinds();
        new OneConfigConfig();
        new ThemeConfig();
        registerCommands();
        registerEventHandlers();
        MainMenuFpsSampler.init();
        ClassPreloader.init();
        //? fabric
        ModMenuShimLoader.enable();
        KaleidoCompat.enable();

        initialized = true;
        LOGGER.info("OneConfig initialization took {}ms", (System.nanoTime() - t1) / 1_000_000.0);
    }

}
