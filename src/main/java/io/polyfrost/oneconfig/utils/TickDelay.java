package io.polyfrost.oneconfig.utils;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TickDelay {
    Integer delay;
    Runnable function;

    public TickDelay(Runnable functionName, int ticks) {
        register();
        delay = ticks;
        function = functionName;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            // Delay expired
            if (delay < 1) {
                run();
                destroy();
            }
            delay--;
        }
    }

    @EventHandler()
    private void destroy() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @EventHandler()
    private void register() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void run() {
        function.run();
    }
}
