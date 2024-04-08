/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.api.config;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.util.ObjectSerializer;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


public class ObjectSerializerTest {
    private final ObjectSerializer objectSerializer = ObjectSerializer.INSTANCE;

    @Test
    public void testSerializePrimitives() {
        Object intTest = objectSerializer.serialize(42, false);
        assertInstanceOf(Integer.class, intTest);
        assertEquals(42, intTest);

        Object longTest = objectSerializer.serialize(42L, false);
        assertInstanceOf(Long.class, longTest);
        assertEquals(42L, longTest);

        Object floatTest = objectSerializer.serialize(42.69f, false);
        assertInstanceOf(Float.class, floatTest);
        assertEquals(42.69f, floatTest);

        Object doubleTest = objectSerializer.serialize(42.69, false);
        assertInstanceOf(Double.class, doubleTest);
        assertEquals(42.69, doubleTest);

        Object booleanTest = objectSerializer.serialize(true, false);
        assertInstanceOf(Boolean.class, booleanTest);
        assertEquals(true, booleanTest);

        Object stringTest = objectSerializer.serialize("test", false);
        assertInstanceOf(String.class, stringTest);
        assertEquals("test", stringTest);
    }

    @Test
    public void testSerializePrimitiveArrays() {
        Integer[] intArray = new Integer[]{1, 2, 3, 4, 5};
        Object intTest = objectSerializer.serialize(intArray, false);
        assertInstanceOf(Integer[].class, intTest);
        assertArrayEquals(intArray, (Integer[]) intTest);

        Long[] longArray = new Long[]{1L, 2L, 3L, 4L, 5L};
        Object longTest = objectSerializer.serialize(longArray, false);
        assertInstanceOf(Long[].class, longTest);
        assertArrayEquals(longArray, (Long[]) longTest);

        Float[] floatArray = new Float[]{1.3f, 2.3f, 3.3f, 4.3f, 5.3f};
        Object floatTest = objectSerializer.serialize(floatArray, false);
        assertInstanceOf(Float[].class, floatTest);
        assertArrayEquals(floatArray, (Float[]) floatTest);

        Double[] doubleArray = new Double[]{1.3, 2.3, 3.3, 4.3, 5.3};
        Object doubleTest = objectSerializer.serialize(doubleArray, false);
        assertInstanceOf(Double[].class, doubleTest);
        assertArrayEquals(doubleArray, (Double[]) doubleTest);

        Boolean[] booleanArray = new Boolean[]{true, false, true};
        Object booleanTest = objectSerializer.serialize(booleanArray, false);
        assertInstanceOf(Boolean[].class, booleanTest);
        assertArrayEquals(booleanArray, (Boolean[]) booleanTest);

        String[] stringArray = new String[]{"test1", "test2", "test3"};
        Object stringTest = objectSerializer.serialize(stringArray, false);
        assertInstanceOf(String[].class, stringTest);
        assertArrayEquals(stringArray, (String[]) stringTest);
    }

    @Test
    public void testSerializePrimitiveLists() {
        List<Integer> intList = Arrays.asList(1, 2, 3, 4, 5);
        Object intTest = objectSerializer.serialize(intList, true);
        assertInstanceOf(List.class, intTest);
        assertEquals(intList, intTest);

        List<Long> longList = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        Object longTest = objectSerializer.serialize(longList, true);
        assertInstanceOf(List.class, longTest);
        assertEquals(longList, longTest);

        List<Float> floatList = Arrays.asList(1.3f, 2.3f, 3.3f, 4.3f, 5.3f);
        Object floatTest = objectSerializer.serialize(floatList, true);
        assertInstanceOf(List.class, floatTest);
        assertEquals(floatList, floatTest);

        List<Double> doubleList = Arrays.asList(1.3, 2.3, 3.3, 4.3, 5.3);
        Object doubleTest = objectSerializer.serialize(doubleList, true);
        assertInstanceOf(List.class, doubleTest);
        assertEquals(doubleList, doubleTest);

        List<Boolean> booleanList = Arrays.asList(true, false, true);
        Object booleanTest = objectSerializer.serialize(booleanList, true);
        assertInstanceOf(List.class, booleanTest);
        assertEquals(booleanList, booleanTest);

        List<String> stringList = Arrays.asList("test1", "test2", "test3");
        Object stringTest = objectSerializer.serialize(stringList, true);
        assertInstanceOf(List.class, stringTest);
        assertEquals(stringList, stringTest);
    }

    @Test
    public void testColor() {
        // Test Serialization
        Color color = new Color(20, 55, 3, 100);
        Object colorTest = objectSerializer.serialize(color, false);
        assertInstanceOf(HashMap.class, colorTest);
        assertTrue(((HashMap<?, ?>) colorTest).containsKey("value"));
        assertInstanceOf(int[].class, ((HashMap<?, ?>) colorTest).get("value"));
        assertTrue(((HashMap<?, ?>) colorTest).containsKey("class"));
        assertInstanceOf(String.class, ((Map<?, ?>) colorTest).get("class"));
        assertEquals("java.awt.Color", ((HashMap<?, ?>) colorTest).get("class"));
        assertArrayEquals(new int[]{20, 55, 3, 100}, (int[]) ((HashMap<?, ?>) colorTest).get("value"));

        // Test Deserialization
        Object deserializedColor = objectSerializer.deserialize((Map<String, Object>) colorTest);
        assertInstanceOf(Color.class, deserializedColor);
        assertEquals(color, deserializedColor);
    }

    // Test serialization and deserialization of a custom object without adapter
    @Test
    public void testObject() {
        // Test Serialization
        Dimension dimension = new Dimension(10, 20);
        Object dimensionTest = objectSerializer.serialize(dimension, false);
        assertInstanceOf(HashMap.class, dimensionTest);
        assertTrue(((HashMap<?, ?>) dimensionTest).containsKey("class"));
        assertInstanceOf(String.class, ((Map<?, ?>) dimensionTest).get("class"));
        assertEquals("java.awt.Dimension", ((HashMap<?, ?>) dimensionTest).get("class"));
        assertTrue(((HashMap<?, ?>) dimensionTest).containsKey("width"));
        assertInstanceOf(Integer.class, ((HashMap<?, ?>) dimensionTest).get("width"));
        assertEquals(10, ((HashMap<?, ?>) dimensionTest).get("width"));
        assertTrue(((HashMap<?, ?>) dimensionTest).containsKey("height"));
        assertInstanceOf(Integer.class, ((HashMap<?, ?>) dimensionTest).get("height"));
        assertEquals(20, ((HashMap<?, ?>) dimensionTest).get("height"));

        // Test Deserialization
        Object deserializedDimension = objectSerializer.deserialize((Map<String, Object>) dimensionTest);
        assertInstanceOf(Dimension.class, deserializedDimension);
        assertEquals(dimension, deserializedDimension);
    }

    @Test
    public void testSerializePrimitiveMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("test1", 1);
        map.put("test2", 2);
        map.put("test3", 3);
        Object mapTest = objectSerializer.serialize(map, true);
        assertInstanceOf(Map.class, mapTest);
        assertEquals(map, mapTest);
    }

    @Test
    public void testMapSerialization() {
        // Test Serialization
        Map<String, Object> map = new HashMap<>();
        map.put("dimension", new Dimension(10, 20));
        map.put("color", new Color(20, 30, 40, 50));
        Object mapTest = objectSerializer.serialize(map, true);
        assertInstanceOf(Map.class, mapTest);

        assertTrue(((Map<?, ?>) mapTest).containsKey("dimension"));
        Object mapDimension = ((Map<?, ?>) mapTest).get("dimension");
        assertInstanceOf(Map.class, mapDimension);
        assertTrue(((Map<?, ?>) mapDimension).containsKey("class"));
        assertInstanceOf(String.class, ((Map<?, ?>) mapDimension).get("class"));
        assertEquals("java.awt.Dimension", ((Map<?, ?>) mapDimension).get("class"));
        assertTrue(((Map<?, ?>) mapDimension).containsKey("width"));
        assertInstanceOf(Integer.class, ((Map<?, ?>) mapDimension).get("width"));
        assertEquals(10, ((Map<?, ?>) mapDimension).get("width"));
        assertTrue(((Map<?, ?>) mapDimension).containsKey("height"));
        assertInstanceOf(Integer.class, ((Map<?, ?>) mapDimension).get("height"));
        assertEquals(20, ((Map<?, ?>) mapDimension).get("height"));

        assertTrue(((Map<?, ?>) mapTest).containsKey("color"));
        Object mapColor = ((Map<?, ?>) mapTest).get("color");
        assertInstanceOf(Map.class, mapColor);
        assertTrue(((Map<?, ?>) mapColor).containsKey("class"));
        assertInstanceOf(String.class, ((Map<?, ?>) mapColor).get("class"));
        assertEquals("java.awt.Color", ((Map<?, ?>) mapColor).get("class"));
        assertTrue(((Map<?, ?>) mapColor).containsKey("value"));
        assertInstanceOf(int[].class, ((Map<?, ?>) mapColor).get("value"));
        assertArrayEquals(new int[]{20, 30, 40, 50}, (int[]) ((Map<?, ?>) mapColor).get("value"));
    }

    enum TestEnum {
        TEST1
    }

    @Test
    public void testEnum() {
        // Test Serialization
        Object enumTest = objectSerializer.serialize(TestEnum.TEST1, false);
        assertInstanceOf(HashMap.class, enumTest);
        assertTrue(((HashMap<?, ?>) enumTest).containsKey("value"));
        assertInstanceOf(String.class, ((HashMap<?, ?>) enumTest).get("value"));
        assertEquals("TEST1", ((HashMap<?, ?>) enumTest).get("value"));
        assertTrue(((HashMap<?, ?>) enumTest).containsKey("class"));
        assertEquals(TestEnum.class.getName(), ((HashMap<?, ?>) enumTest).get("class"));

        // Test Deserialization
        Object deserializedEnum = objectSerializer.deserialize((Map<String, Object>) enumTest);
        assertInstanceOf(TestEnum.class, deserializedEnum);
        assertEquals(TestEnum.TEST1, deserializedEnum);
    }

    @Test
    public void testNull() {
        Object nullTest = objectSerializer.serialize(null, false);
        assertNull(nullTest);

        Object deserializedNull = objectSerializer.deserialize(null);
        assertNull(deserializedNull);
    }
}
