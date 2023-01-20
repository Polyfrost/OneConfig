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
import org.spongepowered.asm.launch.MixinTweaker;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public class OneConfigTweaker implements ITweaker {
    public OneConfigTweaker() {
        for (URL url : Launch.classLoader.getSources()) {
            doMagicMixinStuff(url);
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
                            if (Objects.equals(tweakerClass, "cc.polyfrost.oneconfigwrapper.OneConfigWrapper")) {
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
                                        MixinBootstrap.getPlatform().addContainer(uri);
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