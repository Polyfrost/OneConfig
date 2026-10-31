package org.polyfrost.oneconfig.internal.ui.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.platform.v1.ModInfo;
import org.polyfrost.oneconfig.internal.ui.TestCompatibilityPlatform;

class LoadedModsRefreshTest {
    private static ModInfo mod(String id, String version) {
        return new ModInfo(id, id, version, null, null);
    }

    @AfterEach
    void clearMods() {
        TestCompatibilityPlatform.mods = Set.of();
    }

    @Test
    void refreshAddsAndUpdatesWithoutDroppingModsLeftTheLoader() {
        TestCompatibilityPlatform.mods = Set.of(mod("kept-off", "1"), mod("updated", "1"));
        ModInfo.refreshLoadedMods();

        TestCompatibilityPlatform.mods = Set.of(mod("updated", "2"), mod("added", "1"));
        ModInfo.refreshLoadedMods();

        Map<String, String> versions = ModInfo.getLoadedMods().stream().collect(Collectors.toMap(ModInfo::getId, ModInfo::getVersion));
        assertEquals("1", versions.get("kept-off"));
        assertEquals("2", versions.get("updated"));
        assertEquals("1", versions.get("added"));
    }
}
