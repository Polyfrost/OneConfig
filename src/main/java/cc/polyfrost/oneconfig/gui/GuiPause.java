package cc.polyfrost.oneconfig.gui;

/**
 * Hack that allows GUIs to set whether the game should pause when the GUI is displayed without depending on
 * Minecraft itself.
 */
public interface GuiPause {
    @SuppressWarnings("unused")
    boolean doesGuiPauseGame();
}
