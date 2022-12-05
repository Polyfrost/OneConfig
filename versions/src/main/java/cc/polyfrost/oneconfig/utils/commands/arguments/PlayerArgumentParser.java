package cc.polyfrost.oneconfig.utils.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The player argument parser. Returns a {@link GameProfile}.
 */
public class PlayerArgumentParser extends ArgumentParser<GameProfile> {
    @Nullable
    @Override
    public GameProfile parse(@NotNull String arg) throws Exception {
        return getMatchingPlayers(arg).stream().findFirst().orElseThrow(() -> new Exception("No player found"));
    }

    public static List<GameProfile> getMatchingPlayers(String arg) {
        if (Minecraft.getMinecraft().theWorld == null) return Lists.newArrayList();
        return Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap().stream().map(NetworkPlayerInfo::getGameProfile).filter(gameProfile -> StringUtils.startsWithAny(gameProfile.getName().toLowerCase(), "!", arg.toLowerCase())).collect(Collectors.toList());
    }

    @NotNull
    @Override
    public List<String> complete(String current, Parameter parameter) {
        return getMatchingPlayers(current).stream().map(GameProfile::getName).collect(Collectors.toList());
    }
}
