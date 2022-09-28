package cc.polyfrost.oneconfig.gui.animations;

public class EaseOutBump extends Animation {

    /**
     * @param duration The duration of the animation
     * @param start    The start of the animation
     * @param end      The end of the animation
     * @param reverse  Reverse the animation
     */
    public EaseOutBump(int duration, float start, float end, boolean reverse) {
        super(duration, start, end, reverse);
    }

    // Courtesy of https://easings.net/
    @Override
    protected float animate(float x) {
        double constant1 = 1.7;
        double constant2 = constant1 + 1;

        // return x == 0 ? 0 : (float) (x == 1 ? 1 : Math.pow(2, -0.5 * x) * Math.sin((x * 100 - 2) * c4) + 1);
        return (float) (1 + constant2 * Math.pow(x-1, 3) + constant1 * 1.2 * Math.pow(x-1, 2));
    }
}
