package org.polyfrost.oneconfig.internal.ui.keybind

import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.ui.components.localizedString
import org.polyfrost.oneconfig.internal.ui.components.localizedTitle
import java.util.WeakHashMap

object MinecraftKeybindRegistrar {
    /**
     * Set on a keybind property to keep it out of Minecraft's Controls menu
     *
     * Use it when the property is a view onto a [net.minecraft.client.KeyMapping] the owning mod already
     * registered since mirroring it would splice in a duplicate mapping
     */
    const val NO_MIRROR_METADATA = "oc_no_mc_mirror"

    private val propByBind = WeakHashMap<OneConfigKeybind, Property<*>>()
    private val wiredProps = WeakHashMap<Property<*>, Boolean>()

    private val scannedTrees = WeakHashMap<Tree, Boolean>()
    private var listenerInstalled = false

    @JvmStatic
    @JvmOverloads
    fun scan(tree: Tree, force: Boolean = true) {
        if (!force && scannedTrees.containsKey(tree)) return
        scannedTrees[tree] = true
        installRebindListener()
        val category = "${tree.localizedTitle()} (OneConfig)"
        walk(tree, category)
    }

    private fun installRebindListener() {
        if (listenerInstalled) return
        listenerInstalled = true
        KeybindManager.addRebindListener { old, new ->
            val prop = propByBind.remove(old) ?: return@addRebindListener
            propByBind[new] = prop
            @Suppress("UNCHECKED_CAST")
            (prop as Property<Any>).set(new)
        }
        KeybindManager.addMenuEditListener { bind ->
            val prop = propByBind[bind] ?: return@addMenuEditListener
            @Suppress("UNCHECKED_CAST")
            (prop as Property<Any>).setReferential(bind)
        }
    }

    private fun walk(tree: Tree, category: String) {
        for (node in tree.map.values) {
            when (node) {
                is Tree -> walk(node, category)
                is Property<*> -> wire(node, category)
            }
        }
    }

    private fun wire(prop: Property<*>, category: String) {
        if (prop.getMetadata<Any?>(NO_MIRROR_METADATA) != null) return
        val bind = prop.get() as? OneConfigKeybind ?: return
        bind.name = resolveName(prop)
        bind.category = category
        bind.defaultKeybind = prop.getMetadata<Any?>("default") as? OneConfigKeybind
        if (KeybindManager.isRegistered(bind)) KeybindManager.refreshMinecraftBinding(bind)
        else KeybindManager.register(bind)
        propByBind[bind] = prop
        if (wiredProps.put(prop, true) == null) {
            @Suppress("UNCHECKED_CAST")
            (prop as Property<Any>).addCallback { value ->
                (value as? OneConfigKeybind)?.let { propByBind[it] = prop }
                false
            }
        }
    }

    private fun resolveName(prop: Property<*>): String =
        localizedString(prop.getMetadata("titleKey"), prop.title ?: prop.id ?: "Keybind")
}
