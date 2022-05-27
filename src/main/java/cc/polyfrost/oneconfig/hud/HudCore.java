package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.events.event.HudRenderEvent;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.universal.UResolution;

import java.util.ArrayList;

public class HudCore {
    public static ArrayList<BasicHud> huds = new ArrayList<>();
    public static boolean editing = false;

    @Subscribe
    public void onRender(HudRenderEvent event) {
        if (editing) return;
        for (BasicHud hud : huds) {
            if (hud.enabled)
                hud.drawAll(hud.getXScaled(UResolution.getScaledWidth()), hud.getYScaled(UResolution.getScaledHeight()), hud.scale, true);
        }
    }
}
