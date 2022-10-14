/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
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

package cc.polyfrost.oneconfig.config.elements;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OptionSubcategory {
    private final String name;
    public ArrayList<BasicOption> options = new ArrayList<>();
    public ArrayList<BasicOption> topButtons = new ArrayList<>();
    public ArrayList<BasicOption> bottomButtons = new ArrayList<>();
    private List<BasicOption> filteredOptions = new ArrayList<>();
    private int drawLastY;

    public OptionSubcategory(String name) {
        this.name = name;
    }

    public int draw(long vg, int x, int y, InputHandler inputHandler) {
        String filter = OneConfigGui.INSTANCE == null ? "" : OneConfigGui.INSTANCE.getSearchValue().toLowerCase().trim();
        filteredOptions = options.stream().filter(option -> !option.isHidden() && (filter.equals("") || name.toLowerCase().contains(filter) || option.name.toLowerCase().contains(filter))).collect(Collectors.toList());
        List<BasicOption> filteredTop = topButtons.stream().filter(opt -> !opt.isHidden() && (filter.equals("") || name.toLowerCase().contains(filter) || opt.name.toLowerCase().contains(filter))).collect(Collectors.toList());
        List<BasicOption> filteredBottom = bottomButtons.stream().filter(opt -> !opt.isHidden() && (filter.equals("") || name.toLowerCase().contains(filter) || opt.name.toLowerCase().contains(filter))).collect(Collectors.toList());
        if (filteredOptions.size() == 0 && filteredTop.size() == 0 && filteredBottom.size() == 0) return 0;
        int optionY = y;
        if (!name.equals("")) {
            RenderManager.drawText(vg, name, x, y + 12, Colors.WHITE_90, 24, Fonts.MEDIUM);
            optionY += 36;
        }

/*
// what do we do here TODO
 master
        for (ConfigPageButton page : filteredTop) {
            page.draw(vg, x, optionY, inputHandler);
            optionY += page.getHeight() + 16;
-----
        for (BasicOption opt : filteredTop) {
            opt.draw(vg, x, optionY);
            optionY += opt.getHeight() + 16;
 previews
*/
        }

        if (filteredOptions.size() > 0) {
            int backgroundSize = 16;
            for (int i = 0; i < filteredOptions.size(); i++) {
                BasicOption option = filteredOptions.get(i);
                if (i + 1 < filteredOptions.size()) {
                    BasicOption nextOption = filteredOptions.get(i + 1);
                    if (option.size == 1 && nextOption.size == 1) {
                        backgroundSize += Math.max(option.getHeight(), nextOption.getHeight()) + 16;
                        i++;
                        continue;
                    }
                }
                backgroundSize += option.getHeight() + 16;
            }
            RenderManager.drawRoundedRect(vg, x - 16, optionY, 1024, backgroundSize, Colors.GRAY_900, 20);
            optionY += 16;
        }

        drawLastY = optionY;
        if (filteredOptions.size() > 0) {
            for (int i = 0; i < filteredOptions.size(); i++) {
                BasicOption option = filteredOptions.get(i);
                option.draw(vg, x, optionY, inputHandler);
                option.drawDescription(vg, x, optionY, inputHandler);
                if (i + 1 < filteredOptions.size()) {
                    BasicOption nextOption = filteredOptions.get(i + 1);
                    if (option.size == 1 && nextOption.size == 1) {
                        nextOption.draw(vg, x + 512, optionY, inputHandler);
                        nextOption.drawDescription(vg, x + 512, optionY, inputHandler);
                        optionY += Math.max(option.getHeight(), nextOption.getHeight()) + 16;
                        i++;
                        continue;
                    }
                }
                optionY += option.getHeight() + 16;
            }
            optionY += 16;
        }

/*
// what do we do here as well TODO
 master
        for (ConfigPageButton page : filteredBottom) {
            page.draw(vg, x, optionY, inputHandler);
--------
        for (BasicOption page : filteredBottom) {
            page.draw(vg, x, optionY);
 previews
            optionY += page.getHeight() + 16;
        }
        */

        return optionY - y;
    }

    public void drawLast(long vg, int x, InputHandler inputHandler) {
        for (int i = 0; i < filteredOptions.size(); i++) {
            BasicOption option = filteredOptions.get(i);
            option.drawLast(vg, x, drawLastY, inputHandler);
            if (i + 1 < filteredOptions.size()) {
                BasicOption nextOption = filteredOptions.get(i + 1);
                if (option.size == 1 && nextOption.size == 1) {
                    nextOption.drawLast(vg, x + 512, drawLastY, inputHandler);
                    drawLastY += Math.max(option.getHeight(), nextOption.getHeight()) + 16;
                    i++;
                    continue;
                }
            }
            drawLastY += option.getHeight() + 16;
        }
    }

    public String getName() {
        return name;
    }
}
