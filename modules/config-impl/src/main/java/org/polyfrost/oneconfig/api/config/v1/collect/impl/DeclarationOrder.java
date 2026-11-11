/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */


package org.polyfrost.oneconfig.api.config.v1.collect.impl;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Orders classes by their first source line, as reflection returns inner classes in reverse declaration order
 */
final class DeclarationOrder {
    private DeclarationOrder() {
    }

    static Class<?>[] sort(Class<?>[] classes) {
        Map<Class<?>, Integer> lines = new IdentityHashMap<>();
        for (Class<?> cls : classes) lines.put(cls, firstLine(cls));
        Class<?>[] sorted = classes.clone();
        Arrays.sort(sorted, Comparator.comparingInt(lines::get));
        return sorted;
    }

    private static int firstLine(Class<?> cls) {
        ClassLoader loader = cls.getClassLoader();
        if (loader == null) return Integer.MAX_VALUE;
        try (InputStream in = loader.getResourceAsStream(cls.getName().replace('.', '/') + ".class")) {
            return in == null ? Integer.MAX_VALUE : firstLine(new DataInputStream(in));
        } catch (IOException e) {
            return Integer.MAX_VALUE;
        }
    }

    private static int firstLine(DataInputStream in) throws IOException {
        in.skipBytes(8);
        String[] utf8 = new String[in.readUnsignedShort()];
        for (int i = 1; i < utf8.length; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1 -> utf8[i] = in.readUTF();
                case 3, 4, 9, 10, 11, 12, 17, 18 -> in.skipBytes(4);
                case 5, 6 -> {
                    in.skipBytes(8);
                    i++;
                }
                case 7, 8, 16, 19, 20 -> in.skipBytes(2);
                case 15 -> in.skipBytes(3);
                default -> throw new IOException("Unknown constant pool tag " + tag);
            }
        }
        in.skipBytes(6);
        in.skipBytes(in.readUnsignedShort() * 2);
        int line = Integer.MAX_VALUE;
        for (int member = 0; member < 2; member++) {
            for (int count = in.readUnsignedShort(); count > 0; count--) {
                in.skipBytes(6);
                for (int attrs = in.readUnsignedShort(); attrs > 0; attrs--) {
                    String name = utf8[in.readUnsignedShort()];
                    int length = in.readInt();
                    if (member == 1 && "Code".equals(name)) line = Math.min(line, codeLine(in, utf8));
                    else in.skipBytes(length);
                }
            }
        }
        return line;
    }

    private static int codeLine(DataInputStream in, String[] utf8) throws IOException {
        in.skipBytes(4);
        in.skipBytes(in.readInt());
        in.skipBytes(in.readUnsignedShort() * 8);
        int line = Integer.MAX_VALUE;
        for (int attrs = in.readUnsignedShort(); attrs > 0; attrs--) {
            String name = utf8[in.readUnsignedShort()];
            int length = in.readInt();
            if (!"LineNumberTable".equals(name)) {
                in.skipBytes(length);
                continue;
            }
            for (int entries = in.readUnsignedShort(); entries > 0; entries--) {
                in.skipBytes(2);
                line = Math.min(line, in.readUnsignedShort());
            }
        }
        return line;
    }
}
