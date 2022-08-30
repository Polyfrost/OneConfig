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

/* COPYRIGHT NOTICE: MIT License
 * Copyright (c) 2021 Fudge and NEC contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.platform.LoaderPlatform;
import cc.polyfrost.oneconfig.platform.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Adapted from <a href="https://github.com/natanfudge/Not-Enough-Crashes">NotEnoughCrashes</a> under the <a href="https://opensource.org/licenses/MIT">MIT License</a>
 */
public class LogScanner {
    static final Logger LOGGER = LogManager.getLogger("OneConfig Log Scanner");
    static final boolean FORCE_DEBUG = System.getProperty("oneconfig.debugLogger", "false").equals("true");

    /**
     * Return a set of ActiveMods that have been blamed for the given stacktrace.
     * This will be an empty set if no mods are blamed.
     */
    @NotNull
    public static Set<LoaderPlatform.ActiveMod> identifyFromStacktrace(Throwable e) {
        Set<LoaderPlatform.ActiveMod> mods = new HashSet<>();
        // Include suppressed exceptions too
        visitChildrenThrowables(e, throwable -> {
            for (LoaderPlatform.ActiveMod newMod : identifyFromThrowable(throwable)) {
                if (mods.stream().noneMatch(mod -> mod.id.equals(newMod.id))) {
                    mods.add(newMod);
                }
            }
        });
        return mods;
    }

    /**
     * Attempt to get the caller from a given stacktrace.
     *
     * @return A singleton mod set that contains the caller, or an empty set if no caller is found.
     */
    @NotNull
    public static Set<LoaderPlatform.ActiveMod> identifyCallerFromStacktrace(Throwable e) {
        // first is this method name, second is the method it called, third is what called it
        StackTraceElement target = null;
        int i = 0;
        for (StackTraceElement element : e.getStackTrace()) {
            // ignore the first two
            if (i > 2) {
                // remove any that are native, or called from a system package
                if (!element.isNativeMethod() && !element.getClassName().startsWith("sun.") && !element.getClassName().startsWith("java.")
                        && !element.getClassName().startsWith("javax.") && !element.getClassName().startsWith("jdk.") && !element.getClassName().startsWith("com.sun.")) {
                    target = element;
                    break;
                }
            }
            i++;
        }
        if(target == null) {
            return Collections.emptySet();
        }
        Set<LoaderPlatform.ActiveMod> classMods = identifyFromClass(target.getClassName(), Platform.getLoaderPlatform().getLoadedMods());
        return classMods.isEmpty() ? Collections.emptySet() : classMods;
    }

    private static void visitChildrenThrowables(Throwable e, Consumer<Throwable> visitor) {
        visitor.accept(e);
        for (Throwable child : e.getSuppressed()) visitChildrenThrowables(child, visitor);
    }

    private static Set<LoaderPlatform.ActiveMod> identifyFromThrowable(Throwable e) {
        Set<String> involvedClasses = new LinkedHashSet<>();
        while (e != null) {
            for (StackTraceElement element : e.getStackTrace()) {
                involvedClasses.add(element.getClassName());
            }
            e = e.getCause();
        }

        Set<LoaderPlatform.ActiveMod> mods = new LinkedHashSet<>();
        for (String className : involvedClasses) {
            Set<LoaderPlatform.ActiveMod> classMods = identifyFromClass(className, Platform.getLoaderPlatform().getLoadedMods());
            mods.addAll(classMods);
        }
        return mods;
    }

    private static void debug(Supplier<String> message) {
        if (FORCE_DEBUG) LOGGER.info(message.get());
    }

    // TODO: get a list of mixin transformers that affected the class and blame those too
    @NotNull
    private static Set<LoaderPlatform.ActiveMod> identifyFromClass(String className, @NotNull List<LoaderPlatform.ActiveMod> modMap) {
        // Skip identification for Mixin, one's mod copy of the library is shared with all other mods
        if (className.startsWith("org.spongepowered.asm.mixin.")) {
            debug(() -> "Ignoring class " + className + " for identification because it is a mixin class");
            return Collections.emptySet();
        }

        try {
            // Get the URL of the class (don't initialize classes, though)
            Class<?> clazz = Class.forName(className, false, LogScanner.class.getClassLoader());
            CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                debug(() -> "Ignoring class " + className + " for identification because the code source could not be found");
                return Collections.emptySet(); // Some internal native sun classes
            }
            URL url = codeSource.getLocation();

            if (url == null) {
                LOGGER.warn("Failed to identify mod for " + className);
                return Collections.emptySet();
            }

            // Transform JAR URL to a file URL
            if (url.toURI().toString().startsWith("jar:")) {
                url = new URL(url.toURI().toString().substring(4, url.toURI().toString().lastIndexOf("!")));
            }
            if (url.toURI().toString().endsWith(".class") && Platform.getInstance().isDevelopmentEnvironment()) {
                LOGGER.error("The mod you are currently developing caused this issue, or another class file. Returning 'this'.");
                LOGGER.error("Class: " + className);
                return Collections.singleton(new LoaderPlatform.ActiveMod("this", "this", "Unknown", null));
            }
            Set<LoaderPlatform.ActiveMod> mods = getModsAt(Paths.get(url.toURI()), modMap);
            if (!mods.isEmpty()) {
                //noinspection OptionalGetWithoutIsPresent
                debug(() -> "Successfully placed blame of '" + className + "' on '"
                        + mods.stream().findFirst().get().name + "'");
            }
            return mods;
        } catch (URISyntaxException | ClassNotFoundException | NoClassDefFoundError | MalformedURLException e) {
            debug(() -> "Ignoring class " + className + " for identification because an error occurred");
            return Collections.emptySet(); // we cannot do it
        }
    }

    @NotNull
    private static Set<LoaderPlatform.ActiveMod> getModsAt(Path path, List<LoaderPlatform.ActiveMod> modMap) {
        Set<LoaderPlatform.ActiveMod> mods = modMap.stream().filter(m -> m.source.toPath().equals(path)).collect(Collectors.toSet());
        if (!mods.isEmpty()) return mods;
        else if (Platform.getInstance().isDevelopmentEnvironment()) {
            // For some reason, in dev, the mod being tested has the 'resources' folder as the origin instead of the 'classes' folder.
            String resourcesPathString = path.toString().replace("\\", "/")
                    // Make it work with Architectury as well
                    .replace("common/build/classes/java/main", "fabric/build/resources/main")
                    .replace("common/build/classes/kotlin/main", "fabric/build/resources/main")
                    .replace("classes/java/main", "resources/main")
                    .replace("classes/kotlin/main", "resources/main");
            Path resourcesPath = Paths.get(resourcesPathString);
            return modMap.stream().filter(m -> m.source.toPath().equals(resourcesPath)).collect(Collectors.toSet());
        } else {
            debug(() -> "Mod at path '" + path.toAbsolutePath() + "' is at fault," +
                    " but it could not be found in the map of mod paths: " /*+ modMap*/);
            return Collections.emptySet();
        }
    }
}
