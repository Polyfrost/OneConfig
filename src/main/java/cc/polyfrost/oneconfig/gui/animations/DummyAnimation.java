package cc.polyfrost.oneconfig.gui.animations;

public class DummyAnimation extends Animation{
    /**
     * @param value The value that is returned
     */
    public DummyAnimation(float value) {
        super(value, value, value, false);
    }

    @Override
    protected float animate(float timePassed, float duration, float start, float change) {
        return start;
    }
}
