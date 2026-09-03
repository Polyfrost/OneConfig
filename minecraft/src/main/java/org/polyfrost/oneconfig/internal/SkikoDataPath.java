package org.polyfrost.oneconfig.internal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.platform.v1.DesktopHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SkikoDataPath {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Skiko");
    private static boolean applied = false;

    private SkikoDataPath() {}

    public static synchronized void redirect() {
        if (applied) return;
        applied = true;
        if (System.getProperty("skiko.data.path") != null) return;

        Path gameDir = Paths.get("oneconfig", ".skiko").toAbsolutePath();
        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir", "."), "oneconfig-skiko");

        boolean android = isAndroid();
        boolean ok = android
                ? trySet(tmpDir) || trySet(gameDir)
                : trySet(gameDir) || trySet(tmpDir);

        if (ok) {
            LOGGER.info("Redirected skiko data path to {}", System.getProperty("skiko.data.path"));
        } else {
            LOGGER.warn("Could not create a writable skiko data dir; leaving skiko default (~/.skiko)");
        }

        if (android) preloadSharedStl();
    }

    private static boolean isAndroid() {
        try {
            return DesktopHelper.isAndroid();
        } catch (Throwable e) {
            return System.getProperty("os.version", "").startsWith("Android")
                    || Files.exists(Paths.get("/system/build.prop"));
        }
    }

    private static void preloadSharedStl() {
        try {
            System.loadLibrary("c++_shared");
            LOGGER.info("Preloaded libc++_shared.so for skiko");
        } catch (Throwable e) {
            LOGGER.warn("Could not preload libc++_shared.so ({}); skiko may fail to link", e.toString());
        }
    }

    private static boolean trySet(Path dir) {
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
