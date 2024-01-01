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

import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.ColorAnimation;
import cc.polyfrost.oneconfig.gui.animations.EaseOutQuad;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.scissor.Scissor;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorHelper;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;

import java.util.ArrayList;
import java.util.Objects;

/**
 * A page is a 1056x728 rectangle of the GUI. It is the main content of the gui, and can be switched back and forwards easily. All the content of OneConfig is in a page.
 */
public abstract class Page {
    protected final String title;
    protected Animation scrollAnimation;
    private final ColorAnimation colorAnimation = new ColorAnimation(new ColorPalette(Colors.TRANSPARENT, Colors.GRAY_400_60, Colors.GRAY_400_60), 200);
    protected float scrollTarget;
    private long scrollTime;
    private boolean mouseWasDown, dragging;
    private float yStart;
    protected float scroll;
    public final ArrayList<Page> parents = new ArrayList<>();
    private String previousSearch = "";

    public Page(String title) {
        this.title = title;
    }

    public abstract void draw(long vg, int x, int y, InputHandler inputHandler);

    /**
     * Use this method to draw elements that are static on the page (ignore the scrolling).
     *
     * @return the total height of the elements, so they are excluded from the scissor rectangle.
     */
    public int drawStatic(long vg, int x, int y, InputHandler inputHandler) {
        return 0;
    }

    public void finishUpAndClose() {
        scroll = 0;
        scrollTarget = 0;
        scrollTime = 0;
        scrollAnimation = null;
    }

    public void scrollWithDraw(long vg, int x, int y, InputHandler inputHandler) {
        ScissorHelper scissorHelper = ScissorHelper.INSTANCE;
        int maxScroll = getMaxScrollHeight();
        int scissorOffset = drawStatic(vg, x, y, inputHandler);
        if (OneConfigGui.INSTANCE != null) {
            if (!Objects.equals(previousSearch, OneConfigGui.INSTANCE.getSearchValue())) {
                previousSearch = OneConfigGui.INSTANCE.getSearchValue();
                finishUpAndClose();
            }
        }
        scroll = scrollAnimation == null ? scrollTarget : scrollAnimation.get();
        final float scrollBarLength = (728f / maxScroll) * 728f;
        Scissor scissor = scissorHelper.scissor(vg, x, y + scissorOffset, x + 1056, y + 728 - scissorOffset);
        Scissor inputScissor = inputHandler.blockInputArea(x, y,1056, scissorOffset);
        float dWheel = (float) inputHandler.getDWheel();
        if (dWheel != 0) {
            scrollTarget += dWheel;

            if (scrollTarget > 0f) scrollTarget = 0f;
            else if (scrollTarget < -maxScroll + 728) scrollTarget = -maxScroll + 728;

            scrollAnimation = new EaseOutQuad(150, scroll, scrollTarget, false);
            scrollTime = System.currentTimeMillis();
        } else if (scrollAnimation != null && scrollAnimation.isFinished()) scrollAnimation = null;
        if (maxScroll <= 728) {
            draw(vg, x, y, inputHandler);
            scissorHelper.resetScissor(vg, scissor);
            inputHandler.stopBlock(inputScissor);
            return;
        }
        draw(vg, x, (int) (y + scroll), inputHandler);
        if (dragging && inputHandler.isClicked(true)) {
            dragging = false;
        }

        scissorHelper.resetScissor(vg, scissor);
        inputHandler.stopBlock(inputScissor);
        if (!(scrollBarLength > 727f)) {
            final float scrollBarY = (scroll / maxScroll) * 720f;
            final boolean isMouseDown = Platform.getMousePlatform().isButtonDown(0);
            final boolean scrollHover = inputHandler.isAreaHovered(x + 1042, (int) (y - scrollBarY), 12, (int) scrollBarLength);
            final boolean scrollTimePeriod = (System.currentTimeMillis() - scrollTime < 1000);
            if (scrollHover && isMouseDown && !mouseWasDown) {
                yStart = inputHandler.mouseY();
                dragging = true;
            }
            mouseWasDown = isMouseDown;
            if (dragging) {
                scrollTarget = -(inputHandler.mouseY() - yStart) * maxScroll / 728f;
                if (scrollTarget > 0f) scrollTarget = 0f;
                else if (scrollTarget < -maxScroll + 728) scrollTarget = -maxScroll + 728;
                scrollAnimation = new EaseOutQuad(150, scroll, scrollTarget, false);
            }
            NanoVGHelper.INSTANCE.drawRoundedRect(vg, x + 1048, y - scrollBarY, 4, scrollBarLength, colorAnimation.getColor(scrollHover || scrollTimePeriod, dragging), 4f);
        }
    }

    public String getTitle() {
        return title;
    }

    public void keyTyped(char key, int keyCode) {
    }

    /**
     * Overwrite this method and make it return true if you want this to always be the base in breadcrumbs
     */
    public boolean isBase() {
        return false;
    }

    /**
     * Use this method to set the maximum scroll height of the page.
     */
    public int getMaxScrollHeight() {
        return 728;
    }
}
