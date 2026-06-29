package org.polyfrost.oneconfig.internal.ui.services

import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.GLAssembledInterface
import org.jetbrains.skia.makeGLWithInterface
import org.lwjgl.opengl.GL
import org.lwjgl.system.APIUtil
//? if >=26.1
import org.lwjgl.system.Callback
import org.lwjgl.system.CallbackI
import org.lwjgl.system.FunctionProvider
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.Pointer
import org.lwjgl.system.libffi.FFICIF
import org.lwjgl.system.libffi.LibFFI
import org.slf4j.LoggerFactory
//? if >=26.1
import java.lang.invoke.MethodHandles

internal object GLInterfaceFactory {
    private val LOG = LoggerFactory.getLogger(GLInterfaceFactory::class.java)

    private var getProcCallback: GetProcCallback? = null
    private var getProcAddress: Long = 0L

    fun makeDirectContextViaLwjgl(): DirectContext? {
        val provider = try {
            GL.getFunctionProvider()
        } catch (e: Throwable) {
            LOG.warn("GLInterfaceFactory: GL.getFunctionProvider() unavailable", e)
            null
        } ?: run {
            LOG.warn("GLInterfaceFactory: no active GL function provider, cannot assemble interface")
            return null
        }

        val getProc = try {
            if (getProcAddress == 0L) {
                val cb = GetProcCallback(provider)
                getProcCallback = cb
                getProcAddress = cb.address()
            }
            getProcAddress
        } catch (e: Throwable) {
            LOG.warn("GLInterfaceFactory: failed to create getProc closure", e)
            return null
        }
        if (getProc == 0L) return null

        return try {
            val iface = GLAssembledInterface.createFromNativePointers(0L, getProc)
            DirectContext.makeGLWithInterface(iface).also {
                LOG.info("GLInterfaceFactory: created DirectContext via LWJGL proc loader")
            }
        } catch (e: Throwable) {
            LOG.warn("GLInterfaceFactory: makeGLWithInterface failed", e)
            null
        }
    }

    private class GetProcCallback(
        private val provider: FunctionProvider,
    ) : CallbackI {

        //? if >=26.1
        override fun getDescriptor(): Callback.Descriptor = DESCRIPTOR
        //? if <26.1
        /*override fun getCallInterface(): FFICIF = CALL_INTERFACE*/

        override fun callback(ret: Long, args: Long) {
            var addr = 0L
            try {
                val namePtr = MemoryUtil.memGetAddress(
                    MemoryUtil.memGetAddress(args + Pointer.POINTER_SIZE.toLong())
                )
                if (namePtr != 0L) {
                    addr = provider.getFunctionAddress(MemoryUtil.memUTF8(namePtr))
                }
            } catch (_: Throwable) {
                addr = 0L
            }
            APIUtil.apiClosureRetP(ret, addr)
        }

        companion object {
            private val CALL_INTERFACE: FFICIF = APIUtil.apiCreateCIF(
                LibFFI.FFI_DEFAULT_ABI,
                LibFFI.ffi_type_pointer,
                LibFFI.ffi_type_pointer,
                LibFFI.ffi_type_pointer,
            )
            //? if >=26.1
            private val DESCRIPTOR: Callback.Descriptor = Callback.Descriptor(MethodHandles.lookup(), CALL_INTERFACE)
        }
    }
}
