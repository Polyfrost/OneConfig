package cc.polyfrost.oneconfig.api.events.event;

import net.minecraft.network.Packet;

public class SendPacketEvent extends CancellableEvent {
    public final Packet<?> packet;

    public SendPacketEvent(Packet<?> packet) {
        this.packet = packet;
    }
}
