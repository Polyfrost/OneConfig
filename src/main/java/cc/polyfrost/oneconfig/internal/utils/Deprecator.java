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

package cc.polyfrost.oneconfig.internal.utils;

import cc.polyfrost.oneconfig.internal.config.core.ConfigCore;
import cc.polyfrost.oneconfig.utils.Notifications;

import java.util.Iterator;

/**
 * Class used by OneConfig for deprecation related utilities.
 */
public class Deprecator {
    /**
     * mark a method as deprecated. When a method has this call, it will
     * throw a new exception to grab the name (or package) of the mod that call said method. <br>
     * This will then send a notification detailing this to the user, and throw an UnsupportedOperationException to print a stack to the log.
     */
    public static void markDeprecated() {
        try {
            throw new Exception("This method is deprecated");
        } catch (Exception e) {
            // first is this method name, second is the method it called, third is what called it
            StackTraceElement target = null;
            int i = 0;
            for (StackTraceElement element : e.getStackTrace()) {
                // ignore the first two
                if (i > 2) {
                    // remove any that are native, or called from a system package
                    if (!element.isNativeMethod() && !element.getClassName().startsWith("sun.reflect") && !element.getClassName().startsWith("java.lang")) {
                        target = element;
                        break;
                    }
                }
                i++;
            }

            // turn the full path into a package name
            StringBuilder sb = new StringBuilder();
            int dots = 0;
            if (target != null) {
                for (char c : target.getClassName().toCharArray()) {
                    if (c == '.') dots++;
                    if (dots == 3) break;
                    sb.append(c);
                }
            } else {
                // this should never happen but you know
                sb.append("Unknown");
            }

            String culprit = sb.toString();
            // attempt to get the mods "formal name" from the config mod list
            for (Iterator<String> it = ConfigCore.mods.stream().map(mod -> mod.name).iterator(); it.hasNext(); ) {
                String s = it.next().replaceAll(" ", "");
                if (s.equalsIgnoreCase(culprit.substring(culprit.lastIndexOf(".")))) {
                    culprit = s;
                    break;
                }
            }
            Notifications.INSTANCE.send("Deprecation Warning", "The mod '" + culprit + "' is using a deprecated method, and will no longer work in the future. Please report this to the mod author.");
            try {
                throw new UnsupportedOperationException("Method " + e.getStackTrace()[1].getClassName() + "." + e.getStackTrace()[1].getMethodName() + "() is deprecated; but is still being used by mod " + culprit + "!");
            } catch (UnsupportedOperationException e1) {
                e1.printStackTrace();
            }
        }
    }
}
