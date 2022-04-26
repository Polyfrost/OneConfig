package io.polyfrost.oneconfig.gui.pages;

import io.polyfrost.oneconfig.OneConfig;
import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.data.Mod;
import io.polyfrost.oneconfig.config.data.ModType;
import io.polyfrost.oneconfig.gui.elements.ModCard;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;

import java.util.ArrayList;
import java.util.List;

public class PerformanceModsPage extends Page {
    List<ModCard> modCards = new ArrayList<>();

    public PerformanceModsPage() {
        super("Performance Mods");
        for (Mod mod : OneConfig.loadedMods) {
            if (mod.modType == ModType.PERFORMANCE) {
                modCards.add(new ModCard(mod, null, true, false, false));
            }
        }
    }

    @Override
    public void draw(long vg, int x, int y) {
        int iX = x + 16;
        int iY = y + 16;
        for (ModCard card : modCards) {
            card.draw(vg, iX, iY);
            iX += 260;
            if (iX > x + 796) {
                iX = x + 16;
                iY += 135;
            }
        }
        if (iX == x + 16 && iY == y + 16) {
            RenderManager.drawString(vg, "Looks like there is nothing here. Try getting some more mods!", x + 16, y + 16, OneConfigConfig.WHITE_60, 14f, Fonts.INTER_MEDIUM);
        }
    }
}
