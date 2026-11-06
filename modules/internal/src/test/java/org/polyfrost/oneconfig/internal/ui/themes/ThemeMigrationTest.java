package org.polyfrost.oneconfig.internal.ui.themes;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.internal.ThemeConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeMigrationTest {
    @Test
    void themeLoadBeforeConfigInitKeepsRename() throws Exception {
        Path file = Path.of("config", "themes.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\"activeTheme\":\"PolyGlass Light\"}");
        try {
            ThemeConfig config = new ThemeConfig();
            ThemeRegistry.INSTANCE.loadFromConfig();
            config.preload();
            ThemeRegistry.INSTANCE.loadFromConfig();
            assertEquals("Polyfrosted Light", ThemeConfig.activeTheme);
            assertTrue(ThemeConfig.polyfrostedRenameApplied);
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
