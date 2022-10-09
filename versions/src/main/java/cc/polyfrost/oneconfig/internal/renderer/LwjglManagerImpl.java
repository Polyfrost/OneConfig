package cc.polyfrost.oneconfig.internal.renderer;

import cc.polyfrost.oneconfig.libs.deencapsulation.Deencapsulation;
import cc.polyfrost.oneconfig.renderer.AssetHelper;
import cc.polyfrost.oneconfig.renderer.LwjglManager;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.TinyFD;
import cc.polyfrost.oneconfig.renderer.font.FontHelper;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorHelper;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Remapper;
//#if FORGE==1 && MC<=11202
import org.objectweb.asm.commons.RemappingClassAdapter;
//#else
//$$ import org.objectweb.asm.commons.ClassRemapper;
//#endif
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class LwjglManagerImpl
        //#if MC<=11202
        extends URLClassLoader
        //#endif
        implements LwjglManager {

    //#if MC<=11202
    private static final Object unsafeInstance;
    private static final Method defineClassMethod;
    private static final Map<String, String> remappingMap;

    private static final String LWJGL_FUNCTION_PROVIDER =
            "cc.polyfrost.oneconfig.internal.plugin.hooks.Lwjgl2FunctionProvider";

    private final Set<String> classLoaderInclude = new CopyOnWriteArraySet<>();
    private final Map<String, Class<?>> classCache = new HashMap<>();

    private static final String JAR_NAME = "oneconfig-lwjgl3.jar";
    private static final URL jarFile = getJarFile();
    //#endif

    private final AssetHelper assetHelper;
    private final NanoVGHelper nanoVGHelper;
    private final ScissorHelper scissorHelper;
    private final FontHelper fontHelper;
    private final TinyFD tinyFD;

    public LwjglManagerImpl() throws ReflectiveOperationException {
        //#if MC<=11202
        super(new URL[]{jarFile}, LwjglManager.class.getClassLoader());
        // Internal accessors
        classLoaderInclude.add("cc.polyfrost.oneconfig.internal.renderer.FontHelperImpl");
        classLoaderInclude.add("cc.polyfrost.oneconfig.internal.renderer.ScissorHelperImpl");
        classLoaderInclude.add("cc.polyfrost.oneconfig.internal.renderer.NanoVGHelperImpl");
        classLoaderInclude.add("cc.polyfrost.oneconfig.internal.renderer.AssetHelperImpl");
        classLoaderInclude.add("cc.polyfrost.oneconfig.internal.renderer.TinyFDImpl");
        // Provider
        classLoaderInclude.add(LWJGL_FUNCTION_PROVIDER);
        // Lwjgl
        Arrays.asList("system", "nanovg", "stb", "util.tinyfd", "actually3")
                .forEach(it -> classLoaderInclude.add("org.lwjgl." + it + "."));
        classLoaderInclude.add("org.lwjgl.Version"); // won't work when remapped

        // Keep the path somewhere for LWJGL2 after initializing LWJGL3
        // (this is read in the Lwjgl2FunctionProvider class)
        System.setProperty("oneconfig.lwjgl2.librarypath", System.getProperty("org.lwjgl.librarypath"));
        //#endif

        ClassLoader classLoader =
                //#if MC<=11202
                this
                //#else
                //$$ LwjglManager.class.getClassLoader()
                //#endif
                ;

        // Setup LW3 config
        Class<?> configClass = Class.forName("org.lwjgl.system.Configuration", true, classLoader);
        Method setMethod = configClass.getMethod("set", Object.class);

        //#if MC<=11202
        Object extractDirField = configClass.getField("SHARED_LIBRARY_EXTRACT_DIRECTORY").get(null);
        setMethod.invoke(extractDirField, new File("./OneConfig/temp").getAbsolutePath());
        //#endif

        // stop trying to Class.forName("true") ffs
        Object debugStreamField = configClass.getField("DEBUG_STREAM").get(null);
        setMethod.invoke(debugStreamField, System.err);

        // Initialize helper instances
        try {
            nanoVGHelper = (NanoVGHelper) Class.forName("cc.polyfrost.oneconfig.internal.renderer.NanoVGHelperImpl", true, classLoader).getConstructor().newInstance();
            scissorHelper = (ScissorHelper) Class.forName("cc.polyfrost.oneconfig.internal.renderer.ScissorHelperImpl", true, classLoader).getConstructor().newInstance();
            assetHelper = (AssetHelper) Class.forName("cc.polyfrost.oneconfig.internal.renderer.AssetHelperImpl", true, classLoader).getConstructor().newInstance();
            fontHelper = (FontHelper) Class.forName("cc.polyfrost.oneconfig.internal.renderer.FontHelperImpl", true, classLoader).getConstructor().newInstance();
            tinyFD = (TinyFD) Class.forName("cc.polyfrost.oneconfig.internal.renderer.TinyFDImpl", true, classLoader).getConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
            throw new RuntimeException("fuck", exception);
        }
    }

    //#if MC<=11202
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
        if (!canBeSharedWithMc(name)) {
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

            byte[] classBuffer = IOUtils.toByteArray(classUrl);

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

        //#if MC<=11202
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
        //#if FORGE==1
        RemappingClassAdapter classRemapper = new RemappingClassAdapter(classWriter, remapper);
        //#else
        //$$ ClassRemapper classRemapper = new ClassRemapper(classWriter, remapper);
        //#endif
        classReader.accept(classRemapper, ClassReader.EXPAND_FRAMES);
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
        //#endif

        try {
            return (Class<?>) defineClassMethod.invoke(unsafeInstance, name, b, 0, b.length, /*classLoader = */this, null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("whoops...", e);
        }
    }

    private void transform(ClassNode node) {
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

                method.instructions.clear();
                method.instructions.insert(list);
            }
        }
    }

    static {
        registerAsParallelCapable();

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

        try {
            try {
                Deencapsulation.deencapsulate(Object.class);
                Deencapsulation.deencapsulate(unsafeClass);
            } catch (Throwable ignored) {
            }

            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafeInstance = unsafeField.get(null);

            defineClassMethod = unsafeClass.getDeclaredMethod(
                    "defineClass",
                    String.class,
                    byte[].class,
                    int.class,
                    int.class,
                    ClassLoader.class,
                    ProtectionDomain.class
            );
            defineClassMethod.setAccessible(true);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Error while fetching Unsafe instance.", exception);
        }
    }

    private static synchronized URL getJarFile() {
        final File tempJar = new File("./OneConfig/temp/" + JAR_NAME);
        tempJar.mkdirs();
        try {
            tempJar.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tempJar.deleteOnExit();
        try (InputStream in = LwjglManagerImpl.class.getResourceAsStream("/lwjgl.jar")) {
            assert in != null;
            Files.copy(in, tempJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            return tempJar.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    //#endif

    @Override
    public NanoVGHelper getNanoVGHelper() {
        return nanoVGHelper;
    }

    @Override
    public ScissorHelper getScissorHelper() {
        return scissorHelper;
    }

    @Override
    public AssetHelper getAssetHelper() {
        return assetHelper;
    }

    @Override
    public FontHelper getFontHelper() {
        return fontHelper;
    }

    @Override
    public TinyFD getTinyFD() {
        return tinyFD;
    }
}
