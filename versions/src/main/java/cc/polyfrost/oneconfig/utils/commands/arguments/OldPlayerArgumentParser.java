//#if MC<=11202
package cc.polyfrost.oneconfig.utils.commands.arguments;

import cc.polyfrost.oneconfig.internal.utils.Deprecator;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.stream.Collectors;

import static cc.polyfrost.oneconfig.utils.commands.arguments.PlayerArgumentParser.getUUID;

/**
 * The old player argument parser. Returns an {@link EntityPlayer}.
 * @deprecated Use {@link PlayerArgumentParser} instead.
 */
@Deprecated
public class OldPlayerArgumentParser extends ArgumentParser<EntityPlayer> {
    @Nullable
    @Override
    public EntityPlayer parse(@NotNull String arg) {
        Deprecator.markDeprecated();
        List<EntityPlayer> matchingPlayers = getMatchingPlayers(arg);
        for (EntityPlayer profile : matchingPlayers) {
            return profile;
        }
        return new EntityPlayer(Minecraft.getMinecraft().theWorld, new GameProfile(getUUID(arg), arg)) {
            @Override
            public boolean isSpectator() {
                return false;
            }
        };
    }
    // This only returns players in tab list that match, not all players in the current server, hence why this is deprecated.
    public static List<EntityPlayer> getMatchingPlayers(String arg) {
        if (Minecraft.getMinecraft().theWorld == null) return Lists.newArrayList();
        return Minecraft.getMinecraft().theWorld.getPlayers(EntityPlayer.class,
                player -> player.getName().equals(arg) || player.getUniqueID().toString().equals(arg));
    }

    @NotNull
    @Override
    public List<String> complete(String current, Parameter parameter) {
        return getMatchingPlayers(current).stream().map(EntityPlayer::getName).collect(Collectors.toList());
    }
}
//#endif
