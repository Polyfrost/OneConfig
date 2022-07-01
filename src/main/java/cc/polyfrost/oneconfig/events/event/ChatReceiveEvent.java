package cc.polyfrost.oneconfig.events.event;

/**
 * Called when a chat message is received.
 */
public class ChatReceiveEvent extends CancellableEvent {
    /**
     * The message that was received.
     */
    public final Object message;

    public ChatReceiveEvent(Object message) {
        this.message = message;
    }

    public String getFullyUnformattedMessage() {
        return "";
    }
}
