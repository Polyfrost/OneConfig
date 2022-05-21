package cc.polyfrost.oneconfig.plugin.asm;

import cc.polyfrost.oneconfig.plugin.asm.tweakers.NanoVGGLConfigTransformer;
import cc.polyfrost.oneconfig.plugin.asm.tweakers.VigilantTransformer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collection;

/**
 * Taken from LWJGLTwoPointFive under The Unlicense
 * <a href="https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/">https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/</a>
 * <p>also half taken from asmworkspace by asbyth ty</p>
 */
@SuppressWarnings("unused")
public class ClassTransformer implements IClassTransformer {
    private static final Logger logger = LogManager.getLogger("OneConfig ASM");
    private final Multimap<String, ITransformer> transformerMap = ArrayListMultimap.create();

    public ClassTransformer() {
        registerTransformer(new NanoVGGLConfigTransformer());
        registerTransformer(new VigilantTransformer());
    }

    private void registerTransformer(ITransformer transformer) {
        // loop through names of classes
        for (String cls : transformer.getClassName()) {
            // put the classes into the transformer map
            transformerMap.put(cls, transformer);
        }
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        Collection<ITransformer> transformers = transformerMap.get(transformedName);
        if (transformers.isEmpty()) return basicClass;


        ClassReader reader = new ClassReader(basicClass);
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.EXPAND_FRAMES);

        for (ITransformer transformer : transformers) {
            transformer.transform(transformedName, node);
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        try {
            node.accept(cw);
        } catch (Throwable t) {
            logger.error("Exception when transforming " + transformedName + " : " + t.getClass().getSimpleName());
            t.printStackTrace();
        }
        return cw.toByteArray();
    }
}