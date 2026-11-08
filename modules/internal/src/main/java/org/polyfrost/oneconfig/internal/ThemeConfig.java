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
import org.polyfrost.oneconfig.api.config.v1.annotations.Text;
import org.polyfrost.oneconfig.internal.ui.shell.ShellAppearance;
import org.polyfrost.oneconfig.internal.ui.shell.ShellController;
import org.polyfrost.oneconfig.internal.ui.sound.SoundThemeSelection;
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds;
import org.polyfrost.oneconfig.internal.ui.themes.ProviderKt;

public class ThemeConfig extends Config {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Themes");

    private static final String PREFERENCES_ID = "oneconfig.json";

    private static final String[] MOVED_FROM_PREFERENCES = {"uiScale", "pixelGrid", "pageOpacity", "sidebarOpacity", "glowOpacity", "enableBackgroundBlur", "hudDragUiOpacity", "guiOpenAnimation", "guiClosingAnimation", "animationTime", "showPageAnimations", "pageAnimationDuration", "reducedMotion", "showOpeningPageAnimation"};

    public static final int SOUND_THEME_MATCH = 0;
    public static final int SOUND_THEME_ONECONFIG = 1;
    public static final int SOUND_THEME_MINECRAFT = 2;

    @Text(title = "Active Theme")
    public static String activeTheme = "Polyfrosted Dark";

    @Color(title = "Accent Color", description = "The accent color of the OneConfig Interface", icon = "paintbrush", gradient = true)
    public static PolyColor accentColor = new PolyColor(0xFF2B4BFF);

    @Switch(title = "Animations", description = "Smooth, clean animations in the OneConfig Interface", icon = "refresh")
    public static boolean animations = true;

    @Switch(title = "Polyfrosted Rename Applied")
    public static boolean polyfrostedRenameApplied = false;

    @Include
    public static boolean preferencesMigrated = false;

    @Slider(
        title = "oneconfig.preferences.ui_scale.title",
        subcategory = "oneconfig.preferences.category.gui",
        min = 50f,
        max = 200f,
        step = 5f,
        description = "oneconfig.preferences.ui_scale.description"
    )
    public static float uiScale = 100f;

    @Dropdown(
        title = "oneconfig.preferences.pixel_grid.title",
        subcategory = "oneconfig.preferences.category.gui",
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
        subcategory = "oneconfig.preferences.category.gui",
        min = 0f,
        max = 100f,
        step = 1f,
        description = "oneconfig.preferences.page_opacity.description"
    )
    public static float pageOpacity = 88f;

    @Slider(
        title = "oneconfig.preferences.sidebar_opacity.title",
        subcategory = "oneconfig.preferences.category.gui",
        min = 0f,
        max = 100f,
        step = 1f,
        description = "oneconfig.preferences.sidebar_opacity.description"
    )
    public static float sidebarOpacity = 80f;

    @Slider(
        title = "oneconfig.preferences.glow_opacity.title",
        subcategory = "oneconfig.preferences.category.gui",
        min = 0f,
        max = 100f,
        step = 1f,
        description = "oneconfig.preferences.glow_opacity.description"
    )
    public static float glowOpacity = 25f;

    @Switch(
        title = "oneconfig.preferences.background_blur.title",
        subcategory = "oneconfig.preferences.category.gui",
        description = "oneconfig.preferences.background_blur.description"
    )
    public static boolean enableBackgroundBlur = true;

    @Slider(
        title = "oneconfig.preferences.hud_drag_ui_opacity.title",
        subcategory = "oneconfig.preferences.category.gui",
        min = 0f,
        max = 1f,
        step = 0.05f,
        description = "oneconfig.preferences.hud_drag_ui_opacity.description"
    )
    public static float hudDragUiOpacity = 0.60f;

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

    @Include
    public static int soundTheme = SOUND_THEME_MATCH;

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

    @Override
    protected void initialize(boolean byConfigManager) {
        if (tree == null) {
            polyfrostedRenameApplied = !Files.exists(ConfigManager.active().getFolder().resolve(id));
            preferencesMigrated = false;
        }
        super.initialize(byConfigManager);
        hideIf("activeTheme", () -> true);
        hideIf("polyfrostedRenameApplied", () -> true);
        if (tree == null) {
            return;
        }
        addCallback(
            "pageOpacity", (Number v) -> {
                updateAppearance(v.floatValue(), sidebarOpacity, glowOpacity);
                return false;
            });
        addCallback(
            "sidebarOpacity", (Number v) -> {
                updateAppearance(pageOpacity, v.floatValue(), glowOpacity);
                return false;
            });
        addCallback(
            "glowOpacity", (Number v) -> {
                updateAppearance(pageOpacity, sidebarOpacity, v.floatValue());
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
            "soundTheme", (Number v) -> {
                SoundThemeSelection.INSTANCE.setValue(v.intValue());
                UiSounds.onSoundThemeChanged();
                return false;
            });
    }

    @Override
    protected void onInitialized(Tree tree) {
        if (!preferencesMigrated) migrateFromPreferences(tree);
        updateAppearance(pageOpacity, sidebarOpacity, glowOpacity);
        ProviderKt.setUiScalePercent(uiScale);
        ProviderKt.setPixelGridMode(pixelGrid);
        SoundThemeSelection.INSTANCE.setValue(soundTheme);
    }

    private static void updateAppearance(float page, float sidebar, float glow) {
        ShellAppearance appearance = ShellController.getShared().getAppearance();
        appearance.update(appearance.getFlipTopOptionOrder(), page, sidebar, glow);
    }

    private void migrateFromPreferences(Tree tree) {
        Path legacy = ConfigManager.active().getFolder().resolve(PREFERENCES_ID);
        if (Files.exists(legacy)) {
            Tree old = null;
            try {
                old = ConfigManager.active().getNoRegister(legacy);
            } catch (Exception e) {
                LOGGER.error("Failed to read {} to move appearance settings into {}", PREFERENCES_ID, id, e);
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
