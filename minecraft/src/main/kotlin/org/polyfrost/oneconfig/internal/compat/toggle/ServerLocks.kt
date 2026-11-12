package org.polyfrost.oneconfig.internal.compat.toggle

import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.backend.Backend
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.ui.v1.ModToggles
import org.polyfrost.oneconfig.api.ui.v1.ServerRestrictions
import org.polyfrost.oneconfig.internal.ui.api.PropertyModToggle
import org.polyfrost.oneconfig.internal.ui.api.findMasterSwitch
import org.polyfrost.oneconfig.internal.ui.components.asRenderText

/**
 * Holds off the mods a server restricts whose switch is one of their OneConfig options
 *
 * Mods [ModGates] manages read the restriction through [ModToggles.isEnabled]. Here the switch is masked
 * to off like a gated mod's options, so the value the player chose is never written over
 */
internal object ServerLocks {
    private val LOGGER = LogManager.getLogger("OneConfig/ServerRestrictions")
    private val masks = HashMap<String, ConfigMask>()

    @JvmStatic
    fun register() {
        ServerRestrictions.addListener { sync() }
        ConfigManager.addProfileChangeListener(object : ConfigManager.ProfileChangeListener {
            // the outgoing profile is saved, which must not take the masked switch with it
            override fun onProfileUnloading(profile: String) = liftAll()

            override fun onProfileChanged(newProfile: String) = sync()

            override fun onProfileOperationFinished() = sync()
        })
    }

    @Synchronized
    private fun sync() {
        val restricted = ServerRestrictions.disabledMods()
        masks.keys.filter { it !in restricted }.forEach { masks.remove(it)?.remove() }
        for (modId in restricted) {
            if (modId in masks) continue
            val (tree, path) = switchOf(modId) ?: continue
            val mask = ConfigMask(modId, listOf(tree.id), mapOf(path to false))
            mask.apply()
            if (mask.applied) masks[modId] = mask else LOGGER.warn("Could not switch {} off for the server", modId)
        }
    }

    @Synchronized
    private fun liftAll() {
        masks.values.forEach { it.remove() }
        masks.clear()
    }

    /** The tree holding the switch of [modId] and the path to it, or null when the switch is not a OneConfig option */
    private fun switchOf(modId: String): Pair<Tree, String>? {
        val toggle = ModToggles.toggleFor(modId)
        val switch = when {
            toggle is PropertyModToggle -> toggle.property
            toggle != null -> return null
            else -> treeOf(modId)?.findMasterSwitch() ?: return null
        }
        for (tree in nativeTrees()) {
            tree.pathTo(switch)?.let { return tree to it }
        }
        return null
    }

    private fun treeOf(modId: String): Tree? {
        val name = ModInfo.loadedMods.firstOrNull { it.id == modId }?.name
        return nativeTrees().firstOrNull { tree ->
            val id = tree.id?.removeSuffix(".json") ?: return@firstOrNull false
            id == modId || (name != null && tree.title?.asRenderText().equals(name, ignoreCase = true))
        }
    }

    private fun nativeTrees() = ConfigManager.active().trees().filter {
        it.getMetadata<Any>(Backend.UI_ONLY_METADATA) != true && it.getMetadata<Any>(CompatSnapshots.SNAPSHOT_METADATA) != true
    }

    private fun Tree.pathTo(target: Property<*>, prefix: String = ""): String? {
        for ((key, node) in map) {
            if (node === target) return prefix + key
            if (node is Tree) node.pathTo(target, "$prefix$key.")?.let { return it }
        }
        return null
    }
}
