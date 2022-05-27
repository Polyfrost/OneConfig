package cc.polyfrost.oneconfig.events.event;

import net.minecraft.client.gui.GuiScreen;

public class ScreenOpenEvent extends CancellableEvent {
    public final GuiScreen screen;

    public ScreenOpenEvent(GuiScreen screen) {
        this.screen = screen;
    }
}
