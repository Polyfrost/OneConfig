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

package org.polyfrost.oneconfig.utils;

import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.platform.LoaderPlatform;
import org.polyfrost.oneconfig.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Adapted from <a href="https://github.com/natanfudge/Not-Enough-Crashes">NotEnoughCrashes</a> under the <a href="https://opensource.org/licenses/MIT">MIT License</a>
 */
public final class LogScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("OneConfig/Log Scanner");

    private LogScanner() {
    }

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

        StackTraceElement[] trace = e.getStackTrace();
        if (trace.length < 3) return Collections.emptySet();
        // ignore the first two elements, as they are this method and the method that was marked
        for (int i = 2; i < trace.length; i++) {
            StackTraceElement element = trace[i];
            // remove any that are native, or called from a system package
            String cls = element.getClassName();
            if (element.isNativeMethod()) continue;
            if (cls.startsWith("sun.") || cls.startsWith("com.sun.")) continue;
            if (cls.startsWith("java.") || cls.startsWith("javax.") || cls.startsWith("jdk.")) continue;
            target = element;
            break;
        }
        if (target == null) return Collections.emptySet();
        Set<LoaderPlatform.ActiveMod> classMods = identifyFromClass(target.getClassName());
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
            Set<LoaderPlatform.ActiveMod> classMods = identifyFromClass(className);
            mods.addAll(classMods);
        }
        return mods;
    }

    public static Set<LoaderPlatform.ActiveMod> identifyFromClass(String className) {
        try {
            // Skip identification for Mixin, one's mod copy of the library is shared with all other mods
            if (className.startsWith("org.spongepowered.asm.mixin.")) {
                return Collections.emptySet();
            }
            return identifyFromClass(Class.forName(className, false, LogScanner.class.getClassLoader()));
        } catch (Exception e) {
            return Collections.emptySet(); // we cannot do it
        }
    }

    // TODO: get a list of mixin transformers that affected the class and blame those too

    /**
     * Return a set of ActiveMods that have been associated with the given class.
     */
    @NotNull
    public static Set<LoaderPlatform.ActiveMod> identifyFromClass(Class<?> clazz) {
        List<LoaderPlatform.ActiveMod> modMap = Platform.getLoaderPlatform().getLoadedMods();
        modMap.removeIf(Objects::isNull);

        try {
            CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return Collections.emptySet(); // Some internal native sun classes
            }
            URL url = codeSource.getLocation();

            if (url == null) {
                LOGGER.warn("Failed to identify mod for {}", clazz.getName());
                return Collections.emptySet();
            }

            // Transform JAR URL to a file URL
            URI uri = url.toURI();
            if (uri.toString().startsWith("jar:")) {
                String s = uri.toString();
                uri = new URL(s.substring(4, s.lastIndexOf("!"))).toURI();
            }
            if (uri.toString().endsWith(".class") && Platform.getInstance().isDevelopmentEnvironment()) {
                LOGGER.error("The mod you are currently developing caused this issue, or another class file. Returning 'this'.");
                LOGGER.error("Class: {}", clazz.getName());
                return Collections.singleton(new LoaderPlatform.ActiveMod("this", "this", "Unknown", null));
            }
            return getModsAt(Paths.get(uri), modMap);
        } catch (URISyntaxException | MalformedURLException e) {
            return Collections.emptySet(); // we cannot do it
        }
    }

    @NotNull
    private static Set<LoaderPlatform.ActiveMod> getModsAt(Path path, List<LoaderPlatform.ActiveMod> modMap) {
        Set<LoaderPlatform.ActiveMod> mods = modMap.stream().filter(m -> m.source.equals(path)).collect(Collectors.toSet());
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
            return modMap.stream().filter(m -> m.source.equals(resourcesPath)).collect(Collectors.toSet());
        } else {
            return Collections.emptySet();
        }
    }
}
