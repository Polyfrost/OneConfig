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

package org.polyfrost.oneconfig.test;

import org.polyfrost.oneconfig.gui.elements.BasicButton;
import org.polyfrost.oneconfig.gui.pages.Page;
import org.polyfrost.oneconfig.internal.assets.SVGs;
import org.polyfrost.oneconfig.utils.InputHandler;
import org.polyfrost.oneconfig.utils.color.ColorPalette;

import java.util.ArrayList;

public class ButtonTestPage_Test extends Page {
    private final ArrayList<BasicButton> row1 = new ArrayList<BasicButton>() {{
        add(new BasicButton(254, 48, "Primary", BasicButton.ALIGNMENT_LEFT, ColorPalette.PRIMARY));
        add(new BasicButton(254, 48, "Primary Destructive", BasicButton.ALIGNMENT_LEFT, ColorPalette.PRIMARY_DESTRUCTIVE));
        add(new BasicButton(254, 48, "Secondary", BasicButton.ALIGNMENT_LEFT, ColorPalette.SECONDARY));
        add(new BasicButton(254, 48, "Secondary Destructive", BasicButton.ALIGNMENT_LEFT, ColorPalette.SECONDARY_DESTRUCTIVE));
        add(new BasicButton(254, 48, "Tertiary", BasicButton.ALIGNMENT_LEFT, ColorPalette.TERTIARY));
        add(new BasicButton(254, 48, "Tertiary Destructive", BasicButton.ALIGNMENT_LEFT, ColorPalette.TERTIARY_DESTRUCTIVE));
        add(new BasicButton(254, 48, "LeftIconLA", SVGs.BOX, null, BasicButton.ALIGNMENT_LEFT, ColorPalette.SECONDARY));
        add(new BasicButton(254, 48, "RightIconLA", null, SVGs.BOX, BasicButton.ALIGNMENT_LEFT, ColorPalette.SECONDARY));
        add(new BasicButton(254, 48, "LeftIconCA", SVGs.BOX, null, BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY));
        add(new BasicButton(254, 48, "RightIconCA", null, SVGs.BOX, BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY));
    }};

    private final ArrayList<BasicButton> row2 = new ArrayList<BasicButton>() {{
        add(new BasicButton(170, 32, "32", SVGs.BOX, SVGs.BOX, BasicButton.ALIGNMENT_LEFT, ColorPalette.PRIMARY));
        add(new BasicButton(193, 36, "36", SVGs.BOX, SVGs.BOX, BasicButton.ALIGNMENT_LEFT, ColorPalette.PRIMARY));
        add(new BasicButton(208, 40, "40", SVGs.BOX, SVGs.BOX, BasicButton.ALIGNMENT_LEFT, ColorPalette.PRIMARY));
        add(new BasicButton(254, 48, "48", SVGs.BOX, SVGs.BOX, BasicButton.ALIGNMENT_LEFT, ColorPalette.PRIMARY));
        add(new BasicButton(254, 48, "JustifiedRight", null, SVGs.BOX, BasicButton.ALIGNMENT_JUSTIFIED, ColorPalette.SECONDARY_DESTRUCTIVE));
        add(new BasicButton(254, 48, "JustifiedLeft", SVGs.BOX, null, BasicButton.ALIGNMENT_JUSTIFIED, ColorPalette.SECONDARY_DESTRUCTIVE));
        add(new BasicButton(254, 48, "JustifiedNoIcon", null, null, BasicButton.ALIGNMENT_JUSTIFIED, ColorPalette.PRIMARY));
        add(new BasicButton(254, 48, "TertiaryIcon", SVGs.BOX, null, BasicButton.ALIGNMENT_JUSTIFIED, ColorPalette.TERTIARY_DESTRUCTIVE));

    }};

    private final ArrayList<BasicButton> row3 = new ArrayList<BasicButton>() {{
        add(new BasicButton(254, 48, "Left", SVGs.BOX, SVGs.BOX, BasicButton.ALIGNMENT_LEFT, ColorPalette.PRIMARY));
        add(new BasicButton(254, 48, "Center", SVGs.BOX, SVGs.BOX, BasicButton.ALIGNMENT_CENTER, ColorPalette.PRIMARY));
        add(new BasicButton(254, 48, "Justified", SVGs.BOX, SVGs.BOX, BasicButton.ALIGNMENT_JUSTIFIED, ColorPalette.PRIMARY));
        add(new BasicButton(254, 48, "Disabled :(", SVGs.BOX, SVGs.BOX, BasicButton.ALIGNMENT_LEFT, ColorPalette.PRIMARY));
        add(new BasicButton(254, 48, "Disabled :(", SVGs.BOX, SVGs.BOX, BasicButton.ALIGNMENT_CENTER, ColorPalette.PRIMARY));
        add(new BasicButton(254, 48, "Disabled :(", SVGs.BOX, SVGs.BOX, BasicButton.ALIGNMENT_JUSTIFIED, ColorPalette.PRIMARY));
    }};

    public ButtonTestPage_Test() {
        super("Buttons");
        row3.get(3).disable(true);
        row3.get(4).disable(true);
        row3.get(5).disable(true);
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        int buttonY = y + 16;
        for (BasicButton button : row1) {
            button.draw(vg, x + 16, buttonY, inputHandler);
            buttonY += 64;
        }
        buttonY = y + 16;
        for (BasicButton button : row2) {
            button.draw(vg, x + 286, buttonY, inputHandler);
            buttonY += 64;
        }
        buttonY = y + 16;
        for (BasicButton button : row3) {
            button.draw(vg, x + 556, buttonY, inputHandler);
            buttonY += 64;
        }
    }
}
