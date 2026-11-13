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

import java.util.Arrays;
import java.util.List;
import kotlin.Pair;
import kotlin.Unit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.hud.v1.Hud;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import org.polyfrost.oneconfig.api.hud.v1.TextHud;
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.SharedAmount;
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.SharedColor;
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.SharedOptions;
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.SharedProperty;
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.SharedToggle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private void settle() {
        history.poll(false);
        history.poll(false);
    }

    private Hud place() {
        Hud hud = provider.make(null);
        hud.setAbsolutePosition(10f, 10f);
        HudManager.INSTANCE.getActiveInstances().add(hud);
        return hud;
    }

    static class TestHud extends TextHud {
        TestHud() {
            super("test-history", "Test History HUD", Hud.Category.getINFO(), "", "");
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
