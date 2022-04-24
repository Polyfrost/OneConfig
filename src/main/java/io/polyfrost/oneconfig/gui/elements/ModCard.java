package io.polyfrost.oneconfig.gui.elements;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.data.ModData;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.utils.ColorUtils;
import io.polyfrost.oneconfig.utils.InputUtils;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.nanovg.NanoVG;

public class ModCard extends BasicElement {
    private final String iconPath;
    private final ModData modData;
    private final BasicElement favoriteHitbox = new BasicElement(32, 32, -2, true);
    private boolean active, disabled, favorite;
    private int colorGray = OneConfigConfig.GRAY_600;
    private int colorPrimary = OneConfigConfig.BLUE_600;
    private boolean isHoveredMain = false;

    public ModCard(@NotNull ModData mod, @Nullable String iconPath, boolean active, boolean disabled, boolean favorite) {
        super(244, 119, false);
        this.modData = mod;
        this.iconPath = iconPath;
        this.active = active;
        toggled = active;
        this.disabled = disabled;
        this.favorite = favorite;
    }

    @Override
    public void draw(long vg, int x, int y) {
        if(disabled) NanoVG.nvgGlobalAlpha(vg, 0.5f);
        RenderManager.drawRoundedRectVaried(vg, x, y, width, 87, colorGray, 12f, 12f, 0f, 0f);
        RenderManager.drawRoundedRectVaried(vg, x, y + 87, width, 32, colorPrimary, 0f, 0f, 12f, 12f);
        RenderManager.drawLine(vg, x, y + 86, x + width, y + 86, 2,OneConfigConfig.GRAY_300);
        //RenderManager.drawRect(vg, x, y + 87, width, 12, colorPrimary);
        if(iconPath != null) {
            RenderManager.drawImage(vg, iconPath, x, y, width, 87);
        } else {
            RenderManager.drawImage(vg, "/assets/oneconfig/textures/box.png", x + 98, y + 19, 48, 48);
        }
        //favoriteHitbox.draw(vg, x + 212, y + 87);
        favoriteHitbox.update(x + 212, y + 87);
        favoriteHitbox.currentColor = ColorUtils.getColor(favoriteHitbox.currentColor, favoriteHitbox.colorPalette, favoriteHitbox.hovered, favoriteHitbox.clicked);
        RenderManager.drawRoundedRectVaried(vg, x + 212, y + 87, 32, 32, favoriteHitbox.currentColor, 0f, 0f, 12f, 0f);
        favorite = favoriteHitbox.isToggled();
        RenderManager.drawString(vg, modData.name, x + 12, y + 102, OneConfigConfig.WHITE, 14f, Fonts.INTER_MEDIUM);
        if(favorite) {
            RenderManager.drawImage(vg, "/assets/oneconfig/textures/love.png", x + 220, y + 95, 16, 16);
        } else {
            RenderManager.drawImage(vg, "/assets/oneconfig/textures/love_empty.png", x + 220, y + 95, 16, 16);
        }
        super.update(x, y);
        isHoveredMain = InputUtils.isAreaHovered(x, y, width, 87);
        boolean isHoveredSecondary = InputUtils.isAreaHovered(x, y + 87, width - 32, 32) && !disabled;
        colorGray = ColorUtils.getColor(colorGray, 0, isHoveredMain, clicked && isHoveredMain);
        if(active && !disabled) {
            colorPrimary = ColorUtils.getColor(colorPrimary, 1, isHoveredSecondary, clicked && isHoveredSecondary);
        } else colorPrimary = ColorUtils.smoothColor(colorPrimary, OneConfigConfig.GRAY_500, OneConfigConfig.GRAY_400, isHoveredSecondary, 20f);

        if(clicked && isHoveredMain) {
            if(!active) toggled = false;
        }
        if(clicked && favoriteHitbox.hovered) toggled = false;
        if(clicked && !isHoveredSecondary && active) toggled = true;
        if(!active & disabled) toggled = false;
        //RenderManager.drawString(vg, "active=" + active, x + 300, y + 12, OneConfigConfig.WHITE, 12f, Fonts.INTER_MEDIUM);        // TODO remove debug stuff
        //RenderManager.drawString(vg, "disabled=" + disabled, x + 300, y + 24, OneConfigConfig.WHITE, 12f, Fonts.INTER_MEDIUM);
        //RenderManager.drawString(vg, "favorite=" + favorite, x + 300, y + 36, OneConfigConfig.WHITE, 12f, Fonts.INTER_MEDIUM);


        active = toggled;
        NanoVG.nvgGlobalAlpha(vg, 1f);
    }

    public void onClick() {
        if(isHoveredMain) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage("you thought you opened the config for " + modData.name + " but actually it doesnt exist");
        }
    }

    public ModData getModData() {
        return modData;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isActive() {
        return active;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isFavorite() {
        return favorite;
    }
}
