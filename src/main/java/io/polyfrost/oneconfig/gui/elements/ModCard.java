package io.polyfrost.oneconfig.gui.elements;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModCard extends BasicElement {
    private final String modName, iconPath;
    private boolean active, disabled, favorite;

    public ModCard(@NotNull String modName, @Nullable String iconPath, boolean active, boolean disabled, boolean favorite) {
        super(224, 119, true);
        this.modName = modName;
        this.iconPath = iconPath;
        this.active = active;
        this.disabled = disabled;
        this.favorite = favorite;
    }

    @Override
    public void draw(long vg, int x, int y) {
        RenderManager.drawRoundedRect(vg, x, y, width, 100, OneConfigConfig.GRAY_600, 12f);
        RenderManager.drawRoundedRect(vg, x, y + 75, width, 32, OneConfigConfig.BLUE_600, 12f);
        RenderManager.drawRect(vg, x, y + 75, width, 12, OneConfigConfig.BLUE_600);
        if(iconPath != null) {
            RenderManager.drawImage(vg, iconPath, x, y, width, 87);
        } else {
            RenderManager.drawImage(vg, "assets/oneconfig/textures/box.png", x + 98, y + 19, 40, 40);
        }
        RenderManager.drawString(vg, modName, x + 12, y + 92, OneConfigConfig.WHITE, 14f, Fonts.INTER_MEDIUM);
        if(favorite) {
            // TODO
        }
    }
}
