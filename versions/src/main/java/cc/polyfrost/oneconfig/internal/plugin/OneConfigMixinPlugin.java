package cc.polyfrost.oneconfig.internal.plugin;

import cc.polyfrost.oneconfig.platform.Platform;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OneConfigMixinPlugin implements IMixinConfigPlugin {
    private static boolean isVigilance = false;

    @Override
    public void onLoad(String mixinPackage) {
        try {
            Class.forName("gg.essential.vigilance.Vigilant");
            isVigilance = true;
        } catch (Exception e) {
            isVigilance = false;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !targetClassName.contains("vigilance") || isVigilance;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        if (Platform.getInstance().getMinecraftVersion() >= 11600) {
            ArrayList<String> mixins = new ArrayList<>();
            if (Platform.getInstance().getLoader() == Platform.Loader.FORGE) {
                mixins.add("ClientModLoaderMixin");
            } else {
                mixins.add("FramebufferMixin");
                mixins.add("GameRendererAccessor");
            }
            mixins.add("KeyboardMixin");
            mixins.add("MouseAccessor");
            mixins.add("MouseMixin");
            mixins.add("TickTimeTrackerMixin");
            return mixins;
        } else {
            return null;
        }
    }

    @Override
    public void preApply(String targetClassName, org.spongepowered.asm.lib.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, org.spongepowered.asm.lib.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (mixinClassName.equals("cc.polyfrost.oneconfig.internal.mixin.VigilantMixin")) {
            transform(targetClass);
        }
    }

    /**
     * If anything here is changed, edit the corresponding method in OneConfigMixinPlugin!
     */
    private void transform(org.spongepowered.asm.lib.tree.ClassNode node) {
        if (!node.interfaces.contains("cc/polyfrost/oneconfig/internal/config/compatibility/vigilance/VigilantAccessor")) {
            node.fields.add(new org.spongepowered.asm.lib.tree.FieldNode(Opcodes.ACC_PUBLIC, "oneconfig$config", "Lcc/polyfrost/oneconfig/internal/config/compatibility/vigilance/VigilanceConfig;", null, null));
            node.fields.add(new org.spongepowered.asm.lib.tree.FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, "oneconfig$file", Type.getDescriptor(File.class), null, null));

            node.interfaces.add("cc/polyfrost/oneconfig/internal/config/compatibility/vigilance/VigilantAccessor");
            org.spongepowered.asm.lib.tree.MethodNode methodNode = new org.spongepowered.asm.lib.tree.MethodNode(Opcodes.ACC_PUBLIC, "getPropertyCollector", "()Lgg/essential/vigilance/data/PropertyCollector;", null, null);
            org.spongepowered.asm.lib.tree.LabelNode labelNode = new org.spongepowered.asm.lib.tree.LabelNode();
            methodNode.instructions.add(labelNode);
            methodNode.instructions.add(new org.spongepowered.asm.lib.tree.LineNumberNode(421421, labelNode));
            methodNode.instructions.add(new org.spongepowered.asm.lib.tree.VarInsnNode(Opcodes.ALOAD, 0));
            methodNode.instructions.add(new org.spongepowered.asm.lib.tree.FieldInsnNode(Opcodes.GETFIELD, "gg/essential/vigilance/Vigilant", "propertyCollector", "Lgg/essential/vigilance/data/PropertyCollector;"));
            methodNode.instructions.add(new org.spongepowered.asm.lib.tree.InsnNode(Opcodes.ARETURN));
            node.methods.add(methodNode);

            org.spongepowered.asm.lib.tree.MethodNode methodNode2 = new org.spongepowered.asm.lib.tree.MethodNode(Opcodes.ACC_PUBLIC, "handleOneConfigDependency", "(Lgg/essential/vigilance/data/PropertyData;Lgg/essential/vigilance/data/PropertyData;)V", null, null);
            org.spongepowered.asm.lib.tree.LabelNode labelNode2 = new org.spongepowered.asm.lib.tree.LabelNode();
            org.spongepowered.asm.lib.tree.LabelNode labelNode3 = new org.spongepowered.asm.lib.tree.LabelNode();
            org.spongepowered.asm.lib.tree.LabelNode labelNode4 = new org.spongepowered.asm.lib.tree.LabelNode();
            methodNode2.instructions.add(labelNode2);
            methodNode2.instructions.add(new org.spongepowered.asm.lib.tree.LineNumberNode(15636436, labelNode2));
            methodNode2.instructions.add(new org.spongepowered.asm.lib.tree.VarInsnNode(Opcodes.ALOAD, 0));
            methodNode2.instructions.add(new org.spongepowered.asm.lib.tree.FieldInsnNode(Opcodes.GETFIELD, "gg/essential/vigilance/Vigilant", "oneconfig$config", "Lcc/polyfrost/oneconfig/internal/config/compatibility/vigilance/VigilanceConfig;"));

            methodNode2.instructions.add(new org.spongepowered.asm.lib.tree.JumpInsnNode(Opcodes.IFNULL, labelNode4));

            methodNode2.instructions.add(labelNode3);
            methodNode2.instructions.add(new org.spongepowered.asm.lib.tree.LineNumberNode(15636437, labelNode3));
            methodNode2.instructions.add(new org.spongepowered.asm.lib.tree.VarInsnNode(Opcodes.ALOAD, 0));
            methodNode2.instructions.add(new org.spongepowered.asm.lib.tree.FieldInsnNode(Opcodes.GETFIELD, "gg/essential/vigilance/Vigilant", "oneconfig$config", "Lcc/polyfrost/oneconfig/internal/config/compatibility/vigilance/VigilanceConfig;"));
            methodNode2.instructions.add(new org.spongepowered.asm.lib.tree.VarInsnNode(Opcodes.ALOAD, 1));
            methodNode2.instructions.add(new org.spongepowered.asm.lib.tree.VarInsnNode(Opcodes.ALOAD, 2));
            methodNode2.instructions.add(new org.spongepowered.asm.lib.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cc/polyfrost/oneconfig/internal/config/compatibility/vigilance/VigilanceConfig", "addDependency", "(Lgg/essential/vigilance/data/PropertyData;Lgg/essential/vigilance/data/PropertyData;)V", false));

            methodNode2.instructions.add(labelNode4);
            methodNode2.instructions.add(new org.spongepowered.asm.lib.tree.LineNumberNode(15636438, labelNode4));
            methodNode2.instructions.add(new org.spongepowered.asm.lib.tree.InsnNode(Opcodes.RETURN));
            node.methods.add(methodNode2);

            for (org.spongepowered.asm.lib.tree.MethodNode method : node.methods) {
                if (method.name.equals("initialize")) {
                    org.spongepowered.asm.lib.tree.InsnList list = new org.spongepowered.asm.lib.tree.InsnList();
                    list.add(new org.spongepowered.asm.lib.tree.VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new org.spongepowered.asm.lib.tree.VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new org.spongepowered.asm.lib.tree.VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new org.spongepowered.asm.lib.tree.FieldInsnNode(Opcodes.GETFIELD, "gg/essential/vigilance/Vigilant", "oneconfig$file", Type.getDescriptor(File.class)));
                    list.add(new org.spongepowered.asm.lib.tree.MethodInsnNode(Opcodes.INVOKESTATIC, "cc/polyfrost/oneconfig/internal/plugin/hooks/VigilantHook", "returnNewConfig", "(Lgg/essential/vigilance/Vigilant;Ljava/io/File;)Lcc/polyfrost/oneconfig/internal/config/compatibility/vigilance/VigilanceConfig;", false));
                    list.add(new org.spongepowered.asm.lib.tree.FieldInsnNode(Opcodes.PUTFIELD, "gg/essential/vigilance/Vigilant", "oneconfig$config", "Lcc/polyfrost/oneconfig/internal/config/compatibility/vigilance/VigilanceConfig;"));
                    method.instructions.insertBefore(method.instructions.getLast().getPrevious(), list);
                } else if (method.name.equals("addDependency") && method.desc.equals("(Lgg/essential/vigilance/data/PropertyData;Lgg/essential/vigilance/data/PropertyData;)V")) {
                    org.spongepowered.asm.lib.tree.InsnList list = new org.spongepowered.asm.lib.tree.InsnList();

                    list.add(new org.spongepowered.asm.lib.tree.VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new org.spongepowered.asm.lib.tree.VarInsnNode(Opcodes.ALOAD, 1));
                    list.add(new org.spongepowered.asm.lib.tree.VarInsnNode(Opcodes.ALOAD, 2));
                    list.add(new org.spongepowered.asm.lib.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL, "gg/essential/vigilance/Vigilant", "handleOneConfigDependency", "(Lgg/essential/vigilance/data/PropertyData;Lgg/essential/vigilance/data/PropertyData;)V", false));

                    method.instructions.insertBefore(method.instructions.getLast().getPrevious(), list);
                } else if (method.name.equals("<init>") && method.desc.equals("(Ljava/io/File;Ljava/lang/String;Lgg/essential/vigilance/data/PropertyCollector;Lgg/essential/vigilance/data/SortingBehavior;)V")) {
                    org.spongepowered.asm.lib.tree.InsnList list = new org.spongepowered.asm.lib.tree.InsnList();
                    list.add(new org.spongepowered.asm.lib.tree.VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new org.spongepowered.asm.lib.tree.VarInsnNode(Opcodes.ALOAD, 1));
                    list.add(new org.spongepowered.asm.lib.tree.FieldInsnNode(Opcodes.PUTFIELD, "gg/essential/vigilance/Vigilant", "oneconfig$file", Type.getDescriptor(File.class)));
                    method.instructions.insertBefore(method.instructions.getLast().getPrevious(), list);
                }
            }
        }
    }
}
