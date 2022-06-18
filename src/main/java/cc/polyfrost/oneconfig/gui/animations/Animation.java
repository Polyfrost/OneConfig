package cc.polyfrost.oneconfig.gui.animations;

import cc.polyfrost.oneconfig.utils.gui.GuiUtils;

public abstract class Animation {
    private final boolean reverse;
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
        this.reverse = reverse;
    }

    /**
     * @param deltaTime The time since the last frame
     * @return The new value
     */
    public float get(float deltaTime) {
        timePassed += deltaTime;
        if (timePassed >= duration) timePassed = duration;
        return animate(timePassed / duration) * change + start;
    }

    /**
     * @return The new value
     */
    public float get() {
        return get(GuiUtils.getDeltaTime());
    }

    /**
     * @return If the animation is finished or not
     */
    public boolean isFinished() {
        return timePassed >= duration;
    }

    /**
     * @return If the animation is reversed
     */
    public boolean isReversed() {
        return reverse;
    }

    protected abstract float animate(float x);
}
