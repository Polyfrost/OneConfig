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
 * Renders a user-editable list of text inputs
 * <p>
 * The annotated field must be {@code String[]}
 * <p>
 * Unlike {@link DraggableList} the entries are not a fixed set
 * <p>
 * The user types each value and can add and remove rows
 * <p>
 * The property value is the list of entries in display order
 */
@Option(display = Visualizer.TextListVisualizer.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface TextList {
    String title();

    /**
     * @deprecated OneConfig auto-translates title values that are translation keys so pass the key directly
     */
    @Deprecated
    boolean titleTranslation() default false;

    String description() default "";

    /**
     * @deprecated OneConfig auto-translates description values that are translation keys so pass the key directly
     */
    @Deprecated
    boolean descriptionTranslation() default false;

    String icon() default "";

    String category() default "General";

    /**
     * @deprecated OneConfig auto-translates category values that are translation keys so pass the key directly
     */
    @Deprecated
    boolean categoryTranslation() default false;

    String subcategory() default "General";

    /**
     * @deprecated translation keys will be translated by default
     */
    @Deprecated
    boolean subcategoryTranslation() default false;

    @TranslatedDefault("oneconfig.textinput.placeholder")
    String placeholder() default "oneconfig.textinput.placeholder";

    /**
     * @deprecated OneConfig auto-translates placeholder values that are translation keys so pass the key directly
     */
    @Deprecated
    boolean placeholderTranslation() default false;

    /**
     * Insert a regex expression which will be used to validate each entry
     * <p>
     * Entries that do not match are highlighted and are not written to the property
     */
    String regex() default "";

    /** Maximum number of entries the user may add and {@code 0} means unlimited */
    int maxEntries() default 0;

    /** Allow the user to drag entries to reorder them */
    boolean reorderable() default true;

    /** Label of the button that appends a new entry */
    String addText() default "Add";

    /**
     * @deprecated OneConfig auto-translates addText values that are translation keys so pass the key directly
     */
    @Deprecated
    boolean addTextTranslation() default false;
}
