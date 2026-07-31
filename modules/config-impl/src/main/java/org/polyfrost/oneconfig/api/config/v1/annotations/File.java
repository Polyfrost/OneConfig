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

@Option(display = Visualizer.FileVisualizer.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface File {
    String title();

    /**
     * @deprecated No longer needed. OneConfig now checks whether the given title is a translation
     * key present in the active language, and translates it automatically when it is. Pass the
     * translation key directly as the title and remove this flag.
     */
    @Deprecated
    boolean titleTranslation() default false;

    @Deprecated
    String titleKey() default "";

    String description() default "";

    /**
     * @deprecated No longer needed. OneConfig now checks whether the given description is a translation
     * key present in the active language, and translates it automatically when it is. Pass the
     * translation key directly as the description and remove this flag.
     */
    @Deprecated
    boolean descriptionTranslation() default false;

    @Deprecated
    String descriptionKey() default "";

    String icon() default "";

    /**
     * The file extensions to filter the dialog by, e.g. {@code {".png", ".jpg"}}. Entries may be
     * written as {@code ".png"}, {@code "png"} or {@code "*.png"}; they are all normalised to the
     * {@code *.ext} form tinyfd expects. Leave empty to allow any file.
     */
    String[] types() default {};

    /**
     * A human-readable description for the {@link #types()} filter, e.g. {@code "Images"}.
     */
    String filterName() default "";

    /**
     * Select a directory instead of a file. When {@code true}, {@link #types()} is ignored.
     */
    boolean directory() default false;

    String category() default "General";

    @Deprecated
    String categoryKey() default "";

    /**
     * @deprecated No longer needed. OneConfig now checks whether the given category is a translation
     * key present in the active language, and translates it automatically when it is. Pass the
     * translation key directly as the category and remove this flag.
     */
    @Deprecated
    boolean categoryTranslation() default false;

    String subcategory() default "General";

    @Deprecated
    String subcategoryKey() default "";

    /**
     * @deprecated translation keys will be translated by default.
     */
    @Deprecated
    boolean subcategoryTranslation() default false;

    @TranslatedDefault("oneconfig.filepicker.placeholder")
    String placeholder() default "oneconfig.filepicker.placeholder";

    /**
     * @deprecated No longer needed. OneConfig now checks whether the given placeholder is a translation
     * key present in the active language, and translates it automatically when it is. Pass the
     * translation key directly as the placeholder and remove this flag.
     */
    @Deprecated
    boolean placeholderTranslation() default false;

    @Deprecated
    String placeholderKey() default "";
}
