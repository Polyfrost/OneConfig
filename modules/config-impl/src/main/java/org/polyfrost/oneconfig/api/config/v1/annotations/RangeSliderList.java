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

package org.polyfrost.oneconfig.api.config.v1.annotations;

import java.lang.annotation.*;
import org.polyfrost.oneconfig.api.config.v1.ValueFormatter;
import org.polyfrost.oneconfig.api.config.v1.Visualizer;

/**
 * Renders a user-editable list of ranges with each row being a two-handle slider with a value field for each end
 * <p>
 * The annotated field must be a flat numeric array such as {@code int[]} or {@code float[]} or a {@code List}
 * of numbers holding each range's start then end so {@code {0, 10, 20, 30}} is the ranges 0-10 and 20-30
 * <p>
 * The field must hold an even number of values
 * <p>
 * See {@link RangeNumberList} for the variant without the track
 */
@Option(display = Visualizer.RangeSliderListVisualizer.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface RangeSliderList {
    String title();

    String description() default "";

    String icon() default "";

    String category() default "General";

    String subcategory() default "General";

    float min() default 0f;

    float max() default 100f;

    /** Value the handles snap to multiples of and {@code 0} slides continuously */
    float step() default 1f;

    /** Maximum number of ranges the user may add and {@code 0} means unlimited */
    int maxEntries() default 0;

    /** Allow the user to drag ranges to reorder them */
    boolean reorderable() default true;

    /** Label of the button that appends a new range */
    String addText() default "Add";

    Class<? extends ValueFormatter> formatter() default ValueFormatter.class;

}
