package io.polyfrost.oneconfig.gui.pages;

import io.polyfrost.oneconfig.OneConfig;
import io.polyfrost.oneconfig.config.data.ModData;
import io.polyfrost.oneconfig.gui.elements.BasicButton;
import io.polyfrost.oneconfig.gui.elements.ModCard;

import java.util.ArrayList;
import java.util.List;

public class ModsPage extends Page {
    private final BasicButton allBtn = new BasicButton(49, 40, "All", null, null, 0, BasicButton.ALIGNMENT_CENTER, true);
    private final BasicButton newBtn = new BasicButton(64, 40, "New", null, null, 0, BasicButton.ALIGNMENT_CENTER, true);
    private final BasicButton combatBtn = new BasicButton(104, 40, "Combat", null, null, 0, BasicButton.ALIGNMENT_CENTER, true);
    private final BasicButton hudBtn = new BasicButton(104, 40, "HUD & QoL", null, null, 0, BasicButton.ALIGNMENT_CENTER, true);
    private final BasicButton hypixelBtn = new BasicButton(104, 40, "Hypixel", null, null, 0, BasicButton.ALIGNMENT_CENTER, true);
    private final BasicButton skyblockBtn = new BasicButton(104, 40, "Skyblock", null, null, 0, BasicButton.ALIGNMENT_CENTER, true);
    private final BasicButton utilBtn = new BasicButton(104, 40, "Utility", null, null, 0, BasicButton.ALIGNMENT_CENTER, true);
    private final BasicButton customBtn = new BasicButton(104, 40, "Custom", null, null, 0, BasicButton.ALIGNMENT_CENTER, true);

    private final List<ModCard> modCards = new ArrayList<>();

    public ModsPage() {
        super("Mods");
        for(ModData modData : OneConfig.loadedMods) {
            modCards.add(new ModCard(modData, null, true, false, false));
        }
    }

    public void draw(long vg, int x, int y) {
        allBtn.draw(vg, x + 16, y + 16);
        newBtn.draw(vg, x + 92, y + 16);
        combatBtn.draw(vg, x + 168, y + 16);
        hudBtn.draw(vg, x + 284, y + 16);
        hypixelBtn.draw(vg, x + 400, y + 16);
        skyblockBtn.draw(vg, x + 516, y + 16);
        utilBtn.draw(vg, x + 632, y + 16);
        customBtn.draw(vg, x + 748, y + 16);

        int iX = x + 16;
        int iY = y + 72;
        for(ModCard modCard : modCards) {
            modCard.draw(vg, iX, iY);
            iX += 260;
            if(iX > x + 796) {
                iX = x + 16;
                iY += 135;
            }
        }

    }
}
