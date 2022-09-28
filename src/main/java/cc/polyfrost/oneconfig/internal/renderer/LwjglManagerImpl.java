package cc.polyfrost.oneconfig.internal.renderer;

import cc.polyfrost.oneconfig.renderer.AssetHelper;
import cc.polyfrost.oneconfig.renderer.LwjglManager;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.TinyFD;
import cc.polyfrost.oneconfig.renderer.font.FontHelper;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorHelper;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.ProtectionDomain;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class LwjglManagerImpl extends URLClassLoader implements LwjglManager {
    private final Set<String> implClasses = new CopyOnWriteArraySet<>();

    static {
        registerAsParallelCapable();
    }

    public LwjglManagerImpl() throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        super(new URL[] { getJarFile() }, LwjglManager.class.getClassLoader());
        implClasses.add("cc.polyfrost.oneconfig.internal.renderer.FontHelperImpl");
        implClasses.add("cc.polyfrost.oneconfig.internal.renderer.ScissorHelperImpl");
        implClasses.add("cc.polyfrost.oneconfig.internal.renderer.NanoVGHelperImpl");
        implClasses.add("cc.polyfrost.oneconfig.internal.renderer.AssetHelperImpl");
        implClasses.add("cc.polyfrost.oneconfig.internal.renderer.TinyFDImpl");
        Class<?> configClass = Class.forName("org.lwjgl.system.Configuration", true, this);
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

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            String path = name.replace('.', '/').concat(".class");
            URL url = getParent().getResource(path);
            if (url == null) {
                throw e;
            }
            try {
                byte[] bytes = IOUtils.toByteArray(url);
                return defineClass(name, bytes, 0, bytes.length, (ProtectionDomain) null);
            } catch (IOException e1) {
                throw new ClassNotFoundException(name, e1);
            }
        }
    }
}
