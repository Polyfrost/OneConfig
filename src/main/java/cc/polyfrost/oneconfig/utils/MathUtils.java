package cc.polyfrost.oneconfig.utils;

public class MathUtils {
    public static float clamp(float number) {
        return number < (float) 0.0 ? (float) 0.0 : Math.min(number, (float) 1.0);
    }

    public static float easeOut(float current, float goal, float speed) {
        if (Math.floor(Math.abs(goal - current) * Math.abs(current - goal) * 3) > 0) {
            return current + (goal - current) / speed;
        } else {
            return goal;
        }
    }


    public static float easeInQuad(float current) {
        return current * current;
    }

    /**
     * taken from <a href="https://github.com/jesusgollonet/processing-penner-easing">https://github.com/jesusgollonet/processing-penner-easing</a>
     */
    public static float easeInOutCirc(float t, float b, float c, float d) {
        if ((t /= d / 2) < 1) return -c / 2 * ((float) Math.sqrt(1 - t * t) - 1) + b;
        return c / 2 * ((float) Math.sqrt(1 - (t -= 2) * t) + 1) + b;
    }


}
