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

import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;

public class HomePage extends Page {
    /*private final BasicButton socialsBtn = new BasicButton(184, 36, "Socials", SVGs.SHARE, SVGs.POP_OUT, BasicButton.ALIGNMENT_CENTER, ColorUtils.PRIMARY);
    private final BasicButton discordBtn = new BasicButton(184, 36, "Discord", SVGs.WEBSITE, SVGs.LINK_DIAGONAL, BasicButton.ALIGNMENT_CENTER, ColorUtils.PRIMARY);
    private final BasicButton webBtn = new BasicButton(184, 36, "Website", SVGs.WEBSITE, null, BasicButton.ALIGNMENT_CENTER, ColorUtils.PRIMARY);
    private final BasicButton creditsBtn = new BasicButton(184, 36, "Credits", SVGs.AUDIO_PLAY, SVGs.LINK_DIAGONAL, BasicButton.ALIGNMENT_CENTER, ColorUtils.SECONDARY);
    private final BasicButton guideBtn = new BasicButton(184, 36, "Online Guide", SVGs.HELP_CIRCLE, null, BasicButton.ALIGNMENT_CENTER, ColorUtils.SECONDARY);
    private final BasicButton button1 = new BasicButton(184, BasicButton.SIZE_32, "HELLO", SVGs.MICROSOFT_ICON, SVGs.AUDIO_PLAY, BasicButton.ALIGNMENT_CENTER, ColorUtils.SECONDARY_TRANSPARENT);
    private final BasicButton button2 = new BasicButton(184, BasicButton.SIZE_36, "TESTING", SVGs.MICROSOFT_ICON, null, BasicButton.ALIGNMENT_LEFT, ColorUtils.PRIMARY_DESTRUCTIVE);
    private final BasicButton button3 = new BasicButton(184, BasicButton.SIZE_40, "BOBFISH", SVGs.MICROSOFT_ICON, null, BasicButton.ALIGNMENT_CENTER, ColorUtils.SECONDARY_DESTRUCTIVE);
    private final BasicButton button4 = new BasicButton(184, BasicButton.SIZE_48, "THIS is cool", null, SVGs.AUDIO_PLAY, BasicButton.ALIGNMENT_CENTER, 1);
    private final BasicButton button5 = new BasicButton(184, BasicButton.SIZE_36, "dhwuai", SVGs.MICROSOFT_ICON, SVGs.AUDIO_PLAY, BasicButton.ALIGNMENT_JUSTIFIED, 1);
    private final BasicButton button6 = new BasicButton(184, BasicButton.SIZE_32, "yes", SVGs.MICROSOFT_ICON, SVGs.AUDIO_PLAY, BasicButton.ALIGNMENT_RIGHT, 1);
    private final BasicButton button7 = new BasicButton(184, BasicButton.SIZE_32, "HELLO", SVGs.MICROSOFT_ICON, SVGs.AUDIO_PLAY, BasicButton.ALIGNMENT_CENTER, 1);
    private final BasicButton button8 = new BasicButton(184, BasicButton.SIZE_40, "HELLO", SVGs.MICROSOFT_ICON, SVGs.AUDIO_PLAY, BasicButton.ALIGNMENT_CENTER, ColorUtils.SECONDARY_DESTRUCTIVE);*/


    public HomePage() {
        super("Home Dashboard");
        /*socialsBtn.setClickAction(() -> NetworkUtils.browseLink("https://twitter.com/polyfrost"));
        discordBtn.setClickAction(() -> NetworkUtils.browseLink("https://discord.gg/4BdUuGpMdf"));
        webBtn.setClickAction(() -> NetworkUtils.browseLink("https://polyfrost.cc"));
        creditsBtn.setClickAction(new CreditsPage());
        guideBtn.setClickAction(() -> NetworkUtils.browseLink("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));*/

    }

    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        nanoVGHelper.drawRoundedRect(vg, x, y, 184, 36, -1, 12f);
        nanoVGHelper.drawText(vg, "This is a cool string to test pages", x + 32, y + 72, -1, 36f, Fonts.BOLD);
        nanoVGHelper.drawRoundedRect(vg, x + 350, y + 310, 300, 200, Colors.PRIMARY_600, 14f);
        nanoVGHelper.drawSvg(vg, SVGs.INFO_CIRCLE, x + 20, y + 604, 24, 24);
        nanoVGHelper.drawText(vg, "Info", x + 52, y + 618, Colors.WHITE_90, 24f, Fonts.MEDIUM);
        nanoVGHelper.drawRoundedRect(vg, x + 16, y + 644, 1024, 64, Colors.GRAY_700, 20f);

        // fuck you to whoever put that here
        nanoVGHelper.drawURL(vg, "https://www.youtube.com/watch?v=dQw4w9WgXcQ", x + 100, y + 205, 24, Fonts.MEDIUM, inputHandler);

        /*discordBtn.draw(vg, x + 32, y + 658);
        webBtn.draw(vg, x + 232, y + 658);
        socialsBtn.draw(vg, x + 432, y + 658);
        creditsBtn.draw(vg, x + 632, y + 658);
        guideBtn.draw(vg, x + 832, y + 658);
        if (socialsBtn.isClicked()) {
            OneConfigGui.INSTANCE.initColorSelector(new ColorSelector(new OneColor(new Color(255, 0, 255, 127)), InputUtils.mouseX(), InputUtils.mouseY()));
        }

        button1.draw(vg, x + 100, y + 100);
        button2.draw(vg, x + 100, y + 150);
        button3.draw(vg, x + 100, y + 250);
        button4.draw(vg, x + 100, y + 400);
        button5.draw(vg, x + 100, y + 600);
        button6.draw(vg, x + 350, y + 100);
        button7.draw(vg, x + 350, y + 300);
        button8.draw(vg, x + 350, y + 450);*/
    }

    @Override
    public boolean isBase() {
        return true;
    }
}
