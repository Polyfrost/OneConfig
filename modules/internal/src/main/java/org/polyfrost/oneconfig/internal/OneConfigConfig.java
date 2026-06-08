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
    @Keybind(title = "OneConfig Keybind", titleKey = "oneconfig.preferences.keybind.title", subcategory = "GUI", subcategoryKey = "oneconfig.preferences.category.gui", description = "The keybind used to open the OneConfig menu.", descriptionKey = "oneconfig.preferences.keybind.description")
    public static OneConfigKeybind oneConfigKeybind = new OneConfigKeybind(new int[]{344}, null, KeyModifiers.NONE, 0L, pressed -> true);

    @Switch(title = "Window Blur", titleKey = "oneconfig.preferences.window_blur.title", subcategory = "GUI", subcategoryKey = "oneconfig.preferences.category.gui", description = "Blurs the area behind the OneConfig window.", descriptionKey = "oneconfig.preferences.window_blur.description")
    public static boolean enableWindowBlur = true;

    @Switch(title = "Background Blur", titleKey = "oneconfig.preferences.background_blur.title", subcategory = "GUI", subcategoryKey = "oneconfig.preferences.category.gui", description = "Blurs the full game background when the OneConfig menu is open.", descriptionKey = "oneconfig.preferences.background_blur.description")
    public static boolean enableBackgroundBlur = true;

    @Switch(title = "Use custom GUI scale", titleKey = "oneconfig.preferences.use_custom_scale.title", subcategory = "GUI", subcategoryKey = "oneconfig.preferences.category.gui", description = "Override the Minecraft GUI scale for the OneConfig menu.", descriptionKey = "oneconfig.preferences.use_custom_scale.description")
    public static boolean useCustomScale = false;

    @Slider(title = "Custom GUI scale", titleKey = "oneconfig.preferences.custom_scale.title", subcategory = "GUI", subcategoryKey = "oneconfig.preferences.category.gui", min = 0.5f, max = 2f, step = 0.05f, description = "The custom GUI scale to use for the OneConfig menu.", descriptionKey = "oneconfig.preferences.custom_scale.description")
    public static float customScale = 1f;

    @Dropdown(title = "Opening Behavior", titleKey = "oneconfig.preferences.opening_behavior.title", subcategory = "GUI", subcategoryKey = "oneconfig.preferences.category.gui", options = {"Mods", "Preferences", "Previous page", "Smart reset"}, optionsKey = {"oneconfig.mods", "oneconfig.preferences", "oneconfig.preferences.opening_behavior.previous_page", "oneconfig.preferences.opening_behavior.smart_reset"}, description = "Which page to open when the OneConfig menu is launched.", descriptionKey = "oneconfig.preferences.opening_behavior.description")
    public static int openingBehavior = 3;

    @Switch(title = "Show opening page animation", titleKey = "oneconfig.preferences.show_opening_page_animation.title", subcategory = "GUI", subcategoryKey = "oneconfig.preferences.category.gui", description = "Plays the page animation when the menu is first opened.", descriptionKey = "oneconfig.preferences.show_opening_page_animation.description")
    public static boolean showOpeningPageAnimation = false;

    @Switch(title = "Instant Search", titleKey = "oneconfig.preferences.instant_search.title", subcategory = "GUI", subcategoryKey = "oneconfig.preferences.category.gui", description = "Focuses the search bar when the OneConfig menu opens.", descriptionKey = "oneconfig.preferences.instant_search.description")
    public static boolean instantSearch = true;

    @Slider(title = "Time before reset", titleKey = "oneconfig.preferences.time_before_reset.title", subcategory = "GUI", subcategoryKey = "oneconfig.preferences.category.gui", min = 5f, max = 60f, step = 1f, description = "Seconds of inactivity before the smart reset returns to the default page.", descriptionKey = "oneconfig.preferences.time_before_reset.description")
    public static float timeBeforeReset = 15f;

    @Number(title = "Search Distance", titleKey = "oneconfig.preferences.search_distance.title", subcategory = "Search", subcategoryKey = "oneconfig.preferences.category.search", min = 0f, max = 10f, description = "The maximum Levenshtein distance used when fuzzy-matching search queries.", descriptionKey = "oneconfig.preferences.search_distance.description")
    public static int searchDistance = 2;

    @Switch(title = "Opening Animation", titleKey = "oneconfig.preferences.opening_animation.title", subcategory = "Animations", subcategoryKey = "oneconfig.preferences.category.animations", description = "Plays an animation when opening the OneConfig menu.", descriptionKey = "oneconfig.preferences.opening_animation.description")
    public static boolean guiOpenAnimation = true;

    @Switch(title = "Closing Animation", titleKey = "oneconfig.preferences.closing_animation.title", subcategory = "Animations", subcategoryKey = "oneconfig.preferences.category.animations", description = "Plays an animation when closing the OneConfig menu.", descriptionKey = "oneconfig.preferences.closing_animation.description")
    public static boolean guiClosingAnimation = true;

    @Slider(title = "Opening Time", titleKey = "oneconfig.preferences.opening_time.title", subcategory = "Animations", subcategoryKey = "oneconfig.preferences.category.animations", min = 0.05f, max = 2f, step = 0.05f, description = "Duration of the opening and closing animations, in seconds.", descriptionKey = "oneconfig.preferences.opening_time.description")
    public static float animationTime = 0.6f;

    @Switch(title = "Show Page Animations", titleKey = "oneconfig.preferences.show_page_animations.title", subcategory = "Animations", subcategoryKey = "oneconfig.preferences.category.animations", description = "Animate transitions between pages.", descriptionKey = "oneconfig.preferences.show_page_animations.description")
    public static boolean showPageAnimations = true;

    @Switch(title = "UI Sounds", titleKey = "oneconfig.preferences.ui_sounds.title", subcategory = "Sounds", subcategoryKey = "oneconfig.preferences.category.sounds", description = "Play sound effects when interacting with the OneConfig menu and HUD designer.", descriptionKey = "oneconfig.preferences.ui_sounds.description")
    public static boolean enableUISounds = true;

    @Switch(title = "Menu Sounds", titleKey = "oneconfig.preferences.menu_sounds.title", subcategory = "Sounds", subcategoryKey = "oneconfig.preferences.category.sounds", description = "Play sounds when opening and closing OneConfig screens.", descriptionKey = "oneconfig.preferences.menu_sounds.description")
    public static boolean enableUIMenuSounds = true;

    @Switch(title = "Click Sounds", titleKey = "oneconfig.preferences.click_sounds.title", subcategory = "Sounds", subcategoryKey = "oneconfig.preferences.category.sounds", description = "Play sounds when clicking OneConfig controls.", descriptionKey = "oneconfig.preferences.click_sounds.description")
    public static boolean enableUIClickSounds = true;

    @Switch(title = "Slider Sounds", titleKey = "oneconfig.preferences.slider_sounds.title", subcategory = "Sounds", subcategoryKey = "oneconfig.preferences.category.sounds", description = "Play tick sounds when dragging sliders and ordered lists.", descriptionKey = "oneconfig.preferences.slider_sounds.description")
    public static boolean enableUISliderSounds = true;

    @Switch(title = "HUD Editor Sounds", titleKey = "oneconfig.preferences.hud_editor_sounds.title", subcategory = "Sounds", subcategoryKey = "oneconfig.preferences.category.sounds", description = "Play sounds when selecting, dragging, and resizing HUD elements.", descriptionKey = "oneconfig.preferences.hud_editor_sounds.description")
    public static boolean enableHudEditorSounds = true;

    @Switch(title = "UI Ambience", titleKey = "oneconfig.preferences.ui_ambience.title", subcategory = "Sounds", subcategoryKey = "oneconfig.preferences.category.sounds", description = "Play a soft ambient loop while the OneConfig menu or HUD designer is open.", descriptionKey = "oneconfig.preferences.ui_ambience.description")
    public static boolean enableUIAmbience = true;

    @Switch(title = "Duck Music During Ambience", titleKey = "oneconfig.preferences.duck_music.title", subcategory = "Sounds", subcategoryKey = "oneconfig.preferences.category.sounds", description = "Lower Minecraft music while OneConfig ambience is playing.", descriptionKey = "oneconfig.preferences.duck_music.description")
    public static boolean enableUIMusicDucking = true;

    @Slider(title = "UI Sound Volume", titleKey = "oneconfig.preferences.ui_sound_volume.title", subcategory = "Sounds", subcategoryKey = "oneconfig.preferences.category.sounds", min = 0f, max = 1f, step = 0.05f, description = "Volume of OneConfig's UI sound effects.", descriptionKey = "oneconfig.preferences.ui_sound_volume.description")
    public static float uiSoundVolume = 1f;

    @Slider(title = "UI Ambience Volume", titleKey = "oneconfig.preferences.ui_ambience_volume.title", subcategory = "Sounds", subcategoryKey = "oneconfig.preferences.category.sounds", min = 0f, max = 1f, step = 0.05f, description = "Volume of OneConfig's UI ambience.", descriptionKey = "oneconfig.preferences.ui_ambience_volume.description")
    public static float uiAmbienceVolume = 1f;

    @Slider(title = "Page Animation Duration", titleKey = "oneconfig.preferences.page_animation_duration.title", subcategory = "Animations", subcategoryKey = "oneconfig.preferences.category.animations", min = 0.1f, max = 0.6f, step = 0.05f, description = "Duration of page transition animations, in seconds.", descriptionKey = "oneconfig.preferences.page_animation_duration.description")
    public static float pageAnimationDuration = 0.3f;

    @Switch(title = "Show First Launch Message", titleKey = "oneconfig.preferences.show_first_launch_message.title", subcategory = "General", subcategoryKey = "oneconfig.preferences.category.general", description = "Show the welcome message the first time OneConfig is launched.", descriptionKey = "oneconfig.preferences.show_first_launch_message.description")
    public static boolean showFirstLaunchMessage = true;

    /** The live instance, used to persist programmatic changes (e.g. {@link #markFirstLaunchShown()}). */
    public static OneConfigConfig INSTANCE;

    /**
     * The action run when {@link #oneConfigKeybind} is pressed. Supplied by the minecraft module rather than stored
     * on the keybind itself, because a keybind's action is transient and is lost whenever the keybind is loaded from
     * disk (deserialization rebuilds the keybind with a null action). Keeping it here lets us always reattach it.
     */
    private static Function1<Boolean, Boolean> openAction;

    /** The keybind currently registered with the {@link KeybindManager}, rebuilt whenever the keys or action change. */
    private static OneConfigKeybind registeredKeybind;

    public OneConfigConfig() {
        super("oneconfig.json", "assets/oneconfig/brand/oneconfig.svg", "OneConfig", Category.QOL);
        INSTANCE = this;
    }

    @Override
    protected void initialize(boolean byConfigManager) {
        super.initialize(byConfigManager);
        if (tree == null) return;
        // "Custom GUI scale" only applies when "Use custom GUI scale" is enabled.
        addDependency("customScale", "useCustomScale");
        // "Time before reset" only applies to the smart reset opening behavior (index 3).
        addDependency("timeBeforeReset", "Opening Behavior", () -> openingBehavior == 3 ? Property.Display.SHOWN : Property.Display.HIDDEN);
        addDependency("enableUIMenuSounds", "enableUISounds");
        addDependency("enableUIClickSounds", "enableUISounds");
        addDependency("enableUISliderSounds", "enableUISounds");
        addDependency("enableHudEditorSounds", "enableUISounds");
        addDependency("enableUIMusicDucking", "enableUIAmbience");
        addDependency("uiSoundVolume", "UI Sounds", () -> enableUISounds ? Property.Display.SHOWN : Property.Display.DISABLED);
        addDependency("uiAmbienceVolume", "UI Ambience", () -> enableUIAmbience ? Property.Display.SHOWN : Property.Display.DISABLED);
        addCallback("enableUIAmbience", (Boolean v) -> {
            org.polyfrost.oneconfig.internal.ui.sound.UiSounds.refreshAmbience();
            return false;
        });
        addCallback("enableUIMusicDucking", (Boolean v) -> {
            org.polyfrost.oneconfig.internal.ui.sound.UiSounds.refreshAmbience();
            return false;
        });
        addCallback("uiAmbienceVolume", (Float v) -> {
            org.polyfrost.oneconfig.internal.ui.sound.UiSounds.refreshAmbience();
            return false;
        });
        // Re-register the keybind whenever the user rebinds it, and once now to pick up the value loaded from disk
        // (the loaded keybind carries no action, so it must be rebuilt from its keys plus the supplied open action).
        addCallback("oneConfigKeybind", (OneConfigKeybind kb) -> {
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
        if (src == null || openAction == null || !src.isBound()) return;
        registeredKeybind = new OneConfigKeybind(src.getKeyCodes(), src.getMouseBtns(), src.getMods(), src.getDurationNanos(), openAction);
        KeybindManager.register(registeredKeybind);
    }

    /**
     * Called once after the first-launch message has been shown. Flips {@link #showFirstLaunchMessage} off and
     * persists it so the message is not shown again until the user re-enables it from the menu.
     */
    public static void markFirstLaunchShown() {
        if (!showFirstLaunchMessage) return;
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
