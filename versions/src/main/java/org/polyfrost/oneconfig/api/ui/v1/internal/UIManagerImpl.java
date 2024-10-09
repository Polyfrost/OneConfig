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

package org.polyfrost.oneconfig.api.ui.v1.internal;

//#if MC >= 1.16.5
//$$ import org.lwjgl.opengl.GL;
//#else
import org.lwjgl.opengl.GLContext;
//#endif

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.lwjgl.isolatedloader.Lwjgl3Manager;
import org.polyfrost.lwjgl.isolatedloader.classloader.IsolatedClassLoader;
import org.polyfrost.oneconfig.api.ClassHasOverwrites;
import org.polyfrost.oneconfig.api.ui.v1.api.LwjglApi;
import org.polyfrost.oneconfig.api.ui.v1.api.NanoVgApi;
import org.polyfrost.oneconfig.api.ui.v1.api.StbApi;
import org.polyfrost.oneconfig.api.ui.v1.api.TinyFdApi;
import org.polyfrost.oneconfig.api.ui.v1.UIManager;
import org.polyfrost.oneconfig.api.ui.v1.internal.wrappers.MCWindow;
import org.polyfrost.oneconfig.api.ui.v1.internal.wrappers.PolyUIScreen;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.renderer.Renderer;
import org.polyfrost.polyui.renderer.Window;

import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("unused")
@ClassHasOverwrites("1.16.5-forge")
public class UIManagerImpl implements UIManager {

    private static final String LWJGL_API_PACKAGE = "org.polyfrost.oneconfig.api.ui.v1.api.";
    private static final String LWJGL_IMPL_PACKAGE = "org.polyfrost.oneconfig.api.ui.v1.internal.";

    private static final Logger LOGGER = LogManager.getLogger("OneConfig/LWJGL");

    private PolyUI ui;

    private final Set<String> classLoaderInclude = new HashSet<>();
    private final Map<String, Class<?>> classCache = new HashMap<>();

    private final LwjglApi lwjgl;
    private final NanoVgApi nanoVg;
    private final StbApi stb;
    private final TinyFdApi tinyFD;

    private final Renderer renderer;

    public UIManagerImpl() throws Throwable {
        Lwjgl3Manager.initialize(getClass().getClassLoader(), new String[] { "nanovg", "stb", "tinyfd" });

        IsolatedClassLoader classLoader = Lwjgl3Manager.getClassLoader();

        classLoader.addLoadingException("org.polyfrost.oneconfig.api.ui.v1.api.");

        try {
            boolean gl3 =
                //#if MC >= 1.16.5
                //$$ GL.getCapabilities().OpenGL30;
                //#else
                GLContext.getCapabilities().OpenGL30;
                //#endif

            lwjgl = Lwjgl3Manager.getIsolated(LwjglApi.class, LWJGL_IMPL_PACKAGE + "LwjglImpl");
            nanoVg = Lwjgl3Manager.getIsolated(NanoVgApi.class, LWJGL_IMPL_PACKAGE + "NanoVgImpl", gl3);
            stb = Lwjgl3Manager.getIsolated(StbApi.class, LWJGL_IMPL_PACKAGE + "StbImpl");
            tinyFD = Lwjgl3Manager.getIsolated(TinyFdApi.class, LWJGL_IMPL_PACKAGE + "TinyFdImpl");

            renderer = new RendererImpl(gl3, lwjgl, nanoVg, stb);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get valid rendering implementation", e);
        }
    }

    @Override
    public Renderer getRenderer() {
        return renderer;
    }

    @Override
    public TinyFdApi getTinyFD() {
        return tinyFD;
    }

    @Override
    public Object createPolyUIScreen(@NotNull PolyUI polyUI, float desiredScreenWidth, float desiredScreenHeight, boolean pauses, boolean blurs, Consumer<PolyUI> onClose) {
        return new PolyUIScreen(polyUI, desiredScreenWidth, desiredScreenHeight, pauses, blurs, onClose);
    }

    @Override
    public Window createWindow() {
        return new MCWindow(Minecraft.getMinecraft());
    }

    @Override
    public @NotNull PolyUI getDefaultInstance() {
        return ui == null ? ui = createDefault() : ui;
    }
}
