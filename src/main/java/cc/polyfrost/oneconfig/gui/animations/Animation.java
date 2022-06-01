package cc.polyfrost.oneconfig.gui.animations;

import cc.polyfrost.oneconfig.gui.OneConfigGui;

public abstract class Animation {
    private final float duration;
    private final float start;
    private final float change;
    private float timePassed = 0;

    /**
     * @param duration The duration of the animation
     * @param start    The start of the animation
     * @param end      The end of the animation
     * @param reverse  Reverse the animation
     */
    public Animation(float duration, float start, float end, boolean reverse) {
        this.duration = duration;
        if (reverse) {
            float temp = start;
            start = end;
            end = temp;
        }
        this.start = start;
        this.change = end - start;
    }

    /**
     * @param deltaTime The time since the last frame
     * @return The new value
     */
    public float get(float deltaTime) {
        timePassed += deltaTime;
        if (timePassed >= duration) timePassed = duration;
        return animate(timePassed, duration, start, change);
    }

    /**
     * @return The new value
     */
    public float get() {
        return get(OneConfigGui.getDeltaTimeNullSafe());
    }

    /**
     * @return If the animation is finished or not
     */
    public boolean isFinished() {
        return timePassed >= duration;
    }

    protected abstract float animate(float timePassed, float duration, float start, float change);
}
