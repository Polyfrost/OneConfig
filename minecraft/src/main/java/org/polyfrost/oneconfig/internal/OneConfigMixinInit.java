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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

//todo import org.polyfrost.oneconfig.internal.generated.RelocatedMixins;
//? if moul_compat {
import kotlin.Unit;
import org.polyfrost.oneconfig.internal.generated.RelocatedMixins;
//?}

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
        if (mixinClassName.contains(".compat.toggle.")) return toggleHooksFit(targetClassName, mixinClassName);
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        List<String> mixins = new ArrayList<>();

        //? if = 1.8.9 {
        /*mixins.add("command.Mixin_LegacyChatCompletion");
        mixins.add("fixes.Mixin_RememberUnknownOptions");
        *///?}

        //? moul_compat {
        RelocatedMixins.INSTANCE.register(e -> {
            mixins.add(e);
            return Unit.INSTANCE;
        });
        //? }
        //? moul_compat {
        mixins.add("compat.moulconfig.Mixin_MCConfigEditorIntegration_Firmament");
        // unrelocated targets, e.g. SoftConfig in Skysoft < 0.1.30
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
        //mixins.add("compat.rconfig.Mixin_Configurations");

        //? osl_config_compat
        //mixins.add("compat.osl.Mixin_OslConfigManager");

        mixins.add("Mixin_SimpleReloadInstance");
        mixins.add("Mixin_MainMenuFpsUncap");
        //? if > 1.8.9
        mixins.add("Mixin_VersionedResourcePacks");
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

        //? ukulib_compat
        mixins.add("compat.ukulib.Mixin_BaseConfigScreen");

        //? axolotlclient_config_compat
        mixins.add("compat.axolotlclient.Mixin_AxolotlClientConfigImpl");

        //? mlib_compat
        //mixins.add("compat.mlib.Mixin_MlibOverlays");

        //? skyblocker_compat {
        /*Boolean skyblockerSingleton = declaresStaticMethod("de.hysky.skyblocker.skyblock.fancybars.FancyStatusBars", "initStatic");
        if (skyblockerSingleton != null) {
            mixins.add(skyblockerSingleton
                    ? "compat.skyblocker.Mixin_SkyblockerFancyStatusBarsInstance"
                    : "compat.skyblocker.Mixin_SkyblockerFancyStatusBarsStatic");
        }
        mixins.add("compat.skyblocker.Mixin_SkyblockerWidgetManager");
        *///? }

        //? skyblocker_legacy_hud
        //mixins.add("compat.skyblocker.Mixin_SkyblockerScreenBuilder");

        //? skyblocker_hud_v2
        //mixins.add("compat.skyblocker.Mixin_SkyblockerLayerBuilder");

        //? stella_compat
        //mixins.add("compat.stella.Mixin_Stella");

        //? apec_compat
        //mixins.add("compat.apec.Mixin_ApecMenu");

        //? if > 1.8.9 {
        mixins.add("compat.skyhanni.Mixin_SkyHanniRenderData");

        mixins.add("compat.armorhud.Mixin_ArmorHudWidgetShown");

        mixins.add("compat.firmament.Mixin_FirmamentHudMeta");
        //?}
        // Firmament has no stable release for 26.2 yet
        //? >= 1.21.8 && < 26.2
        //mixins.add("compat.firmament.Mixin_FirmamentContentCapture");

        // master switches for mods that do not have one, see ModGates
        for (String toggle : new String[]{
                "AppleSkinHud", "AppleSkinTooltip", "BetterScreens", "BetterScreensScale", "BetterScreensServer",
                "BlockHighlight", "BlockHighlightLegacy", "BlockHighlightOutline", "Blur", "ChatBlockLegacy",
                "DetailArmorBarRenderer", "LegacySkyblockLocation",
                "BetterHurtCam", "HyBedWarsConfig", "HyBedWarsHud", "HyBedWarsLocation",
                "HyBridgeConfig", "HyBridgeLocation", "HyChatterConfig", "HyChatterLocation", "HyChatterWaypoints",
                "HyInfoLocation", "HyInfoNametags", "HyLobby", "PolyZoom", "Tipper",
                "Freelook", "BlurAnimation", "Bobby", "ChatBlockReceiving",
                "ChatBlockSending", "Chatting", "Controlify", "CustomScoreboard", "DetailArmorBar", "DetailArmorBarDurability",
                "DetailArmorBarInventory", "EffectTimerPlusSave", "Flashback", "FovChanger", "GammaUtilsGamma",
                "GammaUtilsGammaManager", "GammaUtilsNightVision", "GammaUtilsNightVisionManager", "HyModConfig",
                "HyModLocation", "Iconographic", "Jade", "LambdaBetterGrassLayer", "LambdaBetterGrassMode", "LegacySkyblock",
                "MidnightSave", "MountOpacity", "OverflowParticlesConfig", "OverflowParticlesManager",
                "OverflowParticlesParticle", "PresenceFootsteps", "Redaction", "Sciophobia", "ShulkerBoxTooltip",
                "SkyBlockItemList", "SkyBlockPvButton", "SkyBlockPvChat", "SkyBlockPvPartyFinder", "Skyblocker",
                "StatusEffectBars", "TooltipScroll", "Viewmodel", "VignetteAir", "VignetteHealth", "VignetteHunger",
                "VignetteTotem", "WWaypoints", "YaclSave", "Zoomify",
        }) {
            mixins.add("compat.toggle.Mixin_Toggle_" + toggle);
        }
        //? if >= 26.1
        mixins.add("compat.toggle.Mixin_Toggle_WaveyCapes");
        //? if >= 1.21.1 && < 1.21.10
        //mixins.add("compat.toggle.Mixin_Toggle_DetailArmorBarVanilla");

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
        //? if > 1.8.9
        mixins.add("skia.Mixin_ItemAtlasScissor");
        //? if < 1.21.8
        //mixins.add("skia.Mixin_MainTargetRedirect");
        mixins.add("skia.Mixin_DebugOverlayAboveUi");
        //? if < 26.1 && > 1.8.9 {
        /*mixins.add("skia.Mixin_ScreenshotComposite");
        *///? }
        mixins.add("skia.Mixin_InitSkiaFontRenderer");
        mixins.add("skia.Mixin_StartupWarmupOverlay");

        //? if >= 1.21.10 {
        mixins.add("keybind.Mixin_KeybindCategoryLabel");
        //?} elif > 1.8.9
        //mixins.add("keybind.KeyMappingCategoryAccessor");

        mixins.add("keybind.Mixin_OneConfigKeybindRebind");
        mixins.add("keybind.Mixin_KeyMappingResetDetect");
        mixins.add("keybind.Mixin_OptionsSaveDetect");
        mixins.add("keybind.Mixin_OptionsSkipMirrors");

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

    private static Boolean declaresStaticMethod(String className, String methodName) {
        try (InputStream in = OneConfigMixinInit.class.getClassLoader()
                .getResourceAsStream(className.replace('.', '/') + ".class")) {
            if (in == null) return null;
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            for (MethodNode method : node.methods) {
                if (method.name.equals(methodName) && (method.access & Opcodes.ACC_STATIC) != 0) return Boolean.TRUE;
            }
            return Boolean.FALSE;
        } catch (Throwable t) {
            LogManager.getLogger(OneConfigMixinInit.class)
                    .warn("could not read {} to pick a mixin shape, skipping the mixins that depend on it", className, t);
            return null;
        }
    }

    private static ClassNode readClass(String className) throws java.io.IOException {
        try (InputStream in = OneConfigMixinInit.class.getClassLoader()
                .getResourceAsStream(className.replace('.', '/') + ".class")) {
            if (in == null) return null;
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    /**
     * The toggle mixins hook other mods by method name and were written against one build of each
     * <br>
     * A hook that finds nothing is skipped by Mixin but one whose handler does not fit what it finds
     * (static against instance or the wrong return type) fails the whole target class, so those
     * mixins are left out for the build of the mod that is installed
     */
    @SuppressWarnings("unchecked")
    private static boolean toggleHooksFit(String targetClassName, String mixinClassName) {
        try {
            ClassNode target = readClass(targetClassName);
            ClassNode mixin = readClass(mixinClassName);
            if (target == null || mixin == null) return true;
            for (MethodNode handler : mixin.methods) {
                if (handler.visibleAnnotations == null) continue;
                for (AnnotationNode annotation : handler.visibleAnnotations) {
                    if (!annotation.desc.endsWith("/injection/Inject;") || annotation.values == null) continue;
                    int at = annotation.values.indexOf("method");
                    if (at < 0) continue;
                    for (String spec : (List<String>) annotation.values.get(at + 1)) {
                        String problem = hookProblem(target, handler, spec);
                        if (problem == null) continue;
                        LogManager.getLogger(OneConfigMixinInit.class).warn(
                                "Not applying {} to {}: {} {}", mixinClassName, targetClassName, spec, problem);
                        return false;
                    }
                }
            }
            return true;
        } catch (Throwable t) {
            LogManager.getLogger(OneConfigMixinInit.class)
                    .warn("could not check {} against {}, skipping it", mixinClassName, targetClassName, t);
            return false;
        }
    }

    private static String hookProblem(ClassNode target, MethodNode handler, String spec) {
        int paren = spec.indexOf('(');
        String name = paren < 0 ? spec : spec.substring(0, paren);
        String desc = paren < 0 ? null : spec.substring(paren);
        boolean handlerStatic = (handler.access & Opcodes.ACC_STATIC) != 0;
        boolean returnable = handler.desc.contains("CallbackInfoReturnable;");
        String returned = returnedType(handler);

        for (MethodNode method : target.methods) {
            if (!method.name.equals(name) || (desc != null && !method.desc.equals(desc))) continue;
            if (((method.access & Opcodes.ACC_STATIC) != 0) != handlerStatic) {
                return handlerStatic ? "is not static in this build" : "is static in this build";
            }
            Type type = Type.getReturnType(method.desc);
            if (!returnable) {
                if (type.getSort() != Type.VOID) return "returns a value in this build";
            } else if (type.getSort() == Type.VOID) {
                return "returns nothing in this build";
            } else if (returned != null && !returned.equals(type.getDescriptor())) {
                return "returns " + type.getClassName() + " in this build";
            } else if (returned == null && type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY) {
                return "returns " + type.getClassName() + " in this build";
            }
        }
        return null;
    }

    /** The primitive a handler's CallbackInfoReturnable is for or null when it is for an object */
    private static String returnedType(MethodNode handler) {
        if (handler.signature == null) return null;
        if (handler.signature.contains("CallbackInfoReturnable<Ljava/lang/Boolean;>")) return "Z";
        if (handler.signature.contains("CallbackInfoReturnable<Ljava/lang/Float;>")) return "F";
        if (handler.signature.contains("CallbackInfoReturnable<Ljava/lang/Integer;>")) return "I";
        if (handler.signature.contains("CallbackInfoReturnable<Ljava/lang/Double;>")) return "D";
        if (handler.signature.contains("CallbackInfoReturnable<Ljava/lang/Long;>")) return "J";
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

}
