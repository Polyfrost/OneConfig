/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.ui.v1.internal.wrappers;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.utils.v1.IOUtils;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.renderer.Window;
import org.polyfrost.polyui.data.Cursor;

//#if MC>=11300
//$$ import static org.lwjgl.glfw.GLFW.*;
//#endif

@ApiStatus.Internal
public class MCWindow extends Window {
    //#if MC>=11300
    //$$ private final long handle;
    //#endif

    public MCWindow(Minecraft mc) {
        super(Platform.screen().viewportWidth(), Platform.screen().viewportHeight(), Platform.screen().pixelRatio());
        //#if MC>=11300
        //$$ this.handle = mc.getMainWindow().getHandle();
        //#endif
    }

    @Override
    public void close() {
        Platform.screen().close();
    }

    @NotNull
    @Override
    public Window open(@NotNull PolyUI polyUI) {
        Platform.screen().display(new PolyUIScreen(polyUI, 0f, 0f, false, false, null));
        return this;
    }

    @Override
    public boolean supportsRenderPausing() {
        return false;
    }

    @Nullable
    @Override
    public String getClipboard() {
        //#if MC>=11300
        //$$ return glfwGetClipboardString(handle);
        //#else
        return IOUtils.getStringFromClipboard();
        //#endif
    }

    @Override
    public void setClipboard(@Nullable String s) {
        //#if MC>=11300
        //$$ glfwSetClipboardString(handle, s);
        //#else
        IOUtils.copyStringToClipboard(s);
        //#endif
    }

    @Override
    public void setCursor(@NotNull Cursor cursor) {
        //#if MC>=11300
        //$$ switch (cursor) {
        //$$     case Pointer:
        //$$         glfwSetCursor(handle, glfwCreateStandardCursor(GLFW_ARROW_CURSOR));
        //$$         return;
        //$$     case Clicker:
        //$$         glfwSetCursor(handle, glfwCreateStandardCursor(GLFW_HAND_CURSOR));
        //$$         return;
        //$$     case Text:
        //$$         glfwSetCursor(handle, glfwCreateStandardCursor(GLFW_IBEAM_CURSOR));
        //$$ }
        //#endif
    }

    @Override
    public void breakPause() {
    }

    @NotNull
    @Override
    public String getKeyName(int i) {
        String k = Platform.i18n().getKeyName(i, 0);
        return k == null ? "Unknown" : k;
    }
}
