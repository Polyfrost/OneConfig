package cc.polyfrost.oneconfig.internal.plugin.asm.tweakers;

import cc.polyfrost.oneconfig.internal.plugin.asm.ITransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * Taken from LWJGLTwoPointFive under The Unlicense
 * <a href="https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/">https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/</a>
 */
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
                list.add(new TypeInsnNode(Opcodes.NEW, "cc/polyfrost/oneconfig/internal/plugin/hooks/Lwjgl2FunctionProvider"));
                list.add(new InsnNode(Opcodes.DUP));
                list.add(new MethodInsnNode(
                        Opcodes.INVOKESPECIAL,
                        "cc/polyfrost/oneconfig/internal/plugin/hooks/Lwjgl2FunctionProvider",
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

                clearInstructions(method);
                method.instructions.insert(list);
            }
        }
    }
}
