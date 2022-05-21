package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.OneConfig;
import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.pages.ModConfigPage;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import cc.polyfrost.oneconfig.utils.ColorUtils;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.libs.universal.wrappers.UPlayer;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.ModMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        if (disabled) RenderManager.withAlpha(vg, 0.5f);
        RenderManager.drawRoundedRectVaried(vg, x, y, width, 87, colorGray, 12f, 12f, 0f, 0f);
        RenderManager.drawRoundedRectVaried(vg, x, y + 87, width, 32, colorPrimary, 0f, 0f, 12f, 12f);
        RenderManager.drawLine(vg, x, y + 86, x + width, y + 86, 2, OneConfigConfig.GRAY_300);
        if (iconPath != null) {
            RenderManager.drawImage(vg, iconPath, x, y, width, 87);
        } else {
            RenderManager.drawSvg(vg, SVGs.BOX, x + 98, y + 19, 48, 48);
        }
        favoriteHitbox.update(x + 212, y + 87);
        favoriteHitbox.currentColor = ColorUtils.getColor(favoriteHitbox.currentColor, favoriteHitbox.colorPalette, favoriteHitbox.hovered, favoriteHitbox.clicked, OneConfigGui.INSTANCE.getDeltaTime());
        RenderManager.drawRoundedRectVaried(vg, x + 212, y + 87, 32, 32, favoriteHitbox.currentColor, 0f, 0f, 12f, 0f);
        favorite = favoriteHitbox.isToggled();
        RenderManager.drawString(vg, modData.name, x + 12, y + 103, OneConfigConfig.WHITE, 14f, Fonts.MEDIUM);
        if (favorite) {
            RenderManager.drawSvg(vg, SVGs.HEART_FILL, x + 220, y + 95, 16, 16);
        } else {
            RenderManager.drawSvg(vg, SVGs.HEART_OUTLINE, x + 220, y + 95, 16, 16);
        }
        super.update(x, y);
        isHoveredMain = InputUtils.isAreaHovered(x, y, width, 87);
        boolean isHoveredSecondary = InputUtils.isAreaHovered(x, y + 87, width - 32, 32) && !disabled;
        colorGray = ColorUtils.getColor(colorGray, 0, isHoveredMain, clicked && isHoveredMain, OneConfigGui.INSTANCE.getDeltaTime());
        if (active && !disabled) {
            colorPrimary = ColorUtils.getColor(colorPrimary, 1, isHoveredSecondary, clicked && isHoveredSecondary, OneConfigGui.INSTANCE.getDeltaTime());
        } else
            colorPrimary = ColorUtils.smoothColor(colorPrimary, OneConfigConfig.GRAY_500, OneConfigConfig.GRAY_400, isHoveredSecondary, 20f, OneConfigGui.INSTANCE.getDeltaTime());

        if (clicked && isHoveredMain) {
            if (!active) toggled = false;
        }
        if (clicked && favoriteHitbox.hovered) toggled = false;
        if (clicked && !isHoveredSecondary && active) toggled = true;
        if (!active & disabled) toggled = false;

        active = toggled;
        RenderManager.withAlpha(vg, 1f);
    }

    public void onClick() {
        if (isHoveredMain) {
            for (Mod data : OneConfig.loadedMods) {
                if (data.modType != ModType.THIRD_PARTY) {
                    if (data.name.equalsIgnoreCase(modData.name)) {
                        OneConfigGui.INSTANCE.openPage(new ModConfigPage(data.defaultPage));
                        return;
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
                                ClientCommandHandler.instance.getCommands().get(command).processCommand(UPlayer.getPlayer(), new String[]{});
                            } catch (Exception e) {
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
