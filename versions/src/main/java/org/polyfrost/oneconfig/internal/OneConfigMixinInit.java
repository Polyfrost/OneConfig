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

package org.polyfrost.oneconfig.internal;

import org.polyfrost.oneconfig.api.platform.v1.LoaderPlatform;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OneConfigMixinInit implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        List<String> mixins = new ArrayList<>();
        LoaderPlatform.Loaders loader = Platform.loader().getLoader();
        int version = Platform.loader().getMinecraftVersion();

        // Loader-specific mixins
        if (loader == LoaderPlatform.Loaders.FORGE) {
            mixins.add("forge.EventBusMixin");
            if (version < 11300) {
                // legacy forge
                mixins.add("compat.OneConfigV0CompatMixin");
            }
        } else {
            // fabric specific
            mixins.add("fabric.GameRendererAccessor");
            mixins.add("fabric.NetHandlerPlayClientMixin");
            if (version < 11300) {
                // legacy fabric
                mixins.add("commands.ChatScreenMixin");
            }
            if (version > 12000) {
                mixins.add("hypixel.CustomPayloadS2CPacketMixin");
            }
        }

        // Inter-loader mixins
        if (version >= 11600) {
            mixins.add("KeyboardMixin");
            mixins.add("MouseMixin");
            mixins.add("commands.ClientPlayNetworkHandlerMixin");
            mixins.add("commands.HelpCommandAccessor");
            if (version < 12000) {
                // 1.16, 1.17, 1.18, 1.19
                mixins.add("SchemasMixin");
                mixins.add("EntityPlayerSPMixin");
            }
        } else mixins.add("GuiScreenMixin");

        return mixins;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
