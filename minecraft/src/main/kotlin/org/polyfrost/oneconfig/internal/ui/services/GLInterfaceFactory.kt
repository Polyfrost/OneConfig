package org.polyfrost.oneconfig.internal.ui.services

import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.GLAssembledInterface
import org.jetbrains.skia.makeGLWithInterface
import org.lwjgl.opengl.GL
import org.lwjgl.system.FunctionProvider
import org.lwjgl.system.MemoryUtil
import org.slf4j.LoggerFactory
//? if >= 26.3
import org.lwjgl.sdl.SDLVideo.nSDL_GL_GetProcAddress
//? if >= 26.1 {
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
//? } else {
/*import org.lwjgl.system.APIUtil
import org.lwjgl.system.CallbackI
import org.lwjgl.system.Pointer
import org.lwjgl.system.libffi.FFICIF
import org.lwjgl.system.libffi.LibFFI
*///? }

internal object GLInterfaceFactory {
    private val LOG = LoggerFactory.getLogger(GLInterfaceFactory::class.java)

    private var getProcAddress: Long = 0L

    @Volatile
    private var provider: FunctionProvider? = null

    fun makeDirectContextViaLwjgl(): DirectContext? {
        provider = try {
            GL.getFunctionProvider()
        } catch (e: Throwable) {
            LOG.warn("GLInterfaceFactory: GL.getFunctionProvider() unavailable", e)
            null
        }
        if (provider == null) LOG.warn("GLInterfaceFactory: no GL function provider, resolving through SDL only")

        val getProc = try {
            if (getProcAddress == 0L) getProcAddress = createGetProcStub()
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

    @JvmStatic
    private fun lookupProc(name: Long): Long {
        if (name == 0L) return 0L
        //? if >= 26.3 {
        try {
            val sdl = nSDL_GL_GetProcAddress(name)
            if (sdl != 0L) return sdl
        } catch (_: Throwable) {
        }
        //? }
        return try {
            provider?.getFunctionAddress(MemoryUtil.memUTF8(name)) ?: 0L
        } catch (_: Throwable) {
            0L
        }
    }

    //? if >= 26.1 {
    @JvmStatic
    private fun getProcUpcall(context: MemorySegment, name: MemorySegment): MemorySegment =
        MemorySegment.ofAddress(lookupProc(name.address()))

    private fun createGetProcStub(): Long {
        val handle = MethodHandles.lookup().findStatic(
            GLInterfaceFactory::class.java,
            "getProcUpcall",
            MethodType.methodType(
                MemorySegment::class.java,
                MemorySegment::class.java,
                MemorySegment::class.java,
            ),
        )
        return Linker.nativeLinker().upcallStub(
            handle,
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
            Arena.global(),
        ).address()
    }
    //? } else {
    /*private var getProcCallback: GetProcCallback? = null

    private fun createGetProcStub(): Long {
        val cb = GetProcCallback()
        getProcCallback = cb
        return cb.address()
    }

    @FunctionalInterface
    private fun interface GetProcCallbackI : CallbackI {
        override fun getCallInterface(): FFICIF = CALL_INTERFACE

        override fun callback(ret: Long, args: Long) {
            val contextPtr = MemoryUtil.memGetAddress(MemoryUtil.memGetAddress(args))
            val namePtr = MemoryUtil.memGetAddress(
                MemoryUtil.memGetAddress(args + Pointer.POINTER_SIZE.toLong())
            )
            APIUtil.apiClosureRetP(ret, invoke(contextPtr, namePtr))
        }

        fun invoke(context: Long, name: Long): Long

        companion object {
            private val CALL_INTERFACE: FFICIF = APIUtil.apiCreateCIF(
                LibFFI.FFI_DEFAULT_ABI,
                LibFFI.ffi_type_pointer,
                LibFFI.ffi_type_pointer,
                LibFFI.ffi_type_pointer,
            )
        }
    }

    private class GetProcCallback : GetProcCallbackI {
        override fun invoke(context: Long, name: Long): Long = lookupProc(name)
    }
    *///? }
}
