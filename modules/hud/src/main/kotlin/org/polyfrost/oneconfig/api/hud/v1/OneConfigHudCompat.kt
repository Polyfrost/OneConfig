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

    override var hidden: Boolean
        get() {
            hiddenRevision.value
            return wrapper.hidden
        }
        set(value) {
            if (wrapper.hidden == value) return
            wrapper.hidden = value
            hiddenRevision.value++
        }

    override val persistOwnState: Boolean get() = false

    override val profileLocalTree: Boolean get() = false

    override fun update(): Boolean = false
    override fun multipleInstancesAllowed(): Boolean = false

    override fun deletable(): Boolean = false

    override val supportsScale: Boolean get() = wrapper.supportsScale

    override var x: Float by wrapper::x
    override var y: Float by wrapper::y
    override var relativeX: Float by wrapper::x
    override var relativeY: Float by wrapper::y

    override var customScale: Float by wrapper::scale

    private var lastW = 0f
    private var lastH = 0f

    private fun sizeW(): Float {
        val live = runCatching { wrapper.scaledWidth }.getOrDefault(0f)
        if (live > 0f) lastW = live
        return if (live > 0f) live else lastW
    }

    private fun sizeH(): Float {
        val live = runCatching { wrapper.scaledHeight }.getOrDefault(0f)
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

    override val resizeAxes: HudResize get() = wrapper.resizeAxes

    override fun applyEditorWidth(width: Float) {
        wrapper.scaledWidth = width
    }

    override fun updateRelativeX(absX: Float) { x = absX }
    override fun updateRelativeY(absY: Float) { y = absY }

    override fun onEditorDragStart() = wrapper.onDragStart()
    override fun onEditorDragEnd() = wrapper.onDragEnd()
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
            for (prop in linkedProperties()) tree.put(prop)
            trackPlacementPerProfile(tree)
        }
        hud.captureStaticSizeDefaults()
        hud.capturePositionDefaults()
    }

    private fun trackPlacementPerProfile(tree: Tree) {
        excludeFromSnapshots(tree)
        tree.addMetadata(CompatSnapshots.GATE_METADATA, java.util.function.BooleanSupplier { placementReady })
        tree["oc_compat_x"] = placementProperty("x", "X Position", { x }, { x = it })
        tree["oc_compat_y"] = placementProperty("y", "Y Position", { y }, { y = it })
        if (supportsScale) tree["oc_compat_scale"] = placementProperty("scale", "Scale", { scale }, { scale = it })
        tree.addMetadata("custom_save", Runnable { save() })
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
