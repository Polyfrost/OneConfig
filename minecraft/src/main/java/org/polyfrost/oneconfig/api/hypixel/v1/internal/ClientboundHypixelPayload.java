/*
package org.polyfrost.oneconfig.api.hypixel.v1.internal;

import dev.deftu.omnicore.api.network.PacketPayload;
import dev.deftu.omnicore.api.network.codec.StreamCodec;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.error.ErrorReason;
import net.hypixel.modapi.packet.ClientboundHypixelPacket;
import net.hypixel.modapi.serializer.PacketSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class ClientboundHypixelPayload implements CustomPacketPayload {
    private final Identifier id;
    private final ClientboundHypixelPacket packet;
    private final ErrorReason errorReason;
    private final Type<ClientboundHypixelPayload> TYPE = new Type<>(id);

    private ClientboundHypixelPayload(Identifier id, FriendlyByteBuf data) {
        this.id = id;

        PacketSerializer serializer = new PacketSerializer(data);
        boolean isSuccessful = serializer.readBoolean();
        if (!isSuccessful) {
            this.packet = null;
            this.errorReason = ErrorReason.getById(serializer.readVarInt());
            return;
        }

        this.packet = HypixelModAPI.getInstance().getRegistry().createClientboundPacket(id.toString(), serializer);
        this.errorReason = null;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return null;
    }

    @Override
    public @NotNull Identifier getId() {
        return this.id;
    }

    public boolean isSuccessful() {
        return this.packet != null;
    }

    public ClientboundHypixelPacket getPacket() {
        return packet;
    }

    public ErrorReason getErrorReason() {
        return errorReason;
    }

    public static StreamCodec<ClientboundHypixelPayload, FriendlyByteBuf> createCodec(Identifier id) {
        return StreamCodec.ofMember((buf, payload) -> {
            throw new UnsupportedOperationException("Cannot write ClientboundHypixelPayload");
        }, buf -> new ClientboundHypixelPayload(id, buf));
    }
}
*/
