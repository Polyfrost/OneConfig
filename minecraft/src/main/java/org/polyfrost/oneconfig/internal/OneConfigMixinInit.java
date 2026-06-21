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

import kotlin.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
//todo import org.polyfrost.oneconfig.internal.generated.RelocatedMixins;
//? moul_compat {
/*import org.polyfrost.oneconfig.internal.generated.RelocatedMixins;
*///? }
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


        //? moul_compat {
        /*RelocatedMixins.INSTANCE.register(e -> {
            mixins.add(e);
            return Unit.INSTANCE;
        });
        *///? }
        //? moul_compat {
        /*mixins.add("compat.moulconfig.Mixin_MCConfigEditorIntegration_Firmament");
        mixins.add("compat.moulconfig.Mixin_MoulConfigAdapter_DandelionBp");
        *///? }

        //? dandelion_compat
        //mixins.add("compat.DandelionScreenImplMixin");

        //? rconfig_compat
        mixins.add("compat.rconfig.Mixin_Configurations");

        mixins.add("Mixin_SimpleReloadInstance");
        //? yacl_compat
        mixins.add("compat.yacl.Mixin_YetAnotherConfigLib_Builder");

        //? clothconfig_compat
        mixins.add("compat.cloth.Mixin_ConfigBuilderImpl");

        //? tr7zw_compat
        mixins.add("compat.tr7zw.Mixin_AbstractConfigScreen");

        // mixins.add("compat.rconfig.Mixin_Configurations");

        //? modmenu_compat
        mixins.add("compat.Mixin_ModMenu");

        //? neoforge {
        //mixins.add("events.Mixin_ChatReceiveEvent_Forge");
        //mixins.add("events.Mixin_ScreenOpenEvent_Forge");
        //? } else {
        //? < 1.21.2
        //mixins.add("fabric.Mixin_LoadShaderInvoker_Fabric");
        mixins.add("events.Mixin_ScreenOpenEvent_Fabric");
        //? }

        mixins.add("events.Mixin_ModernWindowFocusEvent");
        // mixins.add("command.Mixin_ModernArgumentTypeEntryAccessor");

        mixins.add("skia.Mixin_InitSkia");
        mixins.add("skia.Mixin_SkiaFrame");
        mixins.add("skia.Mixin_SkiaFrameVk");
        //? >= 26.1 {
        mixins.add("blaze3d.GpuDeviceAccessor");
        mixins.add("blaze3d.GlDeviceAccessor");
        mixins.add("skia.Mixin_SkiaFramePresent");
        //? }
        //? < 26.1 {
        /*mixins.add("skia.Mixin_ScreenshotComposite");
        *///? }
        mixins.add("skia.Mixin_InitSkiaFontRenderer");
        mixins.add("skia.Mixin_FixComposeRaceCondition");

        //? >= 1.21.10 {
        mixins.add("keybind.Mixin_KeybindCategoryLabel");
        //? }
        //? < 1.21.10 {
        //mixins.add("keybind.KeyMappingCategoryAccessor");
        //? }

        mixins.add("keybind.Mixin_OneConfigKeybindRebind");
        mixins.add("keybind.Mixin_KeyMappingResetDetect");

        //? cinnabar
        //mixins.add("skia.Mixin_CinnabarSkiaFlush");

        {
            Logger logger = LogManager.getLogger(OneConfigMixinInit.class);
            logger.info("Loaded {} non-common Mixins", mixins.size());

            for (int i = 0; i < mixins.size(); i += 5) {
                int end = Math.min(i + 5, mixins.size());
                List<String> batch = mixins.subList(i, end);
                logger.info("Loaded Mixins: {}", String.join(", ", batch));
            }
        }

        return mixins;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

}
