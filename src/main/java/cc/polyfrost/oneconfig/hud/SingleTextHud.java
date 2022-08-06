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

package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.annotations.Text;

import java.util.Collections;
import java.util.List;

public abstract class SingleTextHud extends TextHud {
    public SingleTextHud(String title, boolean enabled) {
        this(title, enabled, 0, 0);
    }

    public SingleTextHud(String title, boolean enabled, int x, int y) {
        super(enabled, x, y);
        this.title = title;
    }

    /**
     * This function is called every tick
     *
     * @return The new text
     */
    protected abstract String getText(boolean example);

    /**
     * This function is called every frame
     *
     * @return The new text, null to use the cached value
     */
    protected String getTextFrequent(boolean example) {
        return null;
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        lines.add(getCompleteText(getText(example)));
    }

    @Override
    protected void getLinesFrequent(List<String> lines, boolean example) {
        String text = getTextFrequent(example);
        if (text == null) return;
        lines.clear();
        lines.add(getCompleteText(text));
    }

    protected final String getCompleteText(String text) {
        boolean showTitle = !title.trim().isEmpty();
        StringBuilder builder = new StringBuilder();
        if (brackets) {
            builder.append("[");
        }

        if (showTitle && titleLocation == 0) {
            builder.append(title).append(": ");
        }

        builder.append(text);

        if (showTitle && titleLocation == 1) {
            builder.append(" ").append(title);
        }

        if (brackets) {
            builder.append("]");
        }
        return builder.toString();
    }


    @Switch(
            name = "Brackets"
    )
    protected boolean brackets = false;

    @Text(
            name = "Title"
    )
    protected String title;

    @Dropdown(
            name = "Title Location",
            options = {"Left", "Right"}
    )
    protected int titleLocation = 0;
}