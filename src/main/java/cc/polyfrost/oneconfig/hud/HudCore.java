package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.hud.interfaces.BasicHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;

public class HudCore {
    public static ArrayList<BasicHud> huds = new ArrayList<>();
    public static boolean editing = false;

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || editing) return;
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        for (BasicHud hud : huds) {
            hud.drawAll(hud.getXScaled(sr.getScaledWidth()), hud.getYScaled(sr.getScaledHeight()), hud.scale, true);
        }
    }
}
