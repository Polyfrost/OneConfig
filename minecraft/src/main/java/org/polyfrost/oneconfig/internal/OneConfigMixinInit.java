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
import org.polyfrost.oneconfig.internal.generated.RelocatedMixins;
//? }
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OneConfigMixinInit implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/MixinInit");

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        //? skyblocker_compat {
        if (mixinClassName.endsWith(".Mixin_SkyblockerFancyStatusBarsV2")) return skyblockerBarsAreInstanced();
        if (mixinClassName.endsWith(".Mixin_SkyblockerFancyStatusBars")) return !skyblockerBarsAreInstanced();
        //? }
        return true;
    }

    //? skyblocker_compat {
    private static Boolean skyblockerInstanced;

    private static boolean skyblockerBarsAreInstanced() {
        if (skyblockerInstanced == null) {
            skyblockerInstanced = false;
            String path = "de/hysky/skyblocker/skyblock/fancybars/FancyStatusBars.class";
            try (java.io.InputStream in = OneConfigMixinInit.class.getClassLoader().getResourceAsStream(path)) {
                if (in != null) {
                    ClassNode node = new ClassNode();
                    new ClassReader(in).accept(node, ClassReader.SKIP_CODE);
                    for (org.objectweb.asm.tree.MethodNode method : node.methods) {
                        //~ if >= 26.1 'render' -> 'extractRenderState'
                        if (!method.name.equals("extractRenderState")) continue;
                        skyblockerInstanced = (method.access & org.objectweb.asm.Opcodes.ACC_STATIC) == 0;
                        break;
                    }
                }
            } catch (Throwable t) {
                LOGGER.warn("Could not read the installed Skyblocker's status bar shape, assuming static", t);
            }
        }
        return skyblockerInstanced;
    }
    //? }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        List<String> mixins = new ArrayList<>();


        //? moul_compat {
        RelocatedMixins.INSTANCE.register(e -> {
            mixins.add(e);
            return Unit.INSTANCE;
        });
        //? }
        //? moul_compat {
        mixins.add("compat.moulconfig.Mixin_MCConfigEditorIntegration_Firmament");
        // unrelocated targets, e.g. Skysoft's SoftConfig
        mixins.add("compat.moulconfig.Mixin_ConfigProcessorDriver");
        mixins.add("compat.moulconfig.Mixin_MoulConfigProcessor");
        mixins.add("compat.moulconfig.Mixin_MoulConfigEditor");
        mixins.add("compat.moulconfig.Mixin_PropertyImpl");
        mixins.add("compat.moulconfig.Mixin_GuiOptionEditorSlider");
        mixins.add("compat.moulconfig.Mixin_GuiOptionEditorDropdown");
        //? }

        //? dandelion_compat
        //mixins.add("compat.DandelionScreenImplMixin");

        //? odin_compat
        //mixins.add("compat.odin.Mixin_OdinModuleManager");

        //? rconfig_compat
        mixins.add("compat.rconfig.Mixin_Configurations");

        mixins.add("Mixin_SimpleReloadInstance");
        mixins.add("Mixin_MainMenuFpsUncap");
        //? yacl_compat
        mixins.add("compat.yacl.Mixin_YetAnotherConfigLib_Builder");

        //? clothconfig_compat
        mixins.add("compat.cloth.Mixin_ConfigBuilderImpl");

        //? midnightlib_compat
        mixins.add("compat.midnightlib.Mixin_MidnightConfig");
        //? midnightlib_compat
        mixins.add("compat.midnightlib.SliderButtonAccessor");

        //? walksylib_compat
        mixins.add("compat.walksylib.Mixin_WalksyLib_ModEntryPointList");

        //? tr7zw_compat
        mixins.add("compat.tr7zw.Mixin_AbstractConfigScreen");

        //? skycubed_compat {
        /*mixins.add("compat.skycubed.Mixin_SkyCubed");
        mixins.add("compat.skycubed.Mixin_SkyCubedOverlays");
        *///? }

        //? skyblocker_compat {
        mixins.add("compat.skyblocker.Mixin_SkyblockerFancyStatusBars");
        mixins.add("compat.skyblocker.Mixin_SkyblockerFancyStatusBarsV2");
        mixins.add("compat.skyblocker.Mixin_SkyblockerWidgetManager");
        //? }

        //? skyblocker_legacy_hud
        //mixins.add("compat.skyblocker.Mixin_SkyblockerScreenBuilder");

        //? skyblocker_hud_v2
        mixins.add("compat.skyblocker.Mixin_SkyblockerLayerBuilder");

        //? stella_compat
        mixins.add("compat.stella.Mixin_Stella");

        //? apec_compat
        //mixins.add("compat.apec.Mixin_ApecMenu");

        mixins.add("compat.skyhanni.Mixin_SkyHanniRenderData");

        mixins.add("compat.firmament.Mixin_FirmamentHudMeta");
        // Firmament has no stable release for 26.2 yet
        //? >= 1.21.8 && < 26.2
        //mixins.add("compat.firmament.Mixin_FirmamentContentCapture");

        //? modmenu_compat
        mixins.add("compat.Mixin_ModMenu");

        //? neoforge {
        //mixins.add("events.Mixin_ChatReceiveEvent_Forge");
        //mixins.add("events.Mixin_ScreenOpenEvent_Forge");
        //? } else {
        mixins.add("events.Mixin_ScreenOpenEvent_Fabric");
        //? }

        mixins.add("events.Mixin_ModernWindowFocusEvent");

        mixins.add("skia.Mixin_InitSkia");
        mixins.add("skia.Mixin_SkiaFrame");
        mixins.add("skia.Mixin_SkiaFrameVk");
        //? >= 26.1 {
        mixins.add("blaze3d.GpuDeviceAccessor");
        mixins.add("blaze3d.GlDeviceAccessor");
        mixins.add("skia.Mixin_SkiaFramePresent");
        //? }
        //? >= 1.21.8 {
        mixins.add("skia.Mixin_GuiRendererLegacyTarget");
        mixins.add("render.GameRendererAccessor");
        mixins.add("render.GuiRendererAccessor");
        //? }
        //? if < 1.21.8
        //mixins.add("skia.Mixin_MainTargetRedirect");
        mixins.add("skia.Mixin_DebugOverlayAboveUi");
        //? < 26.1 {
        /*mixins.add("skia.Mixin_ScreenshotComposite");
        *///? }
        mixins.add("skia.Mixin_InitSkiaFontRenderer");
        mixins.add("skia.Mixin_FixComposeRaceCondition");

        //? if >= 1.21.10 {
        mixins.add("keybind.Mixin_KeybindCategoryLabel");
        //?} else
        //mixins.add("keybind.KeyMappingCategoryAccessor");

        mixins.add("keybind.Mixin_OneConfigKeybindRebind");
        mixins.add("keybind.Mixin_KeyMappingResetDetect");
        mixins.add("keybind.Mixin_OptionsSaveDetect");

        //? cinnabar
        //mixins.add("skia.Mixin_CinnabarSkiaFlush");

        if (isClassPresent("net.vulkanmod.vulkan.Renderer")) {
            mixins.add("skia.Mixin_VulkanModSkiaFlush");
            //? >= 1.21.10 {
            mixins.add("skia.Mixin_VulkanModBlurSnapshot");
            //? }
        }

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

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, OneConfigMixinInit.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

}
