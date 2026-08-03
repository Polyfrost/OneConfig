package org.polyfrost.oneconfig.internal;

import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown;
import org.polyfrost.oneconfig.api.config.v1.annotations.Keybind;
import org.polyfrost.oneconfig.api.config.v1.annotations.Number;
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeyModifiers;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindUtils;
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind;

import kotlin.jvm.functions.Function1;
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudDesignSession;

public class OneConfigConfig extends Config {
    private static final byte HUD_ACTION_MODS = KeybindUtils.getActionModifier();

    // Keybinds store GLFW key codes (the space the KeybindManager matches against; KeybindOption translates the UI's
    // AWT capture into GLFW). 344 == GLFW_KEY_RIGHT_SHIFT, declared as a literal to avoid a GLFW dependency here. The
    // action that opens the GUI is supplied by the minecraft module via setOpenAction (it cannot be serialized, so it
    // lives outside the keybind).
    @Keybind(
        title = "oneconfig.preferences.keybind.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.keybind.description",
        descriptionTranslation = true
    )
    public static OneConfigKeybind oneConfigKeybind =
        new OneConfigKeybind(new int[] {344}, null, KeyModifiers.NONE, 0L, pressed -> true);

    @Switch(
        title = "oneconfig.preferences.master_hud_enabled.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.hud_editor",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.master_hud_enabled.description",
        descriptionTranslation = true
    )
    public static boolean masterHudEnabled = true;

    @Keybind(
        title = "oneconfig.preferences.hud_settings_keybind.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.hud_editor",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.hud_settings_keybind.description",
        descriptionTranslation = true
    )
    public static OneConfigKeybind hudSettingsKeybind =
        new OneConfigKeybind(new int[] {69}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_visibility_keybind.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.hud_editor",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.hud_visibility_keybind.description",
        descriptionTranslation = true
    )
    public static OneConfigKeybind hudVisibilityKeybind =
        new OneConfigKeybind(new int[] {72}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_lock_keybind.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.hud_editor",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.hud_lock_keybind.description",
        descriptionTranslation = true
    )
    public static OneConfigKeybind hudLockKeybind =
        new OneConfigKeybind(new int[] {76}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_copy_keybind.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.hud_editor",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.hud_copy_keybind.description",
        descriptionTranslation = true
    )
    public static OneConfigKeybind hudCopyKeybind =
        new OneConfigKeybind(new int[] {67}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_cut_keybind.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.hud_editor",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.hud_cut_keybind.description",
        descriptionTranslation = true
    )
    public static OneConfigKeybind hudCutKeybind =
        new OneConfigKeybind(new int[] {88}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_paste_keybind.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.hud_editor",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.hud_paste_keybind.description",
        descriptionTranslation = true
    )
    public static OneConfigKeybind hudPasteKeybind =
        new OneConfigKeybind(new int[] {86}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_duplicate_keybind.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.hud_editor",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.hud_duplicate_keybind.description",
        descriptionTranslation = true
    )
    public static OneConfigKeybind hudDuplicateKeybind =
        new OneConfigKeybind(new int[] {68}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_reset_keybind.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.hud_editor",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.hud_reset_keybind.description",
        descriptionTranslation = true
    )
    public static OneConfigKeybind hudResetKeybind =
        new OneConfigKeybind(new int[] {82}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_delete_keybind.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.hud_editor",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.hud_delete_keybind.description",
        descriptionTranslation = true
    )
    public static OneConfigKeybind hudDeleteKeybind =
        new OneConfigKeybind(new int[] {261}, null, KeyModifiers.NONE, 0L, pressed -> true);

    @Keybind(
        title = "oneconfig.preferences.hud_select_all_keybind.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.hud_editor",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.hud_select_all_keybind.description",
        descriptionTranslation = true
    )
    public static OneConfigKeybind hudSelectAllKeybind =
        new OneConfigKeybind(new int[] {65}, null, HUD_ACTION_MODS, 0L, pressed -> true);

    @Switch(
        title = "oneconfig.preferences.hud_show_keybind_hints.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.hud_editor",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.hud_show_keybind_hints.description",
        descriptionTranslation = true
    )
    public static boolean showKeybindHints = true;

    @Switch(
        title = "oneconfig.preferences.hud_marquee_anim.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.hud_editor",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.hud_marquee_anim.description",
        descriptionTranslation = true
    )
    public static boolean marqueeSelectionAnim = true;

    @Slider(
        title = "oneconfig.preferences.hud_marquee_anim_duration.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.hud_editor",
        subcategoryTranslation = true,
        min = 50f,
        max = 500f,
        step = 10f,
        description = "oneconfig.preferences.hud_marquee_anim_duration.description",
        descriptionTranslation = true
    )
    public static float marqueeSelectionAnimDuration = 150f;

    @Switch(
        title = "oneconfig.preferences.background_blur.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.background_blur.description",
        descriptionTranslation = true
    )
    public static boolean enableBackgroundBlur = true;

    @Slider(
        title = "oneconfig.preferences.hud_drag_ui_opacity.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        min = 0f,
        max = 1f,
        step = 0.05f,
        description = "oneconfig.preferences.hud_drag_ui_opacity.description",
        descriptionTranslation = true
    )
    public static float hudDragUiOpacity = 0.60f;

    @Slider(
        title = "oneconfig.preferences.page_opacity.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        min = 0f,
        max = 100f,
        step = 1f,
        description = "oneconfig.preferences.page_opacity.description",
        descriptionTranslation = true
    )
    public static float pageOpacity = 88f;

    @Slider(
        title = "oneconfig.preferences.sidebar_opacity.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        min = 0f,
        max = 100f,
        step = 1f,
        description = "oneconfig.preferences.sidebar_opacity.description",
        descriptionTranslation = true
    )
    public static float sidebarOpacity = 80f;

    @Slider(
        title = "oneconfig.preferences.glow_opacity.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        min = 0f,
        max = 100f,
        step = 1f,
        description = "oneconfig.preferences.glow_opacity.description",
        descriptionTranslation = true
    )
    public static float glowOpacity = 25f;

    @Switch(
        title = "oneconfig.preferences.pause_game.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.pause_game.description",
        descriptionTranslation = true
    )
    public static boolean pauseGame = false;

    @Switch(
        title = "oneconfig.preferences.use_custom_scale.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.use_custom_scale.description",
        descriptionTranslation = true
    )
    public static boolean useCustomScale = false;

    @Slider(
        title = "oneconfig.preferences.custom_scale.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        min = 0.5f,
        max = 2f,
        step = 0.05f,
        description = "oneconfig.preferences.custom_scale.description",
        descriptionTranslation = true
    )
    public static float customScale = 1f;

    @Dropdown(
        title = "oneconfig.preferences.reduced_res_filter.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        options = {
            "oneconfig.preferences.reduced_res_filter.off",
            "oneconfig.preferences.reduced_res_filter.sharpen",
            "oneconfig.preferences.reduced_res_filter.harden"
        },
        optionsTranslation = true,
        description = "oneconfig.preferences.reduced_res_filter.description",
        descriptionTranslation = true
    )
    public static int reducedResFilter = 0;

    @Slider(
        title = "oneconfig.preferences.ui_sharpening.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        min = 0f,
        max = 1f,
        step = 0.05f,
        description = "oneconfig.preferences.ui_sharpening.description",
        descriptionTranslation = true
    )
    public static float uiSharpening = 0.4f;

    @Dropdown(
        title = "oneconfig.preferences.opening_behavior.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        options = {
            "oneconfig.mods",
            "oneconfig.preferences",
            "oneconfig.preferences.opening_behavior.previous_page",
            "oneconfig.preferences.opening_behavior.smart_reset"
        },
        optionsTranslation = true,
        description = "oneconfig.preferences.opening_behavior.description",
        descriptionTranslation = true
    )
    public static int openingBehavior = 3;

    @Switch(
        title = "oneconfig.preferences.show_opening_page_animation.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.show_opening_page_animation.description",
        descriptionTranslation = true
    )
    public static boolean showOpeningPageAnimation = false;

    @Switch(
        title = "oneconfig.preferences.instant_search.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.instant_search.description",
        descriptionTranslation = true
    )
    public static boolean instantSearch = true;

    @Switch(
        title = "oneconfig.preferences.show_option_action_buttons.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.show_option_action_buttons.description",
        descriptionTranslation = true
    )
    public static boolean showOptionActionButtons = true;

    @Switch(
        title = "oneconfig.preferences.flip_top_option_order.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.flip_top_option_order.description",
        descriptionTranslation = true
    )
    public static boolean flipTopOptionOrder = false;

    @Slider(
        title = "oneconfig.preferences.time_before_reset.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        min = 5f,
        max = 60f,
        description = "oneconfig.preferences.time_before_reset.description",
        descriptionTranslation = true
    )
    public static float timeBeforeReset = 15f;

    @Number(
        title = "oneconfig.preferences.search_distance.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.search",
        subcategoryTranslation = true,
        min = 0f,
        max = 10f,
        description = "oneconfig.preferences.search_distance.description",
        descriptionTranslation = true
    )
    public static int searchDistance = 2;

    @Switch(
        title = "oneconfig.preferences.opening_animation.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.animations",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.opening_animation.description",
        descriptionTranslation = true
    )
    public static boolean guiOpenAnimation = true;

    @Switch(
        title = "oneconfig.preferences.closing_animation.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.animations",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.closing_animation.description",
        descriptionTranslation = true
    )
    public static boolean guiClosingAnimation = true;

    @Slider(
        title = "oneconfig.preferences.opening_time.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.animations",
        subcategoryTranslation = true,
        min = 0.05f,
        max = 2f,
        step = 0.05f,
        description = "oneconfig.preferences.opening_time.description",
        descriptionTranslation = true
    )
    public static float animationTime = 0.6f;

    @Switch(
        title = "oneconfig.preferences.show_page_animations.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.animations",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.show_page_animations.description",
        descriptionTranslation = true
    )
    public static boolean showPageAnimations = true;

    @Switch(
        title = "oneconfig.preferences.ui_sounds.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.sounds",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.ui_sounds.description",
        descriptionTranslation = true
    )
    public static boolean enableUISounds = true;

    @Switch(
        title = "oneconfig.preferences.menu_sounds.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.sounds",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.menu_sounds.description",
        descriptionTranslation = true
    )
    public static boolean enableUIMenuSounds = true;

    @Switch(
        title = "oneconfig.preferences.click_sounds.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.sounds",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.click_sounds.description",
        descriptionTranslation = true
    )
    public static boolean enableUIClickSounds = true;

    @Switch(
        title = "oneconfig.preferences.slider_sounds.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.sounds",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.slider_sounds.description",
        descriptionTranslation = true
    )
    public static boolean enableUISliderSounds = true;

    @Switch(
        title = "oneconfig.preferences.hud_editor_sounds.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.sounds",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.hud_editor_sounds.description",
        descriptionTranslation = true
    )
    public static boolean enableHudEditorSounds = true;

    @Switch(
        title = "oneconfig.preferences.ui_ambience.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.sounds",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.ui_ambience.description",
        descriptionTranslation = true
    )
    public static boolean enableUIAmbience = true;

    @Switch(
        title = "oneconfig.preferences.duck_music.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.sounds",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.duck_music.description",
        descriptionTranslation = true
    )
    public static boolean enableUIMusicDucking = true;

    @Slider(
        title = "oneconfig.preferences.ui_sound_volume.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.sounds",
        subcategoryTranslation = true,
        min = 0f,
        max = 1f,
        step = 0.05f,
        description = "oneconfig.preferences.ui_sound_volume.description",
        descriptionTranslation = true
    )
    public static float uiSoundVolume = 1f;

    @Slider(
        title = "oneconfig.preferences.ui_ambience_volume.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.sounds",
        subcategoryTranslation = true,
        min = 0f,
        max = 1f,
        step = 0.05f,
        description = "oneconfig.preferences.ui_ambience_volume.description",
        descriptionTranslation = true
    )
    public static float uiAmbienceVolume = 1f;

    @Slider(
        title = "oneconfig.preferences.page_animation_duration.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.animations",
        subcategoryTranslation = true,
        min = 0.1f,
        max = 0.6f,
        step = 0.05f,
        description = "oneconfig.preferences.page_animation_duration.description",
        descriptionTranslation = true
    )
    public static float pageAnimationDuration = 0.3f;

    @Switch(
        title = "oneconfig.preferences.show_first_launch_message.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.general",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.show_first_launch_message.description",
        descriptionTranslation = true
    )
    public static boolean showFirstLaunchMessage = true;

    /**
     * The live instance, used to persist programmatic changes (e.g. {@link #markFirstLaunchShown()}).
     */
    public static OneConfigConfig INSTANCE;

    /**
     * The action run when {@link #oneConfigKeybind} is pressed. Supplied by the minecraft module rather than stored
     * on the keybind itself, because a keybind's action is transient and is lost whenever the keybind is loaded from
     * disk (deserialization rebuilds the keybind with a null action). Keeping it here lets us always reattach it.
     */
    private static Function1<Boolean, Boolean> openAction;

    /**
     * The keybind currently registered with the {@link KeybindManager}, rebuilt whenever the keys or action change.
     */
    private static OneConfigKeybind registeredKeybind;

    /**
     * The keybind currently registered with the {@link KeybindManager} for duplicating the selected HUD in the
     * design studio, rebuilt whenever the keys or the config value change.
     */
    private static OneConfigKeybind registeredHudDuplicateKeybind;

    /**
     * The keybinds currently registered with the {@link KeybindManager} for the other HUD editor actions (open
     * settings, toggle visibility, reset to default), rebuilt whenever the keys or the config values change.
     */
    private static final java.util.concurrent.atomic.AtomicReference<OneConfigKeybind>
        registeredHudSettingsKeybind = new java.util.concurrent.atomic.AtomicReference<>();
    private static final java.util.concurrent.atomic.AtomicReference<OneConfigKeybind>
        registeredHudVisibilityKeybind = new java.util.concurrent.atomic.AtomicReference<>();
    private static final java.util.concurrent.atomic.AtomicReference<OneConfigKeybind>
        registeredHudResetKeybind = new java.util.concurrent.atomic.AtomicReference<>();
    private static final java.util.concurrent.atomic.AtomicReference<OneConfigKeybind>
        registeredHudCopyKeybind = new java.util.concurrent.atomic.AtomicReference<>();
    private static final java.util.concurrent.atomic.AtomicReference<OneConfigKeybind>
        registeredHudCutKeybind = new java.util.concurrent.atomic.AtomicReference<>();
    private static final java.util.concurrent.atomic.AtomicReference<OneConfigKeybind>
        registeredHudPasteKeybind = new java.util.concurrent.atomic.AtomicReference<>();
    private static final java.util.concurrent.atomic.AtomicReference<OneConfigKeybind>
        registeredHudDeleteKeybind = new java.util.concurrent.atomic.AtomicReference<>();
    private static final java.util.concurrent.atomic.AtomicReference<OneConfigKeybind>
        registeredHudSelectAllKeybind = new java.util.concurrent.atomic.AtomicReference<>();
    private static final java.util.concurrent.atomic.AtomicReference<OneConfigKeybind>
        registeredHudLockKeybind = new java.util.concurrent.atomic.AtomicReference<>();

    public OneConfigConfig() {
        super("oneconfig.json", "assets/oneconfig/brand/oneconfig-icon.svg", "OneConfig", Category.QOL);
        INSTANCE = this;
    }

    @Override
    protected void initialize(boolean byConfigManager) {
        super.initialize(byConfigManager);
        if (tree == null) {
            return;
        }
        // "Custom GUI scale" only applies when "Use custom GUI scale" is enabled.
        addDependency("customScale", "useCustomScale");
        addDependency(
            "uiSharpening",
            "Reduced-resolution filter",
            () -> reducedResFilter != 0 ? Property.Display.SHOWN : Property.Display.DISABLED);
        // "Time before reset" only applies to the smart reset opening behavior (index 3).
        addDependency(
            "timeBeforeReset",
            "Opening Behavior",
            () -> openingBehavior == 3 ? Property.Display.SHOWN : Property.Display.HIDDEN);
        addDependency("enableUIMenuSounds", "enableUISounds");
        addDependency("enableUIClickSounds", "enableUISounds");
        addDependency("enableUISliderSounds", "enableUISounds");
        addDependency("enableHudEditorSounds", "enableUISounds");
        addDependency("enableUIMusicDucking", "enableUIAmbience");
        addDependency(
            "uiSoundVolume",
            "UI Sounds",
            () -> enableUISounds ? Property.Display.SHOWN : Property.Display.DISABLED);
        addDependency(
            "uiAmbienceVolume",
            "UI Ambience",
            () -> enableUIAmbience ? Property.Display.SHOWN : Property.Display.DISABLED);
        org.polyfrost.oneconfig.internal.ui.shell.ShellState.INSTANCE.setFlipTopOptionOrder(flipTopOptionOrder);
        addCallback(
            "flipTopOptionOrder", (Boolean v) -> {
                org.polyfrost.oneconfig.internal.ui.shell.ShellState.INSTANCE.setFlipTopOptionOrder(v);
                return false;
            });
        org.polyfrost.oneconfig.internal.ui.shell.ShellState.INSTANCE.setPageOpacity(pageOpacity);
        addCallback(
            "pageOpacity", (java.lang.Number v) -> {
                org.polyfrost.oneconfig.internal.ui.shell.ShellState.INSTANCE.setPageOpacity(v.floatValue());
                return false;
            });
        org.polyfrost.oneconfig.internal.ui.shell.ShellState.INSTANCE.setSidebarOpacity(sidebarOpacity);
        addCallback(
            "sidebarOpacity", (java.lang.Number v) -> {
                org.polyfrost.oneconfig.internal.ui.shell.ShellState.INSTANCE.setSidebarOpacity(v.floatValue());
                return false;
            });
        org.polyfrost.oneconfig.internal.ui.shell.ShellState.INSTANCE.setGlowOpacity(glowOpacity);
        addCallback(
            "glowOpacity", (java.lang.Number v) -> {
                org.polyfrost.oneconfig.internal.ui.shell.ShellState.INSTANCE.setGlowOpacity(v.floatValue());
                return false;
            });
        addCallback(
            "enableUIAmbience", (Boolean v) -> {
                org.polyfrost.oneconfig.internal.ui.sound.UiSounds.refreshAmbience();
                return false;
            });
        addCallback(
            "enableUIMusicDucking", (Boolean v) -> {
                org.polyfrost.oneconfig.internal.ui.sound.UiSounds.refreshAmbience();
                return false;
            });
        addCallback(
            "uiAmbienceVolume", (Float v) -> {
                org.polyfrost.oneconfig.internal.ui.sound.UiSounds.refreshAmbience();
                return false;
            });
        // Re-register the keybind whenever the user rebinds it, and once now to pick up the value loaded from disk
        // (the loaded keybind carries no action, so it must be rebuilt from its keys plus the supplied open action).
        addCallback(
            "oneConfigKeybind", (OneConfigKeybind kb) -> {
                refreshKeybind(kb);
                return false;
            });
        refreshKeybind(oneConfigKeybind);
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
            "masterHudEnabled", (Boolean v) -> {
                org.polyfrost.oneconfig.api.hud.v1.HudManager.masterHudEnabled = v;
                return false;
            });
        org.polyfrost.oneconfig.api.hud.v1.HudManager.masterHudEnabled = masterHudEnabled;
    }

    /**
     * Supplies the action run when the OneConfig keybind is pressed. Called by the minecraft module, as the action
     * references platform classes this module cannot depend on. See {@link #openAction}.
     */
    public static void setOpenAction(Function1<Boolean, Boolean> action) {
        openAction = action;
        refreshKeybind(oneConfigKeybind);
    }

    /**
     * Rebuilds the registered keybind from {@code src}'s (GLFW) keys and the supplied {@link #openAction},
     * re-registering it with the {@link KeybindManager}.
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
     * Rebuilds the registered duplicate-HUD keybind from {@code hudDuplicateKeybind}'s (GLFW) keys, re-registering it
     * with the {@link KeybindManager}. The action is resolved from the config field's keys rather than stored on the
     * keybind itself so it survives deserialization.
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
     * Rebuilds one of the additional HUD editor keybinds from its config field's (GLFW) keys, re-registering it
     * with the {@link KeybindManager}. The action is resolved from the config field's keys rather than stored on the
     * keybind itself so it survives deserialization.
     */
    private static void refreshHudEditorKeybind(
        OneConfigKeybind src,
        java.util.concurrent.atomic.AtomicReference<OneConfigKeybind> slot,
        Function1<Boolean, Boolean> action) {
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
            action);
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

    /**
     * Called once after the first-launch message has been shown. Flips {@link #showFirstLaunchMessage} off and
     * persists it so the message is not shown again until the user re-enables it from the menu.
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
}
