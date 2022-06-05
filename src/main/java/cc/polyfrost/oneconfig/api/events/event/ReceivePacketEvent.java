package cc.polyfrost.oneconfig.api.events.event;

import net.minecraft.network.Packet;

public class ReceivePacketEvent extends CancellableEvent {
    public final Packet<?> packet;

    public ReceivePacketEvent(Packet<?> packet) {
        this.packet = packet;
    }
}
