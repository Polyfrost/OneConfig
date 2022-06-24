package cc.polyfrost.oneconfig.events.event;

/**
 * Called when the game is about to shut down.
 * This can be used if anything needs to be done before the screen itself is fully closed
 * or need to do something before another mod does something via {@link Runtime#addShutdownHook(Thread)}.
 *
 * @see ShutdownEvent
 */
public class PreShutdownEvent {
}
