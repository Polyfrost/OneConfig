package org.polyfrost.oneconfig.api.hud.v1;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    private static PlainHud place(String id, HudAnchor growth, float x, float y, float w, float h) {
        PlainHud hud = new PlainHud(id);
        hud.setHidden(false);
        hud.setRenderedW(w);
        hud.setRenderedH(h);
        hud.setGrowthAnchor(growth);
        hud.setAbsolutePosition(x, y);
        return hud;
    }

    private static int link(List<Hud> huds) {
        List<HudBackgroundMerge.Group> groups = HudBackgroundMerge.INSTANCE.computeGroups(huds, 960f, 540f);
        for (HudBackgroundMerge.Group group : groups) {
            for (HudBackgroundMerge.Link l : group.getLinks()) {
                l.getChild().applyMergeLink$hud(l.getParent(), l.getChildPoint(), l.getParentPoint(), l.getAxis());
            }
        }
        return groups.size();
    }

    private static void screen() {
        ConfigManager.active();
        HudManager.guiScreenWidth = 960f;
        HudManager.guiScreenHeight = 540f;
    }

    @Test
    void stackedParentGrowingDoesNotShiftChild() {
        screen();
        PlainHud top = place("stack-top", HudAnchor.TopLeft, 100f, 100f, 100f, 20f);
        PlainHud bottom = place("stack-bottom", HudAnchor.TopLeft, 120f, 120f, 60f, 20f);
        List<Hud> huds = List.of(top, bottom);
        assertEquals(1, link(huds));
        top.setRenderedW(160f);
        assertEquals(120f, bottom.getX(), 0.01f);
        assertEquals(1, link(huds));
        top.setRenderedW(220f);
        assertEquals(120f, bottom.getX(), 0.01f);
    }

    @Test
    void stackedRightGrowingParentDoesNotShiftFlushChild() {
        screen();
        PlainHud top = place("flush-top", HudAnchor.TopRight, 100f, 100f, 100f, 20f);
        PlainHud bottom = place("flush-bottom", HudAnchor.TopLeft, 100f, 120f, 100f, 20f);
        List<Hud> huds = List.of(top, bottom);
        assertEquals(1, link(huds));
        top.setRenderedW(160f);
        assertEquals(40f, top.getX(), 0.01f);
        assertEquals(100f, bottom.getX(), 0.01f);
    }

    @Test
    void sideBySideGrowingDoesNotShiftNeighbour() {
        screen();
        PlainHud left = place("row-left", HudAnchor.TopLeft, 100f, 100f, 100f, 40f);
        PlainHud right = place("row-right", HudAnchor.TopLeft, 200f, 110f, 80f, 20f);
        List<Hud> huds = List.of(left, right);
        assertEquals(1, link(huds));
        right.setRenderedW(140f);
        assertEquals(100f, left.getX(), 0.01f);
        assertEquals(200f, right.getX(), 0.01f);
        assertEquals(110f, right.getY(), 0.01f);
        assertEquals(1, link(huds));
        left.setRenderedW(130f);
        assertEquals(230f, right.getX(), 0.01f);
        assertEquals(110f, right.getY(), 0.01f);
    }

    @Test
    void flushChildGrowingAgainstItsAnchorStaysMerged() {
        screen();
        PlainHud top = place("edge-top", HudAnchor.TopLeft, 100f, 100f, 100f, 20f);
        PlainHud bottom = place("edge-bottom", HudAnchor.TopRight, 100f, 120f, 50f, 20f);
        List<Hud> huds = List.of(top, bottom);
        assertEquals(1, link(huds));
        bottom.setRenderedW(100f);
        assertEquals(100f, bottom.getX(), 0.01f);
        assertEquals(1, link(huds));
    }

    @Test
    void growthAnchorChangeChangesLayoutKey() {
        screen();
        PlainHud a = place("key-a", HudAnchor.TopLeft, 100f, 100f, 100f, 20f);
        PlainHud b = place("key-b", HudAnchor.TopLeft, 100f, 120f, 100f, 20f);
        List<Hud> huds = List.of(a, b);
        int before = HudBackgroundMerge.INSTANCE.layoutKey(huds);
        b.setGrowthAnchorKeepingPosition(HudAnchor.TopRight);
        assertNotEquals(before, HudBackgroundMerge.INSTANCE.layoutKey(huds));
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
