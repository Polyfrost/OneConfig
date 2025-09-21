package org.polyfrost.oneconfig.internal.compat.yacl

//#if MC != 1.20.4 || FABRIC
import dev.isxander.yacl3.api.Controller
import org.polyfrost.oneconfig.api.config.v1.Visualizer

internal interface ExtraHandler<T : Controller<*>> {

    fun canHandle(controller: Controller<*>): Boolean
    fun handle(controller: Controller<*>, builder: YaclPropertyBuilder): Class<out Visualizer>?

}
//#endif