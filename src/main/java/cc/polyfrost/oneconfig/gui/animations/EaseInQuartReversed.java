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
        if (x < 0.25f) return (float) (128 * Math.pow(x, 4) * change + start);
        if (x < 0.75f) return (float) ((-128 * Math.pow(x - 0.5, 4) + 1) * change + start);
        return (float) (128 * Math.pow(x - 1, 4) * change + start);
    }
}
