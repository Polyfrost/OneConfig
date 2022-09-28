package cc.polyfrost.oneconfig.gui.animations;

public class EaseOutExpo extends Animation {

    /**
     * @param duration The duration of the animation
     * @param start    The start of the animation
     * @param end      The end of the animation
     * @param reverse  Reverse the animation
     */
    public EaseOutExpo(int duration, float start, float end, boolean reverse) {
        super(duration, start, end, reverse);
    }

    // Courtesy of https://easings.net/
    @Override
    protected float animate(float x) {
        return x == 1 ? 1 : 1 - (float) Math.pow(2, -10 * x);
    }
}
