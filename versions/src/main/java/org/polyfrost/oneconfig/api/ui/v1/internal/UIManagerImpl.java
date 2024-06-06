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

package org.polyfrost.oneconfig.api.ui.v1.internal;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.tree.*;
import org.polyfrost.oneconfig.api.ClassHasOverwrites;
import org.polyfrost.oneconfig.api.ui.v1.TinyFD;
import org.polyfrost.oneconfig.api.ui.v1.UIManager;
import org.polyfrost.oneconfig.api.ui.v1.internal.wrappers.MCWindow;
import org.polyfrost.oneconfig.api.ui.v1.internal.wrappers.PolyUIScreen;
import org.polyfrost.oneconfig.utils.v1.MHUtils;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.renderer.Renderer;
import org.polyfrost.polyui.renderer.Window;
import org.polyfrost.polyui.unit.Vec2;
import org.polyfrost.polyui.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("unused")
@ClassHasOverwrites("1.16.5-forge")
public class UIManagerImpl
        extends URLClassLoader
        implements UIManager {

    private static final Logger LOGGER = LogManager.getLogger("OneConfig/LWJGL");
    private static final boolean isPojav = checkPojav();
    private static final MethodHandle defineClassMethod;
    private static final Map<String, String> remappingMap;

    private static final String RENDERER_IMPL_PACKAGE = "org.polyfrost.oneconfig.api.ui.v1.internal.";
    private static final String LWJGL_FUNCTION_PROVIDER = "org.polyfrost.oneconfig.internal.legacy.Lwjgl2FunctionProvider";
    private static final String LWJGL_FUNCTION_PROVIDER_ASM = LWJGL_FUNCTION_PROVIDER.replace('.', '/');
    private static final String JAR_NAME = "lwjgl-legacy.jar";
    private static final Path TEMP_DIR = Paths.get("oneconfig", "lwjgl").toAbsolutePath();

    static {
        registerAsParallelCapable();
        if (isPojav) {
            remappingMap = null;
            defineClassMethod = null;
        } else {
            remappingMap = new HashMap<>();
            remappingMap.put("org/lwjgl/BufferUtils", "org/lwjgl/actually3/BufferUtils");
            remappingMap.put("org/lwjgl/PointerBuffer", "org/lwjgl/actually3/PointerBuffer");
            remappingMap.put("org/lwjgl/CLongBuffer", "org/lwjgl/actually3/CLongBuffer");

            Class<?> unsafeClass;
            try {
                unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
            } catch (Throwable throwable) {
                try {
                    unsafeClass = Class.forName("sun.misc.Unsafe");
                } catch (Throwable throwable1) {
                    throw new RuntimeException("Could not find Unsafe class", throwable);
                }
            }

            Object theUnsafe = MHUtils.getStatic(unsafeClass, "theUnsafe").getOrThrow();

            defineClassMethod = MHUtils.getMethodHandle(
                    theUnsafe,
                    "defineClass",
                    /* returns */ Class.class,
                    String.class,
                    byte[].class,
                    int.class,
                    int.class,
                    ClassLoader.class,
                    ProtectionDomain.class
            ).getOrThrow();
        }
    }

    private final Set<String> classLoaderInclude = new HashSet<>();
    private final Map<String, Class<?>> classCache = new HashMap<>();
    private final TinyFD tinyFD;
    private final Renderer renderer;

    public UIManagerImpl() throws Throwable {
        this(false);
    }

    public UIManagerImpl(boolean onlyTinyFD) throws Throwable {
        super(new URL[]{getJarFile()}, UIManager.class.getClassLoader());

        ClassLoader classLoader = isPojav ? getClass().getClassLoader() : this;
        if (!isPojav) {
            // Internal accessors
            classLoaderInclude.add(RENDERER_IMPL_PACKAGE);
            classLoaderInclude.add(LWJGL_FUNCTION_PROVIDER);
            // Lwjgl
            Arrays.asList("nanovg", "actually3", "stb", "util.tinyfd", "system")
                    .forEach(it -> classLoaderInclude.add("org.lwjgl." + it + "."));
            classLoaderInclude.add("org.lwjgl.Version"); // won't work when remapped

            // Keep the path somewhere for LWJGL2 after initializing LWJGL3
            // (this is read in the Lwjgl2FunctionProvider class)
            String libraryPath = System.getProperty("org.lwjgl.librarypath", "");
            if (!libraryPath.isEmpty()) {
                System.setProperty("oneconfig.lwjgl2.librarypath", libraryPath);
            }

            // Setup LW3 config
            Class<?> configClass = Class.forName("org.lwjgl.system.Configuration", true, classLoader);
            MethodHandle setMethod = MHUtils.getMethodHandle(configClass, "set", void.class, Object.class).getOrThrow();

            Object extractDirField = configClass.getField("SHARED_LIBRARY_EXTRACT_DIRECTORY").get(null);
            setMethod.invoke(extractDirField, TEMP_DIR.toString());

            // stop trying to Class.forName("true") ffs
            Object debugStreamField = configClass.getField("DEBUG_STREAM").get(null);
            setMethod.invoke(debugStreamField, System.err);
        }

        try {
            if (!onlyTinyFD) {
                renderer = (Renderer) Class.forName(RENDERER_IMPL_PACKAGE + "RendererImpl", true, classLoader).getField("INSTANCE").get(null);
            } else renderer = null;
            tinyFD = (TinyFD) Class.forName(RENDERER_IMPL_PACKAGE + "TinyFDImpl", true, classLoader).getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get valid rendering implementation", e);
        }
    }

    private static synchronized URL getJarFile() {
        if (isPojav) return null;
        final Path tempJar = TEMP_DIR.resolve(JAR_NAME);
        try (InputStream in = UIManagerImpl.class.getResourceAsStream("/" + JAR_NAME)) {
            if (in == null) throw new IOException("Failed to get " + JAR_NAME);
            Files.createDirectories(TEMP_DIR);
            TEMP_DIR.toFile().deleteOnExit();
            Files.copy(in, tempJar, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            return tempJar.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean checkPojav() {
        try {
            Class.forName("org.lwjgl.glfw.CallbackBridge");
            LOGGER.warn("Pojav detected, letting Pojav handle LWJGL.");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean canBeSharedWithMc(String name) {
        for (String implClass : classLoaderInclude) {
            if (name.startsWith(implClass)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!isPojav && !canBeSharedWithMc(name)) {
            synchronized (getClassLoadingLock(name)) {
                Class<?> cls = findLoadedClass(name);
                if (cls == null) {
                    cls = findClass(name);
                }
                if (resolve) {
                    resolveClass(cls);
                }
                return cls;
            }
        }
        return getParent().loadClass(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (isPojav) {
            return getParent().loadClass(name);
        }
        String remappedName = remappingMap.getOrDefault(
                name.replace('.', '/'),
                name
        ).replace('/', '.');
        String unmappedName = remappingMap.keySet().stream()
                .filter(it -> remappingMap.get(it).equalsIgnoreCase(remappedName.replace('.', '/')))
                .findFirst()
                .orElse(name)
                .replace('/', '.');

        // if it exists in the remapping map
        if (!remappedName.equals(unmappedName)) {
            // if we're being requested class.A and see that it has class.B as
            // a remapped value, load it through the parent classloader
            if (name.equals(unmappedName)) {
                return getParent().loadClass(name);
            }
            // Otherwise, if we get class.B and see that it's class.A remapped,
            // then we need to transform and load class.B ourselves
        }
        if (classCache.containsKey(name)) {
            // ayo cache :D
            return classCache.get(name);
        }

        if (canBeSharedWithMc(remappedName)) {
            // Delegate share-able requests to the parent classloader
            return getParent().loadClass(remappedName);
        }

        try {
            String path = unmappedName.replace('.', '/').concat(".class");
            URL classUrl = null;
            // First check the lwjgl jar
            Enumeration<URL> urls = getResources(path);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();

                // check that the URL is provided from the custom lwjgl jar
                if (!url.toString().contains(JAR_NAME)) continue;

                classUrl = url;
                break;
            }

            if (classUrl == null) {
                classUrl = getParent().getResource(path);
                if (classUrl == null) {
                    throw new ClassNotFoundException(name);
                }
            }

            byte[] classBuffer = IOUtils.toByteArray(classUrl.openStream());

            // define class through Unsafe to bypass package seal checking
            Class<?> clazz = defineClassBypass(unmappedName, classBuffer);
            classCache.put(remappedName, clazz);
            return clazz;
        } catch (IOException ignored) {
        }
        throw new ClassNotFoundException(name);
    }

    private Class<?> defineClassBypass(String name, byte[] b) {
        name = remappingMap.getOrDefault(name.replace('.', '/'), name)
                .replace('/', '.');

        ClassReader classReader = new ClassReader(b);
        Remapper remapper = new Remapper() {
            @Override
            public String map(String desc) {
                if (remappingMap.containsKey(desc)) {
                    return remappingMap.get(desc);
                }
                return desc;
            }
        };
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor mapper = new RemappingClassAdapter(classWriter, remapper);
        classReader.accept(mapper, ClassReader.EXPAND_FRAMES);
        b = classWriter.toByteArray();

        if (name.equalsIgnoreCase("org.lwjgl.nanovg.NanoVGGLConfig")) {
            ClassNode node = new ClassNode();
            classReader = new ClassReader(b);
            classReader.accept(node, ClassReader.EXPAND_FRAMES);

            transform(node);

            classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
            node.accept(classWriter);
            b = classWriter.toByteArray();
        }

        try {
            return (Class<?>) defineClassMethod.invokeExact(name, b, 0, b.length, (ClassLoader) this, (ProtectionDomain) null);
        } catch (Throwable e) {
            throw new RuntimeException("failed to define class " + name, e);
        }
    }

    private void transform(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (method.name.equals("configGL")) {
                InsnList list = new InsnList();

                list.add(new VarInsnNode(Opcodes.LLOAD, 0));
                list.add(new TypeInsnNode(Opcodes.NEW, LWJGL_FUNCTION_PROVIDER_ASM));
                list.add(new InsnNode(Opcodes.DUP));
                list.add(new MethodInsnNode(
                        Opcodes.INVOKESPECIAL,
                        LWJGL_FUNCTION_PROVIDER_ASM,
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

    @Override
    public Renderer getRenderer() {
        return renderer;
    }

    @Override
    public TinyFD getTinyFD() {
        return tinyFD;
    }

    @Override
    public Object createPolyUIScreen(@NotNull PolyUI polyUI, Vec2 desiredResolution, boolean pauses, boolean blurs, Consumer<PolyUI> onClose) {
        return new PolyUIScreen(polyUI, desiredResolution, pauses, blurs, onClose);
    }

    @Override
    public Window createWindow() {
        return new MCWindow(Minecraft.getMinecraft());
    }
}
