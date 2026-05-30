package org.polyfrost.oneconfig.api.platform.v1.internal;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.polyfrost.oneconfig.api.platform.v1.Keys;

public class KeysImpl implements Keys {
    @Override
    public int getKeyLeftShift() {
        return InputConstants.KEY_LSHIFT;
    }

    @Override
    public int getKeyRightShift() {
        return InputConstants.KEY_RSHIFT;
    }

    @Override
    public int getKeyLeftControl() {
        return InputConstants.KEY_LCONTROL;
    }

    @Override
    public int getKeyRightControl() {
        return InputConstants.KEY_RCONTROL;
    }

    @Override
    public int getKeyLeftAlt() {
        return InputConstants.KEY_LALT;
    }

    @Override
    public int getKeyRightAlt() {
        return InputConstants.KEY_RALT;
    }

    @Override
    public int getKeyLeftSuper() {
        return GLFW.GLFW_KEY_LEFT_SUPER;
    }

    @Override
    public int getKeyRightSuper() {
        return GLFW.GLFW_KEY_RIGHT_SUPER;
    }
}
