package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.OneConfig;
import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.pages.ModConfigPage;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.utils.ColorUtils;
import cc.polyfrost.oneconfig.utils.InputUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.ModMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.nanovg.NanoVG;

import java.util.ArrayList;

public class ModCard extends BasicElement {
    private final String iconPath;
    private final Mod modData;
    private final BasicElement favoriteHitbox = new BasicElement(32, 32, -2, true);
    private boolean active, disabled, favorite;
    private int colorGray = OneConfigConfig.GRAY_600;
    private int colorPrimary = OneConfigConfig.BLUE_600;
    private boolean isHoveredMain = false;

    public ModCard(@NotNull Mod mod, @Nullable String iconPath, boolean active, boolean disabled, boolean favorite) {
        super(244, 119, false);
        this.modData = mod;
        this.iconPath = iconPath;
        this.active = active;
        toggled = active;
        this.disabled = disabled;
        this.favorite = favorite;
        favoriteHitbox.setToggled(favorite);
        toggled = active;
    }

    @Override
    public void draw(long vg, int x, int y) {
        if (disabled) NanoVG.nvgGlobalAlpha(vg, 0.5f);
        RenderManager.drawRoundedRectVaried(vg, x, y, width, 87, colorGray, 12f, 12f, 0f, 0f);
        RenderManager.drawRoundedRectVaried(vg, x, y + 87, width, 32, colorPrimary, 0f, 0f, 12f, 12f);
        RenderManager.drawLine(vg, x, y + 86, x + width, y + 86, 2, OneConfigConfig.GRAY_300);
        if (iconPath != null) {
            RenderManager.drawImage(vg, iconPath, x, y, width, 87);
        } else {
            RenderManager.drawImage(vg, Images.MOD_BOX, x + 98, y + 19, 48, 48);
        }
        favoriteHitbox.update(x + 212, y + 87);
        favoriteHitbox.currentColor = ColorUtils.getColor(favoriteHitbox.currentColor, favoriteHitbox.colorPalette, favoriteHitbox.hovered, favoriteHitbox.clicked);
        RenderManager.drawRoundedRectVaried(vg, x + 212, y + 87, 32, 32, favoriteHitbox.currentColor, 0f, 0f, 12f, 0f);
        favorite = favoriteHitbox.isToggled();
        RenderManager.drawString(vg, modData.name, x + 12, y + 103, OneConfigConfig.WHITE, 14f, Fonts.MEDIUM);
        if (favorite) {
            RenderManager.drawImage(vg, Images.FAVORITE, x + 220, y + 95, 16, 16);
        } else {
            RenderManager.drawImage(vg, Images.FAVORITE_OFF, x + 220, y + 95, 16, 16);
        }
        super.update(x, y);
        isHoveredMain = InputUtils.isAreaHovered(x, y, width, 87);
        boolean isHoveredSecondary = InputUtils.isAreaHovered(x, y + 87, width - 32, 32) && !disabled;
        colorGray = ColorUtils.getColor(colorGray, 0, isHoveredMain, clicked && isHoveredMain);
        if (active && !disabled) {
            colorPrimary = ColorUtils.getColor(colorPrimary, 1, isHoveredSecondary, clicked && isHoveredSecondary);
        } else
            colorPrimary = ColorUtils.smoothColor(colorPrimary, OneConfigConfig.GRAY_500, OneConfigConfig.GRAY_400, isHoveredSecondary, 20f);

        if (clicked && isHoveredMain) {
            if (!active) toggled = false;
        }
        if (clicked && favoriteHitbox.hovered) toggled = false;
        if (clicked && !isHoveredSecondary && active) toggled = true;
        if (!active & disabled) toggled = false;

        active = toggled;
        NanoVG.nvgGlobalAlpha(vg, 1f);
    }

    public void onClick() {
        if (isHoveredMain) {
            for (Mod data : OneConfig.loadedMods) {
                if (data.modType != ModType.OTHER) {
                    if (data.name.equalsIgnoreCase(modData.name)) {
                        OneConfigGui.INSTANCE.openPage(new ModConfigPage(data.defaultPage));
                    }
                }
            }
            for (ModMetadata mod : OneConfig.loadedOtherMods) {
                if (mod.name.equalsIgnoreCase(modData.name)) {
                    ArrayList<String> possibleCommands = new ArrayList<>();
                    possibleCommands.add(mod.name.toLowerCase().replace(" ", ""));
                    possibleCommands.add(mod.modId.toLowerCase().replaceAll("[ -_]", ""));
                    if (mod.name.split(" ").length > 1) {
                        StringBuilder result = new StringBuilder();
                        for (String word : mod.name.split(" ")) {
                            if (word.length() == 0) continue;
                            result.append(word.charAt(0));
                        }
                        possibleCommands.add(result.toString().toLowerCase());
                    }
                    for (String command : ClientCommandHandler.instance.getCommands().keySet()) {
                        if (possibleCommands.contains(command)) {
                            try {
                                ClientCommandHandler.instance.getCommands().get(command).processCommand(Minecraft.getMinecraft().thePlayer, new String[]{});
                            } catch (CommandException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        }
                    }
                    return;
                }

            }
        }
    }

    public Mod getModData() {
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
