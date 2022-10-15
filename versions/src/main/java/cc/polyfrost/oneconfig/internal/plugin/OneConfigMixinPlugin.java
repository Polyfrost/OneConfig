/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
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

package cc.polyfrost.oneconfig.internal.plugin;

import cc.polyfrost.oneconfig.platform.Platform;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.lib.tree.JumpInsnNode;
import org.spongepowered.asm.lib.tree.LabelNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.FieldNode;
import org.spongepowered.asm.lib.tree.LineNumberNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;
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
        List<String> mixins = new ArrayList<>();
        if (Platform.getInstance().getLoader().equals(Platform.Loader.FORGE)) {
            mixins.add("EventBusMixin");
        } else if (Platform.getInstance().getLoader().equals(Platform.Loader.FABRIC)) {
            mixins.add("GameRendererAccessor");
            mixins.add("NetHandlerPlayClientMixin");
            mixins.add("FramebufferMixin");
            if (Platform.getInstance().getMinecraftVersion() <= 11202) {
                mixins.add("commands.ChatScreenMixin");
                mixins.add("commands.ScreenMixin");
            }
        }
        if (Platform.getInstance().getMinecraftVersion() >= 11600 || Platform.getInstance().getLoader() == Platform.Loader.FABRIC) {
            mixins.add("TickTimeTrackerMixin");
        }
        if (Platform.getInstance().getMinecraftVersion() >= 11600) {
            if (Platform.getInstance().getLoader() == Platform.Loader.FORGE) {
                mixins.add("ClientModLoaderMixin");
            }
            mixins.add("KeyboardMixin");
            mixins.add("MouseAccessor");
            mixins.add("MouseMixin");
        }
        if (Platform.getInstance().getLoader() == Platform.Loader.FORGE) {
            if (Platform.getInstance().getMinecraftVersion() == 10800 || Platform.getInstance().getMinecraftVersion() == 11200) {
                mixins.add("HudCachingMixin");
            }
        }
        return mixins.isEmpty() ? null : mixins;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (mixinClassName.equals("cc.polyfrost.oneconfig.internal.mixin.VigilantMixin")) {
            transform(targetClass);
        }
    }

    /**
     * If anything here is changed, edit the corresponding method in OneConfigMixinPlugin!
     */
    private void transform(ClassNode node) {
        if (!node.interfaces.contains("cc/polyfrost/oneconfig/internal/config/compatibility/vigilance/VigilantAccessor")) {
            node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "oneconfig$config", "Lcc/polyfrost/oneconfig/internal/config/compatibility/vigilance/VigilanceConfig;", null, null));
            node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, "oneconfig$file", Type.getDescriptor(File.class), null, null));

            node.interfaces.add("cc/polyfrost/oneconfig/internal/config/compatibility/vigilance/VigilantAccessor");
            MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "getPropertyCollector", "()Lgg/essential/vigilance/data/PropertyCollector;", null, null);
            LabelNode labelNode = new LabelNode();
            methodNode.instructions.add(labelNode);
            methodNode.instructions.add(new LineNumberNode(421421, labelNode));
            methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            methodNode.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "gg/essential/vigilance/Vigilant", "propertyCollector", "Lgg/essential/vigilance/data/PropertyCollector;"));
            methodNode.instructions.add(new InsnNode(Opcodes.ARETURN));
            node.methods.add(methodNode);

            MethodNode methodNode2 = new MethodNode(Opcodes.ACC_PUBLIC, "handleOneConfigDependency", "(Lgg/essential/vigilance/data/PropertyData;Lgg/essential/vigilance/data/PropertyData;)V", null, null);
            LabelNode labelNode2 = new LabelNode();
            LabelNode labelNode3 = new LabelNode();
            LabelNode labelNode4 = new LabelNode();
            methodNode2.instructions.add(labelNode2);
            methodNode2.instructions.add(new LineNumberNode(15636436, labelNode2));
            methodNode2.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            methodNode2.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "gg/essential/vigilance/Vigilant", "oneconfig$config", "Lcc/polyfrost/oneconfig/internal/config/compatibility/vigilance/VigilanceConfig;"));

            methodNode2.instructions.add(new JumpInsnNode(Opcodes.IFNULL, labelNode4));

            methodNode2.instructions.add(labelNode3);
            methodNode2.instructions.add(new LineNumberNode(15636437, labelNode3));
            methodNode2.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            methodNode2.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "gg/essential/vigilance/Vigilant", "oneconfig$config", "Lcc/polyfrost/oneconfig/internal/config/compatibility/vigilance/VigilanceConfig;"));
            methodNode2.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            methodNode2.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
            methodNode2.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "cc/polyfrost/oneconfig/internal/config/compatibility/vigilance/VigilanceConfig", "addDependency", "(Lgg/essential/vigilance/data/PropertyData;Lgg/essential/vigilance/data/PropertyData;)V", false));

            methodNode2.instructions.add(labelNode4);
            methodNode2.instructions.add(new LineNumberNode(15636438, labelNode4));
            methodNode2.instructions.add(new InsnNode(Opcodes.RETURN));
            node.methods.add(methodNode2);

            for (MethodNode method : node.methods) {
                if (method.name.equals("initialize")) {
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new FieldInsnNode(Opcodes.GETFIELD, "gg/essential/vigilance/Vigilant", "oneconfig$file", Type.getDescriptor(File.class)));
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "cc/polyfrost/oneconfig/internal/plugin/hooks/VigilantHook", "returnNewConfig", "(Lgg/essential/vigilance/Vigilant;Ljava/io/File;)Lcc/polyfrost/oneconfig/internal/config/compatibility/vigilance/VigilanceConfig;", false));
                    list.add(new FieldInsnNode(Opcodes.PUTFIELD, "gg/essential/vigilance/Vigilant", "oneconfig$config", "Lcc/polyfrost/oneconfig/internal/config/compatibility/vigilance/VigilanceConfig;"));
                    method.instructions.insertBefore(method.instructions.getLast().getPrevious(), list);
                } else if (method.name.equals("addDependency") && method.desc.equals("(Lgg/essential/vigilance/data/PropertyData;Lgg/essential/vigilance/data/PropertyData;)V")) {
                    InsnList list = new InsnList();

                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    list.add(new VarInsnNode(Opcodes.ALOAD, 2));
                    list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "gg/essential/vigilance/Vigilant", "handleOneConfigDependency", "(Lgg/essential/vigilance/data/PropertyData;Lgg/essential/vigilance/data/PropertyData;)V", false));

                    method.instructions.insertBefore(method.instructions.getLast().getPrevious(), list);
                } else if (method.name.equals("<init>") && method.desc.equals("(Ljava/io/File;Ljava/lang/String;Lgg/essential/vigilance/data/PropertyCollector;Lgg/essential/vigilance/data/SortingBehavior;)V")) {
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    list.add(new FieldInsnNode(Opcodes.PUTFIELD, "gg/essential/vigilance/Vigilant", "oneconfig$file", Type.getDescriptor(File.class)));
                    method.instructions.insertBefore(method.instructions.getLast().getPrevious(), list);
                }
            }
        }
    }
}
