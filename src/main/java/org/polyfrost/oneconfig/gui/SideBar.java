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

package org.polyfrost.oneconfig.gui;

import org.polyfrost.oneconfig.gui.animations.Animation;
import org.polyfrost.oneconfig.gui.animations.DummyAnimation;
import org.polyfrost.oneconfig.gui.animations.EaseOutExpo;
import org.polyfrost.oneconfig.gui.elements.BasicButton;
import org.polyfrost.oneconfig.gui.pages.CreditsPage;
import org.polyfrost.oneconfig.gui.pages.ModConfigPage;
import org.polyfrost.oneconfig.gui.pages.ModsPage;
import org.polyfrost.oneconfig.internal.assets.Colors;
import org.polyfrost.oneconfig.internal.assets.SVGs;
import org.polyfrost.oneconfig.internal.config.Preferences;
import org.polyfrost.oneconfig.internal.gui.HudGui;
import org.polyfrost.oneconfig.renderer.NanoVGHelper;
import org.polyfrost.oneconfig.renderer.font.Fonts;
import org.polyfrost.oneconfig.utils.InputHandler;
import org.polyfrost.oneconfig.utils.color.ColorPalette;
import org.polyfrost.oneconfig.utils.gui.GuiUtils;

import java.util.ArrayList;
import java.util.List;

public class SideBar {
    private final List<BasicButton> buttons = new ArrayList<BasicButton>() {{
        int width = 192;
        add(new BasicButton(width, BasicButton.SIZE_36, "Credits", SVGs.COPYRIGHT_FILL, null, BasicButton.ALIGNMENT_LEFT, ColorPalette.TERTIARY));

        add(new BasicButton(width, BasicButton.SIZE_36, "Mods", SVGs.FADERS_HORIZONTAL_BOLD, null, BasicButton.ALIGNMENT_LEFT, ColorPalette.PRIMARY));
        add(new BasicButton(width, BasicButton.SIZE_36, "Profiles", SVGs.USERS_02, null, BasicButton.ALIGNMENT_LEFT, ColorPalette.TERTIARY));

        add(new BasicButton(width, BasicButton.SIZE_36, "Themes", SVGs.BRUSH, null, BasicButton.ALIGNMENT_LEFT, ColorPalette.TERTIARY));
        add(new BasicButton(width, BasicButton.SIZE_36, "Preferences", SVGs.SETTINGS_02, null, BasicButton.ALIGNMENT_LEFT, ColorPalette.TERTIARY));
    }};
    private final BasicButton hudButton = new BasicButton(192, BasicButton.SIZE_36, "Edit HUD", SVGs.LAYOUT_ALT, null, BasicButton.ALIGNMENT_LEFT, ColorPalette.TERTIARY);
    private final BasicButton closeButton = new BasicButton(192, BasicButton.SIZE_36, "Close", SVGs.X_CLOSE, null, BasicButton.ALIGNMENT_LEFT, ColorPalette.TERTIARY_DESTRUCTIVE);
    private int selected = 1;
    private Animation moveAnimation = null;
    private Animation sizeAnimation = null;
    private int y;
    private int sidebarY;

    public SideBar() {
        buttons.get(0).setClickAction(new CreditsPage());
        buttons.get(1).setClickAction(new ModsPage());
        buttons.get(4).setClickAction(new ModConfigPage(Preferences.getInstance().mod.defaultPage, true));
        hudButton.setClickAction(() -> GuiUtils.displayScreen(new HudGui()));
        closeButton.setClickAction(GuiUtils::closeScreen);
        for (BasicButton button : buttons) {
            if (button.hasClickAction()) continue;
            button.disable(true);
        }
    }

    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        final NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;

        this.y = y;
        for (BasicButton button : buttons) {
            if (!button.isClicked()) continue;
            moveSideBar(button, false);
            break;
        }
        if (moveAnimation != null) {
            nanoVGHelper.drawRoundedRect(vg, x + 16, y + moveAnimation.get() - (sizeAnimation.get() - 36) / 2f, 192, sizeAnimation.get(0), Colors.PRIMARY_600, 12);
            if (moveAnimation.isFinished() && sizeAnimation.isFinished()) {
                moveAnimation = null;
                sizeAnimation = null;
                buttons.get(selected).setColorPalette(ColorPalette.PRIMARY);
            }
        }

        sidebarY = y + 44;
        buttons.get(0).draw(vg, x + 16, calcAndIncrementLn(sidebarY), inputHandler);
//        buttons.get(1).draw(vg, x + 16, y + 116, inputHandler);
        nanoVGHelper.drawText(vg, "MOD CONFIG", x + 16, calcAndIncrementLn(sidebarY + 26), Colors.WHITE_50, 12, Fonts.SEMIBOLD);
        sidebarY = sidebarY - 26;
        buttons.get(1).draw(vg, x + 16, calcAndIncrementLn(sidebarY), inputHandler);
        buttons.get(2).draw(vg, x + 16, calcAndIncrementLn(sidebarY), inputHandler);
//        buttons.get(5).draw(vg, x + 16, listNewLn(sidebarY), inputHandler);
        nanoVGHelper.drawText(vg, "PERSONALIZATION", x + 16, calcAndIncrementLn(sidebarY + 26), Colors.WHITE_50, 12, Fonts.SEMIBOLD);
        sidebarY = sidebarY - 26;
        buttons.get(3).draw(vg, x + 16, calcAndIncrementLn(sidebarY), inputHandler);
        buttons.get(4).draw(vg, x + 16, calcAndIncrementLn(sidebarY), inputHandler);
        sidebarY = 0;

        hudButton.draw(vg, x + 16, y + 704, inputHandler);
        closeButton.draw(vg, x + 16, y + 748, inputHandler);
    }

    public void pageOpened(String page) {
        pageOpened(page, false);
    }

    public void pageOpened(String page, boolean instant) {
        for (BasicButton button : buttons) {
            if (!button.getText().equalsIgnoreCase(page)) continue;
            moveSideBar(button, instant);
            return;
        }
    }

    private void moveSideBar(BasicButton button) {
        moveSideBar(button, false);
    }

    private void moveSideBar(BasicButton button, boolean instant) {
        if (button.equals(buttons.get(selected))) return;
        buttons.get(selected).setColorPalette(ColorPalette.TERTIARY);
        if (instant) {
            moveAnimation = new DummyAnimation(button.y - y);
        } else {
            moveAnimation = new EaseOutExpo(300, buttons.get(selected).y - y, button.y - y, false);
        }
        sizeAnimation = new DummyAnimation(36);
        selected = buttons.indexOf(button);
    }

    // Utils
    private int calcAndIncrementLn(int n) {
        sidebarY = n + 36;
        return sidebarY;
    }
}
