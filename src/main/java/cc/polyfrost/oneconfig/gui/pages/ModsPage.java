package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.internal.OneConfig;
import cc.polyfrost.oneconfig.gui.Colors;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.BasicButton;
import cc.polyfrost.oneconfig.gui.elements.ModCard;
import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;

import java.util.ArrayList;
import java.util.List;

public class ModsPage extends Page {

    private final List<ModCard> modCards = new ArrayList<>();
    private final List<BasicButton> modCategories = new ArrayList<>();
    private int size;

    public ModsPage() {
        super("Mods");
        for (Mod modData : OneConfig.loadedMods) {
            modCards.add(OneConfigConfig.favoriteMods.contains(modData.name) ? 0 : modCards.size(), new ModCard(modData, modData.config == null || modData.config.enabled, false, OneConfigConfig.favoriteMods.contains(modData.name)));
        }
        modCategories.add(new BasicButton(64, 32, "All", BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY));
        modCategories.add(new BasicButton(80, 32, "Combat", BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY));
        modCategories.add(new BasicButton(64, 32, "HUD", BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY));
        modCategories.add(new BasicButton(104, 32, "Utility & QoL", BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY));
        modCategories.add(new BasicButton(80, 32, "Hypixel", BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY));
        modCategories.add(new BasicButton(80, 32, "Skyblock", BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY));
        modCategories.add(new BasicButton(88, 32, "3rd Party", BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY));
        for (int i = 0; i < modCategories.size(); i++) {
            modCategories.get(i).setToggleable(true);
            int finalI = i;
            modCategories.get(i).setClickAction(() -> unselect(finalI));
        }
        modCategories.get(0).setToggled(true);
    }

    public void draw(long vg, int x, int y) {
        String filter = OneConfigGui.INSTANCE == null ? "" : OneConfigGui.INSTANCE.getSearchValue().toLowerCase().trim();
        int iX = x + 16;
        int iY = y + 72;
        for (ModCard modCard : modCards) {
            if (inSelection(modCard) && (filter.equals("") || modCard.getModData().name.toLowerCase().contains(filter))) {
                modCard.draw(vg, iX, iY);
                iX += 260;
                if (iX > x + 796) {
                    iX = x + 16;
                    iY += 135;
                }
            }
        }
        size = iY + 119;
        if (iX == x + 16 && iY == y + 72) {
            RenderManager.drawText(vg, "Looks like there is nothing here. Try another category?", x + 16, y + 72, Colors.WHITE_60, 14f, Fonts.MEDIUM);
        }
    }

    @Override
    public int drawStatic(long vg, int x, int y) {
        int iXCat = x + 16;
        boolean selected = false;
        for (BasicButton btn : modCategories) {
            btn.draw(vg, iXCat, y + 16);
            iXCat += btn.getWidth() + 8;
            if (btn.isToggled()) selected = true;
        }
        if (!selected) modCategories.get(0).setToggled(true);
        return 60;
    }

    private void unselect(int index) {
        for (int i = 0; i < modCategories.size(); i++) {
            if (index == i) continue;
            modCategories.get(i).setToggled(false);
        }
    }

    @Override
    public void finishUpAndClose() {
        OneConfigConfig.favoriteMods.clear();
        for (ModCard modCard : modCards) {
            if (modCard.isFavorite()) OneConfigConfig.favoriteMods.add(modCard.getModData().name);
            if (modCard.getModData().config != null && modCard.getModData().config.enabled != modCard.isActive()) {
                modCard.getModData().config.enabled = modCard.isActive();
                modCard.getModData().config.save();
            }
        }
        OneConfig.config.save();
    }

    private boolean inSelection(ModCard modCard) {
        return modCategories.get(0).isToggled() && (OneConfigConfig.allShowShortCut || !modCard.getModData().isShortCut) || (modCategories.get(1).isToggled() && modCard.getModData().modType == ModType.PVP) || (modCategories.get(2).isToggled() && modCard.getModData().modType == ModType.HUD) || (modCategories.get(3).isToggled() && modCard.getModData().modType == ModType.UTIL_QOL) || (modCategories.get(4).isToggled() && modCard.getModData().modType == ModType.HYPIXEL) || (modCategories.get(5).isToggled() && modCard.getModData().modType == ModType.SKYBLOCK) || (modCategories.get(6).isToggled() && modCard.getModData().modType == ModType.THIRD_PARTY);
    }

    @Override
    public int getMaxScrollHeight() {
        //return size;
        return 3298046;
    }

    @Override
    public boolean isBase() {
        return true;
    }
}
