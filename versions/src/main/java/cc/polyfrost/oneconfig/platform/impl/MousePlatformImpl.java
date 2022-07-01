package cc.polyfrost.oneconfig.platform.impl;

import cc.polyfrost.oneconfig.platform.MousePlatform;
import org.lwjgl.input.Mouse;

@SuppressWarnings("unused")
public class MousePlatformImpl implements MousePlatform {

    @Override
    public int getMouseX() {
        return Mouse.getX();
    }

    @Override
    public int getMouseY() {
        return Mouse.getY();
    }

    @Override
    public int getDWheel() {
        return Mouse.getDWheel();
    }

    @Override
    public int getMouseDX() {
        return Mouse.getDX();
    }

    @Override
    public int getMouseDY() {
        return Mouse.getDY();
    }

    @Override
    public boolean next() {
        return Mouse.next();
    }

    @Override
    public boolean getEventButtonState() {
        return Mouse.getEventButtonState();
    }

    @Override
    public int getEventButton() {
        return Mouse.getEventButton();
    }

    @Override
    public boolean isButtonDown(int button) {
        return Mouse.isButtonDown(button);
    }
}
