package org.polyfrost.oneconfig.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SkikoDataPath {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Skiko");
    private static final boolean WINDOWS = System.getProperty("os.name", "").startsWith("Windows");
    private static final int MAX_WINDOWS_PATH = 259;
    private static final String WINDOWS_ARCH = System.getProperty("os.arch", "").matches("aarch64|arm64") ? "arm64" : "x64";
    private static final int ICU_DATA_SUFFIX = ("\\skiko-windows-" + WINDOWS_ARCH + "-").length() + 64 + "\\icudtl.dat".length();
    private static boolean applied = false;

    private SkikoDataPath() {}

    public static synchronized void redirect() {
        if (applied) return;
        applied = true;
        if (System.getProperty("skiko.data.path") != null) return;
        if (trySet(Paths.get("oneconfig", ".skiko").toAbsolutePath())
                || trySet(Paths.get(System.getProperty("java.io.tmpdir", "."), "oneconfig-skiko"))) {
            LOGGER.info("Redirected skiko data path to {}", System.getProperty("skiko.data.path"));
        } else {
            LOGGER.warn("Could not create a writable skiko data dir; leaving skiko default (~/.skiko)");
        }
    }

    private static boolean trySet(Path dir) {
        if (WINDOWS && dir.toString().length() + ICU_DATA_SUFFIX > MAX_WINDOWS_PATH) {
            LOGGER.warn("skiko data dir candidate {} is too long for Skia to load ICU data from", dir);
            return false;
        }
        try {
            Files.createDirectories(dir);
            if (!Files.isWritable(dir)) return false;
            System.setProperty("skiko.data.path", dir.toString());
            return true;
        } catch (Throwable e) {
            LOGGER.warn("skiko data dir candidate {} unusable: {}", dir, e.toString());
            return false;
        }
    }
}
