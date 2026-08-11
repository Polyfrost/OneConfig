package org.polyfrost.oneconfig.api.hud.v1.events;

import org.polyfrost.oneconfig.api.event.v1.events.Event;

/**
 * Event fired when the HUD editor is opened or closed
 */
public class HudEditorToggleEvent implements Event {
    public static final HudEditorToggleEvent OPEN = new HudEditorToggleEvent(true);
    public static final HudEditorToggleEvent CLOSE = new HudEditorToggleEvent(false);
    public final boolean open;

    private HudEditorToggleEvent(boolean open) {
        this.open = open;
    }
}
