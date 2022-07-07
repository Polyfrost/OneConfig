package cc.polyfrost.oneconfig.network.packet;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Packet {
    protected transient UUID uuid;
    protected transient long receivedAt;

    @Nullable
    public UUID getUUID() {
        return this.uuid;
    }

    public long getReceivedAt() {
        return this.receivedAt;
    }

    public void setUUID(@Nullable UUID uuid) {
        this.uuid = uuid;
    }

    public void setReceivedAt(long receivedAt) {
        this.receivedAt = receivedAt;
    }
}
