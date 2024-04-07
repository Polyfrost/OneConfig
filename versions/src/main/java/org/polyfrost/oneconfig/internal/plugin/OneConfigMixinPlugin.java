/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.internal.plugin;

import org.polyfrost.oneconfig.platform.Platform;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OneConfigMixinPlugin implements IMixinConfigPlugin {

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
        Platform.Loader loader = Platform.getInstance().getLoader();
        int version = Platform.getInstance().getMinecraftVersion();

        // Loader-specific mixins
        if (loader == Platform.Loader.FORGE) {
            mixins.add("EventBusMixin");
            if (version == 10809 || version == 11202) {
                // Patcher mixin
                mixins.add("HudCachingMixin");
            }
            if (version >= 11600) {
                mixins.add("ClientModLoaderMixin");
            }
        }
        if (loader == Platform.Loader.FABRIC) {
            mixins.add("GameRendererAccessor");
            mixins.add("NetHandlerPlayClientMixin");
            mixins.add("FramebufferMixin");

            if (version <= 11202) {
                mixins.add("commands.ChatScreenMixin");
                mixins.add("commands.ScreenMixin");
            }
        }

        if (version >= 11600 || loader == Platform.Loader.FABRIC) {
            mixins.add("ClientBuiltinResourcePackProviderMixin");
        }

        // Inter-loader mixins
        if (version >= 11600) {
            mixins.add("KeyboardMixin");
            mixins.add("MouseMixin");
        }

        if (version <= 11202) {
            mixins.add("GuiScreenMixin");
        }

        return mixins.isEmpty() ? null : mixins;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
