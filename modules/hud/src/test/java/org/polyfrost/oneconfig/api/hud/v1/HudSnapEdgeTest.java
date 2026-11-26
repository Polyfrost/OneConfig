package org.polyfrost.oneconfig.api.hud.v1;

import kotlin.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HudSnapEdgeTest {
    static class GrowHud extends TextHud {
        float extra;
        GrowHud(float extra) {
            super("snap-edge-hud", "Snap Edge Hud", Hud.Category.getINFO(), "", "");
            this.extra = extra;
        }
        @Override public String getText() { return "hi"; }
        @Override public Pair<Float, Float> minimumSize() {
            Pair<Float, Float> base = super.minimumSize();
            return new Pair<>(base.getFirst() + extra, base.getSecond());
        }
    }

    private static final float SW = 960f;
    private static final float GROWTH = 60f;

    @BeforeEach
    void setUp() {
        ConfigManager.active();
        HudManager.guiScreenWidth = SW;
        HudManager.guiScreenHeight = 540f;
    }

    // the minimum size is cached per frame, so a new width only shows on the next one
    private static void nextFrame() throws Exception {
        Field frameId = HudManager.class.getDeclaredField("frameId");
        frameId.setAccessible(true);
        frameId.setLong(HudManager.INSTANCE, frameId.getLong(HudManager.INSTANCE) + 1);
    }

    private static GrowHud hud(float width) throws Exception {
        GrowHud hud = new GrowHud(width);
        nextFrame();
        return hud;
    }

    private static void grow(GrowHud hud) throws Exception {
        hud.extra += GROWTH;
        nextFrame();
    }

    private static float centre(Hud hud) { return hud.getX() + hud.getScaledWidth() / 2f; }

    private static float right(Hud hud) { return hud.getX() + hud.getScaledWidth(); }

    @Test
    void recordingAnEdgeDoesNotMoveTheHud() throws Exception {
        GrowHud hud = hud(100f);
        hud.setAbsolutePosition(95f, 300f);
        hud.setSnapEdgesKeepingPosition(HudSnapEdge.End, HudSnapEdge.Center);
        assertEquals(95f, hud.getX(), 0.01f);
        assertEquals(300f, hud.getY(), 0.01f);
    }

    @Test
    void centreSnapOutsideTheCentreThirdHoldsTheCentre() throws Exception {
        GrowHud hud = hud(100f);
        hud.setAbsolutePosition(150f - hud.getScaledWidth() / 2f, 300f);
        hud.setSnapEdgesKeepingPosition(HudSnapEdge.Center, HudSnapEdge.None);
        grow(hud);
        assertEquals(150f, centre(hud), 0.01f);
    }

    @Test
    void endSnapInTheCentreThirdHoldsTheRightEdge() throws Exception {
        GrowHud hud = hud(100f);
        hud.setAbsolutePosition(520f - hud.getScaledWidth(), 300f);
        hud.setSnapEdgesKeepingPosition(HudSnapEdge.End, HudSnapEdge.None);
        grow(hud);
        assertEquals(520f, right(hud), 0.01f);
    }

    @Test
    void snapOutranksAPickedAnchor() throws Exception {
        GrowHud hud = hud(100f);
        hud.setGrowthAnchorKeepingPosition(HudAnchor.TopLeft);
        hud.setAbsolutePosition(SW / 2f - hud.getScaledWidth() / 2f, 200f);
        hud.setSnapEdgesKeepingPosition(HudSnapEdge.Center, HudSnapEdge.None);
        assertEquals(HudAnchor.Top, hud.getEffectiveGrowthAnchor());
        grow(hud);
        assertEquals(SW / 2f, centre(hud), 0.01f);
    }

    @Test
    void pickingAnAnchorReplacesTheSnap() throws Exception {
        GrowHud hud = hud(100f);
        hud.setAbsolutePosition(150f - hud.getScaledWidth() / 2f, 300f);
        hud.setSnapEdgesKeepingPosition(HudSnapEdge.Center, HudSnapEdge.Center);
        float x = hud.getX();
        hud.setGrowthAnchorKeepingPosition(HudAnchor.TopRight);
        assertEquals(HudSnapEdge.None, hud.getSnapEdgeX());
        assertEquals(HudSnapEdge.None, hud.getSnapEdgeY());
        assertEquals(x, hud.getX(), 0.01f);
        float right = right(hud);
        grow(hud);
        assertEquals(right, right(hud), 0.01f);
    }

    @Test
    void movingOffTheLineForgetsOnlyThatAxis() throws Exception {
        GrowHud hud = hud(100f);
        hud.setAbsolutePosition(150f, 300f);
        hud.setSnapEdgesKeepingPosition(HudSnapEdge.Center, HudSnapEdge.End);
        hud.setAbsolutePosition(400f, 300f);
        assertEquals(HudSnapEdge.None, hud.getSnapEdgeX());
        assertEquals(HudSnapEdge.End, hud.getSnapEdgeY());
        assertEquals(400f, hud.getX(), 0.01f);
        assertEquals(300f, hud.getY(), 0.01f);
        hud.setY(120f);
        assertEquals(HudSnapEdge.None, hud.getSnapEdgeY());
        assertEquals(120f, hud.getY(), 0.01f);
    }

    static class DefaultHud extends GrowHud {
        DefaultHud() { super(100f); }
        @Override public Pair<Float, Float> defaultPosition() { return new Pair<>(700f, 300f); }
    }

    private static final String[] POSITION_IDS = {"section", "relativeX", "relativeY"};

    @Test
    void positionDefaultsIgnoreTheSnap() throws Exception {
        DefaultHud provider = new DefaultHud();
        Hud plain = provider.make(null);
        plain.capturePositionDefaults();

        Hud snapped = provider.make(null);
        snapped.setAbsolutePosition(700f, 300f);
        snapped.setSnapEdgesKeepingPosition(HudSnapEdge.Start, HudSnapEdge.End);
        float x = snapped.getX();
        float y = snapped.getY();
        float relX = snapped.getRelativeX();
        float relY = snapped.getRelativeY();
        snapped.capturePositionDefaults();

        for (String id : POSITION_IDS) {
            Object expected = plain.getTree().getProp(id).getMetadata("default");
            assertNotNull(expected, id);
            assertEquals(expected, snapped.getTree().getProp(id).getMetadata("default"), id);
        }
        assertEquals(HudSnapEdge.Start, snapped.getSnapEdgeX());
        assertEquals(HudSnapEdge.End, snapped.getSnapEdgeY());
        assertEquals(relX, snapped.getRelativeX(), 0.0001f);
        assertEquals(relY, snapped.getRelativeY(), 0.0001f);
        assertEquals(x, snapped.getX(), 0.01f);
        assertEquals(y, snapped.getY(), 0.01f);

        snapped.setSnapEdgesKeepingPosition(HudSnapEdge.None, HudSnapEdge.None);
        snapped.setSection((Section) snapped.getTree().getProp("section").getMetadata("default"));
        snapped.setRelativeX(((Number) snapped.getTree().getProp("relativeX").getMetadata("default")).floatValue());
        snapped.setRelativeY(((Number) snapped.getTree().getProp("relativeY").getMetadata("default")).floatValue());
        assertEquals(700f, snapped.getX(), 0.01f);
        assertEquals(300f, snapped.getY(), 0.01f);
    }
}
