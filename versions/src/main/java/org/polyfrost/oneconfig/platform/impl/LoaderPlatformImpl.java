/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.platform.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.platform.LoaderPlatform;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
//#if FABRIC==1
//$$ import net.fabricmc.loader.api.FabricLoader;
//#else
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
//#endif
//#if FORGE==1 && MC>11202
//$$ import net.minecraftforge.fml.ModList;
//$$ import net.minecraftforge.fml.ModLoadingContext;
//#endif

public class LoaderPlatformImpl implements LoaderPlatform {
    @Override
    public boolean isModLoaded(String id) {
        //#if MC>=11600
        //#if FORGE==1
        //$$ return ModList.get().isLoaded(id);
        //#else
        //$$ return FabricLoader.getInstance().isModLoaded(id);
        //#endif
        //#elseif FORGE==1
        return Loader.isModLoaded(id);
        //#else
        //$$ return FabricLoader.getInstance().isModLoaded(id);
        //#endif
    }

    @Override
    public boolean hasActiveModContainer() {
        //#if FORGE==1
        return Loader.instance().activeModContainer() != null;
        //#else
        //$$ return false;
        //#endif
    }

    @Override
    public @NotNull List<ActiveMod> getLoadedMods() {
        try {
            return
                    //#if FORGE==1
                    //#if MC<=11202
                    Loader.instance().getActiveModList().stream().map
                            //#else
                            //$$ ModList.get().applyForEachModContainer
                            //#endif
                            //#else
                            //$$ FabricLoader.getInstance().getAllMods().stream().map
                            //#endif
                                    (this::toActiveMod).collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public ActiveMod getActiveModContainer() {
        //#if FORGE==1
        return toActiveMod(Loader.instance().activeModContainer());
        //#else
        //$$ return null;
        //#endif
    }

    @Override
    public ActiveMod toActiveMod(@Nullable Object in) {
        //#if FORGE==1
        try {
            ModContainer container = (ModContainer) in;
            if (container == null) return null;
            //#if FORGE==1
            //#if MC==11202
            return new ActiveMod(container.getName(), container.getModId(), container.getVersion(), container.getSource().toPath());
            //#else
            //$$ return new ActiveMod(container.getModInfo().getDisplayName(), container.getModId(), container.getModInfo().getVersion().getQualifier(), ModList.get().getModFileById(container.getModId()).getFile().getFilePath());
            //#endif
            //#else
            //$$ return new ActiveMod(container.getMetadata().getName(), container.getMetadata().getId(), container.getMetadata().getVersion().getFriendlyString(), container.getRootPaths().get(0));
            //#endif
        } catch (Exception e) {
            return null;
        }
        //#endif
    }
}
