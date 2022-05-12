package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.OneConfig;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;

public class HudCore {
    public static ArrayList<BasicHud> huds = new ArrayList<>();
    public static boolean editing = false;

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || editing) return;
        int[] sr = OneConfig.getScaledResolution();
        for (BasicHud hud : huds) {
            if(hud.enabled) hud.drawAll(hud.getXScaled(sr[0]), hud.getYScaled(sr[1]), hud.scale, true);
        }
    }
}