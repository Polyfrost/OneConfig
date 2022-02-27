package io.polyfrost.oneconfig.hud;

import io.polyfrost.oneconfig.hud.interfaces.BasicHud;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;

public class HudCore {
    public static ArrayList<BasicHud> huds = new ArrayList<>();

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        for (BasicHud hud : huds)
            hud.drawAll(20,20, 5);
    }
}
