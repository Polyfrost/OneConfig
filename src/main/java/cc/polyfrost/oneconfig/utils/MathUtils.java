package cc.polyfrost.oneconfig.utils;

public final class MathUtils {
    public static float clamp(float number) {
        return clamp(number, 0, 1);
    }

    public static float clamp(float number, float min, float max) {
        return number < min ? min : Math.min(number, max);
    }

    @Deprecated
    public static float easeOut(float current, float goal, float speed) {
        float deltaTime = GuiUtils.getDeltaTime();
        if (Math.round(Math.abs(goal - current) * 100) > 0) {
            return current + (goal - current) / speed * deltaTime;
        } else {
            return goal;
        }
    }

    public static float map(float value, float start1, float stop1, float start2, float stop2) {
        return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1));
    }
}
