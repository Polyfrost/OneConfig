package cc.polyfrost.oneconfig.plugin.asm;

import org.objectweb.asm.tree.ClassNode;

public interface ITransformer {
    String[] getClassName();

    void transform(String transformedName, ClassNode node);
}
