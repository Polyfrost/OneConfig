package cc.polyfrost.oneconfig.lwjgl;

import org.lwjgl.nanovg.NanoVG;

import java.util.ArrayList;

public class ScissorManager {
    private static final ArrayList<Scissor> scissors = new ArrayList<>();

    public static Scissor scissor(long vg, int x, int y, int width, int height) {
        Scissor scissor = new Scissor(x, y, width, height);
        if (scissors.contains(scissor)) return scissor;
        scissors.add(scissor);
        applyScissors(vg);
        return scissor;
    }

    public static void resetScissor(long vg, Scissor scissor) {
        if (scissors.contains(scissor)) {
            scissors.remove(scissor);
            applyScissors(vg);
        }
    }

    private static void applyScissors(long vg) {
        NanoVG.nvgResetScissor(vg);
        if (scissors.size() <= 0) return;
        Scissor finalScissor = new Scissor(scissors.get(0));
        for (int i = 1; i < scissors.size(); i++) {
            Scissor scissor = scissors.get(i);
            int rightX = Math.min(scissor.x + scissor.width, finalScissor.x + finalScissor.width);
            int rightY = Math.min(scissor.y + scissor.height, finalScissor.y + finalScissor.height);
            finalScissor.x = Math.max(finalScissor.x, scissor.x);
            finalScissor.y = Math.max(finalScissor.y, scissor.y);
            finalScissor.width = rightX - finalScissor.x;
            finalScissor.height = rightY - finalScissor.y;
        }
        NanoVG.nvgScissor(vg, finalScissor.x, finalScissor.y, finalScissor.width, finalScissor.height);
    }
}
