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

import cc.polyfrost.oneconfig.utils.LogScanner;
import cc.polyfrost.oneconfig.utils.Notifications;

import java.util.HashSet;
import java.util.Set;

/**
 * Class used by OneConfig for deprecation related utilities.
 */
public final class Deprecator {
    private static final Set<String> warned = new HashSet<>();

    /**
     * mark a method as deprecated. When a method has this call, it will
     * throw a new exception to grab the name (or package) of the mod that
     * called said method. <br>
     * This will then send a notification detailing this to the user, and
     * throw an UnsupportedOperationException to print a stack to the log.
     */
    public static void markDeprecated() {
        try {
            throw new Exception("This method is deprecated");
        } catch (Exception e) {
            String culprit = LogScanner.identifyCallerFromStacktrace(e)
                    .stream()
                    .map(activeMod -> activeMod.name)
                    .findFirst()
                    .orElse("Unknown");

            // sometimes it blames OneConfig as well so
            if (culprit.equals("OneConfig")) return;

            if (warned.add(culprit)) {
                Notifications.INSTANCE.send("Deprecation Warning", "The mod '" + culprit + "' is using a deprecated method, and will no longer work in the future. Please report this to the mod author.");
                try {
                    throw new UnsupportedOperationException("Method " + e.getStackTrace()[1].getClassName() + "." + e.getStackTrace()[1].getMethodName() + "() is deprecated; but is still being used by mod " + culprit + "!");
                } catch (UnsupportedOperationException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
