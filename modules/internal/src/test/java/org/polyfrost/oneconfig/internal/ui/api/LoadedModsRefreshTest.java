package org.polyfrost.oneconfig.internal.ui.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.Tree;
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
    void modsThatLeftTheLoaderAreSeenButNotLoaded() {
        TestCompatibilityPlatform.mods = Set.of(mod("kept-off", "1"), mod("updated", "1"));
        ModInfo.refreshLoadedMods();

        TestCompatibilityPlatform.mods = Set.of(mod("updated", "2"), mod("added", "1"));
        ModInfo.refreshLoadedMods();

        Map<String, String> loaded = ModInfo.getLoadedMods().stream().collect(Collectors.toMap(ModInfo::getId, ModInfo::getVersion));
        assertFalse(loaded.containsKey("kept-off"));
        assertEquals("2", loaded.get("updated"));
        assertEquals("1", loaded.get("added"));

        Map<String, String> seen = ModInfo.getSeenMods().stream().collect(Collectors.toMap(ModInfo::getId, ModInfo::getVersion));
        assertEquals("1", seen.get("kept-off"));
        assertEquals("2", seen.get("updated"));
        assertEquals("1", seen.get("added"));
    }

    @Test
    void aSlowRefreshCannotStoreAnOlderList() throws InterruptedException {
        TestCompatibilityPlatform.mods = Set.of(mod("raced", "1"));
        ModInfo.refreshLoadedMods();

        TestCompatibilityPlatform.mods = Set.of(mod("raced", "2"));
        CountDownLatch reading = new CountDownLatch(1);
        TestCompatibilityPlatform.afterRead = () -> {
            reading.countDown();
            try {
                new CountDownLatch(1).await(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        };
        Thread slow = new Thread(ModInfo::refreshLoadedMods);
        slow.start();
        assertTrue(reading.await(5, TimeUnit.SECONDS));

        TestCompatibilityPlatform.afterRead = () -> {
        };
        TestCompatibilityPlatform.mods = Set.of(mod("raced", "3"));
        Thread fast = new Thread(ModInfo::refreshLoadedMods);
        fast.start();
        slow.join();
        fast.join();

        assertEquals("3", ModInfo.getLoadedMods().iterator().next().getVersion());
    }

    @Test
    void configDataFollowsAnUpdateAndKeepsItsLookupOtherwise() {
        TestCompatibilityPlatform.mods = Set.of(mod("card", "1"));
        ModInfo.refreshLoadedMods();
        TreeConfigData data = new TreeConfigData(Tree.tree("card"), ConfigSource.OC, null);
        assertEquals("1", data.getVersion());

        Set<ModInfo> before = ModInfo.getLoadedMods();
        TestCompatibilityPlatform.mods = Set.of(mod("card", "1"));
        ModInfo.refreshLoadedMods();
        assertSame(before, ModInfo.getLoadedMods());

        TestCompatibilityPlatform.mods = Set.of(mod("card", "2"));
        ModInfo.refreshLoadedMods();
        assertEquals("2", data.getVersion());
    }
}
