package org.polyfrost.oneconfig.api.event.v1.events;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public class AttackEntityEvent extends Event.Cancellable {
    private final Object player;
    private final Object target;

    public AttackEntityEvent(Object player, Object target) {
        this.player = player;
        this.target = target;
    }

    @SuppressWarnings("unchecked")
    public <T> T getPlayer() {
        return (T) player;
    }

    @SuppressWarnings("unchecked")
    public <T> T getTarget() {
        return (T) target;
    }
}
