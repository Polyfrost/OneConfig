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
import org.polyfrost.oneconfig.internal.OneConfigMixinInit;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Mixin-related loading code adapted from EssentialGG's EssentialLoader under GPL-3.0
 * <a href="https://github.com/EssentialGG/EssentialLoader/blob/master/LICENSE">here</a>
 * <p>
 *     The most important part of this code is `OneConfigSourceFile` in the manifest. If a mod creates a custom tweaker
 *     but still wants the Mixin tweaker to be injected, they can add this attribute to their jar manifest to signal
 *     to OneConfig that it should inject the Mixin tweaker regardless of the tweaker class.
 * </p>
 */
@SuppressWarnings("unused")
public class OneConfigTweaker implements ITweaker {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Tweaker");
    private static final String MIXIN_TWEAKER = "org.spongepowered.asm.launch.MixinTweaker";

    public OneConfigTweaker() {
        final List<SourceFile> sourceFiles = getSourceFiles();
        if (!sourceFiles.isEmpty()) {
            for (SourceFile sourceFile : sourceFiles) {
                try {
                    setupSourceFile(sourceFile);
                } catch (Throwable t) {
                    LOGGER.error("failed to setup mixin for {}", sourceFile.path.toString(), t);
                }
            }
        } else if (!isDevelopmentEnvironment()) {
            LOGGER.fatal("Not able to detect jar sources. mixin will NOT work!");
        }

        try {
            injectMixinTweaker();
        } catch (Exception e) {
            LOGGER.error("failed to inject mixin tweaker", e);
        }

        // Duplicated code from OneConfigPreLaunch
        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("mac")) {
            try {
                boolean supportsHiDPI = !Objects.equals(System.getProperty("os.arch"), "aarch64");
                if (!supportsHiDPI) {
                    try {
                        Class<?> clazz = Class.forName("org.lwjgl.Sys", false, OneConfigMixinInit.class.getClassLoader());
                        try {
                            clazz.getDeclaredField("HAS_HIDPI_FIX");
                            supportsHiDPI = true;
                        } catch (NoSuchFieldException ignored) {
                            // Field not found, continue with the default value
                        }
                    } catch (ClassNotFoundException ignored) {

                    }
                }
                if (!supportsHiDPI) {
                    injectLWJGLFix();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to inject LWJGL HiDPI fix, thing may look blurrier than usual...", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setupSourceFile(SourceFile sourceFile) throws Exception {
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
            Method loadCoreMod = CoreModManager.class.getDeclaredMethod("loadCoreMod", LaunchClassLoader.class, String.class, File.class);
            loadCoreMod.setAccessible(true);
            ITweaker tweaker = (ITweaker) loadCoreMod.invoke(null, Launch.classLoader, coreMod, sourceFile.path.toFile());
            ((List<ITweaker>) Launch.blackboard.get("Tweaks")).add(tweaker);
        }

        // If they declared our tweaker but also want to use mixin, then we'll inject the mixin tweaker
        // for them.
        if (sourceFile.mixin) {
            // Mixin will only look at jar files which declare the MixinTweaker as their tweaker class, so we need
            // to manually add our source files for inspection.
            try {
                injectMixinTweaker();

                Class<?> MixinBootstrap = Class.forName("org.spongepowered.asm.launch.MixinBootstrap");
                Class<?> MixinPlatformManager = Class.forName("org.spongepowered.asm.launch.platform.MixinPlatformManager");
                Object platformManager = MixinBootstrap.getDeclaredMethod("getPlatform").invoke(null);
                Method addContainer;
                Object arg;
                try {
                    // Mixin 0.7
                    addContainer = MixinPlatformManager.getDeclaredMethod("addContainer", URI.class);
                    arg = sourceFile.path.toUri();
                } catch (NoSuchMethodException ignored) {
                    // Mixin 0.8
                    Class<?> IContainerHandle = Class.forName("org.spongepowered.asm.launch.platform.container.IContainerHandle");
                    Class<?> ContainerHandleURI = Class.forName("org.spongepowered.asm.launch.platform.container.ContainerHandleURI");
                    addContainer = MixinPlatformManager.getDeclaredMethod("addContainer", IContainerHandle);
                    arg = ContainerHandleURI.getDeclaredConstructor(URI.class).newInstance(sourceFile.path.toUri());
                }
                addContainer.invoke(platformManager, arg);
            } catch (Exception e) {
                LOGGER.error("failed to inject mixin tweaker for {}", path, e);
            }
        }
    }

    private void injectMixinTweaker() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        @SuppressWarnings("unchecked")
        List<String> tweakClasses = (List<String>) Launch.blackboard.get("TweakClasses");

        // If the MixinTweaker is already queued (because of another mod), then there's nothing we need to to
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

        System.out.println("Injecting MixinTweaker from OneConfigTweaker");

        // Otherwise, we need to take things into our own hands because the normal way to chainload a tweaker
        // (by adding it to the TweakClasses list during injectIntoClassLoader) is too late for Mixin.
        // Instead we instantiate the MixinTweaker on our own and add it to the current Tweaks list immediately.
        @SuppressWarnings("unchecked")
        List<ITweaker> tweaks = (List<ITweaker>) Launch.blackboard.get("Tweaks");
        tweaks.add(initMixinTweaker());
    }

    private ITweaker initMixinTweaker() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Launch.classLoader.addClassLoaderExclusion(MIXIN_TWEAKER.substring(0, MIXIN_TWEAKER.lastIndexOf('.')));
        return (ITweaker) Class.forName(MIXIN_TWEAKER, true, Launch.classLoader).newInstance();
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
                boolean manualSourceFile = false;
                try (JarFile jar = new JarFile(file.toFile())) {
                    if (jar.getManifest() != null) {
                        Attributes attributes = jar.getManifest().getMainAttributes();
                        tweakClass = attributes.getValue("TweakClass");
                        coreMod = attributes.getValue("FMLCorePlugin");
                        mixin = attributes.getValue("MixinConfigs") != null;
                        manualSourceFile = attributes.getValue("OneConfigSourceFile") != null; // This is for when a mod uses a custom tweaker but still wants the Mixin tweaker to be injected
                    }
                }
                if (Objects.equals(tweakClass, "org.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker") || manualSourceFile) {
                    sourceFiles.add(new SourceFile(file, coreMod, mixin));
                }
            } catch (Exception e) {
                LOGGER.error("failed to inspect jar file {}, ignoring", url, e);
            }
        }
        return sourceFiles;
    }

    /**
     * The unofficial fork of LWJGL2 by Minecraft Machina/ManyMC breaks HiDPI support on aarch64 macOS systems.
     * This method injects a patched version of the LWJGL2 native library, which fixes the issue.
     * @throws IOException
     */
    private void injectLWJGLFix() throws IOException {
        LOGGER.warn("Injecting LWJGL HiDPI fix, this is only needed for aarch64 macOS systems that don't have updated natives!");
        File tempDir = Files.createTempDirectory("oneconfig-patched-lwjgl2-natives").toFile();
        tempDir.deleteOnExit();
        Path tempFile = tempDir.toPath().resolve("liblwjgl.dylib");
        Path tempFile2 = tempDir.toPath().resolve("liblwjgl-macos-aarch64.dylib");
        try (InputStream is = OneConfigTweaker.class.getResourceAsStream("/patched-lwjgl/liblwjgl.dylib")) {
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        try (InputStream is = OneConfigTweaker.class.getResourceAsStream("/patched-lwjgl/liblwjgl.dylib")) {
            Files.copy(is, tempFile2, StandardCopyOption.REPLACE_EXISTING);
        }
        System.setProperty("org.lwjgl.librarypath", tempDir.getAbsolutePath());
    }

    /**
     * Taken from LWJGLTwoPointFive under The Unlicense
     * <a href="https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/">https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/</a>
     */
    @SuppressWarnings("unchecked")
    private static void removeLWJGLException() {
        try {
            Field f_exceptions = LaunchClassLoader.class.getDeclaredField("classLoaderExceptions");
            f_exceptions.setAccessible(true);
            Set<String> exceptions = (Set<String>) f_exceptions.get(Launch.classLoader);
            exceptions.remove("org.lwjgl.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.oneconfigv1.json");
        Mixins.addConfiguration("mixins.oneconfigv1.init.json");
        removeLWJGLException();

        // performance fix
        classLoader.addTransformerExclusion("kotlin.");
        classLoader.addTransformerExclusion("org.polyfrost.oneconfig.ui.");
        classLoader.addTransformerExclusion("org.polyfrost.polyui.");

        // remove log spam
        classLoader.addTransformerExclusion("org.lwjgl.");
    }

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        boolean captureNext = false;
        for (String arg : args) {
            if (captureNext) {
                Mixins.addConfiguration(arg);
            }
            captureNext = "--mixin".equals(arg);
        }
    }

    @Override
    public String getLaunchTarget() {
        return null;
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }

    private static boolean isDevelopmentEnvironment() {
        Object o = Launch.blackboard.get("fml.deobfuscatedEnvironment");
        return o != null && (boolean) o;
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
}
//#endif
