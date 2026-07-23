package org.polyfrost.oneconfig.internal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        if (trySet(Paths.get("oneconfig", ".skiko").toAbsolutePath())
                || trySet(Paths.get(System.getProperty("java.io.tmpdir", "."), "oneconfig-skiko"))) {
            LOGGER.info("Redirected skiko data path to {}", System.getProperty("skiko.data.path"));
        } else {
            LOGGER.warn("Could not create a writable skiko data dir; leaving skiko default (~/.skiko)");
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
