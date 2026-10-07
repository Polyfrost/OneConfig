package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.runtime.snapshots.Snapshot
import kotlin.math.abs
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.serialize.ObjectSerializer
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.ui.components.settings.bumpResetEpoch
import org.polyfrost.oneconfig.internal.ui.hud.bumpHudUiEpoch
import org.polyfrost.oneconfig.utils.v1.WrappingUtils

private val LOGGER = LogManager.getLogger("OneConfig/HudHistory")

private const val HISTORY_LIMIT = 100
private const val FLOAT_TOLERANCE = 1e-3f
private const val PATH_SEPARATOR = "\n"
private const val OWN_STATE_PREFIX = "\u0000"

// layoutRefW, layoutRefH and posSchema change with the window and the layout pass, so they are never a user edit
private val IGNORED_OPTIONS = setOf("layoutRefW", "layoutRefH", "posSchema", "hudClass")
private val POSITION_OPTIONS = setOf("section", "relativeX", "relativeY")

private class OwnState(val id: String, val get: (Hud) -> Any?, val set: (Hud, Any?) -> Unit)

// HUDs wrapping another mod's element keep these outside their config tree
private val OWN_STATE = listOf(
    OwnState("hidden", { it.hidden }) { hud, v -> hud.hidden = v as Boolean },
    OwnState("locked", { it.locked }) { hud, v -> hud.locked = v as Boolean },
    OwnState("relativeX", { it.relativeX }) { hud, v -> hud.relativeX = v as Float },
    OwnState("relativeY", { it.relativeY }) { hud, v -> hud.relativeY = v as Float },
    OwnState("customScale", { it.customScale }) { hud, v -> hud.customScale = v as Float },
)

private class Value(val stored: Any?, val key: Any?)

private class Entry(val hudClass: Class<out Hud>, val values: Map<String, Value>)

private class Layout(val owner: ConfigManager, val huds: Map<String, Entry>)

private fun sameValue(a: Any?, b: Any?): Boolean =
    if (a is Float && b is Float) abs(a - b) < FLOAT_TOLERANCE else a == b

private fun sameLayout(a: Layout, b: Layout): Boolean {
    if (a.owner !== b.owner || a.huds.size != b.huds.size) return false
    for ((id, entry) in a.huds) {
        val other = b.huds[id] ?: return false
        if (entry.hudClass != other.hudClass || entry.values.size != other.values.size) return false
        for ((path, value) in entry.values) {
            val otherValue = other.values[path] ?: return false
            if (!sameValue(value.key, otherValue.key)) return false
        }
    }
    return true
}

private fun forEachOption(tree: Tree, prefix: String = "", action: (String, Property<*>) -> Unit) {
    for ((id, child) in tree.map) {
        if (id.startsWith("reserved:")) continue
        when (child) {
            is Tree -> forEachOption(child, prefix + id + PATH_SEPARATOR, action)
            is Property<*> -> {
                val path = prefix + id
                if (child.type != Void::class.java && path !in IGNORED_OPTIONS) action(path, child)
            }
        }
    }
}

private fun comparable(prop: Property<*>): Any? {
    val value = prop.get()
    if (value == null || WrappingUtils.isSimpleClass(prop.type)) return value
    return try {
        ObjectSerializer.INSTANCE.serialize(value, true, true)
    } catch (_: Throwable) {
        value
    }
}

private fun capture(): Layout {
    val huds = LinkedHashMap<String, Entry>()
    for (hud in HudManager.activeInstances) {
        val tree = hud.tree ?: continue
        val id = tree.id ?: continue
        val values = LinkedHashMap<String, Value>()
        forEachOption(tree) { path, prop ->
            val value = prop.get()
            values[path] = if (value == null || WrappingUtils.isSimpleClass(prop.type)) {
                Value(value, value)
            } else {
                Value(Config.copyDefault(prop.type, value), comparable(prop))
            }
        }
        for (state in OWN_STATE) {
            if (state.id in values) continue
            val value = state.get(hud)
            values[OWN_STATE_PREFIX + state.id] = Value(value, value)
        }
        huds[id] = Entry(hud.javaClass, values)
    }
    return Layout(ConfigManager.active(), huds)
}

@Suppress("UNCHECKED_CAST")
private fun write(hud: Hud, entry: Entry): Boolean {
    val options = HashMap<String, Property<*>>()
    forEachOption(hud.tree) { path, prop -> options[path] = prop }
    var changed = false
    var linksDropped = false
    for ((path, value) in entry.values) {
        if (path.startsWith(OWN_STATE_PREFIX)) {
            val state = OWN_STATE.first { OWN_STATE_PREFIX + it.id == path }
            if (sameValue(state.get(hud), value.stored)) continue
            state.set(hud, value.stored)
        } else {
            val prop = options[path] ?: continue
            if (sameValue(comparable(prop), value.key)) continue
            // a merge link outranks the stored position, so the HUD would stay put while it is held
            if (path in POSITION_OPTIONS && !linksDropped) {
                hud.dropMergeLinks()
                linksDropped = true
            }
            (prop as Property<Any?>).setAsReferential(Config.copyDefault(prop.type, value.stored))
            bumpResetEpoch(prop)
        }
        changed = true
    }
    return changed
}

private fun revive(id: String, entry: Entry): Hud? {
    val provider = HudManager.getProvider(entry.hudClass) ?: return null
    if (provider.isReal) return null
    return try {
        provider.make(Tree.tree(id))
    } catch (e: Exception) {
        LOGGER.warn("Failed to bring back HUD {}", id, e)
        null
    }
}

private fun restore(target: Layout): List<Hud> {
    val revived = ArrayList<Hud>()
    Snapshot.withMutableSnapshot {
        for (hud in ArrayList(HudManager.activeInstances)) {
            val id = hud.tree?.id ?: continue
            if (id !in target.huds && hud.canDelete()) HudManager.removeHud(hud, delete = true)
        }
        for ((id, entry) in target.huds) {
            val existing = HudManager.instanceById(id)
            val hud = existing ?: revive(id, entry) ?: continue
            val changed = write(hud, entry)
            if (existing == null) {
                HudManager.activeInstances.add(hud)
                HudManager.instancesRevision.intValue++
                HudManager.markProviderKnown(hud)
                hud.setup()
                hud.captureStaticSizeDefaults()
                hud.capturePositionDefaults()
                revived.add(hud)
            } else if (changed) {
                bumpHudUiEpoch(hud)
                hud.updateAndRecalculate()
            }
        }
    }
    HudManager.invalidate()
    return revived
}

/**
 * Undo and redo for the design studio, built from snapshots of every HUD's options
 *
 * Edits come from too many places to record one by one, so [poll] compares the layout against the
 * last recorded one and records a step once it has stopped changing
 */
internal class HudHistory {
    private val undoStack = ArrayDeque<Layout>()
    private val redoStack = ArrayDeque<Layout>()
    private var recorded: Layout? = null
    private var settling: Layout? = null

    private fun reset(now: Layout) {
        undoStack.clear()
        redoStack.clear()
        recorded = now
        settling = null
    }

    private fun record(now: Layout) {
        recorded?.let(undoStack::addLast)
        if (undoStack.size > HISTORY_LIMIT) undoStack.removeFirst()
        redoStack.clear()
        recorded = now
        settling = null
    }

    private fun unrecorded(): Boolean = recorded?.let { !sameLayout(it, capture()) } == true

    val canUndo: Boolean get() = undoStack.isNotEmpty() || unrecorded()

    val canRedo: Boolean get() = redoStack.isNotEmpty() && !unrecorded()

    /** [busy] holds a step open while a gesture is still in progress */
    fun poll(busy: Boolean) {
        val now = capture()
        val last = recorded
        if (last == null || last.owner !== now.owner) {
            reset(now)
            return
        }
        if (busy || sameLayout(last, now)) {
            settling = null
            return
        }
        val previous = settling
        if (previous != null && sameLayout(previous, now)) record(now) else settling = now
    }

    /** The HUDs brought back by the step, or `null` when there was nothing to undo */
    fun undo(): List<Hud>? = step(undoStack, redoStack)

    fun redo(): List<Hud>? = step(redoStack, undoStack)

    private fun step(from: ArrayDeque<Layout>, to: ArrayDeque<Layout>): List<Hud>? {
        val now = capture()
        val last = recorded
        if (last == null || last.owner !== now.owner) {
            reset(now)
            return null
        }
        if (!sameLayout(last, now)) record(now)
        val target = from.removeLastOrNull() ?: return null
        recorded?.let(to::addLast)
        val revived = restore(target)
        recorded = capture()
        settling = null
        return revived
    }
}
