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

package org.polyfrost.oneconfig.api.ui.v1;

import androidx.compose.runtime.snapshots.MutableSnapshot;
import androidx.compose.runtime.snapshots.ObserverHandle;
import androidx.compose.runtime.snapshots.Snapshot;
import kotlin.Unit;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModCardTypesTest {
    @Test
    void assignmentSurvivesUntilTheTypeRegisters() {
        ModCardTypes.assign("mct-pending-mod", "mct.pending");
        assertNull(ModCardTypes.typeFor("mct-pending-mod"));

        ModCardType type = new ModCardType("mct.pending", "Pending");
        ModCardTypes.register(type);

        assertSame(type, ModCardTypes.typeFor("mct-pending-mod"));
    }

    @Test
    void registeringTheSameIdReplacesTheType() {
        ModCardTypes.register(new ModCardType("mct.replaced", "First"));
        ModCardType second = new ModCardType("mct.replaced", "Second", "star", 5);
        ModCardTypes.register(second);

        assertSame(second, ModCardTypes.byId("mct.replaced"));
        assertEquals(1, ModCardTypes.types().stream().filter(t -> t.getId().equals("mct.replaced")).count());
    }

    @Test
    void assignmentsToleratePathAndJsonSuffixedCardIds() {
        ModCardTypes.register(new ModCardType("mct.lenient", "Lenient"));
        ModCardTypes.assign("MCT-Lenient-Mod", "mct.lenient");

        assertNotNull(ModCardTypes.typeFor("mct-lenient-mod"));
        assertNotNull(ModCardTypes.typeFor("mct-lenient-mod.json"));
        assertNotNull(ModCardTypes.typeFor("mct-lenient-mod/main.json"));
        assertNull(ModCardTypes.typeFor("mct-lenient-mod-other"));
    }

    @Test
    void assignAllTypesEveryGivenMod() {
        ModCardTypes.register(new ModCardType("mct.bulk", "Bulk"));
        ModCardTypes.assignAll("mct.bulk", "mct-bulk-a", "mct-bulk-b");

        assertNotNull(ModCardTypes.typeFor("mct-bulk-a"));
        assertNotNull(ModCardTypes.typeFor("mct-bulk-b"));
    }

    @Test
    void explicitAssignmentBeatsAResolver() {
        ModCardTypes.register(new ModCardType("mct.explicit", "Explicit"));
        ModCardTypes.register(new ModCardType("mct.resolved", "Resolved"));
        ModCardTypes.registerResolver(id -> id.startsWith("mct-precedence") ? "mct.resolved" : null);
        ModCardTypes.assign("mct-precedence-mod", "mct.explicit");

        assertEquals("mct.explicit", ModCardTypes.typeFor("mct-precedence-mod").getId());
        assertEquals("mct.resolved", ModCardTypes.typeFor("mct-precedence-other").getId());
    }

    @Test
    void aThrowingResolverIsDroppedRatherThanPropagated() {
        ModCardTypes.register(new ModCardType("mct.throwing", "Throwing"));
        ModCardTypes.registerResolver(id -> {
            if (id.startsWith("mct-throwing")) throw new IllegalStateException("boom");
            return null;
        });

        assertNull(ModCardTypes.typeFor("mct-throwing-mod"));
        assertNull(ModCardTypes.typeFor("mct-throwing-mod"));
    }

    @Test
    void typesSortByPriorityThenTitle() {
        ModCardTypes.register(new ModCardType("mct.order.low", "Zebra", null, 1));
        ModCardTypes.register(new ModCardType("mct.order.high", "Apple", null, 9));
        ModCardTypes.register(new ModCardType("mct.order.mid", "Beetle", null, 9));

        List<String> ordered = ModCardTypes.types().stream()
                .map(ModCardType::getId)
                .filter(id -> id.startsWith("mct.order."))
                .toList();

        assertEquals(List.of("mct.order.high", "mct.order.mid", "mct.order.low"), ordered);
    }

    @Test
    void everyMutationInvalidatesRevisionReaders() {
        ModCardTypeResolver resolver = id -> null;
        assertTrue(invalidates(ModCardTypes::getRevision, () -> ModCardTypes.register(new ModCardType("mct.inv", "Inv"))));
        assertTrue(invalidates(ModCardTypes::getRevision, () -> ModCardTypes.assign("mct-inv", "mct.inv")));
        assertTrue(invalidates(ModCardTypes::getRevision, () -> ModCardTypes.assignAll("mct.inv", "mct-inv-b")));
        assertTrue(invalidates(ModCardTypes::getRevision, () -> ModCardTypes.registerResolver(resolver)));
        assertTrue(invalidates(ModCardTypes::getRevision, () -> ModCardTypes.unassign("mct-inv")));
        assertTrue(invalidates(ModCardTypes::getRevision, () -> ModCardTypes.unregisterResolver(resolver)));
        assertTrue(invalidates(ModCardTypes::getRevision, () -> ModCardTypes.unregister("mct.inv")));
    }

    @Test
    void concurrentMutationsAreAllPublished() {
        ModCardTypes.register(new ModCardType("mct.race", "Race"));
        int before = ModCardTypes.getRevision();
        assertTrue(invalidates(ModCardTypes::getRevision, () -> race(i -> ModCardTypes.assign("mct-race-" + i, "mct.race"))));
        assertEquals(before + RACE_THREADS * RACE_ITERATIONS, ModCardTypes.getRevision());
        for (int i = 0; i < RACE_THREADS * RACE_ITERATIONS; i++) assertNotNull(ModCardTypes.typeFor("mct-race-" + i));
    }

    static final int RACE_THREADS = 4;
    static final int RACE_ITERATIONS = 2000;

    static void race(IntConsumer mutation) {
        Thread[] threads = new Thread[RACE_THREADS];
        for (int t = 0; t < threads.length; t++) {
            int base = t * RACE_ITERATIONS;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < RACE_ITERATIONS; i++) mutation.accept(base + i);
            });
            threads[t].setDaemon(true);
            threads[t].start();
        }
        for (Thread thread : threads) {
            try {
                thread.join(30_000);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            assertFalse(thread.isAlive(), () -> thread.getName() + " still running: " + Arrays.toString(thread.getStackTrace()));
        }
    }

    static boolean invalidates(IntSupplier getter, Runnable mutation) {
        Set<Object> reads = new HashSet<>();
        Snapshot snapshot = Snapshot.Companion.takeSnapshot(state -> {
            reads.add(state);
            return Unit.INSTANCE;
        });
        try {
            snapshot.enter(getter::getAsInt);
        } finally {
            snapshot.dispose();
        }
        Snapshot.Companion.sendApplyNotifications();
        Set<Object> changed = new HashSet<>();
        ObserverHandle observer = Snapshot.Companion.registerApplyObserver((set, applied) -> {
            changed.addAll(set);
            return Unit.INSTANCE;
        });
        try {
            mutation.run();
            Snapshot.Companion.sendApplyNotifications();
        } finally {
            observer.dispose();
        }
        return !reads.isEmpty() && changed.containsAll(reads);
    }

    @Test
    void bumpsFromOverlappingCallerSnapshotsAreNotLost() {
        MutableSnapshot a = Snapshot.Companion.takeMutableSnapshot(null, null);
        MutableSnapshot b = Snapshot.Companion.takeMutableSnapshot(null, null);
        try {
            int before = ModCardTypes.getRevision();
            a.enter(() -> {
                ModCardTypes.assign("mct-snap-a", "mct.snap");
                return null;
            });
            b.enter(() -> {
                ModCardTypes.assign("mct-snap-b", "mct.snap");
                return null;
            });
            a.apply().check();
            b.apply().check();
            assertEquals(before + 2, ModCardTypes.getRevision());
        } finally {
            a.dispose();
            b.dispose();
        }
    }
}
