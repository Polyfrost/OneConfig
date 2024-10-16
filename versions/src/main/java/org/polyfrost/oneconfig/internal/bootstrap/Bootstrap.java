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

package org.polyfrost.oneconfig.internal.bootstrap;

import net.minecraft.launchwrapper.Launch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Bootstrap {
    //#if FORGE && MODERN==0
    private org.polyfrost.oneconfig.internal.legacy.OneConfigTweaker tweaker = new org.polyfrost.oneconfig.internal.legacy.OneConfigTweaker();
    //#endif

    public void init() {
        //#if FORGE && MODERN==0
        Map<String, String> launchArgs = ((Map<String, String>) Launch.blackboard.get("launchArgs"));
        List<String> args = new ArrayList<>();
        for (Map.Entry<String, String> entry : launchArgs.entrySet()) {
            args.add(entry.getKey());
            args.add(entry.getValue());
        }
        tweaker.acceptOptions(args, Launch.minecraftHome, Launch.assetsDir, launchArgs.get("--version"));
        tweaker.injectIntoClassLoader(net.minecraft.launchwrapper.Launch.classLoader);
        //#endif
    }
}
