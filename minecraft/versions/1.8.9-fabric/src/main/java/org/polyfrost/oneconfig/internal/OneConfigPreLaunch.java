package org.polyfrost.oneconfig.internal;
//#if FABRIC
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public class OneConfigPreLaunch implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/PreLaunch");

    @Override
    public void onPreLaunch() {
        //#if MC < 1.13
        // Duplicated code from OneConfigTweaker
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
        //#endif
    }

    /**
     * The unofficial fork of LWJGL2 by Minecraft Machina/ManyMC breaks HiDPI support on aarch64 macOS systems.
     * This method injects a patched version of the LWJGL2 native library, which fixes the issue.
     * @throws IOException
     */
    private void injectLWJGLFix() throws IOException {
        //#if MC < 1.13
        LOGGER.warn("Injecting LWJGL HiDPI fix, this is only needed for aarch64 macOS systems that don't have updated natives!");
        File tempDir = Files.createTempDirectory("oneconfig-patched-lwjgl2-natives").toFile();
        tempDir.deleteOnExit();
        Path tempFile = tempDir.toPath().resolve("liblwjgl.dylib");
        Path tempFile2 = tempDir.toPath().resolve("liblwjgl-macos-aarch64.dylib");
        try (InputStream is = OneConfigPreLaunch.class.getResourceAsStream("/patched-lwjgl/liblwjgl.dylib")) {
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        try (InputStream is = OneConfigPreLaunch.class.getResourceAsStream("/patched-lwjgl/liblwjgl.dylib")) {
            Files.copy(is, tempFile2, StandardCopyOption.REPLACE_EXISTING);
        }
        System.setProperty("org.lwjgl.librarypath", tempDir.getAbsolutePath());
        //#endif
    }
}
//#endif