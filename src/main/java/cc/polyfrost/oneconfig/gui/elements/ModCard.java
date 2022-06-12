package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.internal.OneConfig;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.animations.ColorAnimation;
import cc.polyfrost.oneconfig.gui.pages.ModConfigPage;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.renderer.scissor.Scissor;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorManager;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import cc.polyfrost.oneconfig.utils.color.ColorUtils;
import cc.polyfrost.oneconfig.libs.universal.wrappers.UPlayer;
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
        String cleanName = modData.name.replaceAll("§.", "");
        Scissor scissor = ScissorManager.scissor(vg, x, y, width, height);

        isHoveredMain = InputUtils.isAreaHovered(x, y, width, 87);
        boolean isHoveredSecondary = InputUtils.isAreaHovered(x, y + 87, width - 32, 32) && !disabled;
        if (disabled) RenderManager.setAlpha(vg, 0.5f);
        RenderManager.drawRoundedRectVaried(vg, x, y, width, 87, colorFrame.getColor(isHoveredMain, isHoveredMain && Mouse.isButtonDown(0)), 12f, 12f, 0f, 0f);
        RenderManager.drawRoundedRectVaried(vg, x, y + 87, width, 32, colorToggle.getColor(isHoveredSecondary, isHoveredSecondary && Mouse.isButtonDown(0)), 0f, 0f, 12f, 12f);
        RenderManager.drawLine(vg, x, y + 86, x + width, y + 86, 2, Colors.GRAY_300);
        if (modData.modIcon != null) {
            if (modData.modIcon.toLowerCase().endsWith(".svg"))
                RenderManager.drawSvg(vg, modData.modIcon, x + 98, y + 19, 48, 48);
            else RenderManager.drawImage(vg, modData.modIcon, x + 98, y + 19, 48, 48);
        } else {
            RenderManager.drawText(vg, cleanName, x + Math.max(0, (244 - RenderManager.getTextWidth(vg, cleanName, 16, Fonts.MINECRAFT_BOLD))) / 2f, y + 44, ColorUtils.setAlpha(Colors.WHITE, (int) (colorFrame.getAlpha() * 255)), 16, Fonts.MINECRAFT_BOLD);
        }
        favoriteButton.draw(vg, x + 212, y + 87);
        favorite = favoriteButton.isToggled();
        Scissor scissor2 = ScissorManager.scissor(vg, x, y + 87, width - 32, 32);
        RenderManager.drawText(vg, cleanName, x + 12, y + 103, ColorUtils.setAlpha(Colors.WHITE, (int) (colorToggle.getAlpha() * 255)), 14f, Fonts.MEDIUM);
        ScissorManager.resetScissor(vg, scissor2);
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
        ScissorManager.resetScissor(vg, scissor);
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
