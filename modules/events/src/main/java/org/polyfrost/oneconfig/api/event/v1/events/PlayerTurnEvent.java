package org.polyfrost.oneconfig.api.event.v1.events;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public class PlayerTurnEvent extends Event.Cancellable {
    private final double yawDelta;
    private final double pitchDelta;

    public PlayerTurnEvent(double yawDelta, double pitchDelta) {
        this.yawDelta = yawDelta;
        this.pitchDelta = pitchDelta;
    }

    public double getYawDelta() {
        return yawDelta;
    }

    public double getPitchDelta() {
        return pitchDelta;
    }

    public double component1() {
        return yawDelta;
    }

    public double component2() {
        return pitchDelta;
    }
}
