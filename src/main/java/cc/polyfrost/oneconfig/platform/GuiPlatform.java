package cc.polyfrost.oneconfig.platform;

public interface GuiPlatform {
    Object getCurrentScreen();
    void setCurrentScreen(Object screen);
    boolean isInChat();
    boolean isInDebug();
}
