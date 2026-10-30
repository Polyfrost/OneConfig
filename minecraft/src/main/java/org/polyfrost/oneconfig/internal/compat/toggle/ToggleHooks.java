package org.polyfrost.oneconfig.internal.compat.toggle;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The mixins in {@code mixin.compat.toggle} each mod's switch relies on, see ModGates
 * <br>
 * They hook other mods by method name and were written against one build of each. A hook that finds
 * nothing is skipped by Mixin but one whose handler does not fit what it finds (static against
 * instance or the wrong return type) fails the whole target class, so those mixins are left out for
 * the build of the mod that is installed and the mod is then not given a switch
 * <br>
 * Read by the mixin plugin before the game is up, so this has to stay clear of everything but ASM
 */
public final class ToggleHooks {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/ModToggles");
    private static final String MIXIN_PACKAGE = "org.polyfrost.oneconfig.internal.mixin.";
    private static final String PREFIX = "compat.toggle.Mixin_Toggle_";
    private static final String MODIFY_RETURN_VALUE = "/injector/ModifyReturnValue;";

    /** The key of the hooks every config saved by YACL relies on */
    public static final String YACL = "yacl";

    private static final Map<String, List<String>> HOOKS = new LinkedHashMap<>();

    /** Hooks that keep a mask from being written to disk, which have to be in place and not merely fit */
    private static final Set<String> GUARDS = new HashSet<>(Arrays.asList("YaclSave", "MidnightSave", "EffectTimerPlusSave", "ShulkerBoxTooltipSave"));

    static {
        hook("appleskin", "AppleSkinHud", "AppleSkinTooltip");
        hook("betterhurtcam", "BetterHurtCam");
        hook("blur", "Blur", "BlurAnimation", "MidnightSave", "MidnightLoad");
        hook("bobby", "Bobby");
        hook("chatblock", "ChatBlockLegacy", "ChatBlockReceiving", "ChatBlockSending");
        hook("controlify", "Controlify");
        hook("custom-block-highlight", "BlockHighlight", "BlockHighlightLegacy", "BlockHighlightOutline");
        hook("detailabreconst", "DetailArmorBar", "DetailArmorBarDurability", "DetailArmorBarInventory", "DetailArmorBarRenderer");
        //? if >= 1.21.1 && < 1.21.10
        //hook("detailabreconst", "DetailArmorBarVanilla");
        hook("effecttimerplus", "EffectTimerPlusSave");
        hook("flashback", "Flashback");
        hook("fovchanger", "FovChanger");
        hook("freelook", "Freelook");
        hook("gammautils", "GammaUtilsGamma", "GammaUtilsGammaManager", "GammaUtilsNightVision", "GammaUtilsNightVisionManager");
        hook("hybedwars", "HyBedWarsConfig", "HyBedWarsHud", "HyBedWarsLocation");
        hook("hybridge", "HyBridgeConfig", "HyBridgeLocation");
        hook("hychatter", "HyChatterConfig", "HyChatterLocation", "HyChatterWaypoints");
        hook("hyinfo", "HyInfoLocation", "HyInfoNametags");
        hook("hylobby", "HyLobby");
        hook("hymod", "HyModConfig", "HyModLocation");
        hook("jade", "Jade");
        hook("lambdabettergrass", "LambdaBetterGrassLayer", "LambdaBetterGrassMode");
        hook("lambdynlights", "LambDynamicLights");
        hook("mountopacity", "MountOpacity");
        hook("legacy-skin-layers", "LegacySkinLayers");
        hook("legacy-waveycapes", "LegacyWaveyCapes");
        hook("presencefootsteps", "PresenceFootsteps");
        hook("sciophobia", "Sciophobia");
        hook("shulkerboxtooltip", "ShulkerBoxTooltip", "ShulkerBoxTooltipJoin", "ShulkerBoxTooltipSave");
        hook("skinlayers3d", "SkinLayers", "SkinLayersHeads");
        //? if >= 1.21.1
        hook("skinlayers3d", "SkinLayersSkulls");
        hook("status-effect-bars", "StatusEffectBars");
        // has a switch of its own that one part of it ignores
        hook("tipper", "Tipper");
        hook("tooltipscroll", "TooltipScroll");
        hook("viewmodel", "Viewmodel");
        hook("vignettetweaks", "VignetteAir", "VignetteHealth", "VignetteHunger", "VignetteTotem");
        //? if >= 1.21.1
        hook("waveycapes", "WaveyCapes");
        hook("wwaypoints", "WWaypoints");
        hook(YACL, "YaclSave");
        hook("yacl-load", "YaclLoad");
        hook("zoomify", "Zoomify");
        //? if >= 1.21.1
        hook("zoomify", "ZoomifySpyglass");
    }

    private ToggleHooks() {
    }

    private static void hook(String gate, String... mixins) {
        HOOKS.computeIfAbsent(gate, ignored -> new ArrayList<>()).addAll(Arrays.asList(mixins));
    }

    /** Every toggle mixin of this version, named the way the mixin plugin lists them */
    public static List<String> mixins() {
        List<String> mixins = new ArrayList<>();
        for (List<String> names : HOOKS.values()) {
            for (String name : names) mixins.add(PREFIX + name);
        }
        return mixins;
    }

    public static boolean isToggleMixin(String mixinClassName) {
        return mixinClassName.contains(".compat.toggle.");
    }

    public static boolean has(String gate) {
        return HOOKS.containsKey(gate);
    }

    /**
     * Whether any hook of {@code gate} finds a method of the mod to go onto, without which a mod that
     * is only switched off by its hooks would keep running
     */
    public static boolean lands(String gate) {
        for (String name : HOOKS.getOrDefault(gate, Collections.emptyList())) {
            try {
                ClassNode mixin = readClass(MIXIN_PACKAGE + PREFIX + name);
                if (mixin == null) continue;
                for (String targetName : targets(mixin)) {
                    ClassNode target = readClass(targetName);
                    if (target != null && hooksAnything(target, mixin)) return true;
                }
            } catch (Throwable t) {
                LOGGER.warn("could not check the toggle hook {}", name, t);
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static boolean hooksAnything(ClassNode target, ClassNode mixin) {
        for (MethodNode handler : mixin.methods) {
            if (handler.visibleAnnotations == null) continue;
            for (AnnotationNode annotation : handler.visibleAnnotations) {
                if (!isHook(annotation) || annotation.values == null) continue;
                int at = annotation.values.indexOf("method");
                if (at < 0) continue;
                for (String spec : (List<String>) annotation.values.get(at + 1)) {
                    if (declares(target, spec)) return true;
                }
            }
        }
        return false;
    }

    /** Whether {@code mixinClassName} can go onto the build of {@code targetClassName} that is installed */
    public static boolean fits(String targetClassName, String mixinClassName) {
        try {
            ClassNode target = readClass(targetClassName);
            ClassNode mixin = readClass(mixinClassName);
            if (target == null || mixin == null) return true;
            String problem = problem(target, mixin, false);
            if (problem == null) return true;
            LOGGER.warn("Not applying {} to {}: {}", mixinClassName, targetClassName, problem);
            return false;
        } catch (Throwable t) {
            LOGGER.warn("could not check {} against {}, skipping it", mixinClassName, targetClassName, t);
            return false;
        }
    }

    /**
     * What keeps the hooks of {@code gate} from all going onto the builds that are installed, the same way
     * {@link #fits} decides it for each of them once its target is loaded
     *
     * @return null when nothing does
     */
    public static String problem(String gate) {
        for (String name : HOOKS.getOrDefault(gate, Collections.emptyList())) {
            boolean guard = GUARDS.contains(name);
            try {
                ClassNode mixin = readClass(MIXIN_PACKAGE + PREFIX + name);
                if (mixin == null) return name + " is missing";
                boolean found = false;
                for (String targetName : targets(mixin)) {
                    ClassNode target = readClass(targetName);
                    if (target == null) continue;
                    found = true;
                    String problem = problem(target, mixin, guard);
                    if (problem != null) return name + ": " + problem;
                }
                if (guard && !found) return name + " has nothing to go onto";
            } catch (Throwable t) {
                LOGGER.warn("could not check the toggle hook {}", name, t);
                return name + " could not be checked";
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> targets(ClassNode mixin) {
        if (mixin.invisibleAnnotations == null && mixin.visibleAnnotations == null) return Collections.emptyList();
        List<AnnotationNode> annotations = new ArrayList<>();
        if (mixin.visibleAnnotations != null) annotations.addAll(mixin.visibleAnnotations);
        if (mixin.invisibleAnnotations != null) annotations.addAll(mixin.invisibleAnnotations);
        for (AnnotationNode annotation : annotations) {
            if (!annotation.desc.endsWith("/mixin/Mixin;") || annotation.values == null) continue;
            int at = annotation.values.indexOf("targets");
            // a mixin into the game itself names its target by class and is none of a mod's business
            if (at >= 0) return (List<String>) annotation.values.get(at + 1);
        }
        return Collections.emptyList();
    }

    /** @param complete whether every handler also has to find something to hook */
    @SuppressWarnings("unchecked")
    private static String problem(ClassNode target, ClassNode mixin, boolean complete) {
        for (MethodNode handler : mixin.methods) {
            if (handler.visibleAnnotations == null) continue;
            for (AnnotationNode annotation : handler.visibleAnnotations) {
                if (!isHook(annotation) || annotation.values == null) continue;
                int at = annotation.values.indexOf("method");
                if (at < 0) continue;
                List<String> specs = (List<String>) annotation.values.get(at + 1);
                boolean hooked = false;
                for (String spec : specs) {
                    String problem = hookProblem(target, handler, spec, annotation.desc.endsWith(MODIFY_RETURN_VALUE));
                    if (problem != null) return spec + " " + problem;
                    hooked |= declares(target, spec);
                }
                if (complete && !hooked) return String.join("/", specs) + " is not in this build";
            }
        }
        return null;
    }

    private static boolean declares(ClassNode target, String spec) {
        int paren = spec.indexOf('(');
        String name = paren < 0 ? spec : spec.substring(0, paren);
        String desc = paren < 0 ? null : spec.substring(paren);
        for (MethodNode method : target.methods) {
            if (method.name.equals(name) && (desc == null || method.desc.equals(desc))) return true;
        }
        return false;
    }

    private static boolean isHook(AnnotationNode annotation) {
        return annotation.desc.endsWith("/injection/Inject;") || annotation.desc.endsWith(MODIFY_RETURN_VALUE);
    }

    private static String hookProblem(ClassNode target, MethodNode handler, String spec, boolean modifiesReturn) {
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
            if (modifiesReturn) {
                if (!type.equals(Type.getReturnType(handler.desc))) return "returns " + type.getClassName() + " in this build";
            } else if (!returnable) {
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

    private static ClassNode readClass(String className) throws IOException {
        try (InputStream in = ToggleHooks.class.getClassLoader()
                .getResourceAsStream(className.replace('.', '/') + ".class")) {
            if (in == null) return null;
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }
}
