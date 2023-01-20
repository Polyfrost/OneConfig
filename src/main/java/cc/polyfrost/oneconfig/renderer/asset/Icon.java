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

package cc.polyfrost.oneconfig.renderer.asset;

import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import com.google.gson.annotations.SerializedName;

public class Icon {
    private final Object icon;
    public final Type type;

    public Icon(String filePath) {
        this.icon = filePath.endsWith(".svg") ? new SVG(filePath) : new Image(filePath);
        this.type = filePath.endsWith(".svg") ? Type.SVG : Type.IMAGE;
    }

    public Icon(SVG svg) {
        this.icon = svg;
        this.type = Type.SVG;
    }

    public Icon(Image image) {
        this.icon = image;
        this.type = Type.IMAGE;
    }

    public SVG getSVG() {
        return (SVG) icon;
    }

    public Image getImage() {
        return (Image) icon;
    }

    public void draw(long vg, float x, float y, float width, float height, int color, float scale) {
        if (type == Type.SVG) {
            NanoVGHelper.INSTANCE.drawSvg(vg, getSVG(), x, y, width, height, color, scale);
        } else if (type == Type.IMAGE) {
            NanoVGHelper.INSTANCE.drawImage(vg, getImage(), x, y, width, height, color);
        }
    }

    public void draw(long vg, float x, float y, float width, float height, int color) {
        if (type == Type.SVG) {
            NanoVGHelper.INSTANCE.drawSvg(vg, getSVG(), x, y, width, height, color);
        } else if (type == Type.IMAGE) {
            NanoVGHelper.INSTANCE.drawImage(vg, getImage(), x, y, width, height, color);
        }
    }

    public void draw(long vg, float x, float y, float width, float height) {
        if (type == Type.SVG) {
            NanoVGHelper.INSTANCE.drawSvg(vg, getSVG(), x, y, width, height);
        } else if (type == Type.IMAGE) {
            NanoVGHelper.INSTANCE.drawImage(vg, getImage(), x, y, width, height);
        }
    }

    public enum Type {
        @SerializedName("0")
        SVG,
        @SerializedName("1")
        IMAGE
    }
}
