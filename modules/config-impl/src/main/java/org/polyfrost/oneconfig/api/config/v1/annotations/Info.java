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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Option(display = Visualizer.InfoVisualizer.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Info {
//	Notifications.Type value() default Notifications.Type.Info;

	/** Title for the info block. Due to Java annotation limitations, if the value is unchanged, it will be replaced with the actual type name. */
	@TranslatedDefault("polyui.info")
	String title() default "polyui.info";

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

	/** Icon for the info block. Due to Java annotation limitations, if the value is unchanged, it will be replaced with the actual type icon. */
	String icon() default "polyui/info.svg";

	String category() default "General";

	/**
	 * @deprecated No longer needed. OneConfig now checks whether the given category is a translation
	 * key present in the active language, and translates it automatically when it is. Pass the
	 * translation key directly as the category and remove this flag.
	 */
	@Deprecated
	boolean categoryTranslation() default false;

	@Deprecated
	String categoryKey() default "";

	String subcategory() default "General";

	/**
	 * @deprecated No longer needed. OneConfig now checks whether the given subcategory is a translation
	 * key present in the active language, and translates it automatically when it is. Pass the
	 * translation key directly as the subcategory and remove this flag.
	 */
	@Deprecated
	boolean subcategoryTranslation() default false;

	@Deprecated
	String subcategoryKey() default "";
}
