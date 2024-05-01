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
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.ModCard;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.SearchUtils;

import java.util.ArrayList;

public class SubModsPage extends ModsPage {

    public Mod parentMod;
    private int size;

    public SubModsPage(Mod parentMod) {
        this.parentMod = parentMod;
        setTitle(parentMod.name);
        reloadMods();
    }

    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        String filter = OneConfigGui.INSTANCE == null ? "" : OneConfigGui.INSTANCE.getSearchValue().toLowerCase().trim();
        int iX = x + 16;
        int iY = y + 16;
        ArrayList<ModCard> finalModCards = new ArrayList<>(modCards);
        for (ModCard modCard : finalModCards) {
            if (filter.isEmpty() || SearchUtils.isSimilar(modCard.getModData().name, filter)) {
                if (iY + 135 >= y - scroll && iY <= y + 728 - scroll) modCard.draw(vg, iX, iY, inputHandler);
                iX += 260;
                if (iX > x + 796) {
                    iX = x + 16;
                    iY += 135;
                }
            }
        }
        size = iY - y + 135;
    }

    @Override
    public int drawStatic(long vg, int x, int y, InputHandler inputHandler) {
        return 0;
    }

    @Override
    public int getMaxScrollHeight() {
        return size;
    }

    @Override
    public boolean isBase() {
        return false;
    }
}
