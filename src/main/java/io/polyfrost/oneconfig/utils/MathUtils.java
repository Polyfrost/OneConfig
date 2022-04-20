package io.polyfrost.oneconfig.utils;

public class MathUtils {
    public static float clamp(float number) {
        return number < (float) 0.0 ? (float) 0.0 : Math.min(number, (float) 1.0);
    }

    public static float easeOut(float current, float goal) {
        if (Math.floor(Math.abs(goal - current) / (float) 0.01) > 0) {
            return current + (goal - current) / (float) 100.0;           // this number here controls the speed uh oh
        } else {
            return goal;
        }
    }


}
