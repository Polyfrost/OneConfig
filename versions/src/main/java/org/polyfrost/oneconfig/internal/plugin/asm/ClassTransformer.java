/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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
//#disable-remap
package org.polyfrost.oneconfig.internal.plugin.asm;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

/**
 * Taken from LWJGLTwoPointFive under The Unlicense
 * <a href="https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/">https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/</a>
 * <p>also half taken from asmworkspace by asbyth ty</p>
 */
@SuppressWarnings("unused")
public class ClassTransformer
        //#if FORGE==1 && MC<=11202
        implements net.minecraft.launchwrapper.IClassTransformer
        //#elseif FABRIC==1
        //$$ implements Runnable
        //#endif
{
    private static final Logger logger = LogManager.getLogger("OneConfig ASM");
    private final Multimap<String, ITransformer> transformerMap = ArrayListMultimap.create();
    //#if FORGE==1 && MC<=11202
    private static final boolean outputBytecode = Boolean.parseBoolean(System.getProperty("debugBytecode", "false"));
    //#endif

    public ClassTransformer() {
        //registerTransformer(new VigilantTransformer());
    }

    private void registerTransformer(ITransformer transformer) {
        // loop through names of classes
        for (String cls : transformer.getClassName()) {
            // put the classes into the transformer map
            transformerMap.put(cls, transformer);
        }
    }

    //#if FABRIC==1
    //$$ @Override
    //$$ public void run() {
    //$$     for (java.util.Map.Entry<String, ITransformer> entry : transformerMap.entries()) {
    //$$         com.chocohead.mm.api.ClassTinkerers.addTransformation(entry.getKey(), (node) -> entry.getValue().transform(entry.getKey(), node));
    //$$     }
    //$$ }
    //#endif

    //#if FORGE==1 && MC<=11202
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;

        Collection<ITransformer> transformers = transformerMap.get(transformedName);
        if (transformers.isEmpty()) return bytes;

        ClassReader reader = new ClassReader(bytes);
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

        if (outputBytecode) {
            File bytecodeDirectory = new File("bytecode");
            String transformedClassName;

            // anonymous classes
            if (transformedName.contains("$")) {
                transformedClassName = transformedName.replace('$', '.') + ".class";
            } else {
                transformedClassName = transformedName + ".class";
            }

            if (!bytecodeDirectory.exists()) {
                bytecodeDirectory.mkdirs();
            }

            File bytecodeOutput = new File(bytecodeDirectory, transformedClassName);

            try {
                if (!bytecodeOutput.exists()) {
                    bytecodeOutput.createNewFile();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try (FileOutputStream os = new FileOutputStream(bytecodeOutput)) {
                // write to the generated class to /run/bytecode/classfile.class
                // with the class bytes from transforming
                os.write(cw.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return cw.toByteArray();
    }
    //#endif
}