package org.polyfrost.oneconfig.internal.ui.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.platform.v1.ModInfo;

class HotwireInstalledTest {
    private static HotwireBridge.StoreHit hit(String slug, String title, boolean installed) {
        return new HotwireBridge.StoreHit("id", slug, title, "", "", 0, null, installed);
    }

    private static boolean installed(HotwireBridge.StoreHit hit, ModInfo... mods) {
        return HotwireBridge.INSTANCE.isInstalled(hit, List.of(mods));
    }

    @Test
    void hotwiresOwnAnswerCounts() {
        assertTrue(installed(hit("anything", "Anything", true)));
    }

    @Test
    void aSlugMatchingAModIdCounts() {
        assertTrue(installed(hit("fabric-api", "Fabric API", false), new ModInfo("fabric-api", "Fabric API", "1", null, null)));
    }

    @Test
    void aTitleMatchingAModNameCounts() {
        assertTrue(installed(hit("yacl", "yetanotherconfiglib", false), new ModInfo("yet_another_config_lib_v3", "YetAnotherConfigLib", "1", null, null)));
    }

    @Test
    void anUnrelatedProjectStaysListed() {
        assertFalse(installed(hit("sodium", "Sodium", false), new ModInfo("fabric-api", "Fabric API", "1", null, null)));
    }
}
