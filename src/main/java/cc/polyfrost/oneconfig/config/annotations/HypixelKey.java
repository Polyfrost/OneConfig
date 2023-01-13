/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link Text} as a Hypixel API key.
 * <p>
 *     This can allow OneConfig to
 *     <p>
 *         - automatically set this field when `/api new` is run in chat
 *     </p>
 *     <p>
 *         - sync this field with other fields annotated with {@link HypixelKey}
 *     </p>
 *     <p>
 *         - allow users to set all fields annotated with {@link HypixelKey} to the same value via the Preferences GUI.
 *     </p>
 *     This can be disabled by the user in the Preferences GUI.
 * </p>
 * <p>
 *     Adding this annotation marks the field as non profile specific, see {@link NonProfileSpecific}.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface HypixelKey {
    /**
     * The priority of this field when syncing with other keys.
     * When syncing, the field with the highest priority and valid key will be used.
     * @return The priority of this field when syncing with other keys.
     */
    int priority() default 0;
}
