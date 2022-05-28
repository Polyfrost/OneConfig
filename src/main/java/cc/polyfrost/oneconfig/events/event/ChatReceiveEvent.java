package cc.polyfrost.oneconfig.events.event;


import net.minecraft.util.IChatComponent;

public class ChatReceiveEvent extends CancellableEvent {
    public final IChatComponent message;

    public ChatReceiveEvent(IChatComponent message) {
        this.message = message;
    }
}
