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

import org.polyfrost.oneconfig.api.config.v1.Visualizer;

import java.lang.annotation.*;

/**
 * Renders a user-editable list of numbers, each row being a slider with a value field beside it.
 * <p>
 * The annotated field must be a numeric array ({@code int[]}, {@code float[]}, {@code double[]},
 * {@code long[]}, ...) or a {@code List} of numbers; see {@link NumberList} for how the element
 * type is decided and for the plain number-input variant.
 */
@Option(display = Visualizer.SliderListVisualizer.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface SliderList {
    String title();

    boolean titleTranslation() default false;

    String description() default "";

    boolean descriptionTranslation() default false;

    String icon() default "";

    String category() default "General";

    boolean categoryTranslation() default false;

    String subcategory() default "General";

    boolean subcategoryTranslation() default false;

    float min() default 0f;

    float max() default 100f;

    /** Value the slider snaps to multiples of. {@code 0} slides continuously. */
    float step() default 1f;

    /** Maximum number of entries the user may add. {@code 0} means unlimited. */
    int maxEntries() default 0;

    /** Allow the user to drag entries to reorder them. */
    boolean reorderable() default true;

    /** Label of the button that appends a new entry. */
    String addText() default "Add";

    boolean addTextTranslation() default false;
}
