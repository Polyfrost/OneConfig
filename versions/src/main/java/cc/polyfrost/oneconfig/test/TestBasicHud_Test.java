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

package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.hud.BasicHud;
import cc.polyfrost.oneconfig.internal.assets.Images;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.renderer.LwjglManager;


public class TestBasicHud_Test extends BasicHud {
    @Override
    protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
        LwjglManager.INSTANCE.getNanoVGHelper().setupAndDraw(true, vg -> LwjglManager.INSTANCE.getNanoVGHelper().drawImage(vg, Images.HUE_GRADIENT.filePath, x, y, 50 * scale, 50f * scale));
    }

    @Override
    protected float getWidth(float scale, boolean example) {
        return 50 * scale;
    }

    @Override
    protected float getHeight(float scale, boolean example) {
        return 50 * scale;
    }
}
