@file:JvmName("Dependencies")

package org.polyfrost.oneconfig.api.config.v1

import java.lang.ref.WeakReference
import java.util.function.Supplier
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler

@ApiStatus.Experimental
@JvmOverloads
public fun Property<*>.dependsOn(toggle: Property<Boolean>, expected: Boolean = true, hide: Boolean = false) {
    val off = if (hide) Property.Display.HIDDEN else Property.Display.DISABLED
    addDisplayCondition(Supplier { if (toggle.get() == !expected) off else Property.Display.SHOWN })
    toggle.addDependent(this)
    addDependencyName(toggle)
}

@ApiStatus.Experimental
public fun Property<*>.hiddenWhen(vararg watch: Property<*>, pollEveryTicks: Int = 0, condition: () -> Boolean) {
    displayWhen(Property.Display.HIDDEN, watch, pollEveryTicks, condition)
}

@ApiStatus.Experimental
public fun Property<*>.disabledWhen(vararg watch: Property<*>, pollEveryTicks: Int = 0, condition: () -> Boolean) {
    displayWhen(Property.Display.DISABLED, watch, pollEveryTicks, condition)
}

@ApiStatus.Experimental
public fun Tree.showWhen(parent: String, vararg children: String, hide: Boolean = true, test: (Any?) -> Boolean = { it == true }) {
    val source = requireProperty(parent)
    val off = if (hide) Property.Display.HIDDEN else Property.Display.DISABLED
    for (child in children) {
        forEachProperty(requireNode(child)) { prop ->
            prop.addDisplayCondition(Supplier { if (test(source.get())) Property.Display.SHOWN else off })
            source.addDependent(prop)
        }
    }
}

@ApiStatus.Experimental
@JvmOverloads
public fun Tree.addDependency(option: String, toggle: String, hide: Boolean = false) {
    val source = requireToggle(toggle)
    forEachProperty(requireNode(option)) { if (it !== source) it.dependsOn(source, true, hide) }
}

@ApiStatus.Experimental
public fun Tree.disableAllUnless(toggle: String) {
    val source = requireToggle(toggle)
    forEachProperty(this) { if (it !== source) it.dependsOn(source) }
}

private fun Property<*>.displayWhen(
    off: Property.Display,
    watch: Array<out Property<*>>,
    pollEveryTicks: Int,
    condition: () -> Boolean,
) {
    addDisplayCondition(Supplier { if (condition()) off else Property.Display.SHOWN })
    for (source in watch) source.addDependent(this)
    if (pollEveryTicks > 0) pollDisplay(pollEveryTicks)
}

private fun Property<*>.pollDisplay(everyTicks: Int) {
    val ref = WeakReference(this)
    var ticks = 0
    EventHandler.ofRemoving(TickEvent.End::class.java) {
        val self = ref.get() ?: return@ofRemoving true
        if (++ticks >= everyTicks) {
            ticks = 0
            self.revaluateDisplay()
        }
        false
    }.register()
}

private fun Property<*>.addDependencyName(toggle: Property<*>) {
    val name = toggle.title?.toString() ?: toggle.id ?: return
    val names = getOrPutMetadata<MutableList<String>>("dependencyNames") { ArrayList(3) }
    if (name !in names) names.add(name)
}

private fun Tree.requireNode(id: String): Node =
    get(id) ?: (if ('.' in id) get(*id.split('.').toTypedArray()) else null)
        ?: throw IllegalArgumentException("Tree ${this.id} does not contain $id")

private fun Tree.requireProperty(id: String): Property<*> =
    requireNode(id) as? Property<*> ?: throw IllegalArgumentException("$id in tree ${this.id} is not a property")

@Suppress("UNCHECKED_CAST")
private fun Tree.requireToggle(id: String): Property<Boolean> {
    val prop = requireProperty(id)
    require(prop.type == Boolean::class.javaPrimitiveType) { "$id in tree ${this.id} is not a boolean property" }
    return prop as Property<Boolean>
}

private fun forEachProperty(node: Node, action: (Property<*>) -> Unit) {
    when (node) {
        is Property<*> -> action(node)
        is Tree -> node.onAll { _, child -> forEachProperty(child, action) }
    }
}
