package org.polyfrost.oneconfig.api.ui.v1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.toggle.v1.ModToggle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerRestrictionsTest {
    @AfterEach
    void lift() {
        ServerRestrictions.clear();
    }

    @Test
    void idsAreComparedWithoutCaseOrSuffix() {
        ServerRestrictions.set(Collections.singletonList(" SR-Mod "), Collections.singletonList("sr-hud.json"));

        assertTrue(ServerRestrictions.isModDisabled("sr-mod"));
        assertTrue(ServerRestrictions.isModDisabled("SR-MOD.json"));
        assertTrue(ServerRestrictions.isHudDisabled("SR-Hud"));
        assertFalse(ServerRestrictions.isModDisabled("sr-hud"));
        assertFalse(ServerRestrictions.isHudDisabled("sr-mod"));
    }

    @Test
    void restrictionOverridesTheManagedSwitchWithoutChangingIt() {
        ModToggles.manage("sr-managed");
        ServerRestrictions.set(Collections.singletonList("sr-managed"), Collections.emptyList());
        assertFalse(ModToggles.isEnabled("sr-managed"));

        ServerRestrictions.clear();
        assertTrue(ModToggles.isEnabled("sr-managed"));
    }

    @Test
    void restrictionLeavesASwitchTheUserTurnedOffOff() {
        ModToggles.manage("sr-off");
        ModToggles.setEnabled("sr-off", false);
        ServerRestrictions.set(Collections.singletonList("sr-off"), Collections.emptyList());
        ServerRestrictions.clear();

        assertFalse(ModToggles.isEnabled("sr-off"));
        ModToggles.setEnabled("sr-off", true);
    }

    @Test
    void managedListenersHearTheRestrictionComeAndGo() {
        List<String> events = new ArrayList<>();
        ModToggles.Listener listener = (id, enabled) -> events.add(id + "=" + enabled);
        ModToggles.manage("sr-listened");
        ModToggles.addListener(listener);

        ServerRestrictions.set(Collections.singletonList("sr-listened"), Collections.emptyList());
        ServerRestrictions.set(Collections.singletonList("sr-listened"), Collections.emptyList());
        ServerRestrictions.clear();
        ModToggles.removeListener(listener);

        assertEquals(Arrays.asList("sr-listened=false", "sr-listened=true"), events);
    }

    @Test
    void guardLocksOnlyRestrictedSwitches() {
        boolean[] state = {true};
        ModToggle own = new ModToggle() {
            @Override
            public boolean isEnabled() {
                return state[0];
            }

            @Override
            public void setEnabled(boolean enabled) {
                state[0] = enabled;
            }
        };
        assertSame(own, ServerRestrictions.guard("sr-guarded", own));

        ServerRestrictions.set(Collections.singletonList("sr-guarded"), Collections.singletonList("sr-guarded-hud"));
        ModToggle locked = ServerRestrictions.guard("sr-guarded", own);
        assertNotSame(own, locked);
        assertFalse(locked.isEnabled());
        assertTrue(state[0]);
        assertFalse(ServerRestrictions.guardHud("sr-guarded-hud", own).isEnabled());
        assertSame(own, ServerRestrictions.guardHud("sr-other-hud", own));
    }
}
