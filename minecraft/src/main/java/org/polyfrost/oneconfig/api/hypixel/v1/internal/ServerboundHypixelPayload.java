package org.polyfrost.oneconfig.api.hypixel.v1.internal;

import dev.deftu.omnicore.api.OmniResourceLocation;
import dev.deftu.omnicore.api.network.PacketPayload;
import dev.deftu.omnicore.api.network.codec.StreamCodec;
import net.hypixel.modapi.packet.HypixelPacket;
import net.hypixel.modapi.serializer.PacketSerializer;
//#if MC >= 1.16.5
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//#else
//$ import net.minecraft.network.PacketBuffer;
//$ import net.minecraft.util.ResourceLocation;
//#endif
import org.jetbrains.annotations.NotNull;

public class ServerboundHypixelPayload implements PacketPayload {
    private final ResourceLocation id;
    private final HypixelPacket packet;

    public ServerboundHypixelPayload(HypixelPacket packet) {
        this.id = OmniResourceLocation.createOrThrow(packet.getIdentifier());
        this.packet = packet;
    }

    //#if MC >= 1.16.5
    private void write(FriendlyByteBuf buf) {
        PacketSerializer serializer = new PacketSerializer(buf);
        this.packet.write(serializer);
    }
    //#else
    //$ private void write(PacketBuffer buf) {
    //$     PacketSerializer serializer = new PacketSerializer(buf);
    //$     this.packet.write(serializer);
    //$ }
    //#endif

    @Override
    public @NotNull ResourceLocation getId() {
        return this.id;
    }

    //#if MC >= 1.16.5
    public static StreamCodec<ServerboundHypixelPayload, FriendlyByteBuf> createCodec() {
        return StreamCodec.ofMember(ServerboundHypixelPayload::write, buf -> {
            throw new UnsupportedOperationException("Ccannot read ServerboundHypixelPayload");
        });
    }
    //#else
    //$ public static StreamCodec<ServerboundHypixelPayload, PacketBuffer> createCodec() {
    //$     return StreamCodec.ofMember(ServerboundHypixelPayload::write, buf -> {
    //$         throw new UnsupportedOperationException("Ccannot read ServerboundHypixelPayload");
    //$     });
    //$ }
    //#endif
}
