package cc.polyfrost.oneconfig.gui.animations;

public class DummyAnimation extends Animation {
    protected final float value;

    /**
     * @param value The value that is returned
     */
    public DummyAnimation(float value) {
        super(value, value, value, false);
        this.value = value;
    }

    @Override
    public float get(float deltaTime) {
        return value;
    }

    @Override
    protected float animate(float x) {
        return x;
    }
}
