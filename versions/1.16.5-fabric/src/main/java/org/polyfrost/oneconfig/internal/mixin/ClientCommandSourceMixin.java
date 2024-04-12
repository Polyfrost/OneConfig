package org.polyfrost.oneconfig.internal.mixin;

import net.minecraft.text.LiteralText;
import org.polyfrost.oneconfig.internal.libs.fabric.ClientCommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Fabric Client-side command manager implementation.
 * <br>
 * Taken from the Fabric API under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a>;
 * <a href="https://github.com/FabricMC/fabric/blob/1.20.2/fabric-command-api-v2/src/client/java/net/fabricmc/fabric/impl/command/client/ClientCommandInternals.java">Click here for source</a>
 */
@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(net.minecraft.client.network.ClientCommandSource.class)
abstract class ClientCommandSourceMixin implements ClientCommandSource {
    @Shadow
    @Final
    private MinecraftClient client;

    @Override
    public void sendFeedback(Text message) {
        this.client.inGameHud.getChatHud().addMessage(message);
//        this.client.getNarratorManager().narrate(message);
    }

    @Override
    public void sendError(Text message) {
        sendFeedback(new LiteralText("").append(message).formatted(Formatting.RED));
    }

    @Override
    public MinecraftClient getClient() {
        return client;
    }

    @Override
    public ClientPlayerEntity getPlayer() {
        return client.player;
    }

    @Override
    public ClientWorld getWorld() {
        return client.world;
    }
}