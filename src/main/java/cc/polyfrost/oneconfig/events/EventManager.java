package cc.polyfrost.oneconfig.events;

import cc.polyfrost.oneconfig.libs.eventbus.EventBus;
import cc.polyfrost.oneconfig.libs.eventbus.invokers.LMFInvoker;

public final class EventManager {
    private EventManager() {

    }

    public static final EventManager INSTANCE = new EventManager();
    private final EventBus eventBus = new EventBus(new LMFInvoker(), Throwable::printStackTrace);

    public EventBus getEventBus() {
        return eventBus;
    }

    public void register(Object object) {
        eventBus.register(object);
    }

    public void unregister(Object object) {
        eventBus.unregister(object);
    }

    public void post(Object event) {
        eventBus.post(event);
    }
}
