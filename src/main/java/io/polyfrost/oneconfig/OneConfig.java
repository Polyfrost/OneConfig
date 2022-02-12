package io.polyfrost.oneconfig;

import io.polyfrost.oneconfig.command.OneConfigCommand;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = "oneconfig", name = "OneConfig", version = "${version}")
public class OneConfig {

    @Mod.EventHandler
    public void onFMLInitialization(FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(new OneConfigCommand());
        MinecraftForge.EVENT_BUS.register(this);
    }
}
