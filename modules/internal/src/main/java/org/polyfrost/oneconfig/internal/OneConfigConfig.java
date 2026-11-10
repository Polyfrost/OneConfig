package org.polyfrost.oneconfig.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import kotlin.jvm.functions.Function1;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.config.v1.Node;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown;
import org.polyfrost.oneconfig.api.config.v1.annotations.Include;
import org.polyfrost.oneconfig.api.config.v1.annotations.Keybind;
import org.polyfrost.oneconfig.api.config.v1.annotations.Number;
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;
import org.polyfrost.oneconfig.api.platform.v1.Keys;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeyModifiers;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindUtils;
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind;
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudDesignSession;
import org.polyfrost.oneconfig.internal.ui.shell.ShellState;
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds;

public class OneConfigConfig extends Config {
    private static final Keys KEYS = Platform.compatibility().keys();
    private static final byte HUD_ACTION_MODS = KeybindUtils.getActionModifier();
    private static final byte HUD_REDO_MODS = (byte) (HUD_ACTION_MODS | KeyModifiers.SHIFT);

    // the open action comes from the minecraft module via setOpenAction since it cannot be serialized
    @Keybind(
        title = "oneconfig.preferences.keybind.title",
        subcategory = "oneconfig.preferences.category.gui",
        description = "oneconfig.preferences.keybind.description"
    )
    public static OneConfigKeybind oneConfigKeybind =
        new OneConfigKeybind(new int[] {KEYS.getKeyRightShift()}, null, KeyModifiers.NONE, 0L, pressed -> true);

    @Switch(
        title = "oneconfig.preferences.keybind_closes_gui.title",
        subcategory = "oneconfig.preferences.category.gui",
        description = "oneconfig.preferences.keybind_closes_gui.description"
    )
    public static boolean keybindClosesGui = false;

    @Keybind(
        title = "oneconfig.preferences.hud_settings_keybind.title",
        subcategory = "oneconfig.preferences.category.hud_editor",
        description = "oneconfig.preferences.hud_settings_keybind.description"
    )
    public static OneConfigKeybind hudSettingsKeybind =
        new OneConfigKeybind(new int[] {KEYS.getKeyE()}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_visibility_keybind.title",
        subcategory = "oneconfig.preferences.category.hud_editor",
        description = "oneconfig.preferences.hud_visibility_keybind.description"
    )
    public static OneConfigKeybind hudVisibilityKeybind =
        new OneConfigKeybind(new int[] {KEYS.getKeyH()}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_lock_keybind.title",
        subcategory = "oneconfig.preferences.category.hud_editor",
        description = "oneconfig.preferences.hud_lock_keybind.description"
    )
    public static OneConfigKeybind hudLockKeybind =
        new OneConfigKeybind(new int[] {KEYS.getKeyL()}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_copy_keybind.title",
        subcategory = "oneconfig.preferences.category.hud_editor",
        description = "oneconfig.preferences.hud_copy_keybind.description"
    )
    public static OneConfigKeybind hudCopyKeybind =
        new OneConfigKeybind(new int[] {KEYS.getKeyC()}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_cut_keybind.title",
        subcategory = "oneconfig.preferences.category.hud_editor",
        description = "oneconfig.preferences.hud_cut_keybind.description"
    )
    public static OneConfigKeybind hudCutKeybind =
        new OneConfigKeybind(new int[] {KEYS.getKeyX()}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_paste_keybind.title",
        subcategory = "oneconfig.preferences.category.hud_editor",
        description = "oneconfig.preferences.hud_paste_keybind.description"
    )
    public static OneConfigKeybind hudPasteKeybind =
        new OneConfigKeybind(new int[] {KEYS.getKeyV()}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_duplicate_keybind.title",
        subcategory = "oneconfig.preferences.category.hud_editor",
        description = "oneconfig.preferences.hud_duplicate_keybind.description"
    )
    public static OneConfigKeybind hudDuplicateKeybind =
        new OneConfigKeybind(new int[] {KEYS.getKeyD()}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_reset_keybind.title",
        subcategory = "oneconfig.preferences.category.hud_editor",
        description = "oneconfig.preferences.hud_reset_keybind.description"
    )
    public static OneConfigKeybind hudResetKeybind =
        new OneConfigKeybind(new int[] {KEYS.getKeyR()}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_delete_keybind.title",
        subcategory = "oneconfig.preferences.category.hud_editor",
        description = "oneconfig.preferences.hud_delete_keybind.description"
    )
    public static OneConfigKeybind hudDeleteKeybind =
        new OneConfigKeybind(new int[] {KEYS.getKeyDelete()}, null, KeyModifiers.NONE, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_select_all_keybind.title",
        subcategory = "oneconfig.preferences.category.hud_editor",
        description = "oneconfig.preferences.hud_select_all_keybind.description"
    )
    public static OneConfigKeybind hudSelectAllKeybind =
        new OneConfigKeybind(new int[] {KEYS.getKeyA()}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_undo_keybind.title",
        subcategory = "oneconfig.preferences.category.hud_editor",
        description = "oneconfig.preferences.hud_undo_keybind.description"
    )
    public static OneConfigKeybind hudUndoKeybind =
        new OneConfigKeybind(new int[] {KEYS.getKeyZ()}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_redo_keybind.title",
        subcategory = "oneconfig.preferences.category.hud_editor",
        description = "oneconfig.preferences.hud_redo_keybind.description"
    )
    public static OneConfigKeybind hudRedoKeybind =
        new OneConfigKeybind(new int[] {KEYS.getKeyZ()}, null, HUD_REDO_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_redo_alt_keybind.title",
        subcategory = "oneconfig.preferences.category.hud_editor",
        description = "oneconfig.preferences.hud_redo_alt_keybind.description"
    )
    public static OneConfigKeybind hudRedoAltKeybind =
        new OneConfigKeybind(new int[] {KEYS.getKeyY()}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Switch(
        title = "oneconfig.preferences.hud_show_keybind_hints.title",
        subcategory = "oneconfig.preferences.category.hud_editor",
        description = "oneconfig.preferences.hud_show_keybind_hints.description"
    )
    public static boolean showKeybindHints = true;

    @Switch(
        title = "oneconfig.preferences.hud_marquee_anim.title",
        subcategory = "oneconfig.preferences.category.hud_editor",
        description = "oneconfig.preferences.hud_marquee_anim.description"
    )
    public static boolean marqueeSelectionAnim = true;

    @Slider(
        title = "oneconfig.preferences.hud_marquee_anim_duration.title",
        subcategory = "oneconfig.preferences.category.hud_editor",
        min = 50f,
        max = 500f,
        step = 10f,
        description = "oneconfig.preferences.hud_marquee_anim_duration.description"
    )
    public static float marqueeSelectionAnimDuration = 150f;

    @Slider(
        title = "oneconfig.preferences.hud_drag_ui_opacity.title",
        subcategory = "oneconfig.preferences.category.hud_editor",
        min = 0f,
        max = 100f,
        step = 5f,
        description = "oneconfig.preferences.hud_drag_ui_opacity.description"
    )
    public static float hudDragUiOpacity = 60f;

    @Switch(
        title = "oneconfig.preferences.pause_game.title",
        subcategory = "oneconfig.preferences.category.gui",
        description = "oneconfig.preferences.pause_game.description"
    )
    public static boolean pauseGame = false;

    @Dropdown(
        title = "oneconfig.preferences.remember_last_page.title",
        subcategory = "oneconfig.preferences.category.gui",
        options = {
            "oneconfig.preferences.remember_last_page.never",
            "oneconfig.preferences.remember_last_page.15_seconds",
            "oneconfig.preferences.remember_last_page.1_minute",
            "oneconfig.preferences.remember_last_page.5_minutes",
            "oneconfig.preferences.remember_last_page.always"
        },
        description = "oneconfig.preferences.remember_last_page.description"
    )
    public static int rememberLastPage = 1;

    @Include
    private static int openingBehavior = -1;

    @Include
    private static float timeBeforeReset = 15f;

    @Include
    public static boolean hudEditorTourSeen = false;

    @Include
    public static boolean percentMigrated = false;

    @Switch(
        title = "oneconfig.preferences.restore_hud_editor.title",
        subcategory = "oneconfig.preferences.category.gui",
        description = "oneconfig.preferences.restore_hud_editor.description"
    )
    public static boolean restoreHudEditor = false;

    @Switch(
        title = "oneconfig.preferences.show_option_action_buttons.title",
        subcategory = "oneconfig.preferences.category.gui",
        description = "oneconfig.preferences.show_option_action_buttons.description"
    )
    public static boolean showOptionActionButtons = true;

    @Switch(
        title = "oneconfig.preferences.instant_search.title",
        subcategory = "oneconfig.preferences.category.search",
        description = "oneconfig.preferences.instant_search.description"
    )
    public static boolean instantSearch = true;

    @Keybind(
        title = "oneconfig.preferences.search_keybind.title",
        subcategory = "oneconfig.preferences.category.search",
        description = "oneconfig.preferences.search_keybind.description"
    )
    public static OneConfigKeybind searchKeybind =
        new OneConfigKeybind(new int[] {KEYS.getKeyF()}, null, KeybindUtils.getActionModifier(), 0L, pressed -> true);

    @Number(
        title = "oneconfig.preferences.search_distance.title",
        subcategory = "oneconfig.preferences.category.search",
        min = 0f,
        max = 10f,
        description = "oneconfig.preferences.search_distance.description"
    )
    public static int searchDistance = 2;

    @Switch(
        title = "oneconfig.preferences.ui_sounds.title",
        subcategory = "oneconfig.preferences.category.sounds",
        description = "oneconfig.preferences.ui_sounds.description"
    )
    public static boolean enableUISounds = true;

    @Switch(
        title = "oneconfig.preferences.menu_sounds.title",
        subcategory = "oneconfig.preferences.category.sounds",
        description = "oneconfig.preferences.menu_sounds.description"
    )
    public static boolean enableUIMenuSounds = true;

    @Switch(
        title = "oneconfig.preferences.click_sounds.title",
        subcategory = "oneconfig.preferences.category.sounds",
        description = "oneconfig.preferences.click_sounds.description"
    )
    public static boolean enableUIClickSounds = true;

    @Switch(
        title = "oneconfig.preferences.slider_sounds.title",
        subcategory = "oneconfig.preferences.category.sounds",
        description = "oneconfig.preferences.slider_sounds.description"
    )
    public static boolean enableUISliderSounds = true;

    @Switch(
        title = "oneconfig.preferences.hud_editor_sounds.title",
        subcategory = "oneconfig.preferences.category.sounds",
        description = "oneconfig.preferences.hud_editor_sounds.description"
    )
    public static boolean enableHudEditorSounds = true;

    @Switch(
        title = "oneconfig.preferences.ui_ambience.title",
        subcategory = "oneconfig.preferences.category.sounds",
        description = "oneconfig.preferences.ui_ambience.description"
    )
    public static boolean enableUIAmbience = true;

    @Switch(
        title = "oneconfig.preferences.duck_music.title",
        subcategory = "oneconfig.preferences.category.sounds",
        description = "oneconfig.preferences.duck_music.description"
    )
    public static boolean enableUIMusicDucking = true;

    @Slider(
        title = "oneconfig.preferences.ui_sound_volume.title",
        subcategory = "oneconfig.preferences.category.sounds",
        min = 0f,
        max = 100f,
        step = 5f,
        description = "oneconfig.preferences.ui_sound_volume.description"
    )
    public static float uiSoundVolume = 100f;

    @Slider(
        title = "oneconfig.preferences.ui_ambience_volume.title",
        subcategory = "oneconfig.preferences.category.sounds",
        min = 0f,
        max = 100f,
        step = 5f,
        description = "oneconfig.preferences.ui_ambience_volume.description"
    )
    public static float uiAmbienceVolume = 100f;

    @Switch(
        title = "oneconfig.preferences.show_first_launch_message.title",
        subcategory = "oneconfig.preferences.category.general",
        description = "oneconfig.preferences.show_first_launch_message.description"
    )
    public static boolean showFirstLaunchMessage = true;

    @Switch(
        title = "oneconfig.preferences.profile_switch_toast.title",
        subcategory = "oneconfig.preferences.category.general",
        description = "oneconfig.preferences.profile_switch_toast.description"
    )
    public static boolean profileSwitchToast = true;

    /**
     * The live instance used to persist programmatic changes such as {@link #markFirstLaunchShown()}
     */
    public static OneConfigConfig INSTANCE;

    /**
     * The action run when {@link #oneConfigKeybind} is pressed
     * <p>
     * Held here rather than on the keybind because deserialization rebuilds the keybind with a null action
     */
    private static Function1<Boolean, Boolean> openAction;

    /**
     * The keybind currently registered with the {@link KeybindManager} rebuilt whenever the keys or action change
     */
    private static OneConfigKeybind registeredKeybind;

    /**
     * The keybind registered with the {@link KeybindManager} for duplicating the selected HUD in the design studio
     */
    private static OneConfigKeybind registeredHudDuplicateKeybind;

    /**
     * The keybind registered with the {@link KeybindManager} for focusing the search field
     */
    private static OneConfigKeybind registeredSearchKeybind;

    /**
     * The keybinds registered with the {@link KeybindManager} for the remaining HUD editor actions
     */
    private static final AtomicReference<OneConfigKeybind>
        registeredHudSettingsKeybind = new AtomicReference<>();
    private static final AtomicReference<OneConfigKeybind>
        registeredHudVisibilityKeybind = new AtomicReference<>();
    private static final AtomicReference<OneConfigKeybind>
        registeredHudResetKeybind = new AtomicReference<>();
    private static final AtomicReference<OneConfigKeybind>
        registeredHudCopyKeybind = new AtomicReference<>();
    private static final AtomicReference<OneConfigKeybind>
        registeredHudCutKeybind = new AtomicReference<>();
    private static final AtomicReference<OneConfigKeybind>
        registeredHudPasteKeybind = new AtomicReference<>();
    private static final AtomicReference<OneConfigKeybind>
        registeredHudDeleteKeybind = new AtomicReference<>();
    private static final AtomicReference<OneConfigKeybind>
        registeredHudSelectAllKeybind = new AtomicReference<>();
    private static final AtomicReference<OneConfigKeybind>
        registeredHudLockKeybind = new AtomicReference<>();
    private static final AtomicReference<OneConfigKeybind>
        registeredHudUndoKeybind = new AtomicReference<>();
    private static final AtomicReference<OneConfigKeybind>
        registeredHudRedoKeybind = new AtomicReference<>();
    private static final AtomicReference<OneConfigKeybind>
        registeredHudRedoAltKeybind = new AtomicReference<>();

    public OneConfigConfig() {
        super("oneconfig.json", "assets/oneconfig/brand/oneconfig-icon.svg", "OneConfig", Category.QOL);
        INSTANCE = this;
    }

    @Override
    protected void initialize(boolean byConfigManager) {
        boolean hadPreferences = false;
        if (tree == null) {
            percentMigrated = false;
            uiSoundVolume = 100f;
            uiAmbienceVolume = 100f;
            hudDragUiOpacity = 60f;
            hadPreferences = Files.exists(ConfigManager.active().getFolder().resolve(id));
        }
        super.initialize(byConfigManager);
        if (tree == null) {
            return;
        }
        migrateOpeningBehavior();
        addDependency(
            "restoreHudEditor",
            "Remember last page",
            () -> rememberLastPage != REMEMBER_NEVER ? Property.Display.SHOWN : Property.Display.DISABLED);
        migrateToPercent(hadPreferences);
        for (String option : SOUND_OPTIONS) hideIf(option, "enableUISounds");
        hideIf("enableUIMusicDucking", "enableUIAmbience");
        hideIf("uiAmbienceVolume", "enableUIAmbience");
        hideIf("marqueeSelectionAnimDuration", "marqueeSelectionAnim");
        for (String option : KEYBINDS_PAGE_ONLY) getProperty(option).addMetadata("keybindsPageOnly", true);
        addCallback(
            "enableUIAmbience", (Boolean v) -> {
                UiSounds.refreshAmbience();
                return false;
            });
        addCallback(
            "enableUIMusicDucking", (Boolean v) -> {
                UiSounds.refreshAmbience();
                return false;
            });
        addCallback(
            "uiAmbienceVolume", (Float v) -> {
                UiSounds.refreshAmbience();
                return false;
            });
        // also refreshed once below because a keybind loaded from disk carries no action
        addCallback(
            "oneConfigKeybind", (OneConfigKeybind kb) -> {
                refreshKeybind(kb);
                return false;
            });
        refreshKeybind(oneConfigKeybind);
        addCallback(
            "searchKeybind", (OneConfigKeybind kb) -> {
                refreshSearchKeybind();
                return false;
            });
        refreshSearchKeybind();
        addCallback(
            "hudDuplicateKeybind", (OneConfigKeybind kb) -> {
                refreshHudDuplicateKeybind();
                return false;
            });
        refreshHudDuplicateKeybind();
        addCallback(
            "hudSettingsKeybind", (OneConfigKeybind kb) -> {
                refreshHudSettingsKeybind();
                return false;
            });
        refreshHudSettingsKeybind();
        addCallback(
            "hudVisibilityKeybind", (OneConfigKeybind kb) -> {
                refreshHudVisibilityKeybind();
                return false;
            });
        refreshHudVisibilityKeybind();
        addCallback(
            "hudResetKeybind", (OneConfigKeybind kb) -> {
                refreshHudResetKeybind();
                return false;
            });
        refreshHudResetKeybind();
        addCallback(
            "hudCopyKeybind", (OneConfigKeybind kb) -> {
                refreshHudCopyKeybind();
                return false;
            });
        refreshHudCopyKeybind();
        addCallback(
            "hudCutKeybind", (OneConfigKeybind kb) -> {
                refreshHudCutKeybind();
                return false;
            });
        refreshHudCutKeybind();
        addCallback(
            "hudPasteKeybind", (OneConfigKeybind kb) -> {
                refreshHudPasteKeybind();
                return false;
            });
        refreshHudPasteKeybind();
        addCallback(
            "hudDeleteKeybind", (OneConfigKeybind kb) -> {
                refreshHudDeleteKeybind();
                return false;
            });
        refreshHudDeleteKeybind();
        addCallback(
            "hudSelectAllKeybind", (OneConfigKeybind kb) -> {
                refreshHudSelectAllKeybind();
                return false;
            });
        refreshHudSelectAllKeybind();
        addCallback(
            "hudLockKeybind", (OneConfigKeybind kb) -> {
                refreshHudLockKeybind();
                return false;
            });
        refreshHudLockKeybind();
        addCallback(
            "hudUndoKeybind", (OneConfigKeybind kb) -> {
                refreshHudUndoKeybind();
                return false;
            });
        refreshHudUndoKeybind();
        addCallback(
            "hudRedoKeybind", (OneConfigKeybind kb) -> {
                refreshHudRedoKeybind();
                return false;
            });
        refreshHudRedoKeybind();
        addCallback(
            "hudRedoAltKeybind", (OneConfigKeybind kb) -> {
                refreshHudRedoAltKeybind();
                return false;
            });
        refreshHudRedoAltKeybind();
    }

    private static final String[] SOUND_OPTIONS = {"enableUIMenuSounds", "enableUIClickSounds", "enableUISliderSounds", "enableHudEditorSounds", "uiSoundVolume"};

    private static final String[] KEYBINDS_PAGE_ONLY = {
        "searchKeybind", "hudSettingsKeybind", "hudVisibilityKeybind", "hudLockKeybind", "hudCopyKeybind", "hudCutKeybind",
        "hudPasteKeybind", "hudDuplicateKeybind", "hudResetKeybind", "hudDeleteKeybind", "hudSelectAllKeybind",
        "hudUndoKeybind", "hudRedoKeybind", "hudRedoAltKeybind"
    };

    private void migrateToPercent(boolean hadPreferences) {
        if (percentMigrated) return;
        Float saved;
        Float legacy;
        try {
            saved = hadPreferences ? storedNumber(id, "hudDragUiOpacity") : null;
            legacy = storedNumber("themes.json", "hudDragUiOpacity");
        } catch (Exception e) {
            return;
        }
        if (saved == null || saved <= 1f) {
            if (legacy != null) hudDragUiOpacity = toPercent(legacy);
            else if (saved != null) hudDragUiOpacity = toPercent(saved);
        }
        uiSoundVolume = toPercent(uiSoundVolume);
        uiAmbienceVolume = toPercent(uiAmbienceVolume);
        percentMigrated = true;
        save();
    }

    private static float toPercent(float value) {
        return value <= 1f ? value * 100f : value;
    }

    private static Float storedNumber(String file, String key) throws Exception {
        Path path = ConfigManager.active().getFolder().resolve(file);
        if (!Files.exists(path)) return null;
        Tree stored = ConfigManager.active().getNoRegister(path);
        Node value = stored == null ? null : stored.get(key);
        if (!(value instanceof Property)) return null;
        Object raw = ((Property<?>) value).getAs();
        return raw instanceof java.lang.Number ? ((java.lang.Number) raw).floatValue() : null;
    }

    private static final int REMEMBER_NEVER = 0;
    private static final int REMEMBER_ALWAYS = 4;
    private static final long[] REMEMBER_LAST_PAGE_SECONDS = {15L, 60L, 300L};

    public static boolean remembersPageClosedAt(long closedAt) {
        int option = rememberLastPage;
        if (option <= REMEMBER_NEVER) return false;
        if (option >= REMEMBER_ALWAYS) return true;
        return closedAt > 0L && System.currentTimeMillis() - closedAt <= REMEMBER_LAST_PAGE_SECONDS[option - 1] * 1000L;
    }

    private void migrateOpeningBehavior() {
        if (openingBehavior < 0) return;
        switch (openingBehavior) {
            case 2: // previous page
                rememberLastPage = REMEMBER_ALWAYS;
                break;
            case 3: // smart reset, 37.5 is halfway between 15 seconds and 1 minute
                rememberLastPage = timeBeforeReset > 37.5f ? 2 : 1;
                break;
            default: // mods or preferences
                rememberLastPage = REMEMBER_NEVER;
        }
        openingBehavior = -1;
        save();
    }

    /** When the OneConfig keybind last closed the GUI so the same press cannot immediately reopen it */
    private static volatile long keybindClosedAt = 0L;

    /** How long a keybind-driven close blocks the open action for */
    private static final long KEYBIND_CLOSE_GRACE_MS = 300L;

    /**
     * Records that the OneConfig keybind just closed the GUI
     * <p>
     * The keybind manager evaluates presses at the end of a frame after the screen has been drawn so without this
     * mark the same press would reopen the GUI when the closing animation is disabled
     *
     * @see #consumeKeybindClose()
     */
    public static void notifyKeybindClosedGui() {
        keybindClosedAt = System.currentTimeMillis();
    }

    /**
     * True if the press being handled is the one that just closed the GUI
     * <p>
     * Consumes the mark so only the first press after a close is dropped
     */
    public static boolean consumeKeybindClose() {
        long at = keybindClosedAt;
        if (at == 0L) return false;
        keybindClosedAt = 0L;
        return System.currentTimeMillis() - at <= KEYBIND_CLOSE_GRACE_MS;
    }

    /**
     * Supplies the action run when the OneConfig keybind is pressed
     * <p>
     * Called by the minecraft module because the action references platform classes this module cannot depend on
     *
     * @see #openAction
     */
    public static void setOpenAction(Function1<Boolean, Boolean> action) {
        openAction = action;
        refreshKeybind(oneConfigKeybind);
    }

    /**
     * Rebuilds the registered keybind from {@code src}'s GLFW keys plus {@link #openAction} and re-registers it
     * with the {@link KeybindManager}
     */
    private static void refreshKeybind(OneConfigKeybind src) {
        if (registeredKeybind != null) {
            KeybindManager.unregister(registeredKeybind);
            registeredKeybind = null;
        }
        if (src == null || openAction == null || !src.isBound()) {
            return;
        }
        registeredKeybind = new OneConfigKeybind(
            src.getKeyCodes(),
            src.getMouseBtns(),
            src.getMods(),
            src.getDurationNanos(),
            openAction);
        KeybindManager.register(registeredKeybind);
    }

    /**
     * Rebuilds the registered search keybind from {@code searchKeybind}'s GLFW keys and re-registers it
     * with the {@link KeybindManager}
     * <p>
     * The action is resolved here rather than stored on the keybind so it survives deserialization, and it fires
     * while typing so it still works with a text field focused
     */
    private static void refreshSearchKeybind() {
        if (registeredSearchKeybind != null) {
            KeybindManager.unregister(registeredSearchKeybind);
            registeredSearchKeybind = null;
        }
        OneConfigKeybind src = searchKeybind;
        if (src == null || !src.isBound()) {
            return;
        }
        registeredSearchKeybind = new OneConfigKeybind(
            src.getKeyCodes(),
            src.getMouseBtns(),
            src.getMods(),
            src.getDurationNanos(),
            ShellState::handleSearchKeybind);
        registeredSearchKeybind.setFiresWhileTyping(true);
        KeybindManager.register(registeredSearchKeybind);
    }

    /**
     * Rebuilds the registered duplicate-HUD keybind from {@code hudDuplicateKeybind}'s GLFW keys and re-registers it
     * with the {@link KeybindManager}
     * <p>
     * The action is resolved here rather than stored on the keybind so it survives deserialization
     */
    private static void refreshHudDuplicateKeybind() {
        if (registeredHudDuplicateKeybind != null) {
            KeybindManager.unregister(registeredHudDuplicateKeybind);
            registeredHudDuplicateKeybind = null;
        }
        OneConfigKeybind src = hudDuplicateKeybind;
        if (src == null || !src.isBound()) {
            return;
        }
        registeredHudDuplicateKeybind = new OneConfigKeybind(
            src.getKeyCodes(),
            src.getMouseBtns(),
            src.getMods(),
            src.getDurationNanos(),
            HudDesignSession::handleDuplicate);
        KeybindManager.register(registeredHudDuplicateKeybind);
    }

    /**
     * Rebuilds one of the additional HUD editor keybinds from its config field's GLFW keys and re-registers it
     * with the {@link KeybindManager}
     * <p>
     * The action is resolved here rather than stored on the keybind so it survives deserialization
     */
    private static void refreshHudEditorKeybind(
        OneConfigKeybind src,
        AtomicReference<OneConfigKeybind> slot,
        Function1<Boolean, Boolean> action) {
        refreshHudEditorKeybind(src, slot, action, false);
    }

    private static void refreshHudEditorKeybind(
        OneConfigKeybind src,
        AtomicReference<OneConfigKeybind> slot,
        Function1<Boolean, Boolean> action,
        boolean strictShift) {
        OneConfigKeybind old = slot.getAndSet(null);
        if (old != null) {
            KeybindManager.unregister(old);
        }
        if (src == null || !src.isBound()) {
            return;
        }
        OneConfigKeybind keybind = new OneConfigKeybind(
            src.getKeyCodes(),
            src.getMouseBtns(),
            src.getMods(),
            src.getDurationNanos(),
            action) {
            @Override
            public boolean test(Set<Integer> downKeys, Set<Integer> downMouse, byte currentMods) {
                // a keybind matches with extra modifiers held, so Ctrl+Shift+Z would undo as well as redo
                boolean extraShift = (currentMods & KeyModifiers.SHIFT) != 0 && (getMods() & KeyModifiers.SHIFT) == 0;
                return !(strictShift && extraShift) && super.test(downKeys, downMouse, currentMods);
            }
        };
        KeybindManager.register(keybind);
        slot.set(keybind);
    }

    private static void refreshHudSettingsKeybind() {
        refreshHudEditorKeybind(
            hudSettingsKeybind,
            registeredHudSettingsKeybind,
            HudDesignSession::handleOpenSettings);
    }

    private static void refreshHudVisibilityKeybind() {
        refreshHudEditorKeybind(
            hudVisibilityKeybind,
            registeredHudVisibilityKeybind,
            HudDesignSession::handleToggleVisibility);
    }

    private static void refreshHudResetKeybind() {
        refreshHudEditorKeybind(
            hudResetKeybind,
            registeredHudResetKeybind,
            HudDesignSession::handleReset);
    }

    private static void refreshHudCopyKeybind() {
        refreshHudEditorKeybind(
            hudCopyKeybind,
            registeredHudCopyKeybind,
            HudDesignSession::handleCopy);
    }

    private static void refreshHudCutKeybind() {
        refreshHudEditorKeybind(
            hudCutKeybind,
            registeredHudCutKeybind,
            HudDesignSession::handleCut);
    }

    private static void refreshHudPasteKeybind() {
        refreshHudEditorKeybind(
            hudPasteKeybind,
            registeredHudPasteKeybind,
            HudDesignSession::handlePaste);
    }

    private static void refreshHudDeleteKeybind() {
        refreshHudEditorKeybind(
            hudDeleteKeybind,
            registeredHudDeleteKeybind,
            HudDesignSession::handleDelete);
    }

    private static void refreshHudSelectAllKeybind() {
        refreshHudEditorKeybind(
            hudSelectAllKeybind,
            registeredHudSelectAllKeybind,
            HudDesignSession::handleSelectAll);
    }

    private static void refreshHudLockKeybind() {
        refreshHudEditorKeybind(
            hudLockKeybind,
            registeredHudLockKeybind,
                HudDesignSession::handleLock);
    }

    private static void refreshHudUndoKeybind() {
        refreshHudEditorKeybind(
            hudUndoKeybind,
            registeredHudUndoKeybind,
            HudDesignSession::handleUndo,
            true);
    }

    private static void refreshHudRedoKeybind() {
        refreshHudEditorKeybind(
            hudRedoKeybind,
            registeredHudRedoKeybind,
            HudDesignSession::handleRedo);
    }

    private static void refreshHudRedoAltKeybind() {
        refreshHudEditorKeybind(
            hudRedoAltKeybind,
            registeredHudRedoAltKeybind,
            HudDesignSession::handleRedo);
    }

    /**
     * Called once after the first-launch message has been shown
     * <p>
     * Turns {@link #showFirstLaunchMessage} off and persists it until the user re-enables it from the menu
     */
    public static void markFirstLaunchShown() {
        if (!showFirstLaunchMessage) {
            return;
        }
        showFirstLaunchMessage = false;
        if (INSTANCE != null && INSTANCE.tree != null) {
            try {
                INSTANCE.getProperty("showFirstLaunchMessage").setAs(Boolean.FALSE);
            } catch (Exception ignored) {
            }
            INSTANCE.save();
        }
    }

    public static void markHudEditorTourSeen() {
        if (hudEditorTourSeen) {
            return;
        }
        hudEditorTourSeen = true;
        if (INSTANCE != null && INSTANCE.tree != null) {
            try {
                INSTANCE.getProperty("hudEditorTourSeen").setAs(Boolean.TRUE);
            } catch (Exception ignored) {
            }
            INSTANCE.save();
        }
    }
}
