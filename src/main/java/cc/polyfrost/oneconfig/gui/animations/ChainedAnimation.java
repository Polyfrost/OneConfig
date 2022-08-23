package cc.polyfrost.oneconfig.gui.animations;

public class ChainedAnimation extends Animation {
    private final Animation[] animations;
    private int currentAnimation = 0;
    private float value;

    public ChainedAnimation(Animation... animations) {
        super(1, 0, 0, false);
        this.animations = animations;
    }

    @Override
    public float get(float deltaTime) {
        if (currentAnimation >= animations.length) return value;
        value = animations[currentAnimation].get(deltaTime);
        if (animations[currentAnimation].isFinished()) currentAnimation++;
        return value;
    }

    @Override
    public boolean isFinished() {
        return currentAnimation >= animations.length;
    }

    @Override
    protected float animate(float x) {
        return 0;
    }
}
