package io.polyfrost.oneconfig.gui.pages;

import io.polyfrost.oneconfig.OneConfig;
import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.data.ModData;
import io.polyfrost.oneconfig.config.data.ModType;
import io.polyfrost.oneconfig.gui.elements.BasicButton;
import io.polyfrost.oneconfig.gui.elements.ModCard;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;

import java.util.ArrayList;
import java.util.List;

public class ModsPage extends Page {

    private final List<ModCard> modCards = new ArrayList<>();
    private final List<BasicButton> modCategories = new ArrayList<>();

    public ModsPage() {
        super("Mods");
        for (ModData modData : OneConfig.loadedMods) {
            modCards.add(new ModCard(modData, null, true, false, false));
        }
        for (ModCard card : modCards) {
            if (card.isFavorite()) {
                modCards.remove(card);
                modCards.add(0, card);
            }
        }
        modCategories.add(new BasicButton(64, 32, "All", null, null, 0, BasicButton.ALIGNMENT_CENTER, true));
        modCategories.add(new BasicButton(80, 32, "Combat", null, null, 0, BasicButton.ALIGNMENT_CENTER, true));
        modCategories.add(new BasicButton(64, 32, "HUD", null, null, 0, BasicButton.ALIGNMENT_CENTER, true));
        modCategories.add(new BasicButton(104, 32, "Utility & QoL", null, null, 0, BasicButton.ALIGNMENT_CENTER, true));
        modCategories.add(new BasicButton(80, 32, "Hypixel", null, null, 0, BasicButton.ALIGNMENT_CENTER, true));
        modCategories.add(new BasicButton(80, 32, "Skyblock", null, null, 0, BasicButton.ALIGNMENT_CENTER, true));
        modCategories.add(new BasicButton(88, 32, "3rd Party", null, null, 0, BasicButton.ALIGNMENT_CENTER, true));
        modCategories.get(0).setToggled(true);
    }

    public void draw(long vg, int x, int y) {
        int iXCat = x + 16;
        for (BasicButton btn : modCategories) {
            btn.draw(vg, iXCat, y + 16);
            iXCat += btn.getWidth() + 8;
        }
        if ((modCategories.get(1).isClicked() || modCategories.get(2).isClicked() || modCategories.get(3).isClicked() || modCategories.get(4).isClicked() || modCategories.get(5).isClicked() || modCategories.get(6).isClicked()) && modCategories.get(0).isToggled()) {
            modCategories.get(0).setToggled(false);
        }
        if (!modCategories.get(0).isToggled() && !modCategories.get(1).isToggled() && !modCategories.get(2).isToggled() && !modCategories.get(3).isToggled() && !modCategories.get(4).isToggled() && !modCategories.get(5).isToggled() && !modCategories.get(6).isToggled()) {
            modCategories.get(0).setToggled(true);
        }
        if (modCategories.get(0).isToggled()) {
            for (BasicButton btn : modCategories) {
                if (!btn.getText().equals("All")) {
                    btn.setToggled(false);
                }
            }
        }


        int iX = x + 16;
        int iY = y + 72;
        for (ModCard modCard : modCards) {
            if (modCategories.get(0).isToggled() || (modCategories.get(1).isToggled() && modCard.getModData().modType == ModType.PVP) || (modCategories.get(2).isToggled() && modCard.getModData().modType == ModType.HUD) || (modCategories.get(3).isToggled() && modCard.getModData().modType == ModType.UTIL_QOL) || (modCategories.get(4).isToggled() && modCard.getModData().modType == ModType.HYPIXEL) || (modCategories.get(5).isToggled() && modCard.getModData().modType == ModType.SKYBLOCK) || (modCategories.get(6).isToggled() && modCard.getModData().modType == ModType.OTHER)) {
                modCard.draw(vg, iX, iY);
                iX += 260;
                if (iX > x + 796) {
                    iX = x + 16;
                    iY += 135;
                }
            }
        }
        if (iX == x + 16 && iY == y + 72) {
            RenderManager.drawString(vg, "Looks like there is nothing here. Try another category?", x + 16, y + 72, OneConfigConfig.WHITE_60, 14f, Fonts.INTER_MEDIUM);
        }

    }

}
