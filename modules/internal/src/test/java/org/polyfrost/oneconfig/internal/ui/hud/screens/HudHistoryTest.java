/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.internal.ui.hud.screens;

import kotlin.Pair;
import kotlin.Unit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.hud.v1.Hud;
import org.polyfrost.oneconfig.api.hud.v1.HudAnchor;
import org.polyfrost.oneconfig.api.hud.v1.HudGroups;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import org.polyfrost.oneconfig.api.hud.v1.HudPresetAction;
import org.polyfrost.oneconfig.api.hud.v1.TextHud;
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.SharedAmount;
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.SharedColor;
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.SharedOptions;
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.SharedProperty;
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.SharedToggle;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudHistoryTest {
    private final TestHud provider = new TestHud();
    private final HudHistory history = new HudHistory();

    @BeforeEach
    void setUp() {
        ConfigManager.active();
        HudManager.INSTANCE.getActiveInstances().clear();
        HudManager.register(provider);
    }

    @AfterEach
    void tearDown() {
        HudManager.INSTANCE.unregister(provider, true, true);
        HudManager.INSTANCE.getActiveInstances().clear();
    }

    @Test
    void nothingToUndoOnAFreshLayout() {
        place();
        history.poll(false);

        assertNull(history.undo());
        assertNull(history.redo());
    }

    @Test
    void moveIsUndoneAndRedone() {
        Hud hud = place();
        history.poll(false);

        hud.setAbsolutePosition(200f, 120f);
        settle();

        assertNotNull(history.undo());
        assertEquals(10f, hud.getX(), 0.01f);
        assertEquals(10f, hud.getY(), 0.01f);

        assertNotNull(history.redo());
        assertEquals(200f, hud.getX(), 0.01f);
        assertEquals(120f, hud.getY(), 0.01f);
    }

    @Test
    void optionChangeIsUndone() {
        Hud hud = place();
        history.poll(false);

        hud.setTextColor(0xFF112233);
        hud.setHidden(true);
        settle();

        assertNotNull(history.undo());
        assertEquals(0xFFFFFFFF, hud.getTextColor());
        assertEquals(false, hud.getHidden());
    }

    @Test
    void changeNotYetSettledIsStillUndone() {
        Hud hud = place();
        history.poll(false);

        hud.setAbsolutePosition(200f, 120f);

        assertNotNull(history.undo());
        assertEquals(10f, hud.getX(), 0.01f);
    }

    @Test
    void gestureInProgressIsOneStep() {
        Hud hud = place();
        history.poll(false);

        hud.setAbsolutePosition(100f, 60f);
        history.poll(true);
        history.poll(true);
        hud.setAbsolutePosition(200f, 120f);
        history.poll(true);
        settle();

        assertNotNull(history.undo());
        assertEquals(10f, hud.getX(), 0.01f);
        assertNull(history.undo());
    }

    @Test
    void newChangeDropsTheRedoStack() {
        Hud hud = place();
        history.poll(false);

        hud.setAbsolutePosition(200f, 120f);
        settle();
        assertNotNull(history.undo());

        hud.setAbsolutePosition(300f, 200f);
        settle();

        assertNull(history.redo());
        assertEquals(300f, hud.getX(), 0.01f);
    }

    @Test
    void deletedHudComesBackWithItsStateAndIsDeletedAgainOnRedo() {
        Hud hud = place();
        hud.setAbsolutePosition(200f, 120f);
        hud.setTextColor(0xFF112233);
        String id = hud.getTree().getID();
        history.poll(false);

        HudManager.INSTANCE.removeHud(hud, true);
        settle();
        assertNull(HudManager.INSTANCE.instanceById(id));

        assertEquals(1, history.undo().size());
        Hud revived = HudManager.INSTANCE.instanceById(id);
        assertNotNull(revived);
        assertTrue(HudManager.INSTANCE.getActiveInstances().contains(revived));
        assertEquals(200f, revived.getX(), 0.01f);
        assertEquals(120f, revived.getY(), 0.01f);
        assertEquals(0xFF112233, revived.getTextColor());

        assertNotNull(history.redo());
        assertNull(HudManager.INSTANCE.instanceById(id));
    }

    @Test
    void addedHudIsRemovedOnUndoAndComesBackOnRedo() {
        history.poll(false);

        String id = place().getTree().getID();
        settle();

        assertNotNull(history.undo());
        assertNull(HudManager.INSTANCE.instanceById(id));

        assertEquals(1, history.redo().size());
        assertNotNull(HudManager.INSTANCE.instanceById(id));
    }

    @Test
    void gestureStartedBeforeTheLastSettledIsItsOwnStep() {
        Hud hud = place();
        history.poll(false);

        hud.setAbsolutePosition(100f, 60f);
        history.poll(true);
        history.poll(false);
        hud.setAbsolutePosition(200f, 120f);
        history.poll(true);
        history.poll(true);
        settle();

        assertNotNull(history.undo());
        assertEquals(100f, hud.getX(), 0.01f);
        assertNotNull(history.undo());
        assertEquals(10f, hud.getX(), 0.01f);
    }

    @Test
    void oneEditToSeveralHudsIsOneStep() {
        Hud first = place();
        Hud second = place();
        List<Hud> both = Arrays.asList(first, second);
        float padding = first.getPadLeft();
        boolean background = first.getShowBackground();
        history.poll(false);

        ((SharedToggle) SharedOptions.find("showBackground")).apply(both, !background);
        ((SharedAmount) SharedOptions.find("padLeft")).apply(both, 6f);
        ((SharedColor) SharedOptions.find("textColor")).apply(both, 0xFF112233);
        settle();

        assertNotNull(history.undo());
        for (Hud hud : both) {
            assertEquals(background, hud.getShowBackground());
            assertEquals(padding, hud.getPadLeft(), 0.001f);
            assertEquals(0xFFFFFFFF, hud.getTextColor());
        }
        assertNull(history.undo(), "every HUD went back in the one step");

        assertNotNull(history.redo());
        for (Hud hud : both) {
            assertEquals(!background, hud.getShowBackground());
            assertEquals(6f, hud.getPadLeft(), 0.001f);
            assertEquals(0xFF112233, hud.getTextColor());
        }
        assertNull(history.redo());
    }

    @Test
    void oneEditThroughASharedPropertyIsOneStep() {
        Hud first = place();
        Hud second = place();
        List<Hud> both = Arrays.asList(first, second);
        history.poll(false);
        List<Property<?>> props = Arrays.asList(first.getTree().getProp("brackets"), second.getTree().getProp("brackets"));
        Property<Object> proxy = new SharedProperty("brackets", props).proxy(() -> Unit.INSTANCE);

        proxy.set(true);
        settle();

        for (Hud hud : both) assertEquals(true, ((TextHud) hud).getBrackets());
        assertNotNull(history.undo());
        for (Hud hud : both) assertEquals(false, ((TextHud) hud).getBrackets());
        assertNull(history.undo(), "both HUDs went back in the one step");
    }

    @Test
    void undoAllCanBeRedoneOneStepAtATime() {
        Hud hud = place();
        history.poll(false);

        hud.setAbsolutePosition(100f, 60f);
        settle();
        hud.setAbsolutePosition(200f, 120f);
        settle();

        assertNotNull(history.undoAll());
        assertEquals(10f, hud.getX(), 0.01f);
        assertNull(history.undo());

        assertNotNull(history.redo());
        assertEquals(100f, hud.getX(), 0.01f);
        assertNotNull(history.redo());
        assertEquals(200f, hud.getX(), 0.01f);
        assertNull(history.redo());
    }

    @Test
    void aPresetActionOverSeveralHudsIsOneStep() {
        SecondHud secondProvider = new SecondHud();
        ThirdHud thirdProvider = new ThirdHud();
        HudManager.register(secondProvider, thirdProvider);
        HudGroups.registerPreset("test.history.preset", "History", Arrays.asList(TestHud.class, SecondHud.class, ThirdHud.class), null, null, null,
            Collections.singletonList(new HudPresetAction("chain", "Chain", huds -> {
                for (int i = 1; i < huds.size(); i++) huds.get(i).anchorTo(huds.get(i - 1), HudAnchor.BottomLeft, HudAnchor.TopLeft);
            })), null);
        try {
            List<Hud> huds = Arrays.asList(place(provider), place(secondProvider), place(thirdProvider));
            for (Hud hud : huds) hud.setGroupPath("test.history.preset#one");
            history.poll(false);

            HudGroups.runPresetAction(huds.get(0).getGroup(), HudGroups.presetOf(huds.get(0).getGroup()).getActions().get(0));
            settle();

            assertSame(huds.get(0), huds.get(1).getAnchorParent());
            assertSame(huds.get(1), huds.get(2).getAnchorParent());
            assertNotNull(history.undo());
            assertNull(huds.get(1).getAnchorParent());
            assertNull(huds.get(2).getAnchorParent());
            assertNull(history.undo(), "the whole arrangement went back in the one step");
        } finally {
            HudGroups.unregisterPreset("test.history.preset");
            HudManager.INSTANCE.unregister(secondProvider, true, true);
            HudManager.INSTANCE.unregister(thirdProvider, true, true);
        }
    }

    private void settle() {
        history.poll(false);
        history.poll(false);
    }

    private Hud place() {
        return place(provider);
    }

    private Hud place(Hud from) {
        Hud hud = from.make(null);
        hud.setAbsolutePosition(10f, 10f);
        HudManager.INSTANCE.getActiveInstances().add(hud);
        return hud;
    }

    static class SecondHud extends BaseHud {
        SecondHud() { super("test-history-second"); }
    }

    static class ThirdHud extends BaseHud {
        ThirdHud() { super("test-history-third"); }
    }

    static class TestHud extends BaseHud {
        TestHud() { super("test-history"); }
    }

    static abstract class BaseHud extends TextHud {
        BaseHud(String id) {
            super(id, "Test History HUD", Hud.Category.getINFO(), "", "");
        }

        @Override
        public Pair<Float, Float> defaultPosition() {
            return new Pair<>(10f, 10f);
        }

        @Override
        public String getText() {
            return "test";
        }
    }
}
