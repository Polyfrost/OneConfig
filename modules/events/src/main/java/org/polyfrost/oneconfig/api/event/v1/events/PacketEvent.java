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
     * <br> It returns the {@code Packet} of the running Minecraft version
     */
    @SuppressWarnings("unchecked")
    public <T> T getPacket() {
        return (T) packet;
    }

    public Object component1() {
        return packet;
    }
}
