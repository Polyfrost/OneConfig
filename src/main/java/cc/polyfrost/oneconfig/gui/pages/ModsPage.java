/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.BasicButton;
import cc.polyfrost.oneconfig.gui.elements.ModCard;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.internal.config.core.ConfigCore;
import cc.polyfrost.oneconfig.utils.SearchUtils;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;

import java.util.ArrayList;
import java.util.List;

public class ModsPage extends Page {

    public final ArrayList<ModCard> modCards = new ArrayList<>();
    private final ArrayList<BasicButton> modCategories = new ArrayList<>();
    private int size;

    public ModsPage() {
        super("Mods");
        reloadMods();
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

    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        String filter = OneConfigGui.INSTANCE == null ? "" : OneConfigGui.INSTANCE.getSearchValue().toLowerCase().trim();
        int iX = x + 16;
        int iY = y + 72;
        ArrayList<ModCard> finalModCards = new ArrayList<>(modCards);
        for (ModCard modCard : finalModCards) {
            if (inSelection(modCard) && (filter.equals("") || SearchUtils.isSimilar(modCard.getModData().name, filter))) {
                if (iY + 135 >= y - scroll && iY <= y + 728 - scroll) modCard.draw(vg, iX, iY, inputHandler);
                iX += 260;
                if (iX > x + 796) {
                    iX = x + 16;
                    iY += 135;
                }
            }
        }
        size = iY - y + 135;
        if (iX == x + 16 && iY == y + 72) {
            NanoVGHelper.INSTANCE.drawText(vg, "Looks like there is nothing here. Try another category?", x + 16, y + 72, Colors.WHITE_60, 14f, Fonts.MEDIUM);
        }
    }

    @Override
    public int drawStatic(long vg, int x, int y, InputHandler inputHandler) {
        int iXCat = x + 16;
        boolean selected = false;
        for (BasicButton btn : modCategories) {
            btn.draw(vg, iXCat, y + 16, inputHandler);
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

    private boolean inSelection(ModCard modCard) {
        return modCategories.get(0).isToggled() || (modCategories.get(1).isToggled() && modCard.getModData().modType == ModType.PVP) || (modCategories.get(2).isToggled() && modCard.getModData().modType == ModType.HUD) || (modCategories.get(3).isToggled() && modCard.getModData().modType == ModType.UTIL_QOL) || (modCategories.get(4).isToggled() && modCard.getModData().modType == ModType.HYPIXEL) || (modCategories.get(5).isToggled() && modCard.getModData().modType == ModType.SKYBLOCK) || (modCategories.get(6).isToggled() && modCard.getModData().modType == ModType.THIRD_PARTY);
    }

    public void reloadMods() {
        modCards.clear();
        for (Mod modData : ConfigCore.mods) {
            modCards.add(new ModCard(modData, modData.config == null || modData.config.enabled, false, OneConfigConfig.favoriteMods.contains(modData.name), this));
        }
        if (this instanceof SubModsPage) {
            Mod parent = ((SubModsPage) this).parentMod;
            if (parent == null) return;
            modCards.removeIf(modCard -> !parent.config.subMods.contains(modCard.getModData()) || parent.config.subModSettings == modCard.getModData());
            modCards.add(0, new ModCard(parent.config.subModSettings, true, false, false, this));
        } else {
            for (Mod mod : ConfigCore.subMods) {
                List<ModCard> cards = new ArrayList<>(modCards);
                cards.removeIf(card -> !mod.config.subMods.contains(card.getModData()));
                modCards.removeAll(cards);
            }
        }
    }

    @Override
    public int getMaxScrollHeight() {
        return size;
    }

    @Override
    public boolean isBase() {
        return true;
    }
}
