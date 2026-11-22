package org.polyfrost.oneconfig.api.hud.v1;

import kotlin.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudGroupsTest {
    static abstract class BoxHud extends TextHud {
        BoxHud(String id) { super(id, id, Hud.Category.getINFO(), "", ""); }
        @Override public boolean showByDefault() { return true; }
        @Override public String getText() { return "box"; }
        @Override public Pair<Float, Float> minimumSize() { return new Pair<>(40f, 10f); }
    }

    static class HudA extends BoxHud { HudA() { super("group-a"); } }
    static class HudB extends BoxHud { HudB() { super("group-b"); } }
    static class HudC extends BoxHud { HudC() { super("group-c"); } }
    static class DefaultOne extends BoxHud { DefaultOne() { super("group-default-one"); } }
    static class DefaultTwo extends BoxHud { DefaultTwo() { super("group-default-two"); } }

    static class CollapsingHud extends BoxHud {
        CollapsingHud() { super("group-collapsing"); }
        @Override public boolean collapsesWhenNotShown() { return true; }
    }

    static class PresetSingle extends BoxHud {
        PresetSingle() { super("group-preset-single"); }
        @Override public boolean showByDefault() { return false; }
        @Override public boolean multipleInstancesAllowed() { return false; }
        @Override public Pair<Float, Float> defaultPosition() { return new Pair<>(100f, 50f); }
        static float textScaleInSetup;
        @Override public void setup() { super.setup(); textScaleInSetup = getTextScale(); }
    }

    static class PresetMulti extends BoxHud {
        PresetMulti() { super("group-preset-multi"); }
        @Override public boolean showByDefault() { return false; }
        @Override public Pair<Float, Float> defaultPosition() { return new Pair<>(200f, 50f); }
    }

    static class PresetMultiB extends BoxHud {
        PresetMultiB() { super("group-preset-multi-b"); }
        @Override public boolean showByDefault() { return false; }
        @Override public Pair<Float, Float> defaultPosition() { return new Pair<>(100f, 50f); }
    }

    static class Undeletable extends BoxHud {
        Undeletable() { super("group-undeletable"); }
        @Override public boolean showByDefault() { return false; }
        @Override public boolean multipleInstancesAllowed() { return false; }
        @Override public boolean deletable() { return false; }
        static int removals;
        @Override public void remove() { removals++; }
    }

    static class BrokenSetup extends BoxHud {
        BrokenSetup() { super("group-broken-setup"); }
        @Override public boolean showByDefault() { return false; }
        @Override public void setup() { throw new IllegalStateException("setup"); }
    }

    static class OffOne extends BoxHud {
        OffOne() { super("group-off-one"); }
        @Override public boolean showByDefault() { return false; }
        @Override public Pair<Float, Float> defaultPosition() { return new Pair<>(2000f, 50f); }
    }

    static class OffTwo extends BoxHud {
        OffTwo() { super("group-off-two"); }
        @Override public boolean showByDefault() { return false; }
        @Override public Pair<Float, Float> defaultPosition() { return new Pair<>(2100f, 50f); }
    }

    static class NeverRegistered extends BoxHud { NeverRegistered() { super("group-never-registered"); } }

    static class ArrangeOne extends BoxHud {
        ArrangeOne() { super("group-arrange-one"); }
        @Override public boolean showByDefault() { return false; }
    }

    static class ArrangeTwo extends BoxHud {
        ArrangeTwo() { super("group-arrange-two"); }
        @Override public boolean showByDefault() { return false; }
    }

    static class SlotBase extends BoxHud {
        SlotBase(String id) { super(id); }
        SlotBase() { this("group-slot-base"); }
        @Override public boolean showByDefault() { return false; }
    }

    static class SlotDerived extends SlotBase { SlotDerived() { super("group-slot-derived"); } }

    private static final List<Hud> received = new ArrayList<>();
    private static final HudPresetAction STACK = new HudPresetAction("stack", "Stack", huds ->
        huds.get(1).anchorTo(huds.get(0), HudAnchor.BottomLeft, HudAnchor.TopLeft));
    private static final String DEFAULT_ID = "test.defaults";
    private static HudPreset multiPreset;
    private static HudPreset offPreset;
    private static HudPreset partialPreset;
    private static final String PRESET_ID = "test.preset";
    private static HudPreset preset;
    private static HudPreset brokenPreset;
    private static boolean registered = false;

    private Hud a;
    private Hud b;
    private Hud c;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws Exception {
        ConfigManager.active();
        ConfigManager.openProfile("");
        if (!registered) {
            HudManager.register(new HudA(), new HudB(), new HudC(), new DefaultOne(), new DefaultTwo(), new PresetSingle(), new PresetMulti(), new PresetMultiB(), new ArrangeOne(), new ArrangeTwo(), new SlotBase(), new SlotDerived(), new OffOne(), new OffTwo(), new Undeletable(), new BrokenSetup(), new CollapsingHud());
            HudGroups.registerPreset("test.recording", "Recording", Arrays.asList(HudB.class, HudA.class), null, null, null,
                Collections.singletonList(new HudPresetAction("record", "Record", huds -> { received.clear(); received.addAll(huds); })), null);
            HudGroups.registerPreset("test.arranged", "Arranged", Arrays.asList(ArrangeTwo.class, ArrangeOne.class), null, null, null,
                Collections.singletonList(STACK), "stack");
            HudGroups.registerPreset("test.throwing", "Throwing", Arrays.asList(ArrangeTwo.class, ArrangeOne.class), null, null, null,
                Collections.singletonList(new HudPresetAction("boom", "Boom", huds -> { throw new IllegalStateException("boom"); })), "boom");
            HudGroups.registerPreset("test.slots", "Slots", Arrays.asList(SlotBase.class, SlotDerived.class), null, null, null,
                Collections.singletonList(new HudPresetAction("record", "Record", huds -> { received.clear(); received.addAll(huds); })), null);
            HudGroups.registerPreset(DEFAULT_ID, "Defaults", Arrays.asList(DefaultOne.class, DefaultTwo.class), null, null, null,
                Collections.singletonList(STACK), "stack");
            multiPreset = HudGroups.registerPreset("test.preset.multi", "Multi", Arrays.asList(PresetMultiB.class, PresetMulti.class));
            offPreset = HudGroups.registerPreset("test.offscreen", "Off", Arrays.asList(OffOne.class, OffTwo.class));
            partialPreset = HudGroups.registerPreset("test.partial", "Partial", Arrays.asList(HudA.class, HudB.class, NeverRegistered.class));
            brokenPreset = HudGroups.registerPreset("test.broken", "Broken", Arrays.asList(Undeletable.class, BrokenSetup.class));
            preset = HudGroups.registerPreset(PRESET_ID, "Preset", Arrays.asList(PresetSingle.class, PresetMulti.class));
            HudGroups.registerDefault(DEFAULT_ID, "Defaults", DefaultOne.class, DefaultTwo.class);
            registered = true;
        }
        HudManager.guiScreenWidth = 960f;
        HudManager.guiScreenHeight = 540f;
        relaunch();
        a = only(HudA.class);
        b = only(HudB.class);
        c = only(HudC.class);
        for (Hud hud : Arrays.asList(a, b, c)) {
            hud.setGroupPath("");
            hud.clearAnchor();
            hud.setCustomScale(1f);
            hud.setCustomScale(1f);
            hud.setLocked(false);
            hud.setRenderedW(40f);
            hud.setRenderedH(10f);
        }
        a.setAbsolutePosition(100f, 100f);
        b.setAbsolutePosition(140f, 100f);
        c.setAbsolutePosition(300f, 300f);
    }

    private static Hud only(Class<? extends Hud> cls) {
        List<? extends Hud> huds = HudManager.INSTANCE.getHudsOfType(cls);
        assertEquals(1, huds.size(), "expected one " + cls.getSimpleName());
        return huds.get(0);
    }

    private static void relaunch() throws Exception {
        HudManager.INSTANCE.getActiveInstances().clear();
        set("registryTree", null);
        set("init", false);
        for (Hud p : new ArrayList<>(HudManager.INSTANCE.providers())) {
            Field f = findField(p.getClass(), "tree");
            f.setAccessible(true);
            f.set(p, null);
        }
        HudManager.INSTANCE.initialize();
    }

    private static void set(String name, Object v) throws Exception {
        Field f = HudManager.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(HudManager.INSTANCE, v);
    }

    private static Object get(String name) throws Exception {
        Field f = HudManager.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(HudManager.INSTANCE);
    }

    private static Field findField(Class<?> cls, String name) throws Exception {
        for (Class<?> k = cls; k != null; k = k.getSuperclass()) {
            try { return k.getDeclaredField(name); } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name + " not found on " + cls);
    }

    @Test
    void groupingAndUngroupingTwoHuds() {
        HudGroup group = HudGroups.group(Arrays.asList(a, b));
        assertNotNull(group);
        assertEquals(group, a.getGroup());
        assertEquals(group, b.getGroup());
        assertNull(c.getGroup());
        assertEquals(2, group.getHuds().size());
        assertTrue(group.contains(a));
        assertFalse(group.contains(c));

        float[] box = HudGroups.bounds(group.getHuds());
        assertNotNull(box);
        assertEquals(100f, box[0], 0.001f, "the box starts at the leftmost HUD");
        assertEquals(80f, box[2], 0.001f, "the box spans both 40 wide HUDs sitting side by side");
        assertEquals(10f, box[3], 0.001f);

        HudGroups.ungroup(group);
        assertNull(a.getGroup());
        assertNull(b.getGroup());
        assertTrue(HudGroups.all().isEmpty() || !HudGroups.all().contains(group));
    }

    @Test
    void groupingAGroupWithAHudNestsIt() {
        HudGroup inner = HudGroups.group(Arrays.asList(a, b));
        HudGroup outer = HudGroups.group(Collections.singletonList(c), Collections.singletonList(inner));
        assertNotNull(inner);
        assertNotNull(outer);

        HudGroup nested = a.getGroup();
        assertNotNull(nested);
        assertEquals(outer, nested.getParent(), "the old group now sits inside the new one");
        assertEquals(inner.getId(), nested.getId(), "a nested group keeps its id");
        assertEquals(outer, c.getGroup());
        assertEquals(3, outer.getHuds().size());
        assertEquals(Collections.singletonList(nested), outer.getSubGroups());
        assertEquals(Collections.singletonList(c), outer.getDirectHuds());
        assertEquals(outer, HudGroups.outermostOf(a));
        assertEquals(Arrays.asList(outer, nested), HudGroups.chainOf(a));

        HudGroups.ungroup(outer);
        assertEquals(inner, a.getGroup(), "ungrouping the outer group leaves the inner one intact");
        assertNull(c.getGroup());
    }

    @Test
    void groupingHudsInsideAGroupMakesASubGroup() {
        HudGroup group = HudGroups.group(Arrays.asList(a, b, c));
        assertNotNull(group);
        HudGroup sub = HudGroups.group(Arrays.asList(a, b));
        assertNotNull(sub);
        assertEquals(group, sub.getParent());
        assertEquals(group, c.getGroup());
        assertEquals(3, group.getHuds().size());
    }

    @Test
    void addingToAndRemovingFromAGroup() {
        HudGroup group = HudGroups.group(Arrays.asList(a, b));
        assertNotNull(group);
        assertTrue(HudGroups.addTo(group, Collections.singletonList(c)));
        assertEquals(group, c.getGroup());

        HudGroups.removeFromGroup(c);
        assertNull(c.getGroup());

        HudGroup sub = HudGroups.group(Collections.singletonList(a));
        assertNotNull(sub);
        assertFalse(HudGroups.addTo(sub, Collections.emptyList(), Collections.singletonList(group)),
                "a group cannot be put inside its own sub-group");
        assertEquals(group, sub.getParent());
    }

    @Test
    void movingAGroupOnlyMovesItsRoots() {
        b.anchorTo(a, HudAnchor.Right, HudAnchor.Left);
        float offsetX = b.getAnchorOffsetX();
        HudGroup group = HudGroups.group(Arrays.asList(a, b));
        assertNotNull(group);

        List<Hud> roots = HudGroups.roots(group.getHuds());
        assertEquals(Collections.singletonList(a), roots, "the anchored HUD follows its parent");

        Map<Hud, Pair<Float, Float>> starts = new HashMap<>();
        for (Hud hud : group.getHuds()) starts.put(hud, new Pair<>(hud.getX(), hud.getY()));
        HudGroups.moveFrom(group.getHuds(), starts, 50f, 20f);

        assertEquals(150f, a.getX(), 0.001f);
        assertEquals(120f, a.getY(), 0.001f);
        assertEquals(190f, b.getX(), 0.001f, "the anchored HUD moves exactly once");
        assertEquals(120f, b.getY(), 0.001f);
        assertEquals(offsetX, b.getAnchorOffsetX(), 0.001f, "moving the group keeps the anchor offset");
    }

    @Test
    void movingAGroupAnchoredThroughAnOutsideHud() {
        c.setAbsolutePosition(100f, 120f);
        b.setAbsolutePosition(100f, 140f);
        c.anchorTo(a, HudAnchor.BottomLeft, HudAnchor.TopLeft);
        b.anchorTo(c, HudAnchor.BottomLeft, HudAnchor.TopLeft);
        HudGroup group = HudGroups.group(Arrays.asList(a, b));
        assertNotNull(group);

        assertEquals(2, HudGroups.roots(group.getHuds()).size(), "b follows c, which is not being moved with the group");

        Map<Hud, Pair<Float, Float>> starts = new HashMap<>();
        for (Hud hud : group.getHuds()) starts.put(hud, new Pair<>(hud.getX(), hud.getY()));
        HudGroups.moveFrom(group.getHuds(), starts, 30f, 0f);

        assertEquals(130f, a.getX(), 0.001f);
        assertEquals(130f, c.getX(), 0.001f, "the outside HUD follows its anchor");
        assertEquals(130f, b.getX(), 0.001f, "b ends up moved once even though its parent moved too");
        assertEquals(140f, b.getY(), 0.001f);
    }

    @Test
    void scalingAGroupScalesSizesAndGaps() {
        c.setAbsolutePosition(100f, 130f);
        b.anchorTo(a, HudAnchor.Right, HudAnchor.Left);
        HudGroup group = HudGroups.group(Arrays.asList(a, b, c));
        assertNotNull(group);

        HudGroups.Scaling scaling = HudGroups.scaling(group.getHuds(), 100f, 100f);
        float used = scaling.apply(2f);
        assertEquals(2f, used, 0.001f);

        for (Hud hud : group.getHuds()) {
            assertEquals(2f, hud.getCustomScale(), 0.001f, "the group scales the value the corner handles change");
        }
        assertEquals(100f, a.getX(), 0.001f, "the pivot stays put");
        assertEquals(100f, a.getY(), 0.001f);
        assertEquals(80f, a.getScaledWidth(), 0.001f);
        assertEquals(180f, b.getX(), 0.001f, "the anchored HUD is still flush against the grown one");
        assertEquals(100f, c.getX(), 0.001f);
        assertEquals(160f, c.getY(), 0.001f, "the gap below the pivot doubles");

        float[] box = HudGroups.bounds(group.getHuds());
        assertNotNull(box);
        assertEquals(160f, box[2], 0.001f);
        assertEquals(80f, box[3], 0.001f);

        scaling.apply(1f);
        assertEquals(140f, b.getX(), 0.001f, "scaling back restores the starting layout");
        assertEquals(130f, c.getY(), 0.001f);
    }

    @Test
    void scalingStopsAtTheScaleLimits() {
        float max = 4f;
        float min = 0.25f;
        a.setCustomScale(2f);
        HudGroups.Scaling scaling = HudGroups.scaling(Arrays.asList(a, b), 100f, 100f);
        assertEquals(max / 2f, scaling.getMaxFactor(), 0.001f, "a is already at 2x so it reaches the limit first");
        assertEquals(max / 2f, scaling.apply(10f), 0.001f);
        assertEquals(max, a.getCustomScale(), 0.001f);
        assertEquals(max / 2f, b.getCustomScale(), 0.001f, "the group keeps its proportions at the limit");
        assertEquals(min, scaling.getMinFactor(), 0.001f, "b at 1x reaches the lower limit first");
    }

    @Test
    void groupScaleKeepsProportionsAndMatchEnforcesOne() {
        a.setCustomScale(4f);
        b.setCustomScale(2f);
        HudGroup group = HudGroups.group(Arrays.asList(a, b));
        assertNotNull(group);
        assertEquals(4f, group.getScale(), 0.001f);
        assertFalse(group.getUniformScale());

        HudGroups.setScale(group, 2f);
        assertEquals(2f, a.getCustomScale(), 0.001f);
        assertEquals(1f, b.getCustomScale(), 0.001f, "the ratio between them survives");

        HudGroups.matchScale(group, 2f);
        assertEquals(2f, b.getCustomScale(), 0.001f);
        assertTrue(group.getUniformScale());
    }

    @Test
    void groupScaleHoldsTheEdgeTheGroupSitsNearest() {
        a.setAbsolutePosition(460f, 500f);
        b.setAbsolutePosition(500f, 500f);
        HudGroup group = HudGroups.group(Arrays.asList(a, b));
        assertNotNull(group);

        HudGroups.setScale(group, 2f);
        float[] box = HudGroups.bounds(group.getHuds());
        assertNotNull(box);
        assertEquals(510f, box[1] + box[3], 0.001f, "a group along the bottom keeps its bottom edge");
        assertEquals(500f, box[0] + box[2] / 2f, 0.001f, "and stays centred");
    }

    @Test
    void aGroupIsOnlyLockedWhenEveryHudIs() {
        HudGroup group = HudGroups.group(Arrays.asList(a, b));
        assertNotNull(group);
        a.setLocked(true);
        assertFalse(group.getLocked(), "one locked HUD does not hold the group");

        Map<Hud, Pair<Float, Float>> starts = new HashMap<>();
        for (Hud hud : group.getHuds()) starts.put(hud, new Pair<>(hud.getX(), hud.getY()));
        HudGroups.moveFrom(group.getHuds(), starts, 10f, 0f);
        assertEquals(110f, a.getX(), 0.001f, "the locked HUD moves with its group");

        HudGroups.setLocked(group, true);
        assertTrue(group.getLocked());
        HudGroups.setLocked(group, false);
        assertFalse(a.getLocked(), "unlocking the group frees the HUD locked on its own too");
    }

    @Test
    void looseHudsScaleThroughTheSameCodeAsAGroup() {
        a.setCustomScale(4f);
        b.setCustomScale(2f);
        List<Hud> loose = Arrays.asList(a, b);
        assertEquals(4f, HudGroups.scaleOf(loose), 0.001f, "the biggest scale stands for the selection");
        assertFalse(HudGroups.hasUniformScale(loose));

        HudGroups.setScale(loose, 2f);
        assertEquals(2f, a.getCustomScale(), 0.001f);
        assertEquals(1f, b.getCustomScale(), 0.001f, "the ratio between them survives");

        HudGroups.matchScale(loose, 2f);
        assertEquals(2f, b.getCustomScale(), 0.001f);
        assertTrue(HudGroups.hasUniformScale(loose));
        assertEquals(1f, c.getCustomScale(), 0.001f, "a HUD outside the selection is left alone");
    }

    @Test
    void groupScaleCodeDelegatesToTheLooseOverloads() {
        a.setCustomScale(4f);
        b.setCustomScale(2f);
        HudGroup group = HudGroups.group(Arrays.asList(a, b));
        assertNotNull(group);

        assertEquals(HudGroups.scaleOf(group.getHuds()), group.getScale(), 0.001f);
        assertEquals(HudGroups.hasUniformScale(group.getHuds()), group.getUniformScale());
        assertEquals(HudGroups.scaleRange(group.getHuds()), HudGroups.scaleRange(group));
    }

    @Test
    void looseScaleRangeStopsAtTheFirstHudToReachALimit() {
        float max = 4f;
        a.setCustomScale(2f);
        List<Hud> loose = Arrays.asList(a, b);
        assertEquals(max, HudGroups.scaleRange(loose).getEndInclusive(), 0.001f, "the biggest HUD reaches the limit first");
        assertEquals(2f, HudGroups.scaleOf(loose), 0.001f);
        assertTrue(HudGroups.scaleRange(loose).getStart() < 2f);
    }

    @Test
    void looseHudsLockTogether() {
        List<Hud> loose = Arrays.asList(a, b);
        HudGroups.setLocked(loose, true);
        assertTrue(a.getLocked());
        assertTrue(b.getLocked());
        assertFalse(c.getLocked(), "a HUD outside the selection stays free");
        HudGroups.setLocked(loose, false);
        assertFalse(a.getLocked());
        assertFalse(b.getLocked());
    }

    @Test
    void hudsThatKeepTheirOwnStateStayOutOfGroups() {
        HudCompatRegressionTest.FakeBarWrapper wrapper = new HudCompatRegressionTest.FakeBarWrapper();
        wrapper.register();
        Hud compat = null;
        for (Hud h : HudManager.INSTANCE.getActiveInstances()) if (h instanceof LegacyHudMarker) compat = h;
        assertNotNull(compat);
        try {
            assertFalse(HudGroups.groupable(compat));
            assertTrue(HudGroups.groupable(a));
            assertNull(HudGroups.group(Arrays.asList(compat)), "nothing to group");
            HudGroup group = HudGroups.group(Arrays.asList(a, compat));
            assertNotNull(group);
            assertEquals("", compat.getGroupPath(), "its group could not be saved");
            assertEquals(Arrays.asList(a), group.getHuds());
            assertFalse(HudGroups.addTo(group, Arrays.asList(compat)));
            assertEquals("", compat.getGroupPath());
        } finally {
            HudManager.INSTANCE.getActiveInstances().remove(compat);
        }
    }

    @Test
    void copiesOfAWholeGroupFormANewGroup() {
        HudGroup inner = HudGroups.group(Arrays.asList(a, b));
        HudGroup outer = HudGroups.group(Collections.singletonList(c), Collections.singletonList(inner));
        assertNotNull(outer);
        String innerPath = a.getGroupPath();

        List<String> whole = HudGroups.pathsForCopies(Arrays.asList(a, b, c));
        assertNotEquals(innerPath, whole.get(0), "a fully copied group gets a fresh id");
        assertEquals(whole.get(0), whole.get(1), "the copies stay together");
        assertTrue(whole.get(0).startsWith(whole.get(2) + HudGroups.SEPARATOR), "and keep their nesting");
        assertFalse(whole.get(2).equals(outer.getPath()));

        List<String> partial = HudGroups.pathsForCopies(Collections.singletonList(a));
        assertEquals(innerPath, partial.get(0), "a single copy stays in its original group");

        List<String> innerOnly = HudGroups.pathsForCopies(Arrays.asList(a, b));
        assertTrue(innerOnly.get(0).startsWith(outer.getPath() + HudGroups.SEPARATOR));
        assertNotEquals(innerPath, innerOnly.get(0), "the copied sub-group is new but stays inside the outer group");
    }

    @Test
    void groupsSurviveARelaunch() throws Exception {
        HudGroup group = HudGroups.group(Arrays.asList(a, b));
        assertNotNull(group);
        ConfigManager.active().saveAll();

        relaunch();

        assertEquals(group, only(HudA.class).getGroup());
        assertEquals(group, only(HudB.class).getGroup());
        assertNull(only(HudC.class).getGroup());
    }

    @Test
    void defaultGroupsApplyOnceAndUngroupingSticks() throws Exception {
        relaunchBeforeDefaultsApplied();

        assertEquals(DEFAULT_ID, only(DefaultOne.class).getGroupPath(), "the default group is applied on load");
        assertEquals(DEFAULT_ID, only(DefaultTwo.class).getGroupPath());
        assertEquals("Defaults", only(DefaultOne.class).getGroup().getName());

        HudGroups.ungroup(only(DefaultOne.class).getGroup());
        ConfigManager.active().saveAll();
        relaunch();

        assertEquals("", only(DefaultOne.class).getGroupPath(), "an ungrouped default group stays ungrouped");
        assertEquals("", only(DefaultTwo.class).getGroupPath());
    }

    private static void setCollapsed(Hud hud, boolean collapsed) throws Exception {
        for (Method m : Hud.class.getDeclaredMethods()) {
            if (m.getName().startsWith("setCollapsed")) {
                m.invoke(hud, collapsed);
                return;
            }
        }
        throw new NoSuchMethodException("setCollapsed");
    }

    @Test
    void aCollapsedHudMeasuresNothingAndItsChildSitsAtItsEdge() throws Exception {
        Hud parent = only(CollapsingHud.class);
        parent.setRenderedW(40f);
        parent.setRenderedH(10f);
        parent.setAbsolutePosition(100f, 100f);
        a.anchorTo(parent, HudAnchor.TopRight, HudAnchor.TopLeft);
        a.setAnchorOffsetX(10f);
        a.setAnchorOffsetY(5f);
        try {
            assertEquals(150f, a.getX(), 0.001f);
            assertEquals(105f, a.getY(), 0.001f);

            setCollapsed(parent, true);
            assertTrue(parent.isCollapsed());
            assertEquals(0f, parent.getScaledWidth(), 0f);
            assertEquals(0f, parent.getScaledHeight(), 0f);
            assertEquals(parent.getX(), a.getX(), 0.001f, "the offset is dropped so no gap is left");
            assertEquals(parent.getY() + 5f, a.getY(), 0.001f, "the offset across the stack is kept");

            setCollapsed(parent, false);
            assertEquals(150f, a.getX(), 0.001f);
            assertEquals(105f, a.getY(), 0.001f);
        } finally {
            setCollapsed(parent, false);
        }
    }

    private static void relaunchBeforeDefaultsApplied() throws Exception {
        ((Set<?>) get("knownGroups")).remove(DEFAULT_ID);
        only(DefaultOne.class).setGroupPath("");
        only(DefaultTwo.class).setGroupPath("");
        Method save = HudManager.class.getDeclaredMethod("saveRegistry");
        save.setAccessible(true);
        save.invoke(HudManager.INSTANCE);
        ConfigManager.active().saveAll();
        relaunch();
    }

    private static void removeAll(Class<? extends Hud> cls) {
        for (Hud hud : HudManager.INSTANCE.getHudsOfType(cls)) HudManager.INSTANCE.removeHud(hud, true);
    }

    @Test
    void aPresetAddsOneNamedGroupAtTheDefaultPositions() throws Exception {
        try {
            HudGroup group = HudGroups.addPreset(preset);
            assertNotNull(group);
            assertTrue(group.getPath().startsWith(PRESET_ID + "#"));
            assertEquals("Preset", group.getName());
            assertEquals(2, group.getHuds().size());
            Hud single = only(PresetSingle.class);
            Hud multi = only(PresetMulti.class);
            assertEquals(100f, single.getX(), 0.001f);
            assertEquals(50f, single.getY(), 0.001f);
            assertEquals(200f, multi.getX(), 0.001f);

            ConfigManager.active().saveAll();
            relaunch();

            HudGroup restored = only(PresetSingle.class).getGroup();
            assertEquals(group, restored);
            assertEquals(group, only(PresetMulti.class).getGroup());
            assertEquals("Preset", restored.getName(), "the name comes back from the preset after a restart");
        } finally {
            removeAll(PresetSingle.class);
            removeAll(PresetMulti.class);
        }
    }

    @Test
    void addingAPresetRepeatedlyMakesSeparateGroupsThatDoNotOverlap() {
        try {
            List<HudGroup> groups = new ArrayList<>();
            List<float[]> boxes = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                HudGroup group = HudGroups.addPreset(multiPreset);
                assertNotNull(group);
                assertFalse(groups.contains(group));
                groups.add(group);
                float[] box = HudGroups.bounds(group.getHuds());
                assertNotNull(box);
                boxes.add(box);
            }
            for (int i = 0; i < boxes.size(); i++) {
                for (int j = i + 1; j < boxes.size(); j++) {
                    float[] a = boxes.get(i);
                    float[] b = boxes.get(j);
                    boolean overlap = a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3];
                    assertFalse(overlap, "copy " + j + " covers copy " + i);
                }
            }
        } finally {
            removeAll(PresetMulti.class);
            removeAll(PresetMultiB.class);
        }
    }

    @Test
    void offScreenPresetCopiesAreClampedWithoutOverlapping() {
        try {
            float[] first = HudGroups.bounds(HudGroups.addPreset(offPreset).getHuds());
            float[] second = HudGroups.bounds(HudGroups.addPreset(offPreset).getHuds());
            assertTrue(first[0] + first[2] <= 960f && second[0] + second[2] <= 960f, "both copies are on screen");
            boolean overlap = first[0] < second[0] + second[2] && second[0] < first[0] + first[2]
                && first[1] < second[1] + second[3] && second[1] < first[1] + first[3];
            assertFalse(overlap, "the second copy covers the first");
        } finally {
            removeAll(OffOne.class);
            removeAll(OffTwo.class);
        }
    }

    @Test
    void aPresetCopyInsideAnotherGroupIsNotCovered() {
        try {
            HudGroup first = HudGroups.addPreset(multiPreset);
            HudGroup parent = HudGroups.group(Collections.emptyList(), Collections.singletonList(first));
            assertNotNull(parent);
            float[] nested = HudGroups.bounds(parent.getHuds());
            float[] second = HudGroups.bounds(HudGroups.addPreset(multiPreset).getHuds());
            boolean overlap = nested[0] < second[0] + second[2] && second[0] < nested[0] + nested[2]
                && nested[1] < second[1] + second[3] && second[1] < nested[1] + nested[3];
            assertFalse(overlap, "the second copy covers the nested one");
        } finally {
            removeAll(PresetMulti.class);
            removeAll(PresetMultiB.class);
        }
    }

    @Test
    void aPresetWhoseMemberFailsSetupLeavesNothingBehind() throws Exception {
        String treeId = "huds/group-undeletable";
        int removals = Undeletable.removals;
        assertNull(HudGroups.addPreset(brokenPreset));
        assertEquals(removals + 1, Undeletable.removals, "a member that was set up is removed again");
        assertFalse(HudManager.INSTANCE.hasHudOfType(Undeletable.class));
        assertFalse(HudManager.INSTANCE.hasHudOfType(BrokenSetup.class));
        assertFalse(HudManager.INSTANCE.getProvider(Undeletable.class).isReal());
        assertFalse(ConfigManager.active().exists(treeId));
        assertNull(ConfigManager.active().load(treeId), "no config file is written");
        assertFalse(((Set<?>) get("knownProviders")).contains(Undeletable.class.getName()));
        assertTrue(HudGroups.canAdd(brokenPreset));
    }

    @Test
    void aPresetWithAMissingMemberCannotBeAdded() {
        assertFalse(HudGroups.canAdd(partialPreset));
        assertNull(HudGroups.addPreset(partialPreset));
    }

    @Test
    void aFailedPresetKeepsTheStoredConfigOfASingleInstanceMember() {
        Hud single = HudManager.INSTANCE.getProvider(PresetSingle.class).make(null);
        String treeId = single.getTree().getID();
        try {
            HudManager.addInstance(single);
            ConfigManager.active().save(treeId);
            ConfigManager.active().unregister(treeId);
            HudManager.INSTANCE.removeHud(single, false);
            assertNotNull(ConfigManager.active().load(treeId));

            assertNull(HudGroups.addPreset(preset, hud -> { throw new IllegalStateException("measure"); }));
            assertNotNull(ConfigManager.active().load(treeId), "the stored config survives the rollback");
            assertFalse(HudManager.INSTANCE.hasHudOfType(PresetSingle.class));
            assertFalse(HudManager.INSTANCE.hasHudOfType(PresetMulti.class));
        } finally {
            removeAll(PresetSingle.class);
            removeAll(PresetMulti.class);
            ConfigManager.active().get(treeId);
            ConfigManager.active().delete(treeId);
        }
    }

    @Test
    void aPresetMemberWithStoredConfigIsSetUpWithItsStoredValues() {
        Hud single = HudManager.INSTANCE.getProvider(PresetSingle.class).make(null);
        String treeId = single.getTree().getID();
        try {
            HudManager.addInstance(single);
            single.setTextScale(2f);
            ConfigManager.active().save(treeId);
            ConfigManager.active().unregister(treeId);
            HudManager.INSTANCE.removeHud(single, false);
            single.setTextScale(1f);

            assertNotNull(HudGroups.addPreset(preset));
            assertEquals(2f, PresetSingle.textScaleInSetup);
            assertEquals(2f, only(PresetSingle.class).getTextScale());
        } finally {
            removeAll(PresetSingle.class);
            removeAll(PresetMulti.class);
            ConfigManager.active().get(treeId);
            ConfigManager.active().delete(treeId);
        }
    }

    @Test
    void copiesOfADefaultGroupKeepItsPreset() {
        Hud one = only(DefaultOne.class);
        Hud two = only(DefaultTwo.class);
        one.setGroupPath(DEFAULT_ID);
        two.setGroupPath(DEFAULT_ID);
        List<String> paths = HudGroups.pathsForCopies(Arrays.asList(one, two));
        assertTrue(paths.get(0).startsWith(DEFAULT_ID + HudGroups.PRESET_SEPARATOR), paths.get(0));
        assertEquals("Defaults", new HudGroup(paths.get(0)).getName());
    }

    @Test
    void aPresetIsUnavailableWhileASingleInstanceMemberIsPlaced() {
        try {
            assertTrue(HudGroups.canAdd(preset));
            Hud single = HudManager.INSTANCE.getProvider(PresetSingle.class).make(null);
            HudManager.addInstance(single);

            assertFalse(HudGroups.canAdd(preset));
            int before = HudManager.INSTANCE.getActiveInstances().size();
            assertNull(HudGroups.addPreset(preset));
            assertEquals(before, HudManager.INSTANCE.getActiveInstances().size());
            assertEquals("", single.getGroupPath(), "a HUD already on screen is never pulled into the preset's group");

            HudManager.INSTANCE.removeHud(single, true);
            assertTrue(HudGroups.canAdd(preset));
        } finally {
            removeAll(PresetSingle.class);
            removeAll(PresetMulti.class);
        }
    }

    @Test
    void aGroupIsMadeFromAPresetByItsIdOrTheIdBeforeTheSeparator() {
        HudPreset recording = HudGroups.presets().stream().filter(p -> p.getId().equals("test.recording")).findFirst().get();
        assertEquals(recording, HudGroups.presetOf(new HudGroup("test.recording")));
        assertEquals(recording, HudGroups.presetOf(new HudGroup("outer/test.recording#x1")));
        assertNull(HudGroups.presetOf(new HudGroup("test.recordingx")));
        assertNull(HudGroups.presetOf(new HudGroup("other#test.recording")));
    }

    @Test
    void aPresetActionOnlyReceivesThePresetMembersInMemberOrder() {
        a.setGroupPath("test.recording#x1");
        b.setGroupPath("test.recording#x1");
        c.setGroupPath("test.recording#x1");
        HudGroup group = a.getGroup();
        HudPreset preset = HudGroups.presetOf(group);
        assertNotNull(preset);
        assertEquals(Arrays.asList(b, a), HudGroups.presetMembers(group, preset));

        HudGroups.runPresetAction(group, preset.getActions().get(0));

        assertEquals(Arrays.asList(b, a), received);
    }

    @Test
    void aPresetActionGetsNullForAMissingMemberAndOnlyTheFirstHudOfEachClass() {
        Hud second = HudManager.INSTANCE.getProvider(HudA.class).make(null);
        HudManager.addInstance(second);
        try {
            a.setGroupPath("test.recording#x1");
            second.setGroupPath("test.recording#x1");
            HudGroup group = a.getGroup();
            HudPreset preset = HudGroups.presetOf(group);
            assertNotNull(preset);

            HudGroups.runPresetAction(group, preset.getActions().get(0));

            assertEquals(Arrays.asList(null, a), received);
            assertEquals(Collections.singletonList(a), HudGroups.presetMembers(group, preset));
        } finally {
            HudManager.INSTANCE.removeHud(second, true);
        }
    }

    @Test
    void aMemberTypeTakesItsExactClassBeforeASubclassInstance() {
        Hud base = HudManager.INSTANCE.getProvider(SlotBase.class).make(null);
        Hud derived = HudManager.INSTANCE.getProvider(SlotDerived.class).make(null);
        HudManager.addInstance(derived);
        HudManager.addInstance(base);
        try {
            derived.setGroupPath("test.slots#x1");
            base.setGroupPath("test.slots#x1");
            HudGroup group = base.getGroup();
            HudGroups.runPresetAction(group, HudGroups.presetOf(group).getActions().get(0));

            assertEquals(Arrays.asList(base, derived), received);
        } finally {
            HudManager.INSTANCE.removeHud(base, true);
            HudManager.INSTANCE.removeHud(derived, true);
        }
    }

    @Test
    void aPresetsDefaultActionRunsWhenItIsAdded() {
        try {
            HudGroup group = HudGroups.addPreset(HudGroups.presets().stream().filter(p -> p.getId().equals("test.arranged")).findFirst().get());
            assertNotNull(group);
            assertSame(only(ArrangeTwo.class), only(ArrangeOne.class).getAnchorParent());
        } finally {
            removeAll(ArrangeOne.class);
            removeAll(ArrangeTwo.class);
        }
    }

    @Test
    void aFailingDefaultActionDoesNotStopThePresetBeingAdded() {
        try {
            HudGroup group = HudGroups.addPreset(HudGroups.presets().stream().filter(p -> p.getId().equals("test.throwing")).findFirst().get());
            assertNotNull(group);
            assertEquals(2, group.getHuds().size());
            assertNull(only(ArrangeOne.class).getAnchorParent());
        } finally {
            removeAll(ArrangeOne.class);
            removeAll(ArrangeTwo.class);
        }
    }

    @Test
    void aDefaultGroupsPresetActionRunsOnceUnlessTheMembersAreAlreadyAnchored() throws Exception {
        Hud one = only(DefaultOne.class);
        Hud two = only(DefaultTwo.class);
        one.clearAnchor();
        two.clearAnchor();
        relaunchBeforeDefaultsApplied();
        assertSame(only(DefaultOne.class), only(DefaultTwo.class).getAnchorParent());

        only(DefaultOne.class).setGroupPath("");
        only(DefaultTwo.class).setGroupPath("");
        only(DefaultTwo.class).clearAnchor();
        only(DefaultOne.class).anchorTo(only(DefaultTwo.class), HudAnchor.Right, HudAnchor.Left);
        relaunchBeforeDefaultsApplied();

        assertSame(only(DefaultTwo.class), only(DefaultOne.class).getAnchorParent(), "the user's own anchor is kept");
        assertNull(only(DefaultTwo.class).getAnchorParent());

        only(DefaultOne.class).setGroupPath("");
        only(DefaultTwo.class).setGroupPath("");
        only(DefaultOne.class).clearAnchor();
        only(DefaultTwo.class).clearAnchor();
        only(DefaultTwo.class).anchorTo(only(HudC.class), HudAnchor.Right, HudAnchor.Left);
        relaunchBeforeDefaultsApplied();

        assertSame(only(HudC.class), only(DefaultTwo.class).getAnchorParent(), "an anchor to any other HUD also stops the action");
        assertNull(only(DefaultOne.class).getAnchorParent());
    }

    private static void updateBackgroundGroups(Hud... huds) throws Exception {
        Method update = HudManager.class.getDeclaredMethod("updateBackgroundGroups", List.class, float.class, float.class, float.class);
        update.setAccessible(true);
        update.invoke(HudManager.INSTANCE, Arrays.asList(huds), 960f, 540f, 1f);
    }

    private static void forgetFrame() throws Exception {
        set("lastMergeKey", null);
        set("frameGroups", Collections.emptyList());
        set("mergeCheckHuds", new Hud[0]);
        set("mergeCheckStamps", new int[0]);
        ((Map<?, ?>) get("frameDrawn")).clear();
    }

    @Test
    void aGroupsSingleBackgroundSurvivesARelaunchAndEndsWhenAHudLeaves() throws Exception {
        HudGroup group = HudGroups.group(Arrays.asList(a, b));
        assertNotNull(group);
        assertFalse(HudGroups.hasSingleBackground(group));
        HudGroups.setSingleBackground(group, true);
        assertTrue(HudGroups.hasSingleBackground(group));
        ConfigManager.active().saveAll();

        relaunch();

        HudGroup restored = only(HudA.class).getGroup();
        assertTrue(HudGroups.hasSingleBackground(restored));

        HudGroups.removeFromGroup(only(HudB.class));
        assertEquals("", only(HudB.class).getGroupBackground());
        assertEquals(group.getId(), only(HudA.class).getGroupBackground());

        HudGroups.ungroup(only(HudA.class).getGroup());
        assertEquals("", only(HudA.class).getGroupBackground());
    }

    @Test
    void copiesOfAGroupKeepItsSingleBackgroundUnderTheNewId() {
        HudGroup group = HudGroups.group(Arrays.asList(a, b));
        assertNotNull(group);
        HudGroups.setSingleBackground(group, true);
        List<Hud> originals = Arrays.asList(a, b);
        List<String> paths = HudGroups.pathsForCopies(originals);
        List<Hud> copies = Arrays.asList(a.clone(), b.clone());
        for (int i = 0; i < copies.size(); i++) copies.get(i).setGroupPath(paths.get(i));

        HudGroups.remapCopies(originals, copies);

        HudGroup copied = copies.get(0).getGroup();
        assertNotEquals(group, copied);
        assertEquals(copied.getId(), copies.get(0).getGroupBackground());
        assertEquals(copied.getId(), copies.get(1).getGroupBackground());
        assertEquals(group.getId(), a.getGroupBackground());
    }

    @Test
    void membersOfASingleBackgroundAreDrawnByOneConfigButNotFused() throws Exception {
        HudGroup group = HudGroups.group(Arrays.asList(a, b));
        assertNotNull(group);
        HudGroups.setSingleBackground(group, true);
        HudManager.isConfigUiOpen = true;
        try {
            updateBackgroundGroups(a, b, c);

            assertTrue(a.getBgMerged());
            assertTrue(b.getBgMerged());
            assertFalse(a.getBgFused());
            assertFalse(b.getBgFused());
            assertFalse(c.getBgMerged());
        } finally {
            HudManager.isConfigUiOpen = false;
            forgetFrame();
        }
    }

    @Test
    void theOutermostSingleBackgroundWinsAndTurningItOffLeavesTheInnerOneEmpty() {
        HudGroup outer = HudGroups.group(Collections.singletonList(c), Collections.singletonList(HudGroups.group(Arrays.asList(a, b))));
        assertNotNull(outer);
        HudGroup inner = a.getGroup();
        HudGroups.setSingleBackground(inner, true);
        assertTrue(HudGroups.hasSingleBackground(inner));
        assertFalse(HudGroups.isBackgroundTakenOver(inner));

        HudGroups.setSingleBackground(outer, true);

        assertEquals(outer.getId(), a.getGroupBackground());
        assertEquals(outer.getId(), c.getGroupBackground());
        assertTrue(HudGroups.isBackgroundTakenOver(inner));
        assertTrue(HudGroups.hasSingleBackground(inner));

        HudGroups.setSingleBackground(inner, true);
        HudGroups.setSingleBackground(inner, false);
        assertEquals(outer.getId(), a.getGroupBackground(), "the inner group cannot change what the outer one draws");

        HudGroups.setSingleBackground(outer, false);
        assertEquals("", a.getGroupBackground());
        assertEquals("", c.getGroupBackground());
        assertFalse(HudGroups.hasSingleBackground(inner));
    }

    @Test
    void turningAnOuterBackgroundOffKeepsAnInnerOneItDidNotOwn() {
        HudGroup outer = HudGroups.group(Collections.singletonList(c), Collections.singletonList(HudGroups.group(Arrays.asList(a, b))));
        assertNotNull(outer);
        HudGroup inner = a.getGroup();
        HudGroups.setSingleBackground(inner, true);
        c.setGroupBackground(outer.getId());

        HudGroups.setSingleBackground(outer, false);

        assertEquals(inner.getId(), a.getGroupBackground());
        assertEquals("", c.getGroupBackground());
    }

    @Test
    void aHudJoiningAGroupWithASingleBackgroundDrawsOnIt() {
        HudGroup group = HudGroups.group(Arrays.asList(a, b));
        assertNotNull(group);
        HudGroups.setSingleBackground(group, true);

        assertTrue(HudGroups.addTo(group, Collections.singletonList(c)));

        assertEquals(group.getId(), c.getGroupBackground());
        assertTrue(HudGroups.hasSingleBackground(group));

        HudGroups.removeFromGroup(c);
        assertEquals("", c.getGroupBackground());
    }

    @Test
    void aHudGroupedIntoAGroupWithASingleBackgroundTakesItOver() {
        HudGroup outer = HudGroups.group(Collections.singletonList(c), Collections.singletonList(HudGroups.group(Arrays.asList(a, b))));
        assertNotNull(outer);
        HudGroup inner = a.getGroup();
        HudGroups.setSingleBackground(outer, true);
        Hud extra = HudManager.INSTANCE.getProvider(HudA.class).make(null);
        HudManager.INSTANCE.getActiveInstances().add(extra);
        HudManager.INSTANCE.getInstancesRevision().setIntValue(HudManager.INSTANCE.getInstancesRevision().getIntValue() + 1);
        try {
            assertTrue(HudGroups.addTo(inner, Collections.singletonList(extra)));
            assertEquals(outer.getId(), extra.getGroupBackground());
        } finally {
            HudManager.INSTANCE.removeHud(extra, true);
        }
    }

    @Test
    void aHiddenHudThatKeepsItsBackgroundStaysInTheGroupBackground() throws Exception {
        HudGroup group = HudGroups.group(Arrays.asList(a, b));
        assertNotNull(group);
        HudGroups.setSingleBackground(group, true);
        b.setHidden(true);
        b.setKeepBgWhenHidden(true);
        HudManager.isConfigUiOpen = true;
        try {
            updateBackgroundGroups(a, b);
            assertTrue(b.getBgMerged());

            b.setKeepBgWhenHidden(false);
            forgetFrame();
            updateBackgroundGroups(a, b);
            assertFalse(b.getBgMerged());
        } finally {
            b.setHidden(false);
            b.setKeepBgWhenHidden(false);
            HudManager.isConfigUiOpen = false;
            forgetFrame();
        }
    }

    @Test
    void aBorderedHudStaysOutOfTheGroupBackgroundAndDrawsItsOwn() throws Exception {
        HudGroup group = HudGroups.group(Arrays.asList(a, b));
        assertNotNull(group);
        HudGroups.setSingleBackground(group, true);
        a.setBorderEnabled(true);
        HudManager.isConfigUiOpen = true;
        try {
            updateBackgroundGroups(a, b);

            assertFalse(a.getBgMerged());
            assertTrue(b.getBgMerged());
        } finally {
            a.setBorderEnabled(false);
            HudManager.isConfigUiOpen = false;
            forgetFrame();
        }
    }
}
