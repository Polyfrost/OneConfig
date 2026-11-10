package org.polyfrost.oneconfig.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.compose.render.PolyColor;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.config.v1.Node;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.config.v1.annotations.Color;
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown;
import org.polyfrost.oneconfig.api.config.v1.annotations.Include;
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;
import org.polyfrost.oneconfig.internal.ui.components.AnimationsMotionScale;
import org.polyfrost.oneconfig.internal.ui.components.ReducedMotion;
import org.polyfrost.oneconfig.internal.ui.shell.ShellAppearance;
import org.polyfrost.oneconfig.internal.ui.shell.ShellController;
import org.polyfrost.oneconfig.internal.ui.sound.SoundThemeSelection;
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent;
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds;
import org.polyfrost.oneconfig.internal.ui.themes.GlassTint;
import org.polyfrost.oneconfig.internal.ui.themes.PaletteAccent;
import org.polyfrost.oneconfig.internal.ui.themes.ReduceTransparency;
import org.polyfrost.oneconfig.internal.ui.themes.ProviderKt;

public class ThemeConfig extends Config {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Themes");

    private static final String PREFERENCES_ID = "oneconfig.json";

    private static final String[] MOVED_FROM_PREFERENCES = {"uiScale", "pixelGrid", "pageOpacity", "sidebarOpacity", "glowOpacity", "enableBackgroundBlur", "guiOpenAnimation", "guiClosingAnimation", "animationTime", "showPageAnimations", "pageAnimationDuration", "reducedMotion", "showOpeningPageAnimation"};

    private static final String[] ANIMATION_OPTIONS = {"reducedMotion", "guiOpenAnimation", "guiClosingAnimation", "animationTime", "showPageAnimations", "pageAnimationDuration", "showOpeningPageAnimation"};

    private static final String[] TRANSPARENCY_OPTIONS = {"pageOpacity", "sidebarOpacity", "glowOpacity", "enableBackgroundBlur"};

    public static final int SOUND_THEME_MATCH = 0;
    public static final int SOUND_THEME_ONECONFIG = 1;
    public static final int SOUND_THEME_MINECRAFT = 2;

    @Include
    public static String activeTheme = "Polyfrosted Dark";

    @Color(title = "Accent Color", description = "The accent color of the OneConfig Interface", icon = "paintbrush", gradient = true)
    public static PolyColor accentColor = new PolyColor(0xFF2B4BFF);

    @Switch(title = "Animations", description = "Turns every animation in OneConfig on or off", icon = "refresh")
    public static boolean animations = true;

    @Include
    public static boolean polyfrostedRenameApplied = false;

    @Include
    public static boolean preferencesMigrated = false;

    @Include
    public static boolean flipOrderMigrated = false;

    @Slider(
        title = "oneconfig.preferences.ui_scale.title",
        subcategory = "oneconfig.preferences.category.gui",
        min = 50f,
        max = 200f,
        step = 5f,
        description = "oneconfig.preferences.ui_scale.description"
    )
    public static float uiScale = 100f;

    @Switch(
        title = "oneconfig.preferences.reduce_transparency.title",
        subcategory = "oneconfig.preferences.category.gui",
        description = "oneconfig.preferences.reduce_transparency.description"
    )
    public static boolean reduceTransparency = false;

    @Switch(
        title = "oneconfig.preferences.flip_top_option_order.title",
        subcategory = "oneconfig.preferences.category.gui",
        description = "oneconfig.preferences.flip_top_option_order.description"
    )
    public static boolean flipTopOptionOrder = false;

    @Switch(
        title = "oneconfig.preferences.reduced_motion.title",
        subcategory = "oneconfig.preferences.category.animations",
        description = "oneconfig.preferences.reduced_motion.description"
    )
    public static boolean reducedMotion = false;

    @Switch(
        title = "oneconfig.preferences.opening_animation.title",
        subcategory = "oneconfig.preferences.category.animations",
        description = "oneconfig.preferences.opening_animation.description"
    )
    public static boolean guiOpenAnimation = true;

    @Switch(
        title = "oneconfig.preferences.closing_animation.title",
        subcategory = "oneconfig.preferences.category.animations",
        description = "oneconfig.preferences.closing_animation.description"
    )
    public static boolean guiClosingAnimation = true;

    @Slider(
        title = "oneconfig.preferences.opening_time.title",
        subcategory = "oneconfig.preferences.category.animations",
        min = 0.05f,
        max = 2f,
        step = 0.05f,
        description = "oneconfig.preferences.opening_time.description"
    )
    public static float animationTime = 0.6f;

    @Switch(
        title = "oneconfig.preferences.show_page_animations.title",
        subcategory = "oneconfig.preferences.category.animations",
        description = "oneconfig.preferences.show_page_animations.description"
    )
    public static boolean showPageAnimations = true;

    @Slider(
        title = "oneconfig.preferences.page_animation_duration.title",
        subcategory = "oneconfig.preferences.category.animations",
        min = 0.1f,
        max = 0.6f,
        step = 0.05f,
        description = "oneconfig.preferences.page_animation_duration.description"
    )
    public static float pageAnimationDuration = 0.3f;

    @Switch(
        title = "oneconfig.preferences.show_opening_page_animation.title",
        subcategory = "oneconfig.preferences.category.animations",
        description = "oneconfig.preferences.show_opening_page_animation.description"
    )
    public static boolean showOpeningPageAnimation = false;

    @Dropdown(
        title = "oneconfig.preferences.pixel_grid.title",
        subcategory = "oneconfig.preferences.category.advanced",
        options = {
            "oneconfig.preferences.pixel_grid.auto",
            "oneconfig.preferences.pixel_grid.always",
            "oneconfig.preferences.pixel_grid.never"
        },
        description = "oneconfig.preferences.pixel_grid.description"
    )
    public static int pixelGrid = 0;

    @Slider(
        title = "oneconfig.preferences.page_opacity.title",
        subcategory = "oneconfig.preferences.category.advanced",
        min = 0f,
        max = 100f,
        step = 1f,
        description = "oneconfig.preferences.page_opacity.description"
    )
    public static float pageOpacity = 88f;

    @Slider(
        title = "oneconfig.preferences.sidebar_opacity.title",
        subcategory = "oneconfig.preferences.category.advanced",
        min = 0f,
        max = 100f,
        step = 1f,
        description = "oneconfig.preferences.sidebar_opacity.description"
    )
    public static float sidebarOpacity = 80f;

    @Slider(
        title = "oneconfig.preferences.glow_opacity.title",
        subcategory = "oneconfig.preferences.category.advanced",
        min = 0f,
        max = 100f,
        step = 1f,
        description = "oneconfig.preferences.glow_opacity.description"
    )
    public static float glowOpacity = 25f;

    @Switch(
        title = "oneconfig.preferences.background_blur.title",
        subcategory = "oneconfig.preferences.category.advanced",
        description = "oneconfig.preferences.background_blur.description"
    )
    public static boolean enableBackgroundBlur = true;

    @Include
    public static int soundTheme = SOUND_THEME_MATCH;

    @Include
    public static int glassTint = GlassTint.CLEAR;

    @Include
    public static String paletteAccents = "";

    public static ThemeConfig INSTANCE;

    public ThemeConfig() {
        super("themes.json", "assets/oneconfig/brand/oneconfig-icon.svg", "Themes", Category.QOL);
        INSTANCE = this;
    }

    public static void selectSoundTheme(int value) {
        if (INSTANCE == null || INSTANCE.tree == null) {
            soundTheme = value;
            return;
        }
        INSTANCE.getProperty("soundTheme").setAs(value);
        INSTANCE.save();
    }

    public static void selectGlassTint(int value) {
        setIncluded("glassTint", value);
    }

    public static void savePaletteAccents() {
        setIncluded("paletteAccents", PaletteAccent.encode());
    }

    private static void setIncluded(String key, Object value) {
        if (INSTANCE == null || INSTANCE.tree == null) {
            if (key.equals("glassTint")) glassTint = (Integer) value;
            else paletteAccents = (String) value;
            return;
        }
        INSTANCE.getProperty(key).setAs(value);
        INSTANCE.save();
    }

    @Override
    protected void initialize(boolean byConfigManager) {
        if (tree == null) {
            polyfrostedRenameApplied = !Files.exists(ConfigManager.active().getFolder().resolve(id));
            preferencesMigrated = false;
            flipOrderMigrated = false;
            soundTheme = SOUND_THEME_MATCH;
            reduceTransparency = false;
            glassTint = GlassTint.CLEAR;
            paletteAccents = "";
        }
        super.initialize(byConfigManager);
        if (tree == null) {
            return;
        }
        for (String option : ANIMATION_OPTIONS) hideIf(option, "animations");
        for (String option : TRANSPARENCY_OPTIONS) hideIf(option, () -> reduceTransparency);
        addCallback(
            "pageOpacity", (Number v) -> {
                pageOpacity = v.floatValue();
                updateAppearance();
                return false;
            });
        addCallback(
            "sidebarOpacity", (Number v) -> {
                sidebarOpacity = v.floatValue();
                updateAppearance();
                return false;
            });
        addCallback(
            "glowOpacity", (Number v) -> {
                glowOpacity = v.floatValue();
                updateAppearance();
                return false;
            });
        addCallback(
            "reduceTransparency", (Boolean v) -> {
                reduceTransparency = v;
                ReduceTransparency.setEnabled(v);
                updateAppearance();
                return false;
            });
        addCallback(
            "flipTopOptionOrder", (Boolean v) -> {
                flipTopOptionOrder = v;
                updateAppearance();
                return false;
            });
        addCallback(
            "animations", (Boolean v) -> {
                AnimationsMotionScale.setAnimations(v);
                ReducedMotion.setEnabled(reducedMotion || !v);
                return false;
            });
        addCallback(
            "uiScale", (Number v) -> {
                ProviderKt.setUiScalePercent(v.floatValue());
                return false;
            });
        addCallback(
            "pixelGrid", (Number v) -> {
                ProviderKt.setPixelGridMode(v.intValue());
                return false;
            });
        addCallback(
            "reducedMotion", (Boolean v) -> {
                ReducedMotion.setEnabled(v || !animations);
                return false;
            });
        addCallback(
            "glassTint", (Number v) -> {
                GlassTint.setMode(v.intValue());
                return false;
            });
        addCallback(
            "paletteAccents", (String v) -> {
                PaletteAccent.load(v);
                return false;
            });
        addCallback(
            "soundTheme", (Number v) -> {
                SoundThemeSelection.INSTANCE.setValue(v.intValue());
                UiSounds.onSoundThemeChanged(UiSoundEvent.CLICK);
                return false;
            });
    }

    @Override
    protected void onInitialized(Tree tree) {
        if (!preferencesMigrated) migrateFromPreferences(tree);
        if (!flipOrderMigrated) migrateFlipOrder(tree);
        updateAppearance();
        ProviderKt.setUiScalePercent(uiScale);
        ProviderKt.setPixelGridMode(pixelGrid);
        SoundThemeSelection.INSTANCE.setValue(soundTheme);
        GlassTint.setMode(glassTint);
        ReduceTransparency.setEnabled(reduceTransparency);
        PaletteAccent.load(paletteAccents);
        AnimationsMotionScale.setAnimations(animations);
        ReducedMotion.setEnabled(reducedMotion || !animations);
    }

    private static void updateAppearance() {
        ShellAppearance appearance = ShellController.getShared().getAppearance();
        if (reduceTransparency) {
            appearance.update(flipTopOptionOrder, 100f, 100f, 0f);
        } else {
            appearance.update(flipTopOptionOrder, pageOpacity, sidebarOpacity, glowOpacity);
        }
    }

    private void migrateFlipOrder(Tree tree) {
        Path legacy = ConfigManager.active().getFolder().resolve(PREFERENCES_ID);
        if (Files.exists(legacy)) {
            try {
                Tree old = ConfigManager.active().getNoRegister(legacy);
                Node value = old == null ? null : old.get("flipTopOptionOrder");
                if (value instanceof Property) {
                    getProperty(tree, "flipTopOptionOrder").overwrite(value, false, false, null);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to move flipTopOptionOrder from {} into {}, retrying on the next load", PREFERENCES_ID, id, e);
                return;
            }
        }
        flipOrderMigrated = true;
        save();
    }

    private void migrateFromPreferences(Tree tree) {
        Path legacy = ConfigManager.active().getFolder().resolve(PREFERENCES_ID);
        if (Files.exists(legacy)) {
            Tree old = null;
            try {
                old = ConfigManager.active().getNoRegister(legacy);
            } catch (Exception e) {
                LOGGER.error("Failed to read {} to move appearance settings into {}, retrying on the next load", PREFERENCES_ID, id, e);
                return;
            }
            if (old != null) {
                for (String key : MOVED_FROM_PREFERENCES) {
                    Node value = old.get(key);
                    if (!(value instanceof Property)) continue;
                    try {
                        getProperty(tree, key).overwrite(value, false, false, null);
                    } catch (Exception e) {
                        LOGGER.error("Failed to move {} from {} into {}", key, PREFERENCES_ID, id, e);
                    }
                }
            }
        }
        preferencesMigrated = true;
        save();
    }
}
