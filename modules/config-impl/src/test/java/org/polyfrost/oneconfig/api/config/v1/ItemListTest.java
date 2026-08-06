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
 */

package org.polyfrost.oneconfig.api.config.v1;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.annotations.ItemList;
import org.polyfrost.oneconfig.api.config.v1.collect.impl.OneConfigCollector;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemListTest {
    @Test
    void collectsStringArrayAndListMetadata() {
        Tree tree = new OneConfigCollector().collect(new ValidItemListConfig());

        Property<?> multiple = tree.getProp("multiple");
        assertEquals(Visualizer.ItemListVisualizer.class, multiple.getMetadata("visualizer"));
        assertEquals(Integer.valueOf(0), multiple.getMetadata("maxEntries"));
        assertEquals("oneconfig.itemlist.add", multiple.getMetadata("addTextKey"));

        Property<?> single = tree.getProp("single");
        assertEquals(Integer.valueOf(1), single.getMetadata("maxEntries"));
        assertEquals(false, single.getMetadata("reorderable"));
    }

    @Test
    void rejectsAListWithTheWrongElementType() {
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> new OneConfigCollector().collect(new InvalidElementConfig())
        );
        assertTrue(hasMessage(exception, "String[] or List<String>"));
    }

    @Test
    void rejectsARawListWithoutAnElementType() {
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> new OneConfigCollector().collect(new RawListConfig())
        );
        assertTrue(hasMessage(exception, "String[] or List<String>"));
    }

    @Test
    void rejectsNegativeLimits() {
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> new OneConfigCollector().collect(new InvalidLimitConfig())
        );
        assertTrue(hasMessage(exception, "negative maxEntries"));
    }

    private static boolean hasMessage(Throwable throwable, String text) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(text)) return true;
        }
        return false;
    }

    private static final class ValidItemListConfig extends Config {
        @ItemList(title = "Items")
        private String[] multiple = {"minecraft:diamond"};

        @ItemList(title = "Item", maxEntries = 1, reorderable = false)
        private List<String> single = new ArrayList<>(List.of("minecraft:apple"));

        private ValidItemListConfig() {
            super("item-list-test.json", "Item List Test", Category.OTHER);
        }
    }

    private static final class InvalidElementConfig extends Config {
        @ItemList(title = "Invalid")
        private List<Integer> invalid = List.of(1);

        private InvalidElementConfig() {
            super("invalid-item-list-test.json", "Invalid Item List Test", Category.OTHER);
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class RawListConfig extends Config {
        @ItemList(title = "Invalid")
        private List invalid = new ArrayList();

        private RawListConfig() {
            super("raw-item-list-test.json", "Raw Item List Test", Category.OTHER);
        }
    }

    private static final class InvalidLimitConfig extends Config {
        @ItemList(title = "Invalid", maxEntries = -1)
        private String[] invalid = {};

        private InvalidLimitConfig() {
            super("invalid-item-limit-test.json", "Invalid Item Limit Test", Category.OTHER);
        }
    }
}
