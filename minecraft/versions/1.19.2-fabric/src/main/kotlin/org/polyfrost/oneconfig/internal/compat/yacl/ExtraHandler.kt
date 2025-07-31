package org.polyfrost.oneconfig.internal.compat.yacl

import dev.isxander.yacl3.api.Controller
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import kotlin.reflect.KClass

internal interface ExtraHandler<T : Controller<*>> {

    fun canHandle(controller: Controller<*>): Boolean
    fun handle(controller: Controller<*>, builder: YaclPropertyBuilder): Class<out Visualizer>?

}