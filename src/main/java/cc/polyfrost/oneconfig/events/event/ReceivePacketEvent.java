package cc.polyfrost.oneconfig.events.event;

public class ReceivePacketEvent extends CancellableEvent {
    public final Object packet;

    public ReceivePacketEvent(Object packet) {
        this.packet = packet;
    }
}
