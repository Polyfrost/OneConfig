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

package org.polyfrost.oneconfig.api.config.v1;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.annotations.RangeNumber;
import org.polyfrost.oneconfig.api.config.v1.annotations.RangeNumberList;
import org.polyfrost.oneconfig.api.config.v1.annotations.RangeSlider;
import org.polyfrost.oneconfig.api.config.v1.annotations.RangeSliderList;
import org.polyfrost.oneconfig.api.config.v1.collect.impl.OneConfigCollector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangeOptionsTest {
    @Test
    void collectsRangeMetadata() {
        Tree tree = new OneConfigCollector().collect(new ValidRangeConfig());

        Property<?> number = tree.getProp("number");
        assertEquals(Visualizer.RangeNumberVisualizer.class, number.getMetadata("visualizer"));
        assertEquals(1000000f, (Float) number.getMetadata("max"));

        Property<?> sliders = tree.getProp("sliders");
        assertEquals(Visualizer.RangeSliderListVisualizer.class, sliders.getMetadata("visualizer"));
        assertEquals(5f, (Float) sliders.getMetadata("step"));

        Property<?> numbers = tree.getProp("numbers");
        assertEquals(Visualizer.RangeNumberListVisualizer.class, numbers.getMetadata("visualizer"));
        assertEquals(Integer.valueOf(4), numbers.getMetadata("maxEntries"));
    }

    @Test
    void rejectsAnOddNumberOfRangeValues() {
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> new OneConfigCollector().collect(new OddRangeListConfig())
        );
        assertTrue(hasMessage(exception, "even count"));
    }

    @Test
    void rejectsASingleRangeWithoutExactlyTwoValues() {
        RuntimeException number = assertThrows(
            RuntimeException.class,
            () -> new OneConfigCollector().collect(new ShortRangeNumberConfig())
        );
        assertTrue(hasMessage(number, "exactly a start and an end"));

        RuntimeException slider = assertThrows(
            RuntimeException.class,
            () -> new OneConfigCollector().collect(new LongRangeSliderConfig())
        );
        assertTrue(hasMessage(slider, "exactly a start and an end"));
    }

    @Test
    void rejectsASliderListStepLargerThanItsRange() {
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> new OneConfigCollector().collect(new BadStepConfig())
        );
        assertTrue(hasMessage(exception, "larger than its range"));
    }

    private static boolean hasMessage(Throwable throwable, String text) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(text)) return true;
        }
        return false;
    }

    private static final class ValidRangeConfig extends Config {
        @RangeNumber(title = "Number", max = 1000000f)
        private int[] number = {10, 20};

        @RangeSliderList(title = "Sliders", step = 5f)
        private float[] sliders = {0f, 25f, 50f, 75f};

        @RangeNumberList(title = "Numbers", maxEntries = 4)
        private List<Integer> numbers = new ArrayList<>(List.of(1, 2));

        private ValidRangeConfig() {
            super("range-options-test.json", "Range Options Test", Category.OTHER);
        }
    }

    private static final class OddRangeListConfig extends Config {
        @RangeNumberList(title = "Odd")
        private int[] odd = {1, 2, 3};

        private OddRangeListConfig() {
            super("odd-range-list-test.json", "Odd Range List Test", Category.OTHER);
        }
    }

    private static final class ShortRangeNumberConfig extends Config {
        @RangeNumber(title = "Short")
        private int[] shortRange = {1};

        private ShortRangeNumberConfig() {
            super("short-range-number-test.json", "Short Range Number Test", Category.OTHER);
        }
    }

    private static final class LongRangeSliderConfig extends Config {
        @RangeSlider(title = "Long")
        private float[] longRange = {1f, 2f, 3f};

        private LongRangeSliderConfig() {
            super("long-range-slider-test.json", "Long Range Slider Test", Category.OTHER);
        }
    }

    private static final class BadStepConfig extends Config {
        @RangeSliderList(title = "Bad", min = 0f, max = 1f, step = 5f)
        private float[] bad = {};

        private BadStepConfig() {
            super("bad-range-step-test.json", "Bad Range Step Test", Category.OTHER);
        }
    }
}
