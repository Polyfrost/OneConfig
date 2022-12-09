package cc.polyfrost.oneconfig.renderer.scissor;

import cc.polyfrost.oneconfig.renderer.LwjglManager;

/**
 * Provides an easy way to manage and group scissor rectangles.
 */
public interface ScissorHelper {
    @SuppressWarnings("deprecation")
    ScissorHelper INSTANCE = LwjglManager.INSTANCE.getScissorHelper();

    /**
     * Adds and applies a scissor rectangle to the list of scissor rectangles.
     *
     * @param vg     The NanoVG context.
     * @param x      The x coordinate of the scissor rectangle.
     * @param y      The y coordinate of the scissor rectangle.
     * @param width  The width of the scissor rectangle.
     * @param height The height of the scissor rectangle.
     * @return The scissor rectangle.
     */
    Scissor scissor(long vg, float x, float y, float width, float height);

    /**
     * Resets the scissor rectangle provided.
     *
     * @param vg      The NanoVG context.
     * @param scissor The scissor rectangle to reset.
     */
    void resetScissor(long vg, Scissor scissor);

    /**
     * Clear all scissor rectangles.
     *
     * @param vg The NanoVG context.
     */
    void clearScissors(long vg);

    void save();

    void restore(long vg);
}
