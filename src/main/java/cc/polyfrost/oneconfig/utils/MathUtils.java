package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.gui.OneConfigGui;

public final class MathUtils {
    public static float clamp(float number) {
        return clamp(number, 0, 1);
    }

    public static float clamp(float number, float min, float max) {
        return number < min ? min : Math.min(number, max);
    }

    public static float easeOut(float current, float goal, float speed) {
        float deltaTime = OneConfigGui.INSTANCE == null ? 16 : OneConfigGui.INSTANCE.getDeltaTime();
        if (Math.round(Math.abs(goal - current) * 100) > 0) {
            return current + (goal - current) / speed * deltaTime;
        } else {
            return goal;
        }
    }

    public static float easeInQuad(float current) {
        return current * current;
    }

    /**
     * Adapted from <a href="https://github.com/jesusgollonet/processing-penner-easing">https://github.com/jesusgollonet/processing-penner-easing</a>
     */
    public static float easeInOutCirc(float time, float beginning, float change, float duration) {
        if ((time /= duration / 2) < 1) return -change / 2 * ((float) Math.sqrt(1 - time * time) - 1) + beginning;
        return change / 2 * ((float) Math.sqrt(1 - (time -= 2) * time) + 1) + beginning;
    }

    public static float map(float value, float start1, float stop1, float start2, float stop2) {
        return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1));
    }
}
