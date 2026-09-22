package org.polyfrost.oneconfig.api.hud.v1.events;

import org.polyfrost.oneconfig.api.event.v1.events.Event;

/**
 * Event fired when the HUD editor is opened or closed
 */
public class HudEditorToggleEvent implements Event {
    public static final HudEditorToggleEvent OPEN = new HudEditorToggleEvent(true, false);
    public static final HudEditorToggleEvent CLOSE = new HudEditorToggleEvent(false, false);
    public static final HudEditorToggleEvent SCREEN_REMOVED = new HudEditorToggleEvent(false, true);

    public final boolean open;

    /** Whether the editor screen is already being torn down, so nothing needs to close it */
    public final boolean screenAlreadyGone;

    private HudEditorToggleEvent(boolean open, boolean screenAlreadyGone) {
        this.open = open;
        this.screenAlreadyGone = screenAlreadyGone;
    }
}
