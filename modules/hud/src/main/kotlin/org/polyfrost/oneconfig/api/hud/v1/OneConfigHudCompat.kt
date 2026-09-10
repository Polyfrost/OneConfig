package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import java.util.concurrent.ConcurrentHashMap

private class OneConfigHudCompat(val wrapper: OneConfigHudWrapper) :
    Hud(wrapper.id, wrapper.name, Category.COMPAT), LegacyHudMarker {

    @Composable
    override fun Content() {
    }

    private val hiddenRevision = mutableStateOf(0)

    @Volatile
    private var faulted = false
    private val loggedFailures = ConcurrentHashMap.newKeySet<String>()

    private fun fault(member: String, error: Throwable) {
        if (faulted) return
        faulted = true
        HudManager.LOGGER.error(
            "Disabling compat HUD '${wrapper.id}' from '${wrapper.modId ?: "unknown"}': $member failed, so " +
                "that mod is probably a different version than OneConfig was built against",
            error,
        )
    }

    private fun reportTransient(member: String, error: Throwable) {
        if (!loggedFailures.add(member)) return
        HudManager.LOGGER.error("Compat HUD '${wrapper.id}': $member threw, using a fallback for it", error)
    }

    private inline fun <T> guard(member: String, fallback: T, block: () -> T): T {
        if (faulted) return fallback
        return try {
            block()
        } catch (e: LinkageError) {
            fault(member, e)
            fallback
        } catch (e: Exception) {
            reportTransient(member, e)
            fallback
        }
    }

    override var hidden: Boolean
        get() {
            hiddenRevision.value
            return guard("hidden", true) { wrapper.hidden }
        }
        set(value) {
            guard("hidden", Unit) {
                if (wrapper.hidden != value) {
                    wrapper.hidden = value
                    hiddenRevision.value++
                }
            }
        }

    override val persistOwnState: Boolean get() = false

    override val profileLocalTree: Boolean get() = false

    override fun update(): Boolean = false
    override fun multipleInstancesAllowed(): Boolean = false

    override fun deletable(): Boolean = faulted

    override val supportsScale: Boolean get() = guard("supportsScale", false) { wrapper.supportsScale }

    private var lastX = 0f
    private var lastY = 0f
    private var lastScale = 1f

    override var x: Float
        get() = guard<Float?>("x", null) { wrapper.x }?.also { lastX = it } ?: lastX
        set(value) { guard("x", Unit) { wrapper.x = value; lastX = value } }
    override var y: Float
        get() = guard<Float?>("y", null) { wrapper.y }?.also { lastY = it } ?: lastY
        set(value) { guard("y", Unit) { wrapper.y = value; lastY = value } }
    override var relativeX: Float
        get() = x
        set(value) { x = value }
    override var relativeY: Float
        get() = y
        set(value) { y = value }

    override var customScale: Float
        get() = guard<Float?>("scale", null) { wrapper.scale }?.also { lastScale = it } ?: lastScale
        set(value) { guard("scale", Unit) { wrapper.scale = value; lastScale = value } }

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

    override val resizeAxes: HudResize get() = guard("resizeAxes", HudResize.None) { wrapper.resizeAxes }

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

    private val placementReady: Boolean get() = guard("placementReady", false) { wrapper.placementReady }

    private val ownsPlacement: Boolean get() = guard("ownsPlacement", true) { wrapper.ownsPlacement }

    fun linkedPropertiesGuarded(): List<Property<*>> =
        guard("linkedProperties", emptyList()) { wrapper.linkedProperties() }

    private fun saveWrapper() {
        guard("save", Unit) { wrapper.save() }
    }

    fun trackPlacementPerProfile(tree: Tree) {
        excludeFromSnapshots(tree)
        tree.addMetadata(CompatSnapshots.GATE_METADATA, java.util.function.BooleanSupplier { placementReady })
        if (!ownsPlacement) {
            tree["oc_compat_x"] = placementProperty("x", "X Position", { x }, { x = it })
            tree["oc_compat_y"] = placementProperty("y", "Y Position", { y }, { y = it })
            if (supportsScale) {
                tree["oc_compat_scale"] = placementProperty("scale", "Scale", { customScale }, { customScale = it })
            }
        }
        tree.addMetadata("custom_save", Runnable { saveWrapper() })
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
            for (prop in hud.linkedPropertiesGuarded()) tree.put(prop)
            hud.trackPlacementPerProfile(tree)
        }
        hud.captureStaticSizeDefaults()
        hud.capturePositionDefaults()
    }
}
