package cc.polyfrost.oneconfig.gui.animations;

public class EaseOutBump extends Animation {
    private static final double CONSTANT_1 = 1.7;
    private static final double CONSTANT_2 = 2.7;

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
        // return x == 0 ? 0 : (float) (x == 1 ? 1 : Math.pow(2, -0.5 * x) * Math.sin((x * 100 - 2) * c4) + 1);
        return (float) (1 + CONSTANT_2 * Math.pow(x-1, 3) + CONSTANT_1 * 1.2 * Math.pow(x-1, 2));
    }
}
