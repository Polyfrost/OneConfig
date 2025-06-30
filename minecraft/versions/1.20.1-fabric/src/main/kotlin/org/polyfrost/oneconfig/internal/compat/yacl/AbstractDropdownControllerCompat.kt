package org.polyfrost.oneconfig.internal.compat.yacl

import dev.isxander.yacl3.api.Controller
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownController
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import kotlin.reflect.KClass

internal object AbstractDropdownControllerCompat : ExtraHandler<AbstractDropdownController<Any>> {
    override fun canHandle(controller: Controller<*>) = controller is AbstractDropdownController
    override fun handle(
        controller: Controller<*>,
        builder: YaclPropertyBuilder,
    ): KClass<out Visualizer>? {
        val controller = controller as AbstractDropdownController<Any>
        val values = controller.allowedValues
        builder.setter = { value -> (value as? Int)?.let { controller.setFromString(values[value]); controller.option().applyValue() } }
        builder.getter = { values.indexOf(controller.string).coerceAtMost(0) }
        if (values.isEmpty()) {
            return null
        }
        builder["options"] = values.toTypedArray()

        return Visualizer.DropdownVisualizer::class
    }

}