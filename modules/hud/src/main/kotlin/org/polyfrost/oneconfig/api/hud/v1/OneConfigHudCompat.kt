package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree

private class OneConfigHudCompat(val wrapper: OneConfigHudWrapper) :
    Hud(wrapper.id, wrapper.name, Category.COMPAT), LegacyHudMarker {

    @Composable
    override fun Content() {
    }

    private val hiddenRevision = mutableStateOf(0)

    private val wrapperId = id
    private val wrapperModId = runCatching { wrapper.modId }.getOrNull() ?: "unknown"

    @Volatile
    private var faulted = false

    private fun fault(member: String, error: Throwable) {
        faulted = true
        HudManager.LOGGER.error(
            "Disabling compat HUD '$wrapperId' from '$wrapperModId': $member failed, so " +
                "that mod is probably a different version than OneConfig was built against",
            error,
        )
    }

    private inline fun <T> guard(member: String, fallback: T, block: () -> T): T {
        if (faulted) return fallback
        return try {
            block()
        } catch (e: LinkageError) {
            fault(member, e)
            fallback
        } catch (e: RuntimeException) {
            fault(member, e)
            fallback
        }
    }

    override var hidden: Boolean
        get() {
            hiddenRevision.value
            return guard("hidden", true) { wrapper.hidden }
        }
        set(value) {
            val changed = guard("hidden", false) {
                if (wrapper.hidden == value) false else {
                    wrapper.hidden = value
                    true
                }
            }
            if (changed) hiddenRevision.value++
        }

    fun guardedPlacementReady(): Boolean = guard("placementReady", false) { wrapper.placementReady }

    fun guardedOwnsPlacement(): Boolean = guard("ownsPlacement", false) { wrapper.ownsPlacement }

    fun guardedLinkedProperties(): List<Property<*>> =
        guard("linkedProperties", emptyList()) { wrapper.linkedProperties() }

    fun guardedSave() {
        guard("save", Unit) { wrapper.save() }
    }

    override val persistOwnState: Boolean get() = false

    override val profileLocalTree: Boolean get() = false

    override fun update(): Boolean = false
    override fun multipleInstancesAllowed(): Boolean = false

    override fun deletable(): Boolean = false

    override val supportsScale: Boolean get() = guard("supportsScale", true) { wrapper.supportsScale }

    override var x: Float
        get() = guard("x", 0f) { wrapper.x }
        set(value) { guard("x", Unit) { wrapper.x = value } }
    override var y: Float
        get() = guard("y", 0f) { wrapper.y }
        set(value) { guard("y", Unit) { wrapper.y = value } }
    override var relativeX: Float
        get() = x
        set(value) { x = value }
    override var relativeY: Float
        get() = y
        set(value) { y = value }

    override var customScale: Float
        get() = guard("scale", 1f) { wrapper.scale }
        set(value) { guard("scale", Unit) { wrapper.scale = value } }

    private var lastW = 0f
    private var lastH = 0f

    private fun sizeW(): Float {
        val live = guard("scaledWidth", 0f) { wrapper.scaledWidth }
        if (live > 0f) lastW = live
        return if (live > 0f) live else lastW
    }

    private fun sizeH(): Float {
        val live = guard("scaledHeight", 0f) { wrapper.scaledHeight }
        if (live > 0f) lastH = live
        return if (live > 0f) live else lastH
    }

    override val scaledWidth: Float get() = sizeW()
    override val scaledHeight: Float get() = sizeH()

    override var renderedW: Float
        get() = sizeW()
        set(_) {}
    override var renderedH: Float
        get() = sizeH()
        set(_) {}

    override var staticW: Float
        get() = sizeW()
        set(_) {}
    override var staticH: Float
        get() = sizeH()
        set(_) {}

    override val resizeAxes: HudResize get() = guard("resizeAxes", HudResize.Both) { wrapper.resizeAxes }

    override fun applyEditorWidth(width: Float) {
        guard("scaledWidth", Unit) { wrapper.scaledWidth = width }
    }

    override fun updateRelativeX(absX: Float) { x = absX }
    override fun updateRelativeY(absY: Float) { y = absY }

    override fun onEditorDragStart() {
        guard("onDragStart", Unit) { wrapper.onDragStart() }
    }

    override fun onEditorDragEnd() {
        guard("onDragEnd", Unit) { wrapper.onDragEnd() }
        CompatSnapshots.capture(tree)
    }
}

interface OneConfigHudWrapper {
    var id: String
    var name: String

    val modId: String? get() = null

    var x: Float
    var y: Float

    var scale: Float

    var scaledWidth: Float
    var scaledHeight: Float

    var hidden: Boolean
        get() = false
        set(_) {}

    val supportsScale: Boolean get() = true

    val placementReady: Boolean get() = true

    val ownsPlacement: Boolean get() = false

    val resizeAxes: HudResize get() = HudResize.Both

    fun onDragStart() {}

    fun onDragEnd() {}

    fun linkedProperties(): List<Property<*>> = emptyList()

    fun save() {}

    fun register() {
        val hud = OneConfigHudCompat(this)
        val modId = modId
        if (modId != null) HudManager.register(hud, modId) else HudManager.register(hud)
        hud.make()
        HudManager.activeInstances.add(hud)
        hud.setup()
        val tree = hud.tree
        if (tree != null) {
            for (prop in hud.guardedLinkedProperties()) tree.put(prop)
            trackPlacementPerProfile(hud, tree)
        }
        hud.captureStaticSizeDefaults()
        hud.capturePositionDefaults()
    }

    private fun trackPlacementPerProfile(hud: OneConfigHudCompat, tree: Tree) {
        excludeFromSnapshots(tree)
        tree.addMetadata(CompatSnapshots.GATE_METADATA, java.util.function.BooleanSupplier { hud.guardedPlacementReady() })
        if (!hud.guardedOwnsPlacement()) {
            tree["oc_compat_x"] = placementProperty("x", "X Position", { hud.x }, { hud.x = it })
            tree["oc_compat_y"] = placementProperty("y", "Y Position", { hud.y }, { hud.y = it })
            if (hud.supportsScale) {
                tree["oc_compat_scale"] =
                    placementProperty("scale", "Scale", { hud.customScale }, { hud.customScale = it })
            }
        }
        tree.addMetadata("custom_save", Runnable { hud.guardedSave() })
        CompatSnapshots.track(tree)
    }

    private fun placementProperty(
        key: String,
        name: String,
        getter: () -> Float,
        setter: (Float) -> Unit,
    ): Property<Float> = Properties.functional<Float>(
        { getter() },
        { value -> setter(value) },
        "oc_compat_$key",
        name,
        null,
        Float::class.java,
    ).apply {
        addMetadata(CompatSnapshots.KEY_METADATA, "oc_compat_$key")
        addDisplayCondition { Property.Display.HIDDEN }
    }

    private fun excludeFromSnapshots(tree: Tree) {
        for (node in tree.map.values) {
            when (node) {
                is Property<*> -> node.addMetadata(CompatSnapshots.NO_SNAPSHOT_META, true)
                is Tree -> excludeFromSnapshots(node)
            }
        }
    }
}
