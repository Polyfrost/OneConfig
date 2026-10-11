package org.polyfrost.oneconfig.api.ui.v1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

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
        ModToggles.manage("mt-stored", "mt-stored-other");
        ModToggles.setStore(new ModToggles.Store() {
            @Override
            public Set<String> load() {
                return Collections.singleton("mt-stored");
            }

            @Override
            public void save(Set<String> disabled) {
                saved.clear();
                saved.addAll(disabled);
            }
        });

        assertFalse(ModToggles.isEnabled("mt-stored"));

        ModToggles.setEnabled("mt-stored-other", false);
        assertTrue(saved.contains("mt-stored") && saved.contains("mt-stored-other"));

        ModToggles.setEnabled("mt-stored", true);
        assertFalse(saved.contains("mt-stored"));
    }
}
