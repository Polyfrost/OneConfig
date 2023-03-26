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

package cc.polyfrost.oneconfig.config.elements;

import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigPageButton;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.SearchUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OptionSubcategory {
    private final String name;
    public ArrayList<BasicOption> options = new ArrayList<>();
    public ArrayList<ConfigPageButton> topButtons = new ArrayList<>();
    public ArrayList<ConfigPageButton> bottomButtons = new ArrayList<>();
    private List<BasicOption> filteredOptions = new ArrayList<>();
    private int drawLastY;
    private final String category;

    public OptionSubcategory(String name, String category) {
        this.name = name;
        this.category = category;
    }

    public int draw(long vg, int x, int y, InputHandler inputHandler) {
        NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        String filter = OneConfigGui.INSTANCE == null ? "" : OneConfigGui.INSTANCE.getSearchValue().toLowerCase().trim();
        //todo bugs: "Test Page" in test mod doesn't get filtered nicely
        boolean shouldNotFilter = filter.equals("") || SearchUtils.isSimilar(category, filter) || SearchUtils.isSimilar(name, filter);
        filteredOptions = options.stream().filter(option -> !option.isHidden() && (shouldNotFilter || SearchUtils.isSimilar(option.name, filter))).collect(Collectors.toList());
        List<ConfigPageButton> filteredTop = topButtons.stream().filter(page -> !page.isHidden() && (shouldNotFilter || SearchUtils.isSimilar(page.name, filter) || SearchUtils.isSimilar(page.description, filter))).collect(Collectors.toList());
        List<ConfigPageButton> filteredBottom = bottomButtons.stream().filter(page -> !page.isHidden() && (shouldNotFilter || SearchUtils.isSimilar(page.name, filter) || SearchUtils.isSimilar(page.description, filter))).collect(Collectors.toList());
        if (filteredOptions.size() == 0 && filteredTop.size() == 0 && filteredBottom.size() == 0) return 0;
        int optionY = y;
        if (!name.equals("")) {
            nanoVGHelper.drawText(vg, name, x, y + 12, Colors.WHITE_90, 24, Fonts.MEDIUM);
            optionY += 36;
        }

        for (ConfigPageButton page : filteredTop) {
            page.draw(vg, x, optionY, inputHandler);
            optionY += page.getHeight() + 16;
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
            nanoVGHelper.drawRoundedRect(vg, x - 16, optionY, 1024, backgroundSize, Colors.GRAY_900, 20);
            optionY += 16;
        }

        drawLastY = optionY;
        if (filteredOptions.size() > 0) {
            for (int i = 0; i < filteredOptions.size(); i++) {
                BasicOption option = filteredOptions.get(i);
                option.draw(vg, x, optionY, inputHandler);
                if (i + 1 < filteredOptions.size()) {
                    BasicOption nextOption = filteredOptions.get(i + 1);
                    if (option.size == 1 && nextOption.size == 1) {
                        nextOption.draw(vg, x + 512, optionY, inputHandler);
                        optionY += Math.max(option.getHeight(), nextOption.getHeight()) + 16;
                        i++;
                        continue;
                    }
                }
                optionY += option.getHeight() + 16;
            }
            optionY += 16;
        }

        for (ConfigPageButton page : filteredBottom) {
            page.draw(vg, x, optionY, inputHandler);
            optionY += page.getHeight() + 16;
        }

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
