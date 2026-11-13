package org.polyfrost.oneconfig.api.event.v1.events;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public class BlockUpdateEvent implements Event {
    private final Object pos;
    private final Object state;

    public BlockUpdateEvent(Object pos, Object state) {
        this.pos = pos;
        this.state = state;
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
