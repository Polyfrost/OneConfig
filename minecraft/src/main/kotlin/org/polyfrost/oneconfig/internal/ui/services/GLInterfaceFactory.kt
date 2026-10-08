package org.polyfrost.oneconfig.internal.ui.services

import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.GLAssembledInterface
import org.jetbrains.skia.makeGLWithInterface
import org.lwjgl.system.APIUtil
import org.lwjgl.system.Callback
import org.lwjgl.system.CallbackI
import org.lwjgl.system.FunctionProvider
import org.lwjgl.system.JNI
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.Pointer
import org.lwjgl.system.libffi.LibFFI
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

internal object GLInterfaceFactory {
    private val LOG = LoggerFactory.getLogger(GLInterfaceFactory::class.java)

    private class WindowLibrary(val binding: String, val currentContext: String, val procLoader: String)

    private val SDL = WindowLibrary("org.lwjgl.sdl.SDL", "SDL_GL_GetCurrentContext", "SDL_GL_GetProcAddress")
    private val GLFW = WindowLibrary("org.lwjgl.glfw.GLFW", "glfwGetCurrentContext", "glfwGetProcAddress")

    val sdlVideoDriver: String?
        get() = MemoryUtil.memASCIISafe(call(SDL, "SDL_GetCurrentVideoDriver"))

    private val GET_PROC_CIF by lazy {
        APIUtil.apiCreateCIF(LibFFI.FFI_DEFAULT_ABI, LibFFI.ffi_type_pointer, LibFFI.ffi_type_pointer, LibFFI.ffi_type_pointer)
    }

    private fun findProcLoader(): Long {
        for (library in arrayOf(SDL, GLFW)) {
            if (call(library, library.currentContext) == 0L) continue
            val procLoader = address(library, library.procLoader)
            if (procLoader != 0L) return procLoader
        }
        return 0L
    }

    private fun address(library: WindowLibrary, function: String): Long = try {
        val functions = Class.forName(library.binding).getMethod("getLibrary").invoke(null) as FunctionProvider
        functions.getFunctionAddress(function)
    } catch (_: Throwable) {
        0L
    }

    private fun call(library: WindowLibrary, function: String): Long {
        val address = address(library, function)
        return if (address == 0L) 0L else JNI.invokeP(address)
    }

    fun makeDirectContextViaLwjgl(): DirectContext? = try {
        val procLoader = findProcLoader()
        check(procLoader != 0L) { "no GL context?" }
        val adapter = object : CallbackI {
            //? if >=26.1 || =1.8.9 {
            override fun getDescriptor() = Callback.Descriptor(MethodHandles.lookup(), GET_PROC_CIF)
            //?} else {
            /*override fun getCallInterface() = GET_PROC_CIF
            *///?}
            override fun callback(ret: Long, args: Long) {
                val name = MemoryUtil.memGetAddress(MemoryUtil.memGetAddress(args + Pointer.POINTER_SIZE))
                APIUtil.apiClosureRetP(ret, JNI.invokePP(name, procLoader))
            }
        }.address()
        val iface = try {
            GLAssembledInterface.createFromNativePointers(MemoryUtil.NULL, adapter)
        } finally {
            Callback.free(adapter)
        }
        DirectContext.makeGLWithInterface(iface).also {
            LOG.info("GLInterfaceFactory: created DirectContext via the window library's proc loader")
        }
    } catch (e: Throwable) {
        LOG.warn("GLInterfaceFactory: could not create a GL interface from the window library's proc loader", e)
        null
    }
}
