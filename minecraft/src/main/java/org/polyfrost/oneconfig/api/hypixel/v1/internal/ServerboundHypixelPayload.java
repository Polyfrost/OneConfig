package org.polyfrost.oneconfig.api.hypixel.v1.internal;

import dev.deftu.omnicore.api.OmniResourceLocation;
import dev.deftu.omnicore.api.network.PacketPayload;
import dev.deftu.omnicore.api.network.codec.StreamCodec;
import net.hypixel.modapi.packet.HypixelPacket;
import net.hypixel.modapi.serializer.PacketSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class ServerboundHypixelPayload implements PacketPayload {
    private final Identifier id;
    private final HypixelPacket packet;

    public ServerboundHypixelPayload(HypixelPacket packet) {
        this.id = OmniResourceLocation.createOrThrow(packet.getIdentifier());
        this.packet = packet;
    }

    private void write(FriendlyByteBuf buf) {
        PacketSerializer serializer = new PacketSerializer(buf);
        this.packet.write(serializer);
    }

    @Override
    public @NotNull Identifier getId() {
        return this.id;
    }

    public static StreamCodec<ServerboundHypixelPayload, FriendlyByteBuf> createCodec() {
        return StreamCodec.ofMember(ServerboundHypixelPayload::write, buf -> {
            throw new UnsupportedOperationException("Ccannot read ServerboundHypixelPayload");
        });
    }
}
