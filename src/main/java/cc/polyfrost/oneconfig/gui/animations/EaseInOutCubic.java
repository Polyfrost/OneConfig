package cc.polyfrost.oneconfig.gui.animations;

public class EaseInOutCubic extends Animation {

    /**
     * @param duration The duration of the animation
     * @param start    The start of the animation
     * @param end      The end of the animation
     * @param reverse  Reverse the animation
     */
    public EaseInOutCubic(int duration, float start, float end, boolean reverse) {
        super(duration, start, end, reverse);
    }

    /**
     * Adapted from <a href="https://github.com/jesusgollonet/processing-penner-easing">https://github.com/jesusgollonet/processing-penner-easing</a>
     */
    @Override
    protected float animate(float timePassed, float duration, float start, float change) {
        if ((timePassed /= duration / 2) < 1) return change / 2 * timePassed * timePassed * timePassed + start;
        return change / 2 * ((timePassed -= 2) * timePassed * timePassed + 2) + start;
    }
}
