/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

//#if FORGE==1 && MC<=11202
package org.polyfrost.oneconfig.internal.init;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.CoreModManager;
import org.polyfrost.oneconfig.utils.MHUtils;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.MixinTweaker;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

@SuppressWarnings("unused")
public class OneConfigTweaker implements ITweaker {
    public OneConfigTweaker() {
        try {
            for (URL url : Launch.classLoader.getSources()) {
                doMagicMixinStuff(url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void doMagicMixinStuff(URL url) {
        try {
            URI uri = url.toURI();
            if (Objects.equals(uri.getScheme(), "file")) {
                File file = new File(uri);
                if (file.exists() && file.isFile()) {
                    try (JarFile jarFile = new JarFile(file)) {
                        if (jarFile.getManifest() != null) {
                            Attributes attributes = jarFile.getManifest().getMainAttributes();
                            String tweakerClass = attributes.getValue("TweakClass");
                            if (Objects.equals(tweakerClass, "cc.polyfrost.oneconfigwrapper.OneConfigWrapper") || Objects.equals(tweakerClass, "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker") || Objects.equals(tweakerClass, "org.polyfrost.oneconfig.loader.stage0.OneConfigTweaker")) {
                                CoreModManager.getIgnoredMods().remove(file.getName());
                                CoreModManager.getReparseableCoremods().add(file.getName());
                                String mixinConfig = attributes.getValue("MixinConfigs");
                                if (mixinConfig != null) {
                                    try {
                                        try {
                                            List<String> tweakClasses = (List<String>) Launch.blackboard.get("TweakClasses"); // tweak classes before other mod trolling
                                            if (tweakClasses.contains("org.spongepowered.asm.launch.MixinTweaker")) { // if there's already a mixin tweaker, we'll just load it like "usual"
                                                new MixinTweaker(); // also we might not need to make a new mixin tweawker all the time but im just making sure
                                            } else if (!Launch.blackboard.containsKey("mixin.initialised")) { // if there isnt, we do our own trolling
                                                List<ITweaker> tweaks = (List<ITweaker>) Launch.blackboard.get("Tweaks");
                                                tweaks.add(new MixinTweaker());
                                            }
                                        } catch (Exception ignored) {
                                            // if it fails i *think* we can just ignore it
                                        }
                                        try {
                                            MixinBootstrap.getPlatform().addContainer(uri);
                                        } catch (Exception ignore) {
                                            // fuck you essential
                                            try {
                                                Class<?> containerClass = Class.forName("org.spongepowered.asm.launch.platform.container.IContainerHandle");
                                                Class<?> urlContainerClass = Class.forName("org.spongepowered.asm.launch.platform.container.ContainerHandleURI");
                                                Object container = urlContainerClass.getConstructor(URI.class).newInstance(uri);
                                                //noinspection JavaReflectionInvocation
                                                MixinBootstrap.getPlatform().getClass().getDeclaredMethod("addContainer", containerClass).invoke(MixinBootstrap.getPlatform(), container);
                                            } catch (Exception e) {
                                                throw new RuntimeException("OneConfig's Mixin loading failed. Please contact https://polyfrost.org/discord to resolve this issue!", e);
                                            }
                                        }
                                    } catch (Exception ignored) {

                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        MixinBootstrap.init();
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
        removeLWJGLException();
        OneConfigInit.initialize(new String[]{});
        Launch.blackboard.put("oneconfig.init.initialized", true);
        Launch.classLoader.addClassLoaderExclusion("org.polyfrost.oneconfig.internal.plugin.asm.");

        // performance fix
        Launch.classLoader.addTransformerExclusion("kotlin.");
        Launch.classLoader.addTransformerExclusion("org.polyfrost.oneconfig.ui.");
        Launch.classLoader.addTransformerExclusion("org.polyfrost.polyui.");

        // remove log spam
        Launch.classLoader.addTransformerExclusion("org.lwjgl.");
    }

    /**
     * Taken from LWJGLTwoPointFive under The Unlicense
     * <a href="https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/">https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/</a>
     */
    @SuppressWarnings("unchecked")
    private void removeLWJGLException() {
        try {
            Set<String> exceptions = (Set<String>) MHUtils.getFieldGetter(LaunchClassLoader.class, "classLoaderExceptions", Set.class).getOrThrow().invokeExact(Launch.classLoader);
            exceptions.remove("org.lwjgl.");
        } catch (Throwable e) {
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
