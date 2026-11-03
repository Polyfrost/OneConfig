/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.hud.v1

import java.util.IdentityHashMap
import androidx.compose.runtime.snapshots.Snapshot
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.config.v1.ConfigManager

/**
 * A set of HUDs the editor moves and scales as one box
 *
 * Groups nest, so a group can hold other groups as well as HUDs
 *
 * Membership lives on each HUD as [Hud.groupPath] so it is saved, copied and undone with the rest of
 * the HUD's options, and a group exists for exactly as long as one of its HUDs does
 *
 * Two groups are equal when they have the same [path]
 */
@ApiStatus.Experimental
public class HudGroup internal constructor(
    /** The ids from the outermost group down to this one joined by [HudGroups.SEPARATOR] */
    public val path: String,
) {
    public val id: String get() = path.substring(path.lastIndexOf(HudGroups.SEPARATOR) + 1)

    /** The group this one is nested in, or `null` for a top-level group */
    public val parent: HudGroup? get() {
        val i = path.lastIndexOf(HudGroups.SEPARATOR)
        return if (i <= 0) null else HudGroup(path.substring(0, i))
    }

    /** How many groups this one is nested in */
    public val depth: Int get() = path.count { it == HudGroups.SEPARATOR }

    /** The name a mod gave this group through [HudGroups.registerDefault], if any */
    public val name: String? get() = HudGroups.nameOf(id)

    /** Every live HUD in this group or in one of its sub-groups */
    public val huds: List<Hud> get() = HudManager.activeInstances.filter { it in this }

    /** The live HUDs sitting directly in this group rather than in one of its sub-groups */
    public val directHuds: List<Hud> get() = HudManager.activeInstances.filter { it.groupPath == path }

    /** The groups nested directly in this one */
    public val subGroups: List<HudGroup> get() {
        val prefix = path + HudGroups.SEPARATOR
        val out = LinkedHashSet<HudGroup>()
        for (hud in HudManager.activeInstances) {
            val p = hud.groupPath
            if (!p.startsWith(prefix)) continue
            val end = p.indexOf(HudGroups.SEPARATOR, prefix.length)
            out.add(HudGroup(if (end < 0) p else p.substring(0, end)))
        }
        return out.toList()
    }

    public operator fun contains(hud: Hud): Boolean = HudGroups.isWithin(hud.groupPath, path)

    /** Whether [group] is nested somewhere inside this one, which a group never is in itself */
    public operator fun contains(group: HudGroup): Boolean = group.path != path && HudGroups.isWithin(group.path, path)

    override fun equals(other: Any?): Boolean = other is HudGroup && other.path == path

    override fun hashCode(): Int = path.hashCode()

    override fun toString(): String = "HudGroup($path)"
}

/**
 * Creates, changes and lays out [HudGroup]s
 *
 * Grouping never changes how a HUD renders in game, every HUD keeps its own position and scale and
 * groups only decide what the editor moves and scales together
 */
@ApiStatus.Experimental
public object HudGroups {
    public const val SEPARATOR: Char = '/'

    private const val MIN_SCALE = 0.25f
    private const val MAX_SCALE = 4f

    /** Backstop for walking a chain of HUDs anchored to one another */
    private const val MAX_ANCHOR_DEPTH = 16

    private class DefaultGroup(val id: String, val members: List<Class<out Hud>>)

    private val defaults = LinkedHashMap<String, DefaultGroup>()
    private val names = HashMap<String, String>()

    @Volatile
    private var defaultsPending = false

    /** The group [hud] sits directly in, or `null` when it is not grouped */
    @JvmStatic
    public fun of(hud: Hud): HudGroup? = hud.groupPath.takeIf { it.isNotEmpty() }?.let(::HudGroup)

    /** Every group [hud] is in from the outermost down to the one it sits directly in */
    @JvmStatic
    public fun chainOf(hud: Hud): List<HudGroup> {
        val path = hud.groupPath
        if (path.isEmpty()) return emptyList()
        val out = ArrayList<HudGroup>(2)
        var i = path.indexOf(SEPARATOR)
        while (i >= 0) {
            out.add(HudGroup(path.substring(0, i)))
            i = path.indexOf(SEPARATOR, i + 1)
        }
        out.add(HudGroup(path))
        return out
    }

    /** The outermost group [hud] is in, or `null` when it is not grouped */
    @JvmStatic
    public fun outermostOf(hud: Hud): HudGroup? {
        val path = hud.groupPath
        if (path.isEmpty()) return null
        val i = path.indexOf(SEPARATOR)
        return HudGroup(if (i < 0) path else path.substring(0, i))
    }

    /** Every group with at least one live HUD, each one listed before the groups nested in it */
    @JvmStatic
    public fun all(): List<HudGroup> {
        val out = LinkedHashSet<HudGroup>()
        for (hud in HudManager.activeInstances) out.addAll(chainOf(hud))
        return out.sortedBy { it.depth }
    }

    @JvmStatic
    public fun nameOf(id: String): String? = names[id]

    /**
     * Puts [huds] and [groups] into a new group and returns it
     *
     * The new group goes wherever the items already shared a parent, so grouping two HUDs inside a
     * group makes a sub-group while grouping two top-level groups nests both of them in the new one
     *
     * Returns `null` and changes nothing when there is nothing to group
     */
    @JvmStatic
    @JvmOverloads
    public fun group(huds: Collection<Hud>, groups: Collection<HudGroup> = emptyList()): HudGroup? {
        val units = units(huds, groups)
        if (units.isEmpty()) return null
        val parent = commonParent(units.map { it.parentPath })
        val created = HudGroup(join(parent, newId()))
        move(units, created.path)
        return created
    }

    /**
     * Moves [huds] and [groups] into [target] keeping any sub-groups they form
     *
     * Returns `false` and changes nothing when that would put a group inside itself
     */
    @JvmStatic
    @JvmOverloads
    public fun addTo(target: HudGroup, huds: Collection<Hud>, groups: Collection<HudGroup> = emptyList()): Boolean {
        if (groups.any { it == target || target in it }) return false
        val units = units(huds, groups).filter { it.parentPath != target.path }
        if (units.isEmpty()) return false
        move(units, target.path)
        return true
    }

    /** Dissolves [group], handing everything in it to the group it was nested in */
    @JvmStatic
    public fun ungroup(group: HudGroup) {
        val parent = group.parent?.path.orEmpty()
        Snapshot.withMutableSnapshot {
            for (hud in group.huds) hud.groupPath = join(parent, hud.groupPath.substring(group.path.length).trimStart(SEPARATOR))
        }
        HudManager.invalidate()
    }

    /** Takes [hud] out of the group it sits directly in, handing it to the group around that one */
    @JvmStatic
    public fun removeFromGroup(hud: Hud) {
        val group = of(hud) ?: return
        Snapshot.withMutableSnapshot { hud.groupPath = group.parent?.path.orEmpty() }
        HudManager.invalidate()
    }

    /** Takes [group] out of the group it is nested in, keeping it whole, and hands it to the group around that one */
    @JvmStatic
    public fun removeFromGroup(group: HudGroup) {
        val parent = group.parent ?: return
        move(listOf(Moving(group.huds, parent.path)), parent.parent?.path.orEmpty())
    }

    /**
     * Groups the HUDs of [members] under [id] the first time they are all loaded together
     *
     * Only HUDs that are not grouped yet are added, and once applied the group is never forced back
     * on, so the user can ungroup it and have that stick
     *
     * [id] must not contain [SEPARATOR] and should be prefixed with the mod id so it cannot clash with
     * another mod's group
     */
    @JvmStatic
    public fun registerDefault(id: String, name: String, vararg members: Class<out Hud>) {
        require(id.isNotEmpty() && SEPARATOR !in id) { "HUD group id '$id' must be non-empty and contain no '$SEPARATOR'" }
        defaults[id] = DefaultGroup(id, members.toList())
        names[id] = name
        defaultsPending = true
    }

    internal fun applyPendingDefaults() {
        if (!defaultsPending) return
        defaultsPending = false
        applyDefaults()
    }

    internal fun applyDefaults() {
        defaultsPending = false
        for (default in defaults.values) {
            if (HudManager.isGroupKnown(default.id)) continue
            val members = HudManager.activeInstances.filter { hud ->
                hud.groupPath.isEmpty() && hud.isReal && default.members.any { it.isInstance(hud) }
            }
            if (members.size < 2) continue
            Snapshot.withMutableSnapshot { for (hud in members) hud.groupPath = default.id }
            for (hud in members) {
                val treeId = hud.tree?.id ?: continue
                try {
                    ConfigManager.active().save(treeId)
                } catch (e: Exception) {
                    HudManager.LOGGER.error("Failed to save HUD $treeId after grouping it", e)
                }
            }
            HudManager.markGroupKnown(default.id)
            HudManager.LOGGER.info("Grouped {} HUDs into default group {}", members.size, default.id)
        }
    }

    /**
     * The box around [huds] as `[x, y, width, height]` in gui units, or `null` when none of them has a
     * size yet
     *
     * Read it again whenever it is needed since HUDs change size as their content changes
     */
    @JvmStatic
    @JvmOverloads
    public fun bounds(huds: Collection<Hud>, boundsOf: (Hud) -> FloatArray? = ::boxOf): FloatArray? {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (hud in huds) {
            val b = boundsOf(hud) ?: continue
            minX = minOf(minX, b[0])
            minY = minOf(minY, b[1])
            maxX = maxOf(maxX, b[0] + b[2])
            maxY = maxOf(maxY, b[1] + b[3])
        }
        if (minX > maxX || minY > maxY) return null
        return floatArrayOf(minX, minY, maxX - minX, maxY - minY)
    }

    private fun boxOf(hud: Hud): FloatArray = floatArrayOf(hud.x, hud.y, hud.scaledWidth, hud.scaledHeight)

    /**
     * The HUDs of [huds] that hold their own place on screen
     *
     * The rest hang off another HUD in [huds] by an anchor or a fused background, so moving the roots
     * carries them along and moving them as well would move them twice
     */
    @JvmStatic
    public fun roots(huds: Collection<Hud>): List<Hud> {
        val set = identitySet(huds)
        return huds.filter { hud ->
            // anchors and fused backgrounds hold each axis separately, so a HUD only rides along when
            // both of its axes hang off a HUD that is moving too
            val (px, py) = axisParents(hud)
            !followsWithin(hud, px, set) || !followsWithin(hud, py, set)
        }
    }

    /** Moves the roots of [huds] by ([dx], [dy]) from the positions in [starts] */
    @JvmStatic
    public fun moveFrom(huds: Collection<Hud>, starts: Map<Hud, Pair<Float, Float>>, dx: Float, dy: Float) {
        // parents first, so a HUD still pinned to a moving one on one axis is placed against where it ended up
        for (hud in parentsFirst(roots(huds), huds)) {
            val (sx, sy) = starts[hud] ?: continue
            hud.setAbsolutePosition(sx + dx, sy + dy)
        }
    }

    /**
     * Scales [huds] as one block about a fixed point
     *
     * Take one when a resize gesture starts and [apply] it as the gesture goes on, every call scales
     * from the sizes and positions the HUDs had at the start
     */
    @JvmStatic
    public fun scaling(huds: Collection<Hud>, pivotX: Float, pivotY: Float): Scaling = Scaling(huds, pivotX, pivotY)

    public class Scaling internal constructor(huds: Collection<Hud>, private val pivotX: Float, private val pivotY: Float) {
        private val members: List<Hud>
        private val startScale: FloatArray
        private val startX: FloatArray
        private val startY: FloatArray
        private val startW: FloatArray
        private val startH: FloatArray

        /** The smallest and largest factor that keeps every HUD inside the allowed scale range */
        public val minFactor: Float
        public val maxFactor: Float

        init {
            // anchor parents first so every HUD is placed against where its parent already ended up
            members = parentsFirst(huds, huds)
            startScale = FloatArray(members.size) { members[it].customScale }
            startX = FloatArray(members.size) { members[it].x }
            startY = FloatArray(members.size) { members[it].y }
            startW = FloatArray(members.size) { members[it].renderedW }
            startH = FloatArray(members.size) { members[it].renderedH }
            var lo = 0f
            var hi = Float.POSITIVE_INFINITY
            for (s in startScale) {
                if (s <= 0f) continue
                lo = maxOf(lo, MIN_SCALE / s)
                hi = minOf(hi, MAX_SCALE / s)
            }
            minFactor = lo.coerceAtMost(1f)
            maxFactor = if (hi.isInfinite()) 1f else hi.coerceAtLeast(1f)
        }

        /** Scales everything to [factor] times its starting size and returns the factor actually used */
        public fun apply(factor: Float): Float {
            val f = factor.coerceIn(minFactor, maxFactor)
            // the laid out size only catches up on the next frame, so set it now or every anchor and
            // growth point below would be worked out against the old size
            for (i in members.indices) {
                val hud = members[i]
                hud.customScale = startScale[i] * f
                if (startW[i] > 0f) hud.renderedW = startW[i] * f
                if (startH[i] > 0f) hud.renderedH = startH[i] * f
            }
            for (i in members.indices) {
                members[i].setAbsolutePosition(pivotX + (startX[i] - pivotX) * f, pivotY + (startY[i] - pivotY) * f)
            }
            return f
        }
    }

    internal fun isWithin(path: String, ancestor: String): Boolean =
        ancestor.isNotEmpty() && path.startsWith(ancestor) &&
            (path.length == ancestor.length || path[ancestor.length] == SEPARATOR)

    /** A HUD or group being moved into another group along with the parent path it currently sits in */
    private class Moving(val huds: List<Hud>, val parentPath: String)

    private fun units(huds: Collection<Hud>, groups: Collection<HudGroup>): List<Moving> {
        // a group already holding another selected group or HUD carries it along on its own
        val groupList = groups.distinct().filter { g -> groups.none { it != g && g in it } }
        val out = ArrayList<Moving>(huds.size + groupList.size)
        for (g in groupList) out.add(Moving(g.huds, g.parent?.path.orEmpty()))
        for (hud in huds.distinct()) {
            if (groupList.any { hud in it }) continue
            out.add(Moving(listOf(hud), hud.groupPath))
        }
        return out.filter { it.huds.isNotEmpty() }
    }

    private fun move(units: List<Moving>, into: String) {
        Snapshot.withMutableSnapshot {
            for (unit in units) {
                for (hud in unit.huds) {
                    val rest = hud.groupPath.substring(unit.parentPath.length).trimStart(SEPARATOR)
                    hud.groupPath = join(into, rest)
                }
            }
        }
        HudManager.invalidate()
    }

    private fun commonParent(paths: List<String>): String {
        var common = paths.firstOrNull() ?: return ""
        for (p in paths) {
            while (common.isNotEmpty() && !isWithin(p, common)) {
                common = common.substring(0, maxOf(common.lastIndexOf(SEPARATOR), 0))
            }
        }
        return common
    }

    private fun join(parent: String, child: String): String = when {
        parent.isEmpty() -> child
        child.isEmpty() -> parent
        else -> "$parent$SEPARATOR$child"
    }

    /**
     * The [Hud.groupPath] each copy of [originals] should get when they are duplicated together
     *
     * A group copied whole becomes a new group of the copies while a group only partly copied keeps
     * the copies in the original, so duplicating one HUD leaves it beside the original
     */
    @ApiStatus.Internal
    @JvmStatic
    public fun pathsForCopies(originals: List<Hud>): List<String> {
        val copied = identitySet(originals)
        val taken = takenIds()
        val renamed = HashMap<String, String>()
        return originals.map { hud ->
            chainOf(hud).joinToString(SEPARATOR.toString()) { group ->
                if (group.huds.all { it in copied }) renamed.getOrPut(group.path) { newId(taken) } else group.id
            }
        }
    }

    private fun takenIds(): MutableSet<String> {
        val taken = HashSet<String>()
        for (hud in HudManager.activeInstances) taken.addAll(hud.groupPath.split(SEPARATOR))
        return taken
    }

    private fun newId(taken: MutableSet<String> = takenIds()): String {
        while (true) {
            val id = "g" + Integer.toString((Math.random() * 36.0 * 36 * 36 * 36).toInt(), 36)
            if (taken.add(id)) return id
        }
    }

    private fun identitySet(huds: Collection<Hud>): MutableSet<Hud> =
        java.util.Collections.newSetFromMap(IdentityHashMap<Hud, Boolean>()).also { it.addAll(huds) }

    /** The HUDs [hud]'s x and y positions follow, the same one for both when it is anchored by hand */
    private fun axisParents(hud: Hud): Pair<Hud?, Hud?> {
        val anchor = hud.anchorParent
        if (anchor != null) return anchor to anchor
        return hud.mergeLinkX?.parent to hud.mergeLinkY?.parent
    }

    private fun followsWithin(hud: Hud, parent: Hud?, set: Set<Hud>): Boolean =
        parent != null && parent !== hud && parent in set

    /**
     * [huds] sorted so every HUD comes after the HUDs of [moving] it follows
     *
     * Chains that pass through a HUD outside [moving] count as well, since that HUD is still carried
     * by whatever it hangs off
     */
    private fun parentsFirst(huds: Collection<Hud>, moving: Collection<Hud>): List<Hud> {
        val set = identitySet(moving)
        val memo = IdentityHashMap<Hud, Int>()
        fun depth(hud: Hud, steps: Int): Int {
            memo[hud]?.let { return it }
            if (steps > MAX_ANCHOR_DEPTH) return 0
            // a loop resolves to the screen so treat a HUD already being visited as a root
            memo[hud] = 0
            val (px, py) = axisParents(hud)
            var d = 0
            for (p in arrayOf(px, py)) {
                if (p == null || p === hud) continue
                d = maxOf(d, depth(p, steps + 1) + if (p in set) 1 else 0)
            }
            memo[hud] = d
            return d
        }
        return huds.sortedBy { depth(it, 0) }
    }
}
