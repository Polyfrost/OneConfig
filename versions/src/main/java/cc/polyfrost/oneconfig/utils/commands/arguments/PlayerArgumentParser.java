//#if MC<=11202
package cc.polyfrost.oneconfig.utils.commands.arguments;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerArgumentParser extends ArgumentParser<EntityPlayer> {
    @Nullable
    @Override
    public EntityPlayer parse(@NotNull String arg) throws Exception {
        return getMatchingPlayers(arg).stream().findFirst().orElseThrow(() -> new Exception("No player found"));
    }

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
