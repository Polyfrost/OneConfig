package org.polyfrost.oneconfig.internal.ui.api;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.ui.v1.ModToggles;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModToggleStoreTest {
    @Test
    void aModKeptOnForThisRunStaysOffOnDisk() throws Exception {
        String profile = ConfigManager.activeProfile();
        Path file = ConfigManager.profileDir(profile).resolve("disabled-mods");
        Files.createDirectories(file.getParent());
        Files.write(file, "storekept\nstoreother".getBytes(StandardCharsets.UTF_8));

        Set<String> loaded = ModToggleStore.INSTANCE.load();
        ModToggles.replaceDisabled(live -> {
            Set<String> next = new HashSet<>(live);
            next.addAll(loaded);
            return next;
        });
        assertFalse(ModToggles.isEnabled("storekept"));

        ModToggleStore.keepEnabled("storekept");
        assertTrue(ModToggles.isEnabled("storekept"));
        assertFalse(ModToggles.isEnabled("storeother"));

        ModToggleStore.INSTANCE.save(Set.of("storeother", "storethird"));
        ModToggleStore.INSTANCE.onProfileSaving(profile);

        Set<String> onDisk = new HashSet<>(Files.readAllLines(file, StandardCharsets.UTF_8));
        assertEquals(Set.of("storekept", "storeother", "storethird"), onDisk);
    }
}
