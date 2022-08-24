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

package cc.polyfrost.oneconfig.utils.commands.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to describe methods and arguments.
 * It is used for aliases and for better help messages for your users. <br>
 * <b>Usage on a command method:</b>
 * <pre>{@code
 *  @Descriptor(value = "My command", aliases = {"mycmd"}, description = "My command description")
 *  public void myCommand(String arg1, String arg2) {
 *      // this will allow the user to do /{yourcommandname} myCommand OR /{yourcommandname} mycmd   (aliases)
 *      // It will also display the name of this command, and its description if provided in the help message.
 *  }
 * }</pre> <br>
 * <b>Usage on parameters:</b>
 * <pre>{@code
 *  public void myCommand(@Descriptor("first parameter") int bob, float someParam) {
 *      // this will mark the first parameter as "first parameter" in the help message.
 *      // If its not present, it will just show the TYPE of the parameter (e.g. int).
 *  }
 *
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface Descriptor {
    /**
     * The name of the parameter.
     *
     * @return The name of the parameter.
     */
    String value();
    String description() default "";
    String[] aliases() default {};
    /** The string to autocomplete to if the parameter is empty. set to PLAYER for it to autocomplete with player names. <br>
     * Use a comma separated list for your arguments. */
    String autoCompletesTo() default "";
}
