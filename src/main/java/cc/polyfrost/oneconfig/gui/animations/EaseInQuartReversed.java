package cc.polyfrost.oneconfig.gui.animations;

public class EaseInQuartReversed extends Animation {
    /**
     * @param duration The duration of the animation
     * @param start    The start of the animation
     * @param end      The end of the animation
     * @param reverse  Reverse the animation
     */
    public EaseInQuartReversed(float duration, float start, float end, boolean reverse) {
        super(duration, start, end, reverse);
    }

    @Override
    protected float animate(float timePassed, float duration, float start, float change) {
        float x = timePassed / duration;
        if (x < 0.5f) return (float) (16 * Math.pow(x, 4) * change + start);
        return (float) (Math.pow(2 * x - 2, 4) * change + start);
    }
}
