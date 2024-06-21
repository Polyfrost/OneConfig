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

//#if FORGE==1 && MC<=11202
package cc.polyfrost.oneconfig.internal.plugin.asm;

import cc.polyfrost.oneconfig.internal.init.OneConfigInit;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.CoreModManager;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Mixin-related loading code adapted from EssentialGG's EssentialLoader
 * https://github.com/EssentialGG/EssentialLoader/blob/master/LICENSE
 */
public class OneConfigTweaker implements ITweaker {
    private static final String MIXIN_TWEAKER = "org.spongepowered.asm.launch.MixinTweaker";

    public OneConfigTweaker() {
        try {
            injectMixinTweaker();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        final List<SourceFile> sourceFiles = getSourceFiles();
        if (sourceFiles.isEmpty()) {
            System.out.println("Not able to determine current file. Mod will NOT work");
            return;
        }
        for (SourceFile sourceFile : sourceFiles) {
            try {
                setupSourceFile(sourceFile);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        MixinEnvironment.getDefaultEnvironment().addTransformerExclusion("com.creativemd.itemphysic.ItemTransformer");
    }

    @SuppressWarnings("unchecked")
    private void setupSourceFile(SourceFile sourceFile) throws Exception {
        // Forge will by default ignore a mod file if it contains a tweaker
        // So we need to remove ourselves from that exclusion list
        Field ignoredModFile = CoreModManager.class.getDeclaredField("ignoredModFiles");
        ignoredModFile.setAccessible(true);
        ((List<String>) ignoredModFile.get(null)).remove(sourceFile.file.getName());

        // And instead add ourselves to the mod candidate list
        CoreModManager.getReparseableCoremods().add(sourceFile.file.getName());

        // FML will not load CoreMods if it finds a tweaker, so we need to load the coremod manually if present
        // We do this to reduce the friction of adding our tweaker if a mod has previously been relying on a
        // coremod (cause ordinarily they would have to convert their coremod into a tweaker manually).
        // Mixin takes care of this as well, so we mustn't if it will.
        String coreMod = sourceFile.coreMod;
        if (coreMod != null && !sourceFile.mixin) {
            Method loadCoreMod = CoreModManager.class.getDeclaredMethod("loadCoreMod", LaunchClassLoader.class, String.class, File.class);
            loadCoreMod.setAccessible(true);
            ITweaker tweaker = (ITweaker) loadCoreMod.invoke(null, Launch.classLoader, coreMod, sourceFile.file);
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
                    arg = sourceFile.file.toURI();
                } catch (NoSuchMethodException ignored) {
                    // Mixin 0.8
                    Class<?> IContainerHandle = Class.forName("org.spongepowered.asm.launch.platform.container.IContainerHandle");
                    Class<?> ContainerHandleURI = Class.forName("org.spongepowered.asm.launch.platform.container.ContainerHandleURI");
                    addContainer = MixinPlatformManager.getDeclaredMethod("addContainer", IContainerHandle);
                    arg = ContainerHandleURI.getDeclaredConstructor(URI.class).newInstance(sourceFile.file.toURI());
                }
                addContainer.invoke(platformManager, arg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private List<SourceFile> getSourceFiles() {
        List<SourceFile> sourceFiles = new ArrayList<>();
        for (URL url : Launch.classLoader.getSources()) {
            try {
                URI uri = url.toURI();
                if (!"file".equals(uri.getScheme())) {
                    continue;
                }
                File file = new File(uri);
                if (!file.exists() || !file.isFile()) {
                    continue;
                }
                String tweakClass = null;
                String coreMod = null;
                boolean mixin = false;
                try (JarFile jar = new JarFile(file)) {
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
                e.printStackTrace();
            }
        }
        return sourceFiles;
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

        System.out.println("Injecting MixinTweaker from EssentialSetupTweaker");

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

    private static class SourceFile {
        final File file;
        final String coreMod;
        final boolean mixin;

        private SourceFile(File file, String coreMod, boolean mixin) {
            this.file = file;
            this.coreMod = coreMod;
            this.mixin = mixin;
        }
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
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.oneconfig.json");
        removeLWJGLException();
        Launch.classLoader.registerTransformer(ClassTransformer.class.getName());
        OneConfigInit.initialize(new String[]{});
        Launch.blackboard.put("oneconfig.init.initialized", true);
        Launch.classLoader.addClassLoaderExclusion("cc.polyfrost.oneconfig.internal.plugin.asm.");
    }

    /**
     * Taken from LWJGLTwoPointFive under The Unlicense
     * <a href="https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/">https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/</a>
     */
    private void removeLWJGLException() {
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
    public String getLaunchTarget() {
        return null;
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }
}
//#endif
