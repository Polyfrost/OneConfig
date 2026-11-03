package org.polyfrost.oneconfig.api.hud.v1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudScaleTest {
    static class PlainHud extends TextHud {
        PlainHud() { super("scale-plain", "Plain", Hud.Category.getINFO(), "", ""); }
        @Override public String getText() { return "plain"; }
    }

    static class LegacyHud extends PlainHud implements LegacyHudMarker {
        @Override public boolean getSupportsScale() { return true; }
    }

    @Test
    void scaleIsTheValueTheCornerHandlesDrag() {
        Hud hud = new PlainHud();
        hud.setHudScale(2f);
        assertEquals(2f, hud.getTextScale(), 0.001f);
        assertEquals(2f, hud.getHudScale(), 0.001f);
        assertEquals(1f, hud.getCustomScale(), 0.001f, "the scale stays separate from Custom Scale");
    }

    @Test
    void aStaticBoxStaysInProportion() {
        Hud hud = new PlainHud();
        hud.setStaticWidth(true);
        hud.setStaticW(100f);
        hud.setStaticH(20f);
        hud.setHudScale(1.5f);
        assertEquals(150f, hud.getStaticW(), 0.001f);
        assertEquals(30f, hud.getStaticH(), 0.001f);
    }

    @Test
    void scaleStaysInRange() {
        Hud hud = new PlainHud();
        hud.setHudScale(100f);
        assertEquals(hud.getHudScaleRange().getEndInclusive(), hud.getHudScale(), 0.001f);
        hud.setHudScale(-1f);
        assertEquals(hud.getHudScaleRange().getStart(), hud.getHudScale(), 0.001f);
    }

    @Test
    void legacyHudsScaleThroughCustomScale() {
        Hud hud = new LegacyHud();
        hud.setHudScale(2f);
        assertEquals(2f, hud.getCustomScale(), 0.001f);
        assertEquals(1f, hud.getTextScale(), 0.001f);
    }
}
