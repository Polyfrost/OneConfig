package cc.polyfrost.oneconfig.platform.impl;

import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import cc.polyfrost.oneconfig.libs.universal.UScreen;
import cc.polyfrost.oneconfig.platform.GuiPlatform;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;

@SuppressWarnings("unused")
public class GuiPlatformImpl implements GuiPlatform {

    @Override
    public Object getCurrentScreen() {
        return UScreen.getCurrentScreen();
    }

    @Override
    public void setCurrentScreen(Object screen) {
        UScreen.displayScreen((GuiScreen) screen);
    }

    @Override
    public boolean isInChat() {
        return getCurrentScreen() instanceof GuiChat;
    }

    @Override
    public boolean isInDebug() {
        return UMinecraft.getSettings().showDebugInfo;
    }
}
