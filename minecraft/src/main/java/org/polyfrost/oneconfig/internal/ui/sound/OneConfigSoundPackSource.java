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

package org.polyfrost.oneconfig.internal.ui.sound;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;

public final class OneConfigSoundPackSource implements RepositorySource {
    public static final OneConfigSoundPackSource INSTANCE = new OneConfigSoundPackSource();

    /** {@code .minecraft/oneconfig/sounds}; laid out as a resource pack ({@code pack.mcmeta} + {@code assets/}). */
    public static final Path PACK_ROOT = Paths.get("oneconfig", "sounds");

    private static final String PACK_ID = "oneconfig_sounds";

    private OneConfigSoundPackSource() {
    }

    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        if (!Files.isRegularFile(PACK_ROOT.resolve("pack.mcmeta"))) return;

        PackLocationInfo location = new PackLocationInfo(
                PACK_ID,
                Component.literal("OneConfig Sounds"),
                PackSource.BUILT_IN,
                Optional.empty()
        );
        Pack.ResourcesSupplier resources = new Pack.ResourcesSupplier() {
            @Override
            public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo info) {
                return new PathPackResources(info, PACK_ROOT);
            }

            @Override
            public net.minecraft.server.packs.PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                return new PathPackResources(info, PACK_ROOT);
            }
        };
        PackSelectionConfig selection = new PackSelectionConfig(true, Pack.Position.TOP, true);

        Pack pack = Pack.readMetaAndCreate(location, resources, PackType.CLIENT_RESOURCES, selection);
        if (pack != null) {
            consumer.accept(pack);
        }
    }
}
