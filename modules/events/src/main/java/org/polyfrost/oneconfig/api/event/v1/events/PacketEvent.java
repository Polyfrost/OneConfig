package org.polyfrost.oneconfig.api.event.v1.events;

public abstract class PacketEvent extends Event.Cancellable {
    private final Object packet;

    public PacketEvent(Object packet) {
        this.packet = packet;
    }

    public static final class Send extends PacketEvent {
        public Send(Object packet) {
            super(packet);
        }
    }

    public static final class Receive extends PacketEvent {
        public Receive(Object packet) {
            super(packet);
        }
    }

    /**
     * This is a Duck method because Minecraft versions differ
     * <br> It returns the expected type for that minecraft version
     * <ul>
     *     <li>legacy forge gives a IPacket</li>
     *     <li>modern forge gives a Packet</li>
     *     <li>fabric gives a Packet</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    public <T> T getPacket() {
        return (T) packet;
    }

    public Object component1() {
        return packet;
    }
}
