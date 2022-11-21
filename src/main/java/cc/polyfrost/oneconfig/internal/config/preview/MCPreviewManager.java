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

package cc.polyfrost.oneconfig.internal.config.preview;

import cc.polyfrost.oneconfig.config.preview.MCPreview;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.utils.InputHandler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class MCPreviewManager {
    public static final MCPreviewManager INSTANCE = new MCPreviewManager();
    public final ArrayList<MCPreview> previews = new ArrayList<>();
    private final MethodHandle drawHandle;
    private MCPreviewManager() {
        try {
            Method draw = MCPreview.class.getDeclaredMethod("setupDraw", UMatrixStack.class);
            draw.setAccessible(true);
            drawHandle = MethodHandles.lookup().unreflect(draw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void drawMCPreviews(UMatrixStack matrixStack) {
        for (MCPreview preview : previews) {
            try {
                drawHandle.invoke(preview, matrixStack);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }
}
