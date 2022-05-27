package cc.polyfrost.oneconfig.lwjgl.scissor;

/**
 * A class that represents a scissor rectangle.
 *
 * @see ScissorManager
 */
public class Scissor {
    public float x;
    public float y;
    public float width;
    public float height;

    public Scissor(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Scissor(Scissor scissor) {
        this.x = scissor.x;
        this.y = scissor.y;
        this.width = scissor.width;
        this.height = scissor.height;
    }
}
