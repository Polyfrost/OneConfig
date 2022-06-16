package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;

/**
 * Schedules a Runnable to be called after a certain amount of ticks.
 *
 * If the amount of ticks is below 1, the Runnable will be called immediately.
 */
public class TickDelay {
    private final Runnable function;
    private int delay;

    public TickDelay(Runnable functionName, int ticks) {
        if (ticks < 1) {
            functionName.run();
        } else {
            EventManager.INSTANCE.register(this);
            delay = ticks;
        }
        function = functionName;
    }

    @Subscribe
    protected void onTick(TickEvent event) {
        if (event.stage == Stage.START) {
            // Delay expired
            if (delay < 1) {
                function.run();
                EventManager.INSTANCE.unregister(this);
            }
            delay--;
        }
    }
}
