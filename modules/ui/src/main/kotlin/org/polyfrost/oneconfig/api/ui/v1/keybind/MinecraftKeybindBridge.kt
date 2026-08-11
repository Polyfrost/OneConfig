package org.polyfrost.oneconfig.api.ui.v1.keybind

/**
 * Platform bridge that mirrors [OneConfigKeybind]s into Minecraft's native Controls menu
 */
interface MinecraftKeybindBridge {
    /**
     * Register or refresh the vanilla mapping mirroring [bind]
     *
     * Called when a keybind is registered with the manager
     */
    fun register(bind: OneConfigKeybind)

    /** Remove the vanilla mapping mirroring [bind] if any */
    fun unregister(bind: OneConfigKeybind)

    /**
     * Push [bind]'s current keys/modifiers onto its vanilla mapping
     *
     * Called when OneConfig changes a keybind (such as a rebind in the OneConfig settings UI) so the
     * Controls menu reflects the new value
     */
    fun sync(bind: OneConfigKeybind)
}
