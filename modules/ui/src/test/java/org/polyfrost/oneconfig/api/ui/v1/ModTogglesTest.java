package org.polyfrost.oneconfig.api.ui.v1;

import androidx.compose.runtime.snapshots.MutableSnapshot;
import androidx.compose.runtime.snapshots.Snapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.toggle.v1.ModToggle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModTogglesTest {
    @Test
    void unknownModsHaveNoSwitchAndCountAsEnabled() {
        assertNull(ModToggles.toggleFor("mt-unknown"));
        assertTrue(ModToggles.isEnabled("mt-unknown"));
    }

    @Test
    void managedSwitchTracksItsState() {
        ModToggles.manage("mt-managed");
        ModToggle toggle = ModToggles.toggleFor("mt-managed.json");
        assertNotNull(toggle);
        assertTrue(toggle.isEnabled());

        toggle.setEnabled(false);
        assertFalse(ModToggles.isEnabled("mt-managed"));
        assertFalse(ModToggles.toggleFor("MT-Managed/config").isEnabled());

        toggle.setEnabled(true);
        assertTrue(ModToggles.isEnabled("mt-managed"));
    }

    @Test
    void registeredSwitchWinsOverTheManagedOne() {
        ModToggle own = new ModToggle() {
            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public void setEnabled(boolean enabled) {
            }
        };
        ModToggles.manage("mt-own");
        ModToggles.register("mt-own", own);

        assertSame(own, ModToggles.toggleFor("mt-own"));
    }

    @Test
    void listenersHearOnlyRealChanges() {
        List<String> events = new ArrayList<>();
        ModToggles.Listener listener = (id, enabled) -> events.add(id + "=" + enabled);
        ModToggles.addListener(listener);
        ModToggles.manage("mt-listened");

        ModToggles.setEnabled("mt-listened", true);
        ModToggles.setEnabled("mt-listened", false);
        ModToggles.setEnabled("mt-listened", false);
        ModToggles.removeListener(listener);
        ModToggles.setEnabled("mt-listened", true);

        assertEquals(Collections.singletonList("mt-listened=false"), events);
    }

    @Test
    void storeRestoresAndReceivesTheDisabledMods() {
        Set<String> saved = new HashSet<>();
        String stored = "mt-stored-" + UUID.randomUUID();
        String other = stored + "-other";
        ModToggles.manage(stored, other);
        ModToggles.setStore(new ModToggles.Store() {
            @Override
            public Set<String> load() {
                return Collections.singleton(stored);
            }

            @Override
            public void save(Set<String> disabled) {
                saved.clear();
                saved.addAll(disabled);
            }
        });

        assertFalse(ModToggles.isEnabled(stored));

        ModToggles.setEnabled(other, false);
        assertTrue(saved.contains(stored) && saved.contains(other));

        ModToggles.setEnabled(stored, true);
        assertFalse(saved.contains(stored));
    }

    private static final ModToggle TOGGLE = new ModToggle() {
        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void setEnabled(boolean enabled) {
        }
    };

    @Test
    void everyMutationInvalidatesRevisionReaders() {
        String managed = "mt-inv-managed-" + UUID.randomUUID();
        assertTrue(ModCardTypesTest.invalidates(ModToggles::getRevision, () -> ModToggles.register("mt-inv", TOGGLE)));
        assertTrue(ModCardTypesTest.invalidates(ModToggles::getRevision, () -> ModToggles.manage(managed)));
        assertTrue(ModCardTypesTest.invalidates(ModToggles::getRevision, () -> ModToggles.setEnabled(managed, false)));
        assertTrue(ModCardTypesTest.invalidates(ModToggles::getRevision, () -> ModToggles.setStore(new ModToggles.Store() {
            @Override
            public Set<String> load() {
                return Collections.emptySet();
            }

            @Override
            public void save(Set<String> disabled) {
            }
        })));
    }

    @Test
    void concurrentMutationsAreAllPublished() {
        int before = ModToggles.getRevision();
        assertTrue(ModCardTypesTest.invalidates(ModToggles::getRevision, () -> ModCardTypesTest.race(i -> ModToggles.register("mt-race-" + i, TOGGLE))));
        assertEquals(before + ModCardTypesTest.RACE_THREADS * ModCardTypesTest.RACE_ITERATIONS, ModToggles.getRevision());
        for (int i = 0; i < ModCardTypesTest.RACE_THREADS * ModCardTypesTest.RACE_ITERATIONS; i++) {
            assertSame(TOGGLE, ModToggles.toggleFor("mt-race-" + i));
        }
    }

    @Test
    void bumpsFromOverlappingCallerSnapshotsAreNotLost() {
        MutableSnapshot a = Snapshot.Companion.takeMutableSnapshot(null, null);
        MutableSnapshot b = Snapshot.Companion.takeMutableSnapshot(null, null);
        try {
            int before = ModToggles.getRevision();
            a.enter(() -> {
                ModToggles.register("mt-snap-a", TOGGLE);
                return null;
            });
            b.enter(() -> {
                ModToggles.register("mt-snap-b", TOGGLE);
                return null;
            });
            a.apply().check();
            b.apply().check();
            assertEquals(before + 2, ModToggles.getRevision());
        } finally {
            a.dispose();
            b.dispose();
        }
    }
}
