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

//#if FORGE && MC<=11202
package org.polyfrost.oneconfig.internal.legacy;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.CoreModManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.utils.v1.MHUtils;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Mixin-related loading code adapted from EssentialGG's EssentialLoader under GPL-3.0
 * <a href="https://github.com/EssentialGG/EssentialLoader/blob/master/LICENSE">here</a>
 */
@SuppressWarnings("unused")
public class OneConfigTweaker implements ITweaker {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Tweaker");
    private static final String MIXIN_TWEAKER = "org.spongepowered.asm.launch.MixinTweaker";
    private static MethodHandle loadCoreMod;
    private static Consumer<URI> addContainer;

    public OneConfigTweaker() {
        final List<SourceFile> sourceFiles = getSourceFiles();
        if (sourceFiles.isEmpty()) {
            if (!Platform.getLoaderPlatform().isDevelopmentEnvironment()) LOGGER.fatal("Not able to detect jar sources. mixin will NOT work!");
            return;
        }
        try {
            injectMixinTweaker();
        } catch (Exception e) {
            LOGGER.error("failed to inject mixin tweaker", e);
        }
        for (SourceFile sourceFile : sourceFiles) {
            try {
                setupSourceFile(sourceFile);
            } catch (Throwable t) {
                LOGGER.error("failed to setup mixin for {}", sourceFile.path.toString(), t);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void setupSourceFile(SourceFile sourceFile) throws Throwable {
        String path = sourceFile.path.toString();
        // Forge will by default ignore a mod file if it contains a tweaker
        // So we need to remove ourselves from that exclusion list
        CoreModManager.getIgnoredMods().remove(path);

        // And instead add ourselves to the mod candidate list
        CoreModManager.getReparseableCoremods().add(path);

        // FML will not load CoreMods if it finds a tweaker, so we need to load the coremod manually if present
        // We do this to reduce the friction of adding our tweaker if a mod has previously been relying on a
        // coremod (cause ordinarily they would have to convert their coremod into a tweaker manually).
        // Mixin takes care of this as well, so we mustn't if it will.
        String coreMod = sourceFile.coreMod;
        if (coreMod != null && !sourceFile.mixin) {
            ITweaker tweaker = loadCoreMod(Launch.classLoader, coreMod, sourceFile.path.toFile());
            ((List<ITweaker>) Launch.blackboard.get("Tweaks")).add(tweaker);
        }

        // Mixin will only look at jar files which declare the MixinTweaker as their tweaker class, so we need
        // to manually add our source files for inspection.
        if (sourceFile.mixin) addContainer(sourceFile.path.toUri());
    }

    private static ITweaker loadCoreMod(LaunchClassLoader loader, String cls, File src) throws Throwable {
        MethodHandle mh = loadCoreMod == null ? loadCoreMod = createLoadCoreMod() : loadCoreMod;
        return (ITweaker) mh.invoke(loader, src, src);
    }

    private static MethodHandle createLoadCoreMod() {
        return MHUtils.getStaticMethodHandle(CoreModManager.class, "loadCoreMod", ITweaker.class, LaunchClassLoader.class, String.class, File.class).getOrThrow();
    }

    private static void addContainer(URI uri) {
        Consumer<URI> c = addContainer == null ? addContainer = createAddContainer() : addContainer;
        c.accept(uri);
    }

    private static Consumer<URI> createAddContainer() {
        try {
            Class<?> MixinBootstrap = Class.forName("org.spongepowered.asm.launch.MixinBootstrap");
            Class<?> MixinPlatformManager = Class.forName("org.spongepowered.asm.launch.platform.MixinPlatformManager");
            Object platformManager = MHUtils.invokeStatic(MixinBootstrap, "getPlatform");
            try {
                // Mixin 0.7
                return MHUtils.getConsumerFunctionHandle(platformManager, "addContainer", URI.class).getOrThrow();
            } catch (Throwable ignored) {
                // Mixin 0.8
                Class<?> IContainerHandle = Class.forName("org.spongepowered.asm.launch.platform.container.IContainerHandle");
                Class<?> ContainerHandleURI = Class.forName("org.spongepowered.asm.launch.platform.container.ContainerHandleURI");
                // as we can't refer to IContainerHandle, we have to use unchecked casts here.
                // noinspection unchecked
                Consumer<Object> addWrappedContainer = (Consumer<Object>) MHUtils.getConsumerFunctionHandle(MixinPlatformManager, "addContainer", IContainerHandle).getOrThrow();
                MethodHandle wrapper = MHUtils.getConstructorHandle(ContainerHandleURI, URI.class).getOrThrow();
                return it -> addWrappedContainer.accept(MHUtils.invokeCatching(wrapper, it).getOrThrow());
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to crack addContainer method. mixin will not work!", t);
        }
    }

    private static List<SourceFile> getSourceFiles() {
        List<SourceFile> sourceFiles = new ArrayList<>();
        for (URL url : Launch.classLoader.getSources()) {
            try {
                URI uri = url.toURI();
                if (!"file".equals(uri.getScheme())) {
                    continue;
                }
                Path file = Paths.get(uri);
                if (!Files.exists(file) || !Files.isRegularFile(file)) {
                    continue;
                }
                String tweakClass = null;
                String coreMod = null;
                boolean mixin = false;
                try (JarFile jar = new JarFile(file.toFile())) {
                    if (jar.getManifest() != null) {
                        Attributes attributes = jar.getManifest().getMainAttributes();
                        tweakClass = attributes.getValue("TweakClass");
                        coreMod = attributes.getValue("FMLCorePlugin");
                        mixin = attributes.getValue("MixinConfigs") != null;
                    }
                }
                if (Objects.equals(tweakClass, "cc.polyfrost.oneconfigwrapper.OneConfigWrapper") || Objects.equals(tweakClass, "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker")) {
                    sourceFiles.add(new SourceFile(file, coreMod, mixin));
                }
            } catch (Exception e) {
                LOGGER.error("failed to inspect jar file {}, ignoring", url, e);
            }
        }
        return sourceFiles;
    }

    private static void injectMixinTweaker() throws ClassNotFoundException {
        @SuppressWarnings("unchecked")
        List<String> tweakClasses = (List<String>) Launch.blackboard.get("TweakClasses");

        // If the MixinTweaker is already queued (because of another mod), then there's nothing we need to do
        if (tweakClasses.contains(MIXIN_TWEAKER)) {
            // Except we do need to initialize the MixinTweaker immediately so we can add containers
            // for our mods.
            // This is idempotent, so we can call it without adding to the tweaks list (and we must not add to
            // it because the queued tweaker will already get added and there is nothing we can do about that).
            initMixinTweaker();
            return;
        }

        // If it is already booted, we're also good to go
        if (Launch.blackboard.get("mixin.initialised") != null) {
            return;
        }

        LOGGER.info("Injecting MixinTweaker from OneConfigTweaker");

        // Otherwise, we need to take things into our own hands because the normal way to chainload a tweaker
        // (by adding it to the TweakClasses list during injectIntoClassLoader) is too late for Mixin.
        // Instead we instantiate the MixinTweaker on our own and add it to the current Tweaks list immediately.
        @SuppressWarnings("unchecked")
        List<ITweaker> tweaks = (List<ITweaker>) Launch.blackboard.get("Tweaks");
        tweaks.add(initMixinTweaker());
    }

    private static ITweaker initMixinTweaker() throws ClassNotFoundException {
        Launch.classLoader.addClassLoaderExclusion(MIXIN_TWEAKER.substring(0, MIXIN_TWEAKER.lastIndexOf('.')));
        return (ITweaker) MHUtils.instantiate(Class.forName(MIXIN_TWEAKER, true, Launch.classLoader), false).getOrThrow();
    }

    private static class SourceFile {
        final Path path;
        final String coreMod;
        final boolean mixin;

        private SourceFile(Path path, String coreMod, boolean mixin) {
            this.path = path;
            this.coreMod = coreMod;
            this.mixin = mixin;
        }
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.oneconfig.json");
        removeLWJGLException();

        // performance fix
        classLoader.addTransformerExclusion("kotlin.");
        classLoader.addTransformerExclusion("org.polyfrost.oneconfig.ui.");
        classLoader.addTransformerExclusion("org.polyfrost.polyui.");

        // remove log spam
        classLoader.addTransformerExclusion("org.lwjgl.");
    }

    /**
     * Taken from LWJGLTwoPointFive under The Unlicense
     * <a href="https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/">https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/</a>
     */
    @SuppressWarnings("unchecked")
    private static void removeLWJGLException() {
        try {
            Set<String> exceptions = (Set<String>) MHUtils.getField(Launch.classLoader, "classLoaderExceptions").getOrThrow();
            exceptions.remove("org.lwjgl.");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
    }

    @Override
    public String getLaunchTarget() {
        return null;
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }
}
//#endif
