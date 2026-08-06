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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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
}
