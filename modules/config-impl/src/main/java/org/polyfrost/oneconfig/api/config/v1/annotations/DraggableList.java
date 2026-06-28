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
 * Renders a reorderable list. The annotated field must be {@code String[]}.
 * <p>
 * When {@code checkable = false} (default), the property value is the full ordered list.
 * When {@code checkable = true}, {@code options} holds all items and the property value
 * is the ordered subset of enabled items.
 */
@Option(display = Visualizer.DraggableListVisualizer.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface DraggableList {
    String title();

    boolean titleTranslation() default false;

    @Deprecated
    String titleKey() default "";

    String description() default "";

    boolean descriptionTranslation() default false;

    @Deprecated
    String descriptionKey() default "";

    String icon() default "";

    String category() default "General";

    boolean categoryTranslation() default false;

    @Deprecated
    String categoryKey() default "";

    String subcategory() default "General";

    boolean subcategoryTranslation() default false;

    @Deprecated
    String subcategoryKey() default "";

    /** Full set of items. Required when {@code checkable = true}; optional otherwise. */
    String[] options() default {};

    boolean optionsTranslation() default false;

    @Deprecated
    String[] optionsKey() default {};

    /** Show checkboxes to enable/disable individual items. */
    boolean checkable() default false;
}
