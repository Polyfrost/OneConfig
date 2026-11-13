@file:JvmName("TreeObjects")

package org.polyfrost.oneconfig.api.config.v1

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.config.v1.collect.impl.OneConfigCollector
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.OptionWidgets

@ApiStatus.Experimental
@JvmSynthetic
public fun treeFromObject(obj: Any, id: String? = null): Tree {
    val tree = Tree(id ?: (obj as? Config)?.id, null, null, null)
    OneConfigCollector().handle(tree, obj, 0)
    tree.wireConditions(ArrayDeque())
    return tree
}

@ApiStatus.Experimental
public object Trees {
    @JvmStatic
    @JvmOverloads
    public fun fromObject(obj: Any, id: String? = null): Tree = treeFromObject(obj, id)
}

@ApiStatus.Experimental
@JvmOverloads
@OptIn(ExperimentalOneConfigUiApi::class)
public fun Property<*>.setOptions(options: List<String>, labels: Map<String, String> = emptyMap()) {
    if (getMetadata<Any?>(MetaKeys.OPTIONS.name) is Array<*>) addMetadata(MetaKeys.OPTIONS.name, options.toTypedArray())
    else this[MetaKeys.OPTIONS] = options.toList()
    removeMetadata("optionsKey")
    this[MetaKeys.LABELS] = if (labels.isEmpty()) null else options.map { labels[it] ?: it }
    OptionWidgets.refresh(this)
}

private fun Tree.wireConditions(enclosing: ArrayDeque<Tree>) {
    enclosing.addFirst(this)
    onAll { _, node ->
        when (node) {
            is Tree -> node.wireConditions(enclosing)
            is Property<*> -> {
                val conditions = node.consumeMetadata<Array<String>>("conditions") ?: return@onAll
                for (cond in conditions) {
                    val condition = enclosing.firstNotNullOfOrNull { scope ->
                        scope.getProp(cond) ?: if (cond.indexOf('.') >= 0) scope.getProp(*cond.split('.').toTypedArray()) else null
                    } ?: throw IllegalArgumentException("Property ${node.id} is dependant on property $cond, but that property does not exist")
                    require(condition.type == Boolean::class.javaPrimitiveType) {
                        "Property ${node.id} is dependant on property $cond, but it is not a boolean property"
                    }
                    node.addDisplayCondition(Property.recast<Boolean>(condition), false)
                }
            }
        }
    }
    enclosing.removeFirst()
}
