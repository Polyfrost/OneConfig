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
 * Renders a user-editable list of colors, each row opening the usual color picker.
 * <p>
 * The annotated field must be {@code int[]} or a {@code List<Integer>} of packed ARGB values.
 * {@code java.awt.Color[]} is also accepted. Chroma is not offered here, as there is nowhere to
 * store the per-entry chroma flag; use a single {@link Color} option for that.
 * <p>
 * This is the list counterpart of {@link Color}: rows can be added, removed and dragged into a
 * different order, and the property value is the list of colors in display order.
 */
@Option(display = Visualizer.ColorListVisualizer.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface ColorList {
    String title();

    /**
     * @deprecated No longer needed. OneConfig now checks whether the given title is a translation
     * key present in the active language, and translates it automatically when it is. Pass the
     * translation key directly as the title and remove this flag.
     */
    @Deprecated
    boolean titleTranslation() default false;

    String description() default "";

    /**
     * @deprecated No longer needed. OneConfig now checks whether the given description is a translation
     * key present in the active language, and translates it automatically when it is. Pass the
     * translation key directly as the description and remove this flag.
     */
    @Deprecated
    boolean descriptionTranslation() default false;

    String icon() default "";

    String category() default "General";

    /**
     * @deprecated No longer needed. OneConfig now checks whether the given category is a translation
     * key present in the active language, and translates it automatically when it is. Pass the
     * translation key directly as the category and remove this flag.
     */
    @Deprecated
    boolean categoryTranslation() default false;

    String subcategory() default "General";

    /**
     * @deprecated translation keys will be translated by default.
     */
    @Deprecated
    boolean subcategoryTranslation() default false;

    /** Whether the picker exposes an opacity control. */
    boolean alpha() default true;

    /** Maximum number of entries the user may add. {@code 0} means unlimited. */
    int maxEntries() default 0;

    /** Allow the user to drag entries to reorder them. */
    boolean reorderable() default true;

    /** Label of the button that appends a new entry. */
    String addText() default "Add";

    /**
     * @deprecated No longer needed. OneConfig now checks whether the given addText is a translation
     * key present in the active language, and translates it automatically when it is. Pass the
     * translation key directly as the addText and remove this flag.
     */
    @Deprecated
    boolean addTextTranslation() default false;
}
