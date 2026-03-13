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

package org.polyfrost.oneconfig.api.platform.v1.internal;

import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.platform.v1.LoaderPlatform;

import java.nio.file.Path;

//#if FABRIC
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
//#else
//#if MC >= 1.15.2
//$ // TODO
//#else
//$ import net.minecraft.launchwrapper.Launch;
//#endif
//#endif

public class LoaderPlatformImpl implements LoaderPlatform {
    @Override
    //#if FABRIC
    @SuppressWarnings("deprecation")
    //#endif
    public void addToClasspath(@NotNull Path path) {
        //#if FORGE-LIKE
        //#if MC > 1.12.2
        //$ throw new UnsupportedOperationException("TODO"); // hiiii init!!!
        //#else
        //$ try {
        //$     Launch.classLoader.addURL(path.toUri().toURL());
        //$ } catch (Exception e) {
        //$     throw new RuntimeException(e);
        //$ }
        //#endif
        //#else
        try {
            FabricLauncherBase.getLauncher().addToClassPath(path);
        } catch (Exception e) {
            try {
                net.fabricmc.loader.launch.common.FabricLauncherBase.getLauncher().propose(path.toUri().toURL());
            } catch (Exception e2) {
                throw new RuntimeException(e2);
            }
        }
        //#endif
    }

}
