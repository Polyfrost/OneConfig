package cc.polyfrost.oneconfig.platform;

public interface MousePlatform {
    int getMouseX();
    int getMouseY();
    int getDWheel();
    int getMouseDX();
    int getMouseDY();

    boolean next();
    boolean getEventButtonState();
    int getEventButton();

    boolean isButtonDown(int button);
}
