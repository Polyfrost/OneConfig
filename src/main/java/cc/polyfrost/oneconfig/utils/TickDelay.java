package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;

public class TickDelay {
    private int delay;
    private final Runnable function;

    public TickDelay(Runnable functionName, int ticks) {
        EventManager.INSTANCE.getEventBus().register(this);
        delay = ticks;
        function = functionName;
    }

    @Subscribe
    public void onTick(TickEvent event) {
        if (event.stage == Stage.START) {
            // Delay expired
            if (delay < 1) {
                function.run();
                EventManager.INSTANCE.getEventBus().unregister(this);
            }
            delay--;
        }
    }
}
