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
//$$ import org.polyfrost.oneconfig.api.platform.v1.Platform;
//$$ import org.polyfrost.lwjgl.isolatedloader.Lwjgl3Downloader;
//$$ import java.nio.file.Path;
//#else
import org.polyfrost.lwjgl.isolatedloader.Lwjgl3Manager;
import org.polyfrost.lwjgl.isolatedloader.classloader.IsolatedClassLoader;
//#endif

import dev.deftu.omnicore.api.OmniResourceLocation;
import dev.deftu.omnicore.api.client.render.DefaultVertexFormats;
import dev.deftu.omnicore.api.client.render.DrawMode;
import dev.deftu.omnicore.api.client.render.GlCapabilities;
import dev.deftu.omnicore.api.client.render.pipeline.OmniRenderPipeline;
import dev.deftu.omnicore.api.client.render.pipeline.OmniRenderPipelines;
import dev.deftu.omnicore.api.client.render.OmniRenderingContext;
import dev.deftu.omnicore.api.client.render.state.OmniBlendState;
import dev.deftu.omnicore.api.client.screen.OmniScreen;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.ClassHasOverwrites;
import org.polyfrost.oneconfig.api.ui.v1.api.*;
import org.polyfrost.oneconfig.api.ui.v1.UIManager;
import org.polyfrost.oneconfig.api.ui.v1.internal.wrappers.MCWindow;
import org.polyfrost.oneconfig.api.ui.v1.internal.wrappers.PolyUIScreen;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.data.Font;
import org.polyfrost.polyui.renderer.Renderer;
import org.polyfrost.polyui.renderer.Window;

import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("unused")
@ClassHasOverwrites("1.16.5-forge")
public class UIManagerImpl implements UIManager {

    private static final String LWJGL_API_PACKAGE = "org.polyfrost.oneconfig.api.ui.v1.api.";
    private static final String LWJGL_IMPL_PACKAGE = "org.polyfrost.oneconfig.api.ui.v1.internal.";

    private static final Font MC_FONT = Font.of("minecraft/assets/fonts/nowhere", null, false, Font.Weight.Regular);

    private static final Logger LOGGER = LogManager.getLogger("OneConfig/LWJGL");

    private PolyUI ui;
    private OmniRenderPipeline pipeline;
    private OmniRenderingContext ctx;

    private final Set<String> classLoaderInclude = new HashSet<>();
    private final Map<String, Class<?>> classCache = new HashMap<>();

//    private final NanoVgApi nanoVg;
    private final NanoSvgApi nanoSvg;
    private final StbApi stb;
    private final TinyFdApi tinyFD;

    private final Renderer renderer;

    public UIManagerImpl() throws Throwable {
        // Tells the isolated LWJGL3 loader that we want to download & load a slightly older LWJGL version
        // because these Minecraft versions explode otherwise. This is likely due to the natives being
        // moved in ~3.2.3
        //#if MC >= 1.16.5
        //$$ System.setProperty("isolatedlwjgl3loader.ignoreSystemModule", "true");
        //$$ int major = org.lwjgl.Version.VERSION_MAJOR;
        //$$ int minor = org.lwjgl.Version.VERSION_MINOR;
        //$$ int patch = org.lwjgl.Version.VERSION_REVISION;
        //$$ System.setProperty("isolatedlwjgl3loader.lwjglVersion", major + "." + minor + "." + patch);
        //#endif

        String[] lwjglModules = new String[]{"nanovg", "stb", "tinyfd"};

        //#if MC >= 1.16.5
        //$$ Set<Path> nativesPaths = Lwjgl3Downloader.downloadNatives(lwjglModules);
        //$$ for (Path path : nativesPaths) {
        //$$     Platform.loader().addToClasspath(path);
        //$$ }
        //#else
        Lwjgl3Manager.initialize(getClass().getClassLoader(), lwjglModules);

        IsolatedClassLoader classLoader = Lwjgl3Manager.getClassLoader();

        classLoader.addLoadingException("kotlin.");
        classLoader.addLoadingException("org.polyfrost.oneconfig.api.ui.v1.api.");
        //#endif

        try {
            boolean isGl3 = GlCapabilities.isGl3Available();

            //#if MC >= 1.16.5
//            //$$ nanoVg = new NanoVgImpl(isGl3);
            //$$ nanoSvg = new NanoSvgImpl();
            //$$ stb = new StbImpl();
            //$$ tinyFD = new TinyFdImpl();
            //#else
//            nanoVg = Lwjgl3Manager.getIsolated(NanoVgApi.class, LWJGL_IMPL_PACKAGE + "NanoVgImpl", isGl3);
            nanoSvg = Lwjgl3Manager.getIsolated(NanoSvgApi.class, LWJGL_IMPL_PACKAGE + "NanoSvgImpl");
            stb = Lwjgl3Manager.getIsolated(StbApi.class, LWJGL_IMPL_PACKAGE + "StbImpl");
            tinyFD = Lwjgl3Manager.getIsolated(TinyFdApi.class, LWJGL_IMPL_PACKAGE + "TinyFdImpl");
            //#endif

//            renderer = new NVGRendererImpl(isGl3, nanoVg, nanoSvg, stb);
             renderer = new GLRendererImpl(nanoSvg, stb);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get valid rendering implementation", e);
        }
    }

    @Override
    public @Nullable RendererExt getRendererExt() {
        return (renderer instanceof RendererExt ? (RendererExt) renderer : null);
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
    public OmniScreen createPolyUIScreen(@NotNull PolyUI polyUI, float designedWidth, float designedHeight, boolean pauses, boolean blurs, Consumer<PolyUI> onClose) {
        return new PolyUIScreen(polyUI, designedWidth, designedHeight, pauses, blurs, onClose);
    }

    @Override
    public Window createWindow() {
        return new MCWindow(Minecraft.getMinecraft());
    }

    @Override
    public @NotNull PolyUI getDefaultInstance() {
        return ui == null ? ui = createDefault() : ui;
    }

    public OmniRenderPipeline getRenderPipeline() {
        if (pipeline == null) {
            this.pipeline = OmniRenderPipelines.builderWithDefaultShader(
                    OmniResourceLocation.createOrThrow("oneconfig", "uimanager"),
                    DefaultVertexFormats.POSITION_TEXTURE_COLOR,
                    DrawMode.QUADS
            ).setBlendState(OmniBlendState.ALPHA).build();
        }

        return pipeline;
    }

    @Override
    public void __setRenderingContext(OmniRenderingContext renderingContext) {
        ctx = renderingContext;
    }

    @Override
    public OmniRenderingContext getRenderingContext() {
        return ctx;
    }

    @Override
    public Font getMCFont() {
        return MC_FONT;
    }
}
