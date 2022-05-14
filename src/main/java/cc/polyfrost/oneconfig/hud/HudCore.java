package cc.polyfrost.oneconfig.hud;

import gg.essential.universal.UResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;

public class HudCore {
    public static ArrayList<BasicHud> huds = new ArrayList<>();
    public static boolean editing = false;

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || editing) return;
        for (BasicHud hud : huds) {
            if(hud.enabled) hud.drawAll(hud.getXScaled(UResolution.getScaledWidth()), hud.getYScaled(UResolution.getScaledHeight()), hud.scale, true);
        }
    }
}
