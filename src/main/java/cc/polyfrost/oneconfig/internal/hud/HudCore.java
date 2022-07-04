package cc.polyfrost.oneconfig.internal.hud;

import cc.polyfrost.oneconfig.events.event.HudRenderEvent;
import cc.polyfrost.oneconfig.hud.Hud;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.universal.UResolution;

import java.util.ArrayList;

public class HudCore {
    public static ArrayList<Hud> huds = new ArrayList<>();
    public static boolean editing = false;

    @Subscribe
    public void onRender(HudRenderEvent event) {
        if (editing) return;
        for (Hud hud : huds) {
            if (!hud.isEnabled()) continue;
            hud.drawAll(event.matrices, hud.getXScaled(UResolution.getScaledWidth()), hud.getYScaled(UResolution.getScaledHeight()), hud.scale, true);
        }
    }
}
