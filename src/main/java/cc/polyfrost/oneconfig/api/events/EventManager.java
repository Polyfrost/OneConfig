package cc.polyfrost.oneconfig.api.events;

import me.kbrewster.eventbus.EventBus;
import me.kbrewster.eventbus.invokers.LMFInvoker;

/**
 * Manages all events from OneConfig.
 */
public final class EventManager {
    /**
     * The instance of the {@link EventManager}.
     */
    public static final EventManager INSTANCE = new EventManager();
    private final EventBus eventBus = new EventBus(new LMFInvoker(), Throwable::printStackTrace);

    private EventManager() {

    }

    /**
     * Returns the {@link EventBus} instance.
     *
     * @return The {@link EventBus} instance.
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Registers an object to the {@link EventBus}.
     *
     * @param object The object to register.
     * @see EventBus#register(Object)
     */
    public void register(Object object) {
        eventBus.register(object);
    }

    /**
     * Unregisters an object from the {@link EventBus}.
     *
     * @param object The object to unregister.
     * @see EventBus#unregister(Object)
     */
    public void unregister(Object object) {
        eventBus.unregister(object);
    }

    /**
     * Posts an event to the {@link EventBus}.
     *
     * @param event The event to post.
     * @see EventBus#post(Object)
     */
    public void post(Object event) {
        eventBus.post(event);
    }
}
