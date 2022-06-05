package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.OneConfig;
import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.animations.ColorAnimation;
import cc.polyfrost.oneconfig.gui.pages.ModConfigPage;
import cc.polyfrost.oneconfig.libs.universal.wrappers.UPlayer;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.ModMetadata;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;

public class ModCard extends BasicElement {
    private final Mod modData;
    private final BasicButton favoriteButton = new BasicButton(32, 32, SVGs.HEART_OUTLINE, BasicButton.ALIGNMENT_CENTER, ColorPalette.TERTIARY);
    private final ColorAnimation colorFrame = new ColorAnimation(ColorPalette.SECONDARY);
    private final ColorAnimation colorToggle = new ColorAnimation(ColorPalette.PRIMARY);
    private boolean active, disabled, favorite;
    private boolean isHoveredMain = false;

    public ModCard(@NotNull Mod mod, boolean active, boolean disabled, boolean favorite) {
        super(244, 119, false);
        this.modData = mod;
        this.active = active;
        toggled = active;
        this.disabled = disabled;
        this.favorite = favorite;
        favoriteButton.setToggled(favorite);
        toggled = active;
    }

    @Override
    public void draw(long vg, int x, int y) {
        super.update(x, y);
        isHoveredMain = InputUtils.isAreaHovered(x, y, width, 87);
        boolean isHoveredSecondary = InputUtils.isAreaHovered(x, y + 87, width - 32, 32) && !disabled;
        if (disabled) RenderManager.setAlpha(vg, 0.5f);
        RenderManager.drawRoundedRectVaried(vg, x, y, width, 87, colorFrame.getColor(isHoveredMain, isHoveredMain && Mouse.isButtonDown(0)), 12f, 12f, 0f, 0f);
        RenderManager.drawRoundedRectVaried(vg, x, y + 87, width, 32, colorToggle.getColor(isHoveredSecondary, isHoveredSecondary && Mouse.isButtonDown(0)), 0f, 0f, 12f, 12f);
        RenderManager.drawLine(vg, x, y + 86, x + width, y + 86, 2, OneConfigConfig.GRAY_300);
        if (modData.modIcon != null) {
            if (modData.modIcon.toLowerCase().endsWith(".svg"))
                RenderManager.drawSvg(vg, modData.modIcon, x + 98, y + 19, 48, 48);
            else RenderManager.drawImage(vg, modData.modIcon, x + 98, y + 19, 48, 48);
        } else {
            RenderManager.drawText(vg, modData.name, x + 122 - RenderManager.getTextWidth(vg, modData.name, 24, Fonts.MINECRAFT) / 2f, y + 44, OneConfigConfig.WHITE, 24, Fonts.MINECRAFT);
        }
        favoriteButton.draw(vg, x + 212, y + 87);
        favorite = favoriteButton.isToggled();
        RenderManager.drawText(vg, modData.name, x + 12, y + 103, OneConfigConfig.WHITE, 14f, Fonts.MEDIUM);
        if (favorite) favoriteButton.setLeftIcon(SVGs.HEART_FILL);
        else favoriteButton.setLeftIcon(SVGs.HEART_OUTLINE);

        if (clicked && isHoveredMain) {
            if (!active) toggled = false;
        }
        if (clicked && favoriteButton.hovered) toggled = false;
        if (clicked && !isHoveredSecondary && active) toggled = true;
        if (!active & disabled) toggled = false;

        active = toggled;
        colorToggle.setPalette(active ? ColorPalette.PRIMARY : ColorPalette.SECONDARY);
        RenderManager.setAlpha(vg, 1f);
    }

    public void onClick() {
        if (isHoveredMain) {
            for (Mod data : OneConfig.loadedMods) {
                if (!data.isShortCut) {
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

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isFavorite() {
        return favorite;
    }
}
