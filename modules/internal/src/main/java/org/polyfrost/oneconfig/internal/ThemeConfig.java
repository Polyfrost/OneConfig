package org.polyfrost.oneconfig.internal;

import org.polyfrost.compose.render.PolyColor;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.annotations.Color;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;
import org.polyfrost.oneconfig.api.config.v1.annotations.Text;

public class ThemeConfig extends Config {
    @Text(title = "Active Theme")
    public static String activeTheme = "PolyGlass Dark";

    @Color(title = "Accent Color", description = "The accent color of the OneConfig Interface", icon = "paintbrush")
    public static PolyColor accentColor = new PolyColor(0xFF2B4BFF);

    @Switch(title = "Animations", description = "Smooth, clean animations in the OneConfig Interface", icon = "refresh")
    public static boolean animations = true;

    public ThemeConfig() {
        super("themes.json", "assets/oneconfig/brand/oneconfig-icon.svg", "Themes", Category.QOL);
    }

    @Override
    protected void initialize(boolean byConfigManager) {
        super.initialize(byConfigManager);
        hideIf("activeTheme", () -> true);
    }
}
