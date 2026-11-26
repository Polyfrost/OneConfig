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

import androidx.compose.runtime.snapshots.Snapshot
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import java.util.IdentityHashMap

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

    /** The name a mod gave this group through [HudGroups.registerDefault] or [HudGroups.registerPreset], if any */
    public val name: String? get() = HudGroups.nameOf(id)

    /** Every live HUD in this group or in one of its sub-groups */
    public val huds: List<Hud> get() = HudManager.activeInstances.filter { it in this }

    /** The live HUDs sitting directly in this group rather than in one of its sub-groups */
    public val directHuds: List<Hud> get() = HudManager.activeInstances.filter { it.groupPath == path }

    /**
     * Whether the whole group is locked in place, which is when every HUD in it is locked
     *
     * A HUD locked on its own only holds still inside the group and still moves with it
     */
    public val locked: Boolean get() = huds.let { members -> members.isNotEmpty() && members.all { it.locked } }

    /** The largest [Hud.customScale] among the HUDs that can be scaled */
    public val scale: Float get() = HudGroups.scaleOf(huds)

    /** Whether every HUD that can be scaled is at the same [Hud.customScale] */
    public val uniformScale: Boolean get() = HudGroups.hasUniformScale(huds)

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

private val HUD_SCALE_RANGE = 0.25f..4f
private const val DEFAULT_HUD_WIDTH = 60f
private const val DEFAULT_HUD_HEIGHT = 20f
private const val LAYOUT_GAP = 4f

/**
 * Creates, changes and lays out [HudGroup]s
 *
 * Grouping never changes how a HUD renders in game, every HUD keeps its own position and scale and
 * groups only decide what the editor moves and scales together
 */
@ApiStatus.Experimental
public object HudGroups {
    public const val SEPARATOR: Char = '/'

    /** Splits the preset id from the unique part in the id of a group made from a [HudPreset] */
    public const val PRESET_SEPARATOR: Char = '#'

    /** Backstop for walking a chain of HUDs anchored to one another */
    private const val MAX_ANCHOR_DEPTH = 16

    private class DefaultGroup(val id: String, val members: List<Class<out Hud>>)

    private val defaults = LinkedHashMap<String, DefaultGroup>()
    private val names = HashMap<String, String>()
    private val presets = LinkedHashMap<String, HudPreset>()

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

    /** The name registered for the group with [id], which a group made from a [HudPreset] has through its preset */
    @JvmStatic
    public fun nameOf(id: String): String? =
        names[id] ?: presetNamed(id.substringBefore(PRESET_SEPARATOR, ""))?.name

    private fun presetNamed(id: String): HudPreset? = synchronized(presets) { presets[id] }

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

    /** Locks or unlocks every HUD in [group] */
    @JvmStatic
    public fun setLocked(group: HudGroup, locked: Boolean): Unit = setLocked(group.huds, locked)

    /** Locks or unlocks every HUD in [huds] */
    @JvmStatic
    public fun setLocked(huds: Collection<Hud>, locked: Boolean) {
        Snapshot.withMutableSnapshot { for (hud in huds) hud.locked = locked }
    }

    /** The largest [Hud.customScale] among the HUDs of [huds] that can be scaled */
    @JvmStatic
    public fun scaleOf(huds: Collection<Hud>): Float = scalable(huds).maxOfOrNull { it.customScale } ?: 1f

    /** Whether every HUD of [huds] that can be scaled is at the same [Hud.customScale] */
    @JvmStatic
    public fun hasUniformScale(huds: Collection<Hud>): Boolean = scalable(huds).map { it.customScale }.distinct().size <= 1

    /** The values [HudGroup.scale] can take while every HUD stays inside its own [HUD_SCALE_RANGE] */
    @JvmStatic
    public fun scaleRange(group: HudGroup): ClosedFloatingPointRange<Float> = scaleRange(group.huds)

    /** The values [scaleOf] can take for [huds] while every HUD stays inside its own [HUD_SCALE_RANGE] */
    @JvmStatic
    public fun scaleRange(huds: Collection<Hud>): ClosedFloatingPointRange<Float> {
        val scaling = Scaling(huds, 0f, 0f)
        val scale = scaleOf(huds)
        return scale * scaling.minFactor..scale * scaling.maxFactor
    }

    /**
     * Sets [HudGroup.scale], scaling the whole group as one block so HUDs at different scales keep
     * their proportions
     *
     * The group stays against the edge of the screen it sits nearest
     */
    @JvmStatic
    public fun setScale(group: HudGroup, scale: Float): Unit = setScale(group.huds, scale)

    /**
     * Sets [scaleOf] for [huds], scaling them as one block so HUDs at different scales keep their
     * proportions
     *
     * The block stays against the edge of the screen it sits nearest
     */
    @JvmStatic
    public fun setScale(huds: Collection<Hud>, scale: Float) {
        val current = scaleOf(huds)
        val box = bounds(huds) ?: return
        if (current <= 0f) return
        val sw = HudManager.guiScreenWidth
        val sh = HudManager.guiScreenHeight
        Snapshot.withMutableSnapshot {
            Scaling(huds, box[0] + box[2] * pivotFraction(box[0] + box[2] / 2f, sw), box[1] + box[3] * pivotFraction(box[1] + box[3] / 2f, sh))
                .apply(scale / current)
        }
    }

    /** Puts every HUD in [group] that can be scaled at the same [scale] */
    @JvmStatic
    public fun matchScale(group: HudGroup, scale: Float): Unit = matchScale(group.huds, scale)

    /** Puts every HUD in [huds] that can be scaled at the same [scale] */
    @JvmStatic
    public fun matchScale(huds: Collection<Hud>, scale: Float) {
        Snapshot.withMutableSnapshot {
            for (hud in parentsFirst(scalable(huds), huds)) {
                val previous = hud.customScale
                hud.customScale = scale.coerceIn(HUD_SCALE_RANGE)
                resized(hud, hud.renderedW, hud.renderedH, if (previous > 0f) hud.customScale / previous else 1f)
            }
        }
    }

    private fun pivotFraction(center: Float, screen: Float): Float = when {
        screen <= 0f || center < screen / GRID_SIZE -> 0f
        center < screen * 2f / GRID_SIZE -> 0.5f
        else -> 1f
    }

    /**
     * Tells [hud] the size it will have after its scale changed by [factor] from a box of [w] by [h]
     *
     * The laid out size only catches up on the next frame, so without this every anchor and growth
     * point would be worked out against the old size
     */
    private fun resized(hud: Hud, w: Float, h: Float, factor: Float) {
        hud.forgetMinimumSize()
        if (w > 0f) hud.renderedW = w * factor
        if (h > 0f) hud.renderedH = h * factor
    }

    /**
     * The HUDs of [huds] the corner handles can resize
     *
     * The rest keep their size when their group scales and only move with it
     */
    @JvmStatic
    public fun scalable(huds: Collection<Hud>): List<Hud> = huds.filter {
        it.resizeAxes == HudResize.Both && (it as? LegacyHudMarker)?.supportsScale != false
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
                (hud.groupPath.isEmpty() || inDefault(hud, default.id)) && hud.isReal && groupable(hud) &&
                    default.members.any { it.isInstance(hud) }
            }
            if (members.size < 2) continue
            if (!default.members.all { type -> members.any(type::isInstance) }) continue
            val added = members.filter { it.groupPath.isEmpty() }
            Snapshot.withMutableSnapshot { for (hud in added) hud.groupPath = default.id }
            val preset = presetNamed(default.id)
            val action = preset?.defaultAction
            if (preset != null && action != null && members.none { it.isAnchored }) {
                arrangeSafely(action, alignedMembers(preset, members))
            }
            if (!saveTrees(members)) continue
            HudManager.markGroupKnown(default.id)
            HudManager.LOGGER.info("Grouped {} HUDs into default group {}", members.size, default.id)
        }
    }

    private fun inDefault(hud: Hud, id: String): Boolean =
        hud.groupPath == id || hud.groupPath.startsWith(id + SEPARATOR)

    private fun saveTrees(huds: Collection<Hud>): Boolean {
        var saved = true
        for (hud in huds) {
            val treeId = hud.tree?.id ?: continue
            try {
                if (!ConfigManager.active().save(treeId)) {
                    HudManager.LOGGER.error("Failed to save HUD {} after grouping it", treeId)
                    saved = false
                }
            } catch (e: Exception) {
                HudManager.LOGGER.error("Failed to save HUD $treeId after grouping it", e)
                saved = false
            }
        }
        return saved
    }

    /** Registers [members] as a preset the HUD library adds as one group, replacing any preset with the same [id] */
    @JvmStatic
    @JvmOverloads
    public fun registerPreset(
        id: String,
        name: String,
        members: List<Class<out Hud>>,
        configId: String? = null,
        icon: String? = null,
        layout: HudPresetLayout? = null,
        actions: List<HudPresetAction> = emptyList(),
        defaultAction: String? = null,
    ): HudPreset {
        require(id.isNotEmpty() && SEPARATOR !in id && PRESET_SEPARATOR !in id) {
            "HUD preset id '$id' must be non-empty and contain neither '$SEPARATOR' nor '$PRESET_SEPARATOR'"
        }
        require(name.isNotBlank()) { "HUD preset '$id' needs a name" }
        val distinct = members.distinct()
        require(distinct.size >= 2) { "HUD preset '$id' needs at least two different HUDs" }
        require(actions.map { it.id }.distinct().size == actions.size) { "HUD preset '$id' has actions with the same id" }
        val default = defaultAction?.let { wanted ->
            requireNotNull(actions.firstOrNull { it.id == wanted }) { "HUD preset '$id' has no action '$wanted'" }
        }
        val preset = HudPreset(id, name, distinct, configId, icon, layout, actions.toList(), default)
        synchronized(presets) { presets[id] = preset }
        HudManager.notifyRegistrationChanged()
        return preset
    }

    @ApiStatus.Internal
    @JvmStatic
    public fun unregisterPreset(id: String) {
        if (synchronized(presets) { presets.remove(id) } != null) HudManager.notifyRegistrationChanged()
    }

    /** Every registered preset in the order they were registered */
    @JvmStatic
    public fun presets(): List<HudPreset> = synchronized(presets) { presets.values.toList() }

    /** Whether [preset] can be added right now */
    @JvmStatic
    public fun canAdd(preset: HudPreset): Boolean {
        val providers = providersOf(preset)
        return providers.size == preset.members.size && providers.none { !it.multipleInstancesAllowed() && HudManager.hasHudOfType(it::class.java) }
    }

    /** Adds every HUD of [preset] in one new group, or returns `null` and changes nothing when it cannot be added */
    @JvmStatic
    public fun addPreset(preset: HudPreset): HudGroup? = addPreset(preset) { null }

    @ApiStatus.Internal
    @JvmStatic
    public fun addPreset(preset: HudPreset, naturalSize: (Hud) -> Pair<Float, Float>?): HudGroup? {
        if (!canAdd(preset)) return null
        val providers = providersOf(preset)
        val built = ArrayList<Hud>(providers.size)
        val setUp = ArrayList<Hud>(providers.size)
        val instances: List<Hud>
        val sizes: List<Pair<Float, Float>>
        val positions: List<Pair<Float, Float>>
        try {
            instances = providers.map { provider ->
                val hud = if (provider.isReal) provider else provider.build().also {
                    built.add(it)
                    it.loadStoredValues()
                }
                if (hud !in HudManager.activeInstances) {
                    setUp.add(hud)
                    hud.setup()
                }
                hud
            }
            sizes = instances.mapIndexed { i, hud -> estimatedSize(hud, naturalSize(providers[i])) }
            positions = placed(preset, presetLayout(preset, instances, sizes), sizes)
        } catch (e: Throwable) {
            if (e.isFatalHudFailure()) throw e
            for (hud in setUp) {
                try {
                    hud.remove()
                } catch (t: Throwable) {
                    if (t.isFatalHudFailure()) throw t
                    HudManager.LOGGER.error("Failed to remove HUD ${hud.title} after its preset failed", t)
                }
            }
            built.forEach(Hud::detachTree)
            HudManager.LOGGER.error("Failed to add HUD preset ${preset.id}", e)
            return null
        }
        built.forEach(Hud::registerTree)
        val path = "${preset.id}$PRESET_SEPARATOR${newId()}"
        Snapshot.withMutableSnapshot {
            for ((i, hud) in instances.withIndex()) {
                if (!hud.staticWidth) {
                    hud.renderedW = sizes[i].first
                    hud.renderedH = sizes[i].second
                }
                hud.setAbsolutePosition(positions[i].first, positions[i].second)
                hud.groupPath = path
            }
        }
        for (hud in instances) {
            HudManager.markProviderKnown(hud)
            if (hud in HudManager.activeInstances) continue
            HudManager.activeInstances.add(hud)
            HudManager.instancesRevision.intValue++
            try {
                hud.captureStaticSizeDefaults()
                hud.capturePositionDefaults()
            } catch (e: Throwable) {
                if (e.isFatalHudFailure()) throw e
                HudManager.LOGGER.error("Failed to capture the defaults of HUD ${hud.title}", e)
            }
        }
        val defaultAction = preset.defaultAction
        if (defaultAction != null && instances.none { it.isAnchored }) arrangeSafely(defaultAction, alignedMembers(preset, instances))
        HudManager.invalidate()
        if (saveTrees(instances) && preset.id in defaults) HudManager.markGroupKnown(preset.id)
        return HudGroup(path)
    }

    /** The preset a group was made from, which is the one registered under its id or under the part of its id before [PRESET_SEPARATOR] */
    @JvmStatic
    public fun presetOf(group: HudGroup): HudPreset? = presetNamed(group.id.substringBefore(PRESET_SEPARATOR))

    /** The HUDs of [group] that are members of [preset], in the order the preset lists them */
    @JvmStatic
    public fun presetMembers(group: HudGroup, preset: HudPreset): List<Hud> = presetSlots(group, preset).filterNotNull()

    private fun presetSlots(group: HudGroup, preset: HudPreset): List<Hud?> =
        alignedMembers(preset, group.huds.filter { hud -> chainOf(hud).none { it in group && presetOf(it) != null } })

    /** Applies [action] to the members of [group] as a single undo step, dropping its changes to snapshot state such as positions and anchors if it fails */
    @JvmStatic
    public fun runPresetAction(group: HudGroup, action: HudPresetAction) {
        val preset = presetOf(group) ?: return
        val slots = presetSlots(group, preset)
        if (!arrangeSafely(action, slots)) return
        HudManager.invalidate()
        saveTrees(slots.filterNotNull())
    }

    private fun alignedMembers(preset: HudPreset, huds: List<Hud>): List<Hud?> {
        val taken = identitySet(emptyList())
        val exact = preset.members.map { type -> huds.firstOrNull { it.javaClass == type && taken.add(it) } }
        return preset.members.mapIndexed { i, type -> exact[i] ?: huds.firstOrNull { type.isInstance(it) && taken.add(it) } }
    }

    private fun arrangeSafely(action: HudPresetAction, huds: List<Hud?>): Boolean = try {
        Snapshot.withMutableSnapshot { action.arrangement.arrange(huds) }
        true
    } catch (e: Throwable) {
        if (e.isFatalHudFailure()) throw e
        HudManager.LOGGER.error("HUD preset action ${action.id} failed", e)
        false
    }

    @ApiStatus.Internal
    @JvmStatic
    public fun presetLayout(preset: HudPreset, huds: List<Hud>, sizes: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        val custom = preset.layout?.let {
            try {
                it.positions(huds, sizes).takeIf { positions -> positions.size == huds.size }
            } catch (e: Throwable) {
                if (e.isFatalHudFailure()) throw e
                HudManager.LOGGER.error("Layout of HUD preset ${preset.id} failed", e)
                null
            }
        }
        return custom ?: defaultLayout(huds, sizes)
    }

    @ApiStatus.Internal
    @JvmStatic
    public fun estimatedSize(hud: Hud, natural: Pair<Float, Float>?): Pair<Float, Float> {
        val scale = hud.effectiveScale
        val (minW, minH) = hud.minimumSize()
        val w = if (hud.staticWidth) {
            hud.staticW.takeIf { it > 0f }?.times(scale)
                ?: natural?.first?.takeIf { it > 0f }?.let { maxOf(it, minW) * scale }
        } else {
            hud.renderedW.takeIf { it > 0f }
                ?: natural?.first?.takeIf { it > 0f }?.times(scale)
                ?: minW.takeIf { it > 0f }?.times(scale)
                ?: hud.staticW.takeIf { it > 0f }?.times(scale)
        } ?: DEFAULT_HUD_WIDTH
        val h = if (hud.staticWidth) {
            hud.staticH.takeIf { it > 0f }?.times(scale)
                ?: natural?.second?.takeIf { it > 0f }?.let { maxOf(it, minH) * scale }
        } else {
            hud.renderedH.takeIf { it > 0f }
                ?: natural?.second?.takeIf { it > 0f }?.times(scale)
                ?: minH.takeIf { it > 0f }?.times(scale)
                ?: hud.staticH.takeIf { it > 0f }?.times(scale)
        } ?: DEFAULT_HUD_HEIGHT
        return w to h
    }

    @ApiStatus.Internal
    @JvmStatic
    public fun providersOf(preset: HudPreset): List<Hud> =
        preset.members.mapNotNull(HudManager::getProvider).filter { groupable(it) && it !is LegacyHudMarker }

    private fun defaultLayout(huds: List<Hud>, sizes: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        val defaults = huds.map {
            try {
                it.defaultPosition()
            } catch (e: Throwable) {
                if (e.isFatalHudFailure()) throw e
                HudManager.LOGGER.error("Default position of HUD ${it.title} failed", e)
                null
            }
        }
        val placed = huds.indices.filter { defaults[it] != null }
        val right = placed.maxOfOrNull { defaults[it]!!.first + sizes[it].first } ?: 0f
        val top = placed.minOfOrNull { defaults[it]!!.second } ?: 0f
        var x = right
        return huds.indices.map { i ->
            defaults[i] ?: (x to top).also { x += sizes[i].first + LAYOUT_GAP }
        }
    }

    private fun placed(preset: HudPreset, positions: List<Pair<Float, Float>>, sizes: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        val left = positions.minOf { it.first }
        val top = positions.minOf { it.second }
        val box = floatArrayOf(
            left,
            top,
            positions.indices.maxOf { positions[it].first + sizes[it].first } - left,
            positions.indices.maxOf { positions[it].second + sizes[it].second } - top,
        )
        val sw = HudManager.guiScreenWidth
        val sh = HudManager.guiScreenHeight
        val copyPrefix = "${preset.id}$PRESET_SEPARATOR"
        val earlier = HudManager.activeInstances
            .groupBy { hud ->
                val ids = hud.groupPath.split(SEPARATOR)
                val copy = ids.indexOfFirst { it.startsWith(copyPrefix) }
                if (copy < 0) null else ids.subList(0, copy + 1)
            }
            .filterKeys { it != null }
            .values.mapNotNull { bounds(it) }
        fun free(x: Float, y: Float) = earlier.none { o ->
            x < o[0] + o[2] && o[0] < x + box[2] && y < o[1] + o[3] && o[1] < y + box[3]
        }
        val stepX = box[2] + LAYOUT_GAP
        val stepY = box[3] + LAYOUT_GAP
        val centred = (sw - box[2]) / 2f to (sh - box[3]) / 2f
        val grid = generateSequence(0f) { it + stepY }.takeWhile { it + box[3] <= sh }.flatMap { gy ->
            generateSequence(0f) { it + stepX }.takeWhile { it + box[2] <= sw }.map { gx -> gx to gy }
        }
        fun onScreen(p: Pair<Float, Float>) =
            p.first.coerceIn(0f, maxOf(0f, sw - box[2])) to p.second.coerceIn(0f, maxOf(0f, sh - box[3]))
        val (x, y) = (sequenceOf(box[0] to box[1], centred) + grid).map { onScreen(it) }.firstOrNull { (cx, cy) -> free(cx, cy) }
            ?: onScreen(box[0] + earlier.size * LAYOUT_GAP to box[1] + earlier.size * LAYOUT_GAP)
        val dx = x - box[0]
        val dy = y - box[1]
        return positions.map { (it.first + dx) to (it.second + dy) }
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
        private val scales: BooleanArray
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
            scales = BooleanArray(members.size).also { flags ->
                val can = identitySet(scalable(members))
                for (i in members.indices) flags[i] = members[i] in can
            }
            startScale = FloatArray(members.size) { members[it].customScale }
            startX = FloatArray(members.size) { members[it].x }
            startY = FloatArray(members.size) { members[it].y }
            startW = FloatArray(members.size) { members[it].renderedW }
            startH = FloatArray(members.size) { members[it].renderedH }
            var lo = 0f
            var hi = Float.POSITIVE_INFINITY
            for (i in members.indices) {
                val s = startScale[i]
                if (!scales[i] || s <= 0f) continue
                val range = HUD_SCALE_RANGE
                lo = maxOf(lo, range.start / s)
                hi = minOf(hi, range.endInclusive / s)
            }
            minFactor = lo.coerceAtMost(1f)
            maxFactor = if (hi.isInfinite()) 1f else hi.coerceAtLeast(1f)
        }

        /** Scales everything to [factor] times its starting size and returns the factor actually used */
        public fun apply(factor: Float): Float {
            val f = factor.coerceIn(minFactor, maxFactor)
            for (i in members.indices) {
                if (!scales[i]) continue
                val hud = members[i]
                hud.customScale = startScale[i] * f
                resized(hud, startW[i], startH[i], f)
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

    /** Whether [hud] can be in a group, which a HUD that keeps its own state elsewhere cannot since the group could not be saved */
    @JvmStatic
    public fun groupable(hud: Hud): Boolean = hud.persistOwnState

    private fun units(huds: Collection<Hud>, groups: Collection<HudGroup>): List<Moving> {
        // a group already holding another selected group or HUD carries it along on its own
        val groupList = groups.distinct().filter { g -> groups.none { it != g && g in it } }
        val out = ArrayList<Moving>(huds.size + groupList.size)
        for (g in groupList) out.add(Moving(g.huds, g.parent?.path.orEmpty()))
        for (hud in huds.distinct()) {
            // a HUD that keeps its own state elsewhere has nowhere to save which group it is in
            if (!groupable(hud) || groupList.any { hud in it }) continue
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
                if (group.huds.all { it in copied }) renamed.getOrPut(group.path) { copyId(group.id, taken) } else group.id
            }
        }
    }

    private fun copyId(id: String, taken: MutableSet<String>): String {
        val preset = id.substringBefore(PRESET_SEPARATOR)
        val keep = PRESET_SEPARATOR in id || presetNamed(preset) != null
        return if (keep) "$preset$PRESET_SEPARATOR${newId(taken)}" else newId(taken)
    }

    private fun takenIds(): MutableSet<String> {
        val taken = HashSet<String>()
        for (hud in HudManager.activeInstances) {
            for (id in hud.groupPath.split(SEPARATOR)) {
                taken.add(id)
                taken.add(id.substringAfter(PRESET_SEPARATOR))
            }
        }
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
