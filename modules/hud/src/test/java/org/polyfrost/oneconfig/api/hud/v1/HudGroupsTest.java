package org.polyfrost.oneconfig.api.hud.v1;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kotlin.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private static final String DEFAULT_ID = "test.defaults";
    private static boolean registered = false;

    private Hud a;
    private Hud b;
    private Hud c;

    @BeforeEach
    void setUp() throws Exception {
        ConfigManager.active();
        ConfigManager.openProfile("");
        if (!registered) {
            HudManager.register(new HudA(), new HudB(), new HudC(), new DefaultOne(), new DefaultTwo());
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
        // b hangs off c which hangs off a, and only a and b are grouped
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

        for (Hud hud : group.getHuds()) assertEquals(2f, hud.getCustomScale(), 0.001f);
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
        a.setCustomScale(2f);
        HudGroups.Scaling scaling = HudGroups.scaling(Arrays.asList(a, b), 100f, 100f);
        assertEquals(2f, scaling.getMaxFactor(), 0.001f, "a is already at 2x so the group can only double");
        assertEquals(2f, scaling.apply(10f), 0.001f);
        assertEquals(4f, a.getCustomScale(), 0.001f);
        assertEquals(2f, b.getCustomScale(), 0.001f);
        assertEquals(0.25f, scaling.getMinFactor(), 0.001f);
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
        // start as if the group had never been applied, earlier runs may have saved it as known
        ((Set<?>) get("knownGroups")).remove(DEFAULT_ID);
        only(DefaultOne.class).setGroupPath("");
        only(DefaultTwo.class).setGroupPath("");
        Method save = HudManager.class.getDeclaredMethod("saveRegistry");
        save.setAccessible(true);
        save.invoke(HudManager.INSTANCE);
        ConfigManager.active().saveAll();
        relaunch();

        assertEquals(DEFAULT_ID, only(DefaultOne.class).getGroupPath(), "the default group is applied on load");
        assertEquals(DEFAULT_ID, only(DefaultTwo.class).getGroupPath());
        assertEquals("Defaults", only(DefaultOne.class).getGroup().getName());

        HudGroups.ungroup(only(DefaultOne.class).getGroup());
        ConfigManager.active().saveAll();
        relaunch();

        assertEquals("", only(DefaultOne.class).getGroupPath(), "an ungrouped default group stays ungrouped");
        assertEquals("", only(DefaultTwo.class).getGroupPath());
    }
}
