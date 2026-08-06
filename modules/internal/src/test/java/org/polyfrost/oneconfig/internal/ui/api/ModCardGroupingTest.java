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

package org.polyfrost.oneconfig.internal.ui.api;

import kotlin.ranges.IntRange;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.ui.v1.ModCardType;
import org.polyfrost.oneconfig.api.ui.v1.ModCardTypes;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModCardGroupingTest {
    private static final String FIRST_TYPE = "grouping.first";
    private static final String SECOND_TYPE = "grouping.second";

    @BeforeAll
    static void registerTypes() {
        ModCardTypes.register(new ModCardType(FIRST_TYPE, "First", null, 20));
        ModCardTypes.register(new ModCardType(SECOND_TYPE, "Second", null, 10));
        ModCardTypes.assignAll(FIRST_TYPE, "grouping-a", "grouping-b");
        ModCardTypes.assignAll(SECOND_TYPE, "grouping-c");
    }

    @Test
    void untypedModsRankAheadOfEveryGroup() {
        int untyped = ModCardGroupingKt.modCardGroupRank("grouping-untyped");
        int first = ModCardGroupingKt.modCardGroupRank("grouping-a");
        int second = ModCardGroupingKt.modCardGroupRank("grouping-c");

        assertEquals(ModCardGroupingKt.UNTYPED_MOD_CARD_RANK, untyped);
        assertTrue(untyped < first, "untyped should sort first");
        assertTrue(first < second, "higher priority types should sort first");
    }

    @Test
    void headersAreInsertedWhereTheGroupChanges() {
        List<ModGridEntry> entries = build(Set.of(), "grouping-untyped", "grouping-a", "grouping-b", "grouping-c");

        assertEquals(
                List.of("grouping-untyped", "modCardType:" + FIRST_TYPE, "grouping-a", "grouping-b",
                        "modCardType:" + SECOND_TYPE, "grouping-c"),
                entries.stream().map(e -> e.getKey().toString()).toList()
        );
    }

    @Test
    void aGroupWithNoVisibleCardsProducesNothing() {
        List<ModGridEntry> entries = build(Set.of(), "grouping-untyped", "grouping-c");

        assertEquals(
                List.of("grouping-untyped", "modCardType:" + SECOND_TYPE, "grouping-c"),
                entries.stream().map(e -> e.getKey().toString()).toList()
        );
    }

    @Test
    void collapsedGroupsKeepTheirHeaderAndDropTheirCards() {
        List<ModGridEntry> entries = build(Set.of(FIRST_TYPE), "grouping-a", "grouping-b");

        assertEquals(1, entries.size());
        ModGridEntry.Header header = (ModGridEntry.Header) entries.get(0);
        assertFalse(header.getExpanded());
        assertEquals(2, header.getCardCount());
    }

    @Test
    void swapsAreRefusedAcrossAndOntoHeaders() {
        // [untyped, HEADER, a, b, HEADER, c]
        List<ModGridEntry> entries = build(Set.of(), "grouping-untyped", "grouping-a", "grouping-b", "grouping-c");

        assertTrue(ModCardGroupingKt.sameModGroup(entries, 2, 3), "cards of one group may swap");
        assertFalse(ModCardGroupingKt.sameModGroup(entries, 0, 2), "a header sits between the groups");
        assertFalse(ModCardGroupingKt.sameModGroup(entries, 2, 1), "a header is never a drop slot");
        assertFalse(ModCardGroupingKt.sameModGroup(entries, 3, 5), "different groups may not swap");
    }

    @Test
    void groupBoundsCoverTheContiguousCardBlock() {
        List<ModGridEntry> entries = build(Set.of(), "grouping-untyped", "grouping-a", "grouping-b", "grouping-c");

        IntRange untyped = ModCardGroupingKt.modGroupBounds(entries, 0);
        assertEquals(0, untyped.getFirst());
        assertEquals(0, untyped.getLast());

        IntRange first = ModCardGroupingKt.modGroupBounds(entries, 3);
        assertEquals(2, first.getFirst());
        assertEquals(3, first.getLast());

        assertTrue(ModCardGroupingKt.modGroupBounds(entries, 1).isEmpty(), "a header has no card block");
    }

    private static List<ModGridEntry> build(Set<String> collapsed, String... ids) {
        List<ConfigData> cards = java.util.Arrays.stream(ids)
                .map(id -> (ConfigData) new TestConfigData(id))
                .sorted(Comparator.comparingInt(card -> ModCardGroupingKt.modCardGroupRank(card.getId())))
                .toList();
        return ModCardGroupingKt.buildModGridEntries(cards, collapsed::contains);
    }

    private record TestConfigData(String id) implements ConfigData {
        @Override
        public String getId() {
            return id;
        }

        @Override
        public Object getTitle() {
            return id;
        }

        @Override
        public String getIcon() {
            return null;
        }

        @Override
        public ConfigSource getSource() {
            return ConfigSource.OC;
        }

        @Override
        public Config.Category getCategory() {
            return Config.Category.OTHER;
        }
    }
}
