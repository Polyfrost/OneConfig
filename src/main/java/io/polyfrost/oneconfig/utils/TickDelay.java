package io.polyfrost.oneconfig.utils;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TickDelay {
    private int delay;
    private final Runnable function;

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

    private void destroy() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    private void register() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void run() {
        function.run();
    }
}
