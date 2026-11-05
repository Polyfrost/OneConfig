package org.polyfrost.oneconfig.api.hud.v1;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HudExternalPlacementTest {
    static class PlainHud extends TextHud {
        PlainHud() { super("plain-hud", "Plain Hud", Hud.Category.getINFO(), "", ""); }
        @Override public String getText() { return "hi"; }
    }

    static class MovedHud extends PlainHud {
        @Override public ExternalPlacement getExternalPlacement() { return new ExternalPlacement("HUDMover", true, false); }
    }

    @Test
    void externalPlacementDefaultsToNull() {
        ConfigManager.active();
        assertNull(new PlainHud().getExternalPlacement());
    }

    @Test
    void externalPlacementCanBeOverridden() {
        ConfigManager.active();
        ExternalPlacement placement = new MovedHud().getExternalPlacement();
        assertEquals("HUDMover", placement.getMod());
        assertEquals(new ExternalPlacement("HUDMover", true, false), placement);
    }
}
