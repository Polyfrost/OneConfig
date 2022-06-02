package cc.polyfrost.oneconfig.gui.animations;

public class EaseOutQuad extends Animation {

    /**
     * @param duration The duration of the animation
     * @param start    The start of the animation
     * @param end      The end of the animation
     * @param reverse  Reverse the animation
     */
    public EaseOutQuad(int duration, float start, float end, boolean reverse) {
        super(duration, start, end, reverse);
    }

    /**
     * Adapted from <a href="https://github.com/jesusgollonet/processing-penner-easing">https://github.com/jesusgollonet/processing-penner-easing</a>
     */
    @Override
    protected float animate(float timePassed, float duration, float start, float change) {
        return -change * (timePassed /= duration) * (timePassed - 2) + start;
    }
}
