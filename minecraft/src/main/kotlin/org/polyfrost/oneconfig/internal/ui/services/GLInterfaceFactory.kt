package org.polyfrost.oneconfig.internal.ui.services

import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.GLAssembledInterface
import org.jetbrains.skia.makeGLWithInterface
import org.slf4j.LoggerFactory

//? if sdl {
import org.lwjgl.sdl.SDL
//?} else {
/*import org.lwjgl.glfw.GLFW
*///?}

internal object GLInterfaceFactory {
    private val LOG = LoggerFactory.getLogger(GLInterfaceFactory::class.java)

    private val procLoader: Long by lazy {
        //? if sdl {
        SDL.getLibrary().getFunctionAddress("SDL_GL_GetProcAddress")
        //?} else
        //GLFW.getLibrary().getFunctionAddress("glfwGetProcAddress")
    }

    fun makeDirectContextViaLwjgl(): DirectContext? = try {
        val iface = GLAssembledInterface.createFromProcAddress(procLoader)
        DirectContext.makeGLWithInterface(iface).also {
            LOG.info("GLInterfaceFactory: created DirectContext via the window library's proc loader")
        }
    } catch (e: Throwable) {
        LOG.warn("GLInterfaceFactory: could not create a GL interface from the window library's proc loader", e)
        null
    }
}
