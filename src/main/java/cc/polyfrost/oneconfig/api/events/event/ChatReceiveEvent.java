package cc.polyfrost.oneconfig.api.events.event;


import net.minecraft.util.IChatComponent;

/**
 * Called when a chat message is received.
 */
public class ChatReceiveEvent extends CancellableEvent {
    /**
     * The message that was received.
     */
    public final IChatComponent message;

    public ChatReceiveEvent(IChatComponent message) {
        this.message = message;
    }
}
