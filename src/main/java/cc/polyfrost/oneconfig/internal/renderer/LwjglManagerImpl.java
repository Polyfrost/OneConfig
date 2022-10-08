package cc.polyfrost.oneconfig.internal.renderer;

import cc.polyfrost.oneconfig.renderer.AssetHelper;
import cc.polyfrost.oneconfig.renderer.LwjglManager;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.TinyFD;
import cc.polyfrost.oneconfig.renderer.font.FontHelper;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorHelper;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class LwjglManagerImpl extends URLClassLoader implements LwjglManager {
    private final Set<String> implClasses = new CopyOnWriteArraySet<>();
    private final Map<String, Class<?>> classCache = new HashMap<>();

    private static final Object unsafeInstance;
    private static final Method defineClassMethod;
    private static final Map<String, String> hackyRemapping;

    private static final URL jarFile = getJarFile();

    public LwjglManagerImpl() throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        super(new URL[] { jarFile }, LwjglManager.class.getClassLoader());
        implClasses.add("cc.polyfrost.oneconfig.internal.renderer.FontHelperImpl");
        implClasses.add("cc.polyfrost.oneconfig.internal.renderer.ScissorHelperImpl");
        implClasses.add("cc.polyfrost.oneconfig.internal.renderer.NanoVGHelperImpl");
        implClasses.add("cc.polyfrost.oneconfig.internal.renderer.AssetHelperImpl");
        implClasses.add("cc.polyfrost.oneconfig.internal.renderer.TinyFDImpl");

        Class<?> configClass = findClass("org.lwjgl.system.Configuration");
        Object extractDirField = configClass.getField("SHARED_LIBRARY_EXTRACT_DIRECTORY").get(null);
        Method setMethod = configClass.getMethod("set", Object.class);
        setMethod.invoke(extractDirField, new File("./OneConfig/temp").getAbsolutePath());
    }

    private static synchronized URL getJarFile() {
        final File tempJar = new File("./OneConfig/temp/oneconfig-lwjgl3.jar");
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

    @Override
    public NanoVGHelper getNanoVGHelper() {
        try {
            return (NanoVGHelper) Class.forName("cc.polyfrost.oneconfig.internal.renderer.NanoVGHelperImpl", true, this).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ScissorHelper getScissorHelper() {
        try {
            return (ScissorHelper) Class.forName("cc.polyfrost.oneconfig.internal.renderer.ScissorHelperImpl", true, this).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AssetHelper getAssetHelper() {
        try {
            return (AssetHelper) Class.forName("cc.polyfrost.oneconfig.internal.renderer.AssetHelperImpl", true, this).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FontHelper getFontHelper() {
        try {
            return (FontHelper) Class.forName("cc.polyfrost.oneconfig.internal.renderer.FontHelperImpl", true, this).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TinyFD getTinyFD() {
        try {
            return (TinyFD) Class.forName("cc.polyfrost.oneconfig.internal.renderer.TinyFDImpl", true, this).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean canBeSharedWithMc(String name) {
        if (name.startsWith("org.lwjgl.")) {
            return false; // MC may have a different version
        }
        for (String implClass : implClasses) {
            if (name.startsWith(implClass)) {
                return false; // depends on above lwjgl
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
        } else {
            return super.loadClass(name, resolve);
        }
    }

    private Class<?> defineClassBypass(String name, byte[] b) {
        ClassReader classReader = new ClassReader(b);
        Remapper remapper = new Remapper() {
            @Override
            public String map(String desc) {
                return hackyRemapping.getOrDefault(desc, desc);
            }
        };
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
        ClassRemapper classRemapper = new ClassRemapper(classWriter, remapper);
        classReader.accept(classRemapper, ClassReader.EXPAND_FRAMES);

        b = classWriter.toByteArray();
        name = hackyRemapping.getOrDefault(name.replace('.', '/'), name).replace('/', '.');

        try {
            File file = new File("/tmp/lwjglgamer/" + name.replace(".", "/") + ".class");
            file.getParentFile().mkdirs();
            if (file.exists()) file.delete();
            file.createNewFile();
            Files.write(file.toPath(), b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            System.out.println("Defining " + name);
            return (Class<?>) defineClassMethod.invoke(unsafeInstance, name, b, 0, b.length, /*classLoader = */null, null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("whoops...", e);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String finalName = name;

        name = hackyRemapping.keySet()
                .stream()
                .filter(it -> hackyRemapping.get(it).equals(finalName.replace('.', '/')))
                .findFirst()
                .orElse(name)
                .replace('/', '.');

        System.out.println("findClass(" + name + ")");

        if (classCache.containsKey(name)) {
            return classCache.get(name);
        }

        try {
            if (!canBeSharedWithMc(name)) {
                Enumeration<URL> enumer = getResources(
                        name.replace('.', '/') + ".class"
                );
                while (enumer.hasMoreElements()) {
                    URL url = enumer.nextElement();
                    if (!url.toString().contains("oneconfig-lwjgl3")) {
                        continue;
                    }
                    InputStream stream = url.openStream();
                    byte[] barr = IOUtils.toByteArray(stream);
                    Class<?> clazz = defineClassBypass(name, barr);
                    classCache.put(name, clazz);
                    return clazz;
                }
            }
            if (findLoadedClass(name) != null) {
                return findLoadedClass(name);
            }
            return super.findClass(name);
        } catch (ClassNotFoundException | IOException ignored) {
        }
        return getParent().loadClass(name);
    }

    static {
        registerAsParallelCapable();

        hackyRemapping = new HashMap<>();
        hackyRemapping.put("org/lwjgl/BufferUtils", "org/lwjgl/actually3/BufferUtils");
        hackyRemapping.put("org/lwjgl/CLongBuffer", "org/lwjgl/actually3/CLongBuffer");
        hackyRemapping.put("org/lwjgl/PointerBuffer", "org/lwjgl/actually3/PointerBuffer");

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
}
