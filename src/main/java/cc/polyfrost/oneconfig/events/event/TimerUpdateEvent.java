package cc.polyfrost.oneconfig.events.event;

public class TimerUpdateEvent {

    public final boolean updatedDeltaTicks;

    public final Object timer;

    public TimerUpdateEvent(Object timer, boolean updatedDeltaTicks) {
        this.timer = timer;
        this.updatedDeltaTicks = updatedDeltaTicks;
    }
}