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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Class used by OneConfig for deprecation related utilities.
 */
public final class Deprecator {
    private static final List<String> recentlyWarned = new ArrayList<>();

    // spam protector
    static {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                recentlyWarned.clear();
            }
        }, TimeUnit.MINUTES.toMillis(3), TimeUnit.MINUTES.toMillis(3));
    }

    /**
     * mark a method as deprecated. When a method has this call, it will
     * throw a new exception to grab the name (or package) of the mod that call said method. <br>
     * This will then send a notification detailing this to the user, and throw an UnsupportedOperationException to print a stack to the log.
     */
    public static void markDeprecated() {
        try {
            throw new Exception("This method is deprecated");
        } catch (Exception e) {
            String culprit = LogScanner.identifyCallerFromStacktrace(e).stream().map(activeMod -> activeMod.name).findFirst().orElse("Unknown");
            if (!recentlyWarned.contains(culprit)) {
                recentlyWarned.add(culprit);
                Notifications.INSTANCE.send("Deprecation Warning", "The mod '" + culprit + "' is using a deprecated method, and will no longer work in the future. Please report this to the mod author.");
            }
        }
    }
}
