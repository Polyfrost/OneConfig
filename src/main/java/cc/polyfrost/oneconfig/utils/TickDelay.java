package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import me.kbrewster.eventbus.Subscribe;

/**
 * Schedules a Runnable to be called after a certain amount of ticks.
 */
public class TickDelay {
    private final Runnable function;
    private int delay;

    public TickDelay(Runnable functionName, int ticks) {
        EventManager.getEventManager().register(this);
        delay = ticks;
        function = functionName;
    }

    @Subscribe
    protected void onTick(TickEvent event) {
        if (event.stage == Stage.START) {
            // Delay expired
            if (delay < 1) {
                function.run();
                EventManager.getEventManager().unregister(this);
            }
            delay--;
        }
    }
}
