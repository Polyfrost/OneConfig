package cc.polyfrost.oneconfig.plugin.asm.tweakers;

import cc.polyfrost.oneconfig.compat.VigilanceConfig;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.plugin.asm.ITransformer;
import gg.essential.vigilance.Vigilant;
import gg.essential.vigilance.data.PropertyCollector;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.File;

public class VigilantTransformer implements ITransformer {
    @Override
    public String[] getClassName() {
        return new String[]{"gg.essential.vigilance.Vigilant"};
    }

    @Override
    public void transform(String transformedName, ClassNode node) {
        node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "oneconfig$config", Type.getDescriptor(VigilanceConfig.class), null, null));
        node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, "oneconfig$file", Type.getDescriptor(File.class), null, null));

        node.interfaces.add("cc/polyfrost/oneconfig/compat/VigilantAccessor");
        MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "getPropertyCollector", "()Lgg/essential/vigilance/data/PropertyCollector;", null, null);
        LabelNode labelNode = new LabelNode();
        methodNode.instructions.add(labelNode);
        methodNode.instructions.add(new LineNumberNode(421421, labelNode));
        methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        methodNode.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "gg/essential/vigilance/Vigilant", "propertyCollector", Type.getDescriptor(PropertyCollector.class)));
        methodNode.instructions.add(new InsnNode(Opcodes.ARETURN));
        node.methods.add(methodNode);

        MethodNode methodNode2 = new MethodNode(Opcodes.ACC_PUBLIC, "handleOneConfigDependency", "(Lgg/essential/vigilance/data/PropertyData;Lgg/essential/vigilance/data/PropertyData;)V", null, null);
        LabelNode labelNode2 = new LabelNode();
        LabelNode labelNode3 = new LabelNode();
        LabelNode labelNode4 = new LabelNode();
        methodNode2.instructions.add(labelNode2);
        methodNode2.instructions.add(new LineNumberNode(15636436, labelNode2));
        methodNode2.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        methodNode2.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "gg/essential/vigilance/Vigilant", "oneconfig$config", Type.getDescriptor(VigilanceConfig.class)));

        methodNode2.instructions.add(new JumpInsnNode(Opcodes.IFNULL, labelNode4));

        methodNode2.instructions.add(labelNode3);
        methodNode2.instructions.add(new LineNumberNode(15636437, labelNode3));
        methodNode2.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        methodNode2.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "gg/essential/vigilance/Vigilant", "oneconfig$config", Type.getDescriptor(VigilanceConfig.class)));
        methodNode2.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        methodNode2.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        methodNode2.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(VigilanceConfig.class), "addDependency", "(Lgg/essential/vigilance/data/PropertyData;Lgg/essential/vigilance/data/PropertyData;)V", false));

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
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(getClass()), "returnNewConfig", "(Lgg/essential/vigilance/Vigilant;Ljava/io/File;)Lcc/polyfrost/oneconfig/compat/VigilanceConfig;", false));
                list.add(new FieldInsnNode(Opcodes.PUTFIELD, "gg/essential/vigilance/Vigilant", "oneconfig$config", Type.getDescriptor(VigilanceConfig.class)));
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

    @SuppressWarnings("unused")
    public static VigilanceConfig returnNewConfig(Vigilant vigilant, File file) {
        if (vigilant != null && Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
            return new VigilanceConfig(new Mod(!vigilant.getGuiTitle().equals("Settings") ? vigilant.getGuiTitle() : Loader.instance().activeModContainer() == null ? "Unknown" : Loader.instance().activeModContainer().getName(), ModType.VIGILANCE, Loader.instance().activeModContainer() == null ? "Unknown" : Loader.instance().activeModContainer().getName(), Loader.instance().activeModContainer() == null ? "1.0.0" : Loader.instance().activeModContainer().getVersion()), file.getAbsolutePath(), vigilant);
        } else {
            return null;
        }
    }
}
