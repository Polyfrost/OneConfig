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
    @Keybind(title = "OneConfig Keybind", subcategory = "GUI", description = "The keybind used to open the OneConfig menu.")
    public static OneConfigKeybind oneConfigKeybind = new OneConfigKeybind(new int[]{344}, null, KeyModifiers.NONE, 0L, pressed -> true);

    @Switch(title = "Window Blur", subcategory = "GUI", description = "Blurs the area behind the OneConfig window.")
    public static boolean enableWindowBlur = true;

    @Switch(title = "Background Blur", subcategory = "GUI", description = "Blurs the full game background when the OneConfig menu is open.")
    public static boolean enableBackgroundBlur = true;

    @Switch(title = "Use custom GUI scale", subcategory = "GUI", description = "Override the Minecraft GUI scale for the OneConfig menu.")
    public static boolean useCustomScale = false;

    @Slider(title = "Custom GUI scale", subcategory = "GUI", min = 0.5f, max = 2f, step = 0.05f, description = "The custom GUI scale to use for the OneConfig menu.")
    public static float customScale = 1f;

    @Dropdown(title = "Opening Behavior", subcategory = "GUI", options = {"Mods", "Preferences", "Previous page", "Smart reset"}, description = "Which page to open when the OneConfig menu is launched.")
    public static int openingBehavior = 3;

    @Switch(title = "Show opening page animation", subcategory = "GUI", description = "Plays the page animation when the menu is first opened.")
    public static boolean showOpeningPageAnimation = false;

    @Slider(title = "Time before reset", subcategory = "GUI", min = 5f, max = 60f, step = 1f, description = "Seconds of inactivity before the smart reset returns to the default page.")
    public static float timeBeforeReset = 15f;

    @Number(title = "Search Distance", subcategory = "Search", min = 0f, max = 10f, description = "The maximum Levenshtein distance used when fuzzy-matching search queries.")
    public static int searchDistance = 2;

    @Switch(title = "Opening Animation", subcategory = "Animations", description = "Plays an animation when opening the OneConfig menu.")
    public static boolean guiOpenAnimation = true;

    @Switch(title = "Closing Animation", subcategory = "Animations", description = "Plays an animation when closing the OneConfig menu.")
    public static boolean guiClosingAnimation = true;

    @Slider(title = "Opening Time", subcategory = "Animations", min = 0.05f, max = 2f, step = 0.05f, description = "Duration of the opening and closing animations, in seconds.")
    public static float animationTime = 0.6f;

    @Switch(title = "Show Page Animations", subcategory = "Animations", description = "Animate transitions between pages.")
    public static boolean showPageAnimations = true;

    @Switch(title = "UI Sounds", subcategory = "Sounds", description = "Play sound effects when interacting with the OneConfig menu and HUD designer.")
    public static boolean enableUISounds = true;

    @Switch(title = "Menu Sounds", subcategory = "Sounds", description = "Play sounds when opening and closing OneConfig screens.")
    public static boolean enableUIMenuSounds = true;

    @Switch(title = "Click Sounds", subcategory = "Sounds", description = "Play sounds when clicking OneConfig controls.")
    public static boolean enableUIClickSounds = true;

    @Switch(title = "Slider Sounds", subcategory = "Sounds", description = "Play tick sounds when dragging sliders and ordered lists.")
    public static boolean enableUISliderSounds = true;

    @Switch(title = "HUD Editor Sounds", subcategory = "Sounds", description = "Play sounds when selecting, dragging, and resizing HUD elements.")
    public static boolean enableHudEditorSounds = true;

    @Switch(title = "UI Ambience", subcategory = "Sounds", description = "Play a soft ambient loop while the OneConfig menu or HUD designer is open.")
    public static boolean enableUIAmbience = true;

    @Switch(title = "Duck Music During Ambience", subcategory = "Sounds", description = "Lower Minecraft music while OneConfig ambience is playing.")
    public static boolean enableUIMusicDucking = true;

    @Slider(title = "UI Sound Volume", subcategory = "Sounds", min = 0f, max = 1f, step = 0.05f, description = "Volume of OneConfig's UI sound effects.")
    public static float uiSoundVolume = 1f;

    @Slider(title = "UI Ambience Volume", subcategory = "Sounds", min = 0f, max = 1f, step = 0.05f, description = "Volume of OneConfig's UI ambience.")
    public static float uiAmbienceVolume = 1f;

    @Slider(title = "Page Animation Duration", subcategory = "Animations", min = 0.1f, max = 0.6f, step = 0.05f, description = "Duration of page transition animations, in seconds.")
    public static float pageAnimationDuration = 0.3f;

    @Switch(title = "Show First Launch Message", subcategory = "General", description = "Show the welcome message the first time OneConfig is launched.")
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
