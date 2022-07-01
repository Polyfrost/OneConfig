package cc.polyfrost.oneconfig.events.event;


public class SendPacketEvent extends CancellableEvent {
    public final Object packet;

    public SendPacketEvent(Object packet) {
        this.packet = packet;
    }
}
