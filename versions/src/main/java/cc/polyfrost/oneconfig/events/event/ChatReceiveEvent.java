package cc.polyfrost.oneconfig.events.event;

import cc.polyfrost.oneconfig.libs.universal.wrappers.message.UTextComponent;
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

    public String getFullyUnformattedMessage() {
        return UTextComponent.Companion.stripFormatting(message.getUnformattedText());
    }
}
