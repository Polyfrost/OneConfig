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
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind;

import kotlin.jvm.functions.Function1;

public class OneConfigConfig extends Config {
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
        title = "oneconfig.preferences.window_blur.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.window_blur.description",
        descriptionTranslation = true
    )
    public static boolean enableWindowBlur = true;

    @Switch(
        title = "oneconfig.preferences.background_blur.title",
        titleTranslation = true,
        subcategory = "oneconfig.preferences.category.gui",
        subcategoryTranslation = true,
        description = "oneconfig.preferences.background_blur.description",
        descriptionTranslation = true
    )
    public static boolean enableBackgroundBlur = true;

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
