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

@Option(display = Visualizer.NumberVisualizer.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Number {
    String title();

    /**
     * @deprecated OneConfig auto-translates title values that are translation keys so pass the key directly
     */
    @Deprecated
    boolean titleTranslation() default false;

    @Deprecated
    String titleKey() default "";

    String description() default "";

    /**
     * @deprecated OneConfig auto-translates description values that are translation keys so pass the key directly
     */
    @Deprecated
    boolean descriptionTranslation() default false;

    @Deprecated
    String descriptionKey() default "";

    String icon() default "";

    String category() default "General";

    /**
     * @deprecated OneConfig auto-translates category values that are translation keys so pass the key directly
     */
    @Deprecated
    boolean categoryTranslation() default false;

    @Deprecated
    String categoryKey() default "";

    String subcategory() default "General";

    /**
     * @deprecated translation keys will be translated by default
     */
    @Deprecated
    boolean subcategoryTranslation() default false;

    @Deprecated
    String subcategoryKey() default "";

    String unit() default "";

    /**
     * @deprecated OneConfig auto-translates unit values that are translation keys so pass the key directly
     */
    @Deprecated
    boolean unitTranslation() default false;

    @Deprecated
    String unitKey() default "";

    float min() default -10f;

    float max() default 100f;

    @TranslatedDefault("oneconfig.numberinput.placeholder")
    String placeholder() default "oneconfig.numberinput.placeholder";

    /**
     * @deprecated OneConfig auto-translates placeholder values that are translation keys so pass the key directly
     */
    @Deprecated
    boolean placeholderTranslation() default false;

    @Deprecated
    String placeholderKey() default "";
}
