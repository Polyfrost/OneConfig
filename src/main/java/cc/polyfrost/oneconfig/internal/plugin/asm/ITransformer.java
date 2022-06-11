package cc.polyfrost.oneconfig.internal.plugin.asm;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public interface ITransformer {
    String[] getClassName();

    void transform(String transformedName, ClassNode node);

    default void clearInstructions(MethodNode methodNode) {
        methodNode.instructions.clear();

        // dont waste time clearing local variables if they're empty
        if (!methodNode.localVariables.isEmpty()) {
            methodNode.localVariables.clear();
        }

        // dont waste time clearing try-catches if they're empty
        if (!methodNode.tryCatchBlocks.isEmpty()) {
            methodNode.tryCatchBlocks.clear();
        }
    }
}
