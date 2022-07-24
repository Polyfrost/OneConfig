package cc.polyfrost.oneconfig.events;

import cc.polyfrost.oneconfig.config.core.InvalidConfigException;
import cc.polyfrost.oneconfig.libs.eventbus.EventBus;
import cc.polyfrost.oneconfig.libs.eventbus.exception.ExceptionHandler;
import cc.polyfrost.oneconfig.libs.eventbus.invokers.LMFInvoker;
import org.jetbrains.annotations.NotNull;

/**
 * Manages all events from OneConfig.
 */
public final class EventManager {
    /**
     * The instance of the {@link EventManager}.
     */
    public static final EventManager INSTANCE = new EventManager();
    private final EventBus eventBus = new EventBus(new LMFInvoker(), new OneConfigExceptionHandler());

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


    /**
     * Bypass to allow special exceptions to actually crash
     */
    private static class OneConfigExceptionHandler implements ExceptionHandler {
        @Override
        public void handle(@NotNull Exception e) {
            if(e instanceof InvalidConfigException) {
                throw (InvalidConfigException) e;
            }
            else e.printStackTrace();
        }
    }
}
