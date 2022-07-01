package cc.polyfrost.oneconfig.internal.plugin.hooks;

import org.lwjgl.system.FunctionProvider;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * Taken from LWJGLTwoPointFive under The Unlicense
 * <a href="https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/">https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/</a>
 */
public class Lwjgl2FunctionProvider implements FunctionProvider {

    private static final Class<?> GLContext;
    private final Method m_getFunctionAddress;

    static {
        try {
            GLContext = Class.forName("org.lwjgl.opengl.GLContext");
        } catch (Throwable t) {
            throw new RuntimeException("Unable to initialize LWJGL2FunctionProvider", t);
        }
    }

    public Lwjgl2FunctionProvider() {
        try {
            m_getFunctionAddress = GLContext.getDeclaredMethod("getFunctionAddress", String.class);
            m_getFunctionAddress.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getFunctionAddress(CharSequence functionName) {
        try {
            return (long) m_getFunctionAddress.invoke(null, functionName.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getFunctionAddress(ByteBuffer byteBuffer) {
        throw new UnsupportedOperationException();
    }
}