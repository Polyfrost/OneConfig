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
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Scoreboard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.compose.render.RenderContext;
import org.polyfrost.oneconfig.api.commands.v1.CommandManager;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.config.v1.Properties;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.InitializationEvent;
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent;
import org.polyfrost.oneconfig.api.event.v1.events.WorldEvent;
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.polyfrost.oneconfig.api.config.v1.Tree.tree;

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
    private static final String KEYBIND_STATE_FILE = "keybind-conflicts.json";
    private static final String CHECKED_KEYBINDS = "checkedKeybinds";
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

    /**
     * Unbinds Minecraft keybinds that would conflict with OneConfig's own Right Shift keybind.
     * <br>
     * Each keybind is only ever considered once, on the launch it is first seen, and the names of the keybinds that
     * have been considered are remembered across launches. That way a mod installed long after OneConfig still has its
     * Right Shift bind cleared when it first shows up, while a bind the user has since moved onto Right Shift
     * themselves is left alone. OneConfig's own mirrors of its keybinds are never touched.
     */
    private static void unbindRightShiftMinecraftKeybinds() {
        Tree state = ConfigManager.internal().register(
                tree(KEYBIND_STATE_FILE).put(
                        Properties.simple(CHECKED_KEYBINDS, "Checked Keybinds",
                                "Minecraft keybinds which have already been checked against OneConfig's keybind.",
                                new String[0], String[].class)
                )
        ).get();
        Property<?> prop = state.getProp(CHECKED_KEYBINDS);
        Set<String> checked = checkedKeybinds(prop);

        Minecraft minecraft = Minecraft.getInstance();
        String rightShift = InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_RSHIFT).getName();
        List<String> unbound = new ArrayList<>();
        boolean anyNew = false;

        for (KeyMapping keyMapping : MinecraftKeybindProvider.INSTANCE.managedMappings()) {
            if (!checked.add(keyMapping.getName())) continue;
            anyNew = true;
            if (rightShift.equals(keyMapping.saveString())) {
                keyMapping.setKey(InputConstants.UNKNOWN);
                unbound.add(keyMapping.getName());
            }
        }

        if (!unbound.isEmpty()) {
            KeyMapping.resetMapping();
            minecraft.options.save();
            LOGGER.info("Unbound {} Minecraft keybind(s) using Right Shift: {}", unbound.size(), unbound);
        }
        if (anyNew) {
            prop.setAs(checked.toArray(new String[0]));
            ConfigManager.internal().save(KEYBIND_STATE_FILE);
        }
    }

    private static Set<String> checkedKeybinds(Property<?> prop) {
        Object stored = prop.get();
        Set<String> out = new LinkedHashSet<>();
        if (stored instanceof Object[]) {
            for (Object name : (Object[]) stored) {
                if (name != null) out.add(name.toString());
            }
        } else if (stored instanceof Iterable<?>) {
            for (Object name : (Iterable<?>) stored) {
                if (name != null) out.add(name.toString());
            }
        }
        return out;
    }

    /**
     * Mirrors vanilla's tab list visibility check, which is not just the keybind being held:
     * on a single player world with no other listed players and no LIST scoreboard objective,
     * the tab list stays hidden even while the key is down.
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

        float sw = graphics.guiWidth();
        float sh = graphics.guiHeight();
        // guiWidth()/guiHeight() are already GUI-scaled (== Screen dimensions), do not divide by guiScale again.
        HudManager.guiScreenWidth = sw;
        HudManager.guiScreenHeight = sh;

        // Update HUD visibility state for per-HUD filtering
        HudManager.isDebugScreenVisible = Minecraft.getInstance().getDebugOverlay().showDebugScreen();
        HudManager.isTabListVisible = isTabListVisible();
        HudManager.isGuiScreenOpen = Platform.screen().current() != null;
        //~ if >= 26.2 'screen' -> 'gui.screen()'
        HudManager.isChatScreenOpen = Minecraft.getInstance().screen instanceof ChatScreen;
        HudManager.inWorld = true;
        HudManager.targetPixelWidth = Minecraft.getInstance().getWindow().getWidth();
        HudManager.targetPixelHeight = Minecraft.getInstance().getWindow().getHeight();

        if (!SkiaCtx.INSTANCE.suppressInGameHudRender) {
            LegacyHudRenderer.INSTANCE.renderLive(graphics);
        }
        //? if >= 26.1 {
        else org.polyfrost.oneconfig.internal.ui.hud.LegacyHudOffscreen.INSTANCE.render();
        //? } else {
        /*else LegacyHudRenderer.INSTANCE.renderLive(graphics);
        *///? }
        // Records the F3 overlay offscreen so Skia can put it above the Compose UI instead of below the blur.
        // Blitted through the post-compose renderer, so it must run every frame regardless of the HUD dirty gate.
        org.polyfrost.oneconfig.internal.ui.hud.DebugOverlayOffscreen.INSTANCE.render();
        if (HudManager.INSTANCE.beginFrame(sw, sh)) {
            SkiaCtx.INSTANCE.queueHudDraw(() -> {
                var ctx = new RenderContext(SkiaCtx.INSTANCE.getCanvas());
                HudManager.INSTANCE.render(ctx, sw, sh);
            });
            // Render Skia HUDs into the offscreen TextureTarget.
            // The mixin blits the texture onto MC's render target afterwards.
            SkiaCtx.INSTANCE.drawNow();
        }
    }

    private static void installNotificationRenderer() {
        SkiaCtx.INSTANCE.setNotifRenderer(() -> {
            var window = Minecraft.getInstance().getWindow();
            float sw = window.getGuiScaledWidth();
            float sh = window.getGuiScaledHeight();
            if (sw <= 0f || sh <= 0f) return;
            var ctx = new RenderContext(SkiaCtx.INSTANCE.getCanvas());
            NotificationsRenderer.render(ctx, sw, sh);
        });
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
                    if (HudManager.INSTANCE.isEditorOpen() && e.getScreen() != null && !(e.getScreen() instanceof HudEditorUIScreen)) {
                        HudManager.INSTANCE.closeEditor();
                    }
                });
        EventManager.register(
                InitializationEvent.class, e -> {
                    ConfigManager.initialize();
                    unbindRightShiftMinecraftKeybinds();
                    org.polyfrost.oneconfig.api.config.v1.CompatSnapshots.setDispatcher(r -> {
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc != null) mc.execute(r);
                        else r.run();
                    });
                    org.polyfrost.oneconfig.internal.ui.keybind.MinecraftKeybindProfiles.init();
                    ConfigRegistry.INSTANCE.loadFrom(ConfigManager.active(), ConfigSource.OC);
                    org.polyfrost.oneconfig.internal.ui.hud.BuiltinHudRegistrar.register();
                    org.polyfrost.oneconfig.internal.compat.FirmamentHudCompat.register();
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
        installNotificationRenderer();
        MainMenuFpsSampler.init();
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
