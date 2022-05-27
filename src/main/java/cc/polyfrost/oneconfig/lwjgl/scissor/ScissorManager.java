package cc.polyfrost.oneconfig.lwjgl.scissor;

import org.lwjgl.nanovg.NanoVG;

import java.util.ArrayList;

/**
 * Provides an easy way to manage and group scissor rectangles.
 */
public class ScissorManager {
    private static final ArrayList<Scissor> scissors = new ArrayList<>();

    /**
     * Adds and applies a scissor rectangle to the list of scissor rectangles.
     * @param vg The NanoVG context.
     * @param x The x coordinate of the scissor rectangle.
     * @param y The y coordinate of the scissor rectangle.
     * @param width The width of the scissor rectangle.
     * @param height The height of the scissor rectangle.
     * @return The scissor rectangle.
     */
    public static Scissor scissor(long vg, float x, float y, float width, float height) {
        Scissor scissor = new Scissor(x, y, width, height);
        if (scissors.contains(scissor)) return scissor;
        scissors.add(scissor);
        applyScissors(vg);
        return scissor;
    }

    /**
     * Resets the scissor rectangle provided.
     * @param vg The NanoVG context.
     * @param scissor The scissor rectangle to reset.
     */
    public static void resetScissor(long vg, Scissor scissor) {
        if (scissors.contains(scissor)) {
            scissors.remove(scissor);
            applyScissors(vg);
        }
    }

    /**
     * Clear all scissor rectangles.
     * @param vg The NanoVG context.
     */
    public static void clearScissors(long vg) {
        scissors.clear();
        NanoVG.nvgResetScissor(vg);
    }

    private static void applyScissors(long vg) {
        NanoVG.nvgResetScissor(vg);
        if (scissors.size() <= 0) return;
        Scissor finalScissor = new Scissor(scissors.get(0));
        for (int i = 1; i < scissors.size(); i++) {
            Scissor scissor = scissors.get(i);
            float rightX = Math.min(scissor.x + scissor.width, finalScissor.x + finalScissor.width);
            float rightY = Math.min(scissor.y + scissor.height, finalScissor.y + finalScissor.height);
            finalScissor.x = Math.max(finalScissor.x, scissor.x);
            finalScissor.y = Math.max(finalScissor.y, scissor.y);
            finalScissor.width = rightX - finalScissor.x;
            finalScissor.height = rightY - finalScissor.y;
        }
        NanoVG.nvgScissor(vg, finalScissor.x, finalScissor.y, finalScissor.width, finalScissor.height);
    }
}
