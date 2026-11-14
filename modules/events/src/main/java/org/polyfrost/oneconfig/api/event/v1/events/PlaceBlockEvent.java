package org.polyfrost.oneconfig.api.event.v1.events;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public class PlaceBlockEvent implements Event {
    private final Object player;
    private final Object level;
    private final Object pos;
    private final Object state;

    public PlaceBlockEvent(Object player, Object level, Object pos, Object state) {
        this.player = player;
        this.level = level;
        this.pos = pos;
        this.state = state;
    }

    @SuppressWarnings("unchecked")
    public <T> T getPlayer() {
        return (T) player;
    }

    @SuppressWarnings("unchecked")
    public <T> T getLevel() {
        return (T) level;
    }

    @SuppressWarnings("unchecked")
    public <T> T getPos() {
        return (T) pos;
    }

    @SuppressWarnings("unchecked")
    public <T> T getState() {
        return (T) state;
    }
}
