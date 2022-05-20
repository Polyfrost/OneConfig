package cc.polyfrost.oneconfig.plugin.asm.tweakers;

import cc.polyfrost.oneconfig.plugin.asm.ITransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class NanoVGGLConfigTransformer implements ITransformer {
    @Override
    public String[] getClassName() {
        return new String[]{"org.lwjgl.nanovg.NanoVGGLConfig"};
    }

    @Override
    public void transform(String transformedName, ClassNode node) {
        for (MethodNode method : node.methods) {
            if (method.name.equals("configGL")) {
                InsnList list = new InsnList();

                list.add(new VarInsnNode(Opcodes.LLOAD, 0));
                list.add(new TypeInsnNode(Opcodes.NEW, "cc/polyfrost/oneconfig/lwjgl/plugin/Lwjgl2FunctionProvider"));
                list.add(new InsnNode(Opcodes.DUP));
                list.add(new MethodInsnNode(
                        Opcodes.INVOKESPECIAL,
                        "cc/polyfrost/oneconfig/lwjgl/plugin/Lwjgl2FunctionProvider",
                        "<init>",
                        "()V",
                        false
                ));
                list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "org/lwjgl/nanovg/NanoVGGLConfig",
                        "config",
                        "(JLorg/lwjgl/system/FunctionProvider;)V",
                        false
                ));
                list.add(new InsnNode(Opcodes.RETURN));

                method.instructions.clear();
                method.instructions.insert(list);
            }
        }
    }
}
