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

package org.polyfrost.oneconfig.internal;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.compose.render.PolyGlyphPage;
import org.polyfrost.oneconfig.api.event.v1.events.FramebufferRenderEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.internal.ui.compose.StbFonts;
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

public final class ClassPreloader {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/ClassPreloader");
    private static final String LIST = "/assets/oneconfig/open-classes.txt";
    private static final String GLYPHS = "/assets/oneconfig/open-glyphs.txt";
    private static final String RECORD = System.getProperty("oneconfig.debug.recordOpenClasses");
    private static final String RECORD_GLYPHS = System.getProperty("oneconfig.debug.recordOpenGlyphs");
    private static final boolean IN_WORLD = Boolean.getBoolean("oneconfig.debug.preloadInWorld");
    private static final String[] SKIP = {"java.", "javax.", "jdk.", "sun.", "com.sun.", "net.minecraft."};

    private static boolean recorded = false;

    private ClassPreloader() {
    }

    public static void init() {
        if (RECORD != null || RECORD_GLYPHS != null || Boolean.getBoolean("oneconfig.debug.noPreload")) return;
        EventHandler.ofRemoving(FramebufferRenderEvent.End.class, e -> {
            if (!(Platform.screen().current() instanceof TitleScreen) && !(IN_WORLD && Minecraft.getInstance().level != null)) return false;
            Runnable warm = prepareWarmUp();
            Thread thread = new Thread(() -> {
                preload();
                if (warm != null) warm.run();
            }, "OneConfig Class Preloader");
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
            return true;
        }).register();
    }

    private static Runnable prepareWarmUp() {
        try {
            return OneConfigUIScreen.prepareWarmUp();
        } catch (Throwable t) {
            LOGGER.warn("Failed to prepare the OneConfig UI warm-up", t);
            return null;
        }
    }

    private static void preload() {
        long start = System.nanoTime();
        ClassLoader loader = ClassPreloader.class.getClassLoader();
        int loaded = 0;
        int missing = 0;
        try (InputStream in = ClassPreloader.class.getResourceAsStream(LIST)) {
            if (in == null) return;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String name;
            while ((name = reader.readLine()) != null) {
                if (name.isEmpty()) continue;
                try {
                    Class.forName(name, false, loader).getDeclaredMethods();
                    loaded++;
                } catch (Exception | LinkageError ignored) {
                    missing++;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read the class preload list", e);
        }
        LOGGER.info("Preloaded {} classes ({} missing) in {}ms", loaded, missing, (System.nanoTime() - start) / 1_000_000L);
        prerasterizeGlyphs();
    }

    private static void prerasterizeGlyphs() {
        long start = System.nanoTime();
        int count = 0;
        try (InputStream in = ClassPreloader.class.getResourceAsStream(GLYPHS)) {
            if (in == null) return;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty() && StbFonts.INSTANCE.prerasterize(line)) count++;
            }
        } catch (Exception | LinkageError e) {
            LOGGER.warn("Failed to prerasterize the menu glyphs", e);
        }
        LOGGER.info("Prerasterized {} glyphs in {}ms", count, (System.nanoTime() - start) / 1_000_000L);
    }

    public static void recordFirstOpen() {
        if (recorded) return;
        recorded = true;
        if (RECORD_GLYPHS != null) recordGlyphs();
        if (RECORD == null) return;
        Recording recording = new Recording();
        recording.enable("jdk.ClassLoad").withoutThreshold().withoutStackTrace();
        recording.start();
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(8000L);
                recording.stop();
                Path dump = Files.createTempFile("oneconfig-open", ".jfr");
                recording.dump(dump);
                recording.close();
                Set<String> names = new LinkedHashSet<>();
                for (RecordedEvent event : RecordingFile.readAllEvents(dump)) {
                    if (!event.getEventType().getName().equals("jdk.ClassLoad")) continue;
                    String name = event.getClass("loadedClass").getName();
                    if (!skipped(name)) names.add(name);
                }
                Files.delete(dump);
                Path out = Paths.get(RECORD).toAbsolutePath();
                Files.write(out, names, StandardCharsets.UTF_8);
                LOGGER.info("Recorded {} classes loaded by the first menu open to {}", names.size(), out);
            } catch (Exception e) {
                LOGGER.error("Failed to record the classes loaded by the first menu open", e);
            }
        }, "OneConfig Class Recorder");
        thread.setDaemon(true);
        thread.start();
    }

    private static void recordGlyphs() {
        Set<String> glyphs = StbFonts.INSTANCE.recordGlyphs();
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(8000L);
                PolyGlyphPage.setRecorder(null);
                Path out = Paths.get(RECORD_GLYPHS).toAbsolutePath();
                Files.write(out, new TreeSet<>(glyphs), StandardCharsets.UTF_8);
                LOGGER.info("Recorded {} glyphs rasterized by the first menu open to {}", glyphs.size(), out);
            } catch (Exception e) {
                LOGGER.error("Failed to record the glyphs rasterized by the first menu open", e);
            }
        }, "OneConfig Glyph Recorder");
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean skipped(String name) {
        if (name.indexOf('/') >= 0 || name.contains("$$Lambda") || name.contains(".0x") || name.contains("_Test")) return true;
        for (String prefix : SKIP) if (name.startsWith(prefix)) return true;
        return false;
    }
}
