package org.polyfrost.oneconfig.internal.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class MacExclusiveFullscreenReset {
    private static final boolean MAC = System.getProperty("os.name", "").startsWith("Mac");
    private static final String PREFIX = "exclusiveFullscreen:";

    private MacExclusiveFullscreenReset() {}

    public static boolean pending(File optionsFile) {
        return MAC && optionsFile != null && !Files.exists(marker(optionsFile));
    }

    public static void reset(File optionsFile) {
        try {
            if (Files.exists(optionsFile.toPath())) {
                Path path = optionsFile.toPath().toRealPath();
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                boolean changed = false;
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.startsWith(PREFIX) && "true".equals(line.substring(PREFIX.length()).trim())) {
                        lines.set(i, PREFIX + "false");
                        changed = true;
                    }
                }
                if (changed) {
                    Path temp = path.resolveSibling(path.getFileName() + ".ocfg-tmp");
                    Files.write(temp, lines, StandardCharsets.UTF_8);
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            Path marker = marker(optionsFile);
            Files.createDirectories(marker.getParent());
            if (!Files.exists(marker)) Files.createFile(marker);
        } catch (IOException ignored) {
        }
    }

    private static Path marker(File optionsFile) {
        return optionsFile.toPath().toAbsolutePath().resolveSibling("oneconfig").resolve("macos-exclusive-fullscreen-reset");
    }
}
