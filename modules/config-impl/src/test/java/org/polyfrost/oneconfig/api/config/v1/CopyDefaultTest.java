package org.polyfrost.oneconfig.api.config.v1;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public class CopyDefaultTest {
    @Test
    void copiesLists() {
        List<String> original = new ArrayList<>(Arrays.asList("one", "two"));
        @SuppressWarnings("unchecked")
        List<String> copy = (List<String>) Config.copyDefault(List.class, original);
        assertNotSame(original, copy);
        assertEquals(original, copy);
        original.clear();
        original.add("edited");
        assertEquals(Arrays.asList("one", "two"), copy);
    }

    @Test
    void copiesArrays() {
        String[] original = {"one", "two"};
        String[] copy = (String[]) Config.copyDefault(String[].class, original);
        assertNotSame(original, copy);
        assertEquals(String[].class, copy.getClass());
        original[0] = "edited";
        assertEquals("one", copy[0]);
    }

    @Test
    void copiesPrimitiveArrays() {
        int[] original = {1, 2};
        int[] copy = (int[]) Config.copyDefault(int[].class, original);
        assertNotSame(original, copy);
        original[0] = 9;
        assertEquals(1, copy[0]);
    }

    @Test
    void copiesMaps() {
        Map<String, String> original = new LinkedHashMap<>();
        original.put("a", "1");
        Map<?, ?> copy = (Map<?, ?>) Config.copyDefault(Map.class, original);
        assertNotSame(original, copy);
        original.clear();
        assertEquals(1, copy.size());
    }

    @Test
    void copiesUnmodifiableCollectionsToAWritableOne() {
        List<String> original = Collections.unmodifiableList(new ArrayList<>(Arrays.asList("one")));
        @SuppressWarnings("unchecked")
        List<String> copy = (List<String>) Config.copyDefault(List.class, original);
        assertNotSame(original, copy);
        assertEquals(original, copy);
    }

    @Test
    void leavesSimpleValuesAlone() {
        String value = "one";
        assertSame(value, Config.copyDefault(String.class, value));
    }
}
