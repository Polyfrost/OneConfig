package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.OneConfig;
import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.gui.elements.BasicButton;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.gui.elements.ModCard;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;

import java.util.ArrayList;
import java.util.List;

public class ModsPage extends Page {

    private final List<ModCard> modCards = new ArrayList<>();
    private final List<BasicButton> modCategories = new ArrayList<>();

    public ModsPage() {
        super("Mods");
        for (Mod modData : OneConfig.loadedMods) {
            modCards.add(new ModCard(modData, null, true, false, false));
        }
        for (ModCard card : modCards) {
            if (card.isFavorite()) {
                modCards.remove(card);
                modCards.add(0, card);
            }
        }
        modCategories.add(new BasicButton(64, 32, "All", null, null, 0, BasicButton.ALIGNMENT_CENTER, true, () -> unselect(0)));
        modCategories.add(new BasicButton(80, 32, "Combat", null, null, 0, BasicButton.ALIGNMENT_CENTER, true, () -> unselect(1)));
        modCategories.add(new BasicButton(64, 32, "HUD", null, null, 0, BasicButton.ALIGNMENT_CENTER, true, () -> unselect(2)));
        modCategories.add(new BasicButton(104, 32, "Utility & QoL", null, null, 0, BasicButton.ALIGNMENT_CENTER, true, () -> unselect(3)));
        modCategories.add(new BasicButton(80, 32, "Hypixel", null, null, 0, BasicButton.ALIGNMENT_CENTER, true, () -> unselect(4)));
        modCategories.add(new BasicButton(80, 32, "Skyblock", null, null, 0, BasicButton.ALIGNMENT_CENTER, true, () -> unselect(5)));
        modCategories.add(new BasicButton(88, 32, "3rd Party", null, null, 0, BasicButton.ALIGNMENT_CENTER, true, () -> unselect(6)));
        modCategories.get(0).setToggled(true);
    }

    public void draw(long vg, int x, int y) {
        int iXCat = x + 16;
        for (BasicButton btn : modCategories) {
            btn.draw(vg, iXCat, y + 16);
            iXCat += btn.getWidth() + 8;
        }

        int iX = x + 16;
        int iY = y + 72;
        for (ModCard modCard : modCards) {
            if (modCategories.get(0).isToggled() && (OneConfigConfig.thirdPartyAll || modCard.getModData().modType != ModType.OTHER) || (modCategories.get(1).isToggled() && modCard.getModData().modType == ModType.PVP) || (modCategories.get(2).isToggled() && modCard.getModData().modType == ModType.HUD) || (modCategories.get(3).isToggled() && modCard.getModData().modType == ModType.UTIL_QOL) || (modCategories.get(4).isToggled() && modCard.getModData().modType == ModType.HYPIXEL) || (modCategories.get(5).isToggled() && modCard.getModData().modType == ModType.SKYBLOCK) || (modCategories.get(6).isToggled() && modCard.getModData().modType == ModType.OTHER)) {
                modCard.draw(vg, iX, iY);
                iX += 260;
                if (iX > x + 796) {
                    iX = x + 16;
                    iY += 135;
                }
            }
        }
        if (iX == x + 16 && iY == y + 72) {
            RenderManager.drawString(vg, "Looks like there is nothing here. Try another category?", x + 16, y + 72, OneConfigConfig.WHITE_60, 14f, Fonts.MEDIUM);
        }
    }

    private void unselect(int index) {
        for (int i = 0; i < modCategories.size(); i++) {
            if (index == i) continue;
            modCategories.get(i).setToggled(false);
        }
    }

    @Override
    public boolean isBase() {
        return true;
    }
}
