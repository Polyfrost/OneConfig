package org.polyfrost.oneconfig.api.hypixel.v1.internal;

import dev.deftu.omnicore.api.OmniIdentifier;
import dev.deftu.omnicore.api.network.PacketPayload;
import dev.deftu.omnicore.api.network.codec.StreamCodec;
import net.hypixel.modapi.packet.HypixelPacket;
import net.hypixel.modapi.serializer.PacketSerializer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ServerboundHypixelPayload implements PacketPayload {
    private final ResourceLocation id;
    private final HypixelPacket packet;

    public ServerboundHypixelPayload(HypixelPacket packet) {
        this.id = OmniIdentifier.createOrThrow(packet.getIdentifier());
        this.packet = packet;
    }

    private void write(PacketBuffer buf) {
        PacketSerializer serializer = new PacketSerializer(buf);
        this.packet.write(serializer);
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return this.id;
    }

    public static StreamCodec<ServerboundHypixelPayload, PacketBuffer> createCodec() {
        return StreamCodec.ofMember(ServerboundHypixelPayload::write, buf -> {
            throw new UnsupportedOperationException("Ccannot read ServerboundHypixelPayload");
        });
    }
}
