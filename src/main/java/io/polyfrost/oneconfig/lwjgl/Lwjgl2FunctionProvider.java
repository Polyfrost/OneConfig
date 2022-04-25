package io.polyfrost.oneconfig.lwjgl;

import org.lwjgl.opengl.GLContext;
import org.lwjgl.system.FunctionProvider;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * Taken from LWJGLTwoPointFive under The Unlicense
 * <a href="https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/">https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/</a>
 */
public class Lwjgl2FunctionProvider implements FunctionProvider {

    private final Method m_getFunctionAddress;

    public Lwjgl2FunctionProvider() {
        try {
            m_getFunctionAddress = GLContext.class.getDeclaredMethod("getFunctionAddress", String.class);
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