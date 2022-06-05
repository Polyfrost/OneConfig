package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.api.events.event.HudRenderEvent;
import gg.essential.universal.UResolution;
import me.kbrewster.eventbus.Subscribe;

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
