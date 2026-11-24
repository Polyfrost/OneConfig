package org.polyfrost.oneconfig.api.hud.v1;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudBackgroundMergeTest {
    static class PlainHud extends TextHud {
        PlainHud(String id) { super(id, "Merge Hud", Hud.Category.getINFO(), "", ""); }
        @Override public String getText() { return "hi"; }
    }

    private static Map<String, Float> corners(List<float[]> rects, float... radii) {
        List<HudBackgroundMerge.Loop> loops = HudBackgroundMerge.INSTANCE.outlines(rects, radii);
        assertEquals(1, loops.size());
        HudBackgroundMerge.Loop loop = loops.get(0);
        Map<String, Float> out = new HashMap<>();
        for (int k = 0; k < loop.getRadii().length; k++) {
            out.put((int) loop.getPoints()[k * 2] + "," + (int) loop.getPoints()[k * 2 + 1], loop.getRadii()[k]);
        }
        return out;
    }

    @Test
    void outerCornersKeepTheirOwnRadius() {
        Map<String, Float> corners = corners(
            List.of(new float[]{0, 0, 100, 20}, new float[]{0, 20, 100, 20}),
            4f, 10f
        );
        assertEquals(Map.of("0,0", 4f, "100,0", 4f, "100,40", 10f, "0,40", 10f), corners);
    }

    @Test
    void stepTakesTheSmallerRadius() {
        Map<String, Float> corners = corners(
            List.of(new float[]{0, 0, 100, 20}, new float[]{0, 20, 60, 20}),
            10f, 4f
        );
        assertEquals(
            Map.of("0,0", 10f, "100,0", 10f, "100,20", 10f, "60,20", 4f, "60,40", 4f, "0,40", 4f),
            corners
        );
    }

    @Test
    void scaledHudsStillMerge() {
        ConfigManager.active();
        PlainHud a = new PlainHud("merge-a");
        PlainHud b = new PlainHud("merge-b");
        b.setCustomScale(1.35f);
        assertTrue(HudManager.backgroundStylesMatch(a, b));
    }
}
