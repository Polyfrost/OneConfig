package cc.polyfrost.oneconfig.gui.animations;

import cc.polyfrost.oneconfig.gui.OneConfigGui;

public abstract class Animation {
    private final int duration;
    private final float start;
    private final float change;
    private long timePassed = 0;

    /**
     * @param duration The duration of the animation
     * @param start    The start of the animation
     * @param end      The end of the animation
     * @param reverse  Reverse the animation
     */
    public Animation(int duration, float start, float end, boolean reverse) {
        this.duration = duration;
        this.start = start;
        if (!reverse) this.change = end - start;
        else this.change = start - end;
    }

    /**
     * @param deltaTime The time since the last frame
     * @return The new value
     */
    public float get(long deltaTime) {
        timePassed += deltaTime;
        if (timePassed >= duration) return start + change;
        float value = animate(timePassed, duration, start, change);
        System.out.println(value);
        return value;
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

    protected abstract float animate(long timePassed, int duration, float start, float change);
}
