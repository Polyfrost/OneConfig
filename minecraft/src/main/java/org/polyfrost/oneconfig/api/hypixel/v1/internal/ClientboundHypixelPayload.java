package org.polyfrost.oneconfig.api.hypixel.v1.internal;

import dev.deftu.omnicore.api.network.PacketPayload;
import dev.deftu.omnicore.api.network.codec.StreamCodec;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.error.ErrorReason;
import net.hypixel.modapi.packet.ClientboundHypixelPacket;
import net.hypixel.modapi.serializer.PacketSerializer;
//#if MC >= 1.16.5
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//#else
//$ import net.minecraft.network.PacketBuffer;
//$ import net.minecraft.util.ResourceLocation;
//#endif
import org.jetbrains.annotations.NotNull;

public class ClientboundHypixelPayload implements PacketPayload {
    private final ResourceLocation id;
    private final ClientboundHypixelPacket packet;
    private final ErrorReason errorReason;

    //#if MC >= 1.16.5
    private ClientboundHypixelPayload(ResourceLocation id, FriendlyByteBuf data) {
    //#else
    //$ private ClientboundHypixelPayload(ResourceLocation id, PacketBuffer data) {
    //#endif
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
    public @NotNull ResourceLocation getId() {
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

    //#if MC >= 1.16.5
    public static StreamCodec<ClientboundHypixelPayload, FriendlyByteBuf> createCodec(ResourceLocation id) {
        return StreamCodec.ofMember((buf, payload) -> {
            throw new UnsupportedOperationException("Cannot write ClientboundHypixelPayload");
        }, buf -> new ClientboundHypixelPayload(id, buf));
    }
    //#else
    //$ public static StreamCodec<ClientboundHypixelPayload, PacketBuffer> createCodec(ResourceLocation id) {
    //$     return StreamCodec.ofMember((buf, payload) -> {
    //$         throw new UnsupportedOperationException("Cannot write ClientboundHypixelPayload");
    //$     }, buf -> new ClientboundHypixelPayload(id, buf));
    //$ }
    //#endif
}
