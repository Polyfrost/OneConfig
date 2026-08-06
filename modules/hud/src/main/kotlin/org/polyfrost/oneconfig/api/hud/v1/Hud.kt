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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.background
import org.polyfrost.compose.layout.PolyAlign
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.runtime.PolyComposeRuntime
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Properties.ktProperty
import org.polyfrost.oneconfig.api.config.v1.Properties.simple
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.config.v1.backend.Backend
import org.polyfrost.oneconfig.api.hud.v1.HudManager.LOGGER
import org.polyfrost.oneconfig.api.platform.v1.Platform

enum class Font {
    Minecraft,
    Poppins;
}

enum class Weight {
    Thin,
    Regular,
    Medium,
    Bold,
    Black;
}

enum class Section {
    TopLeft, TopCenter, TopRight,
    CenterLeft, Center, CenterRight,
    BottomLeft, BottomCenter, BottomRight
}

enum class HudAnchor {
    Auto,
    TopLeft, Top, TopRight,
    Left, Center, Right,
    BottomLeft, Bottom, BottomRight;

    fun toAlign(): PolyAlign? = when (this) {
        Auto -> null
        TopLeft -> PolyAlign.TopLeft
        Top -> PolyAlign.Top
        TopRight -> PolyAlign.TopRight
        Left -> PolyAlign.Left
        Center -> PolyAlign.Center
        Right -> PolyAlign.Right
        BottomLeft -> PolyAlign.BottomLeft
        Bottom -> PolyAlign.Bottom
        BottomRight -> PolyAlign.BottomRight
    }

    companion object {
        @JvmStatic
        fun of(align: PolyAlign): HudAnchor = when (align) {
            PolyAlign.TopLeft -> TopLeft
            PolyAlign.Top -> Top
            PolyAlign.TopRight -> TopRight
            PolyAlign.Left -> Left
            PolyAlign.Center -> Center
            PolyAlign.Right -> Right
            PolyAlign.BottomLeft -> BottomLeft
            PolyAlign.Bottom -> Bottom
            PolyAlign.BottomRight -> BottomRight
        }
    }
}

private const val GRID_SIZE = 3

/** Backstop for walking a chain of HUDs anchored to one another. */
private const val MAX_ANCHOR_DEPTH = 16

@Suppress("EqualsOrHashCode", "UnstableApiUsage")
abstract class Hud(id: String, title: String, val category: Category) : Cloneable, Config(id, null, title, null) {
    private var _staticWidth: MutableState<Boolean> = mutableStateOf(false)
    var staticWidth: Boolean
        get() = _staticWidth.value
        set(v) {
            val wasStatic = _staticWidth.value
            _staticWidth.value = v
            if (!wasStatic && v) {
                val (minW, minH) = minimumSize()
                if (renderedW > 0f || renderedH > 0f) {
                    val scale = effectiveScale
                    staticW = maxOf(renderedW / scale, minW)
                    staticH = maxOf(renderedH / scale, minH)
                } else {
                    val natural = measureNaturalContentSize()
                    if (natural != null) {
                        staticW = maxOf(natural.first, minW)
                        staticH = maxOf(natural.second, minH)
                    }
                }
            } else if (wasStatic && !v) {
                // Switching from static → dynamic: seed rendered dimensions from the
                // static values so the HUD doesn't collapse to 0×0 until the render loop
                // recalculates the natural content size.
                val (minW, minH) = minimumSize()
                val scale = effectiveScale
                renderedW = maxOf(staticW, minW) * scale
                renderedH = maxOf(staticH, minH) * scale
            }
        }

    protected fun measureNaturalContentSize(): Pair<Float, Float>? {
        val rt = _runtime ?: return null
        val wasStatic = _staticWidth.value
        return try {
            _staticWidth.value = false
            rt.frame(2000f, 2000f)
            val w = rt.root.width
            val h = rt.root.height
            if (w > 0f && h > 0f) w to h else null
        } catch (_: Throwable) {
            null
        } finally {
            _staticWidth.value = wasStatic
        }
    }

    /**
     * Fills in [staticW]/[staticH] when static sizing is enabled but dimensions are unset or invalid
     * (e.g. [TextHud]'s unmeasured sentinel of {@code -1f}). Safe to call after a reset-to-default.
     */
    fun reseedStaticSizeIfNeeded() {
        if (!staticWidth) return
        if (staticW > 0f && staticH > 0f) return
        reseedStaticWidth()
        reseedStaticHeight()
    }

    /** Re-measures content and applies a new static width. */
    fun reseedStaticWidth() {
        if (!staticWidth) return
        val (minW, _) = minimumSize()
        val natural = measureNaturalContentSize()
        staticW = if (natural != null && natural.first > 0f) {
            maxOf(natural.first, minW)
        } else {
            maxOf(padLeft + padRight + 80f, minW)
        }
    }

    /** Re-measures content and applies a new static height. */
    fun reseedStaticHeight() {
        if (!staticWidth) return
        val (_, minH) = minimumSize()
        val natural = measureNaturalContentSize()
        staticH = if (natural != null && natural.second > 0f) {
            maxOf(natural.second, minH)
        } else {
            maxOf(padTop + padBottom + 16f, minH)
        }
    }

    /**
     * Stores reset defaults for [staticW]/[staticH]. The default is the *natural* content size, never
     * the current one: this runs after stored values have been loaded, so recording [staticW] directly
     * would capture whatever size the user last resized to and make "reset to default" a no-op.
     * When the content cannot be measured yet no default is stored, and resetting falls back to
     * re-measuring at that point (see `reseedStaticWidth`).
     */
    fun captureStaticSizeDefaults(force: Boolean = false) {
        val t = tree ?: return
        if (!staticWidth) return
        val wProp = t.getProp("staticW")
        val hProp = t.getProp("staticH")
        val needW = wProp != null && (force || wProp.getMetadata<Any?>("default") == null)
        val needH = hProp != null && (force || hProp.getMetadata<Any?>("default") == null)
        if (!needW && !needH) return
        val (naturalW, naturalH) = measureNaturalContentSize() ?: return
        val (minW, minH) = minimumSize()
        if (needW && naturalW > 0f) wProp!!.addMetadata("default", maxOf(naturalW, minW))
        if (needH && naturalH > 0f) hProp!!.addMetadata("default", maxOf(naturalH, minH))
    }

    /**
     * Stores the reset defaults for [section]/[relativeX]/[relativeY] derived from [defaultPosition].
     */
    fun capturePositionDefaults() {
        val t = tree ?: return
        if (!persistOwnState) return
        val (dx, dy) = defaultPosition()
        val curSection = section
        val curX = relativeX
        val curY = relativeY
        setAbsolutePosition(dx, dy)
        t.getProp("section")?.addMetadata("default", section)
        t.getProp("relativeX")?.addMetadata("default", relativeX)
        t.getProp("relativeY")?.addMetadata("default", relativeY)
        section = curSection
        relativeX = curX
        relativeY = curY
    }

    @Switch(title = "Show in F3")
    var showInF3 = true

    @Switch(title = "Show in Tab")
    var showInTab = true

    @Switch(title = "Show in GUIs")
    var showInScreens = true

    @Switch(title = "Show in Chat")
    var showInChat = true

    var toggleKey: Int = -1
    var showKey: Int = -1

    private var _section: MutableState<Section> = mutableStateOf(Section.TopLeft)
    var section: Section get() = _section.value; set(v) { _section.value = v }

    private var _relativeX: MutableState<Float> = mutableStateOf(0f)
    open var relativeX: Float get() = _relativeX.value; set(v) { _relativeX.value = v }

    private var _relativeY: MutableState<Float> = mutableStateOf(0f)
    open var relativeY: Float get() = _relativeY.value; set(v) { _relativeY.value = v }

    private var _renderedW: MutableState<Float> = mutableStateOf(0f)
    open var renderedW: Float get() = _renderedW.value; set(v) { _renderedW.value = v }

    private var _renderedH: MutableState<Float> = mutableStateOf(0f)
    open var renderedH: Float get() = _renderedH.value; set(v) { _renderedH.value = v }

    private var minSizeFrame = Long.MIN_VALUE
    private var minSize: Pair<Float, Float> = 0f to 0f

    @ApiStatus.Internal
    fun frameMinimumSize(): Pair<Float, Float> {
        val frame = HudManager.frameId
        if (minSizeFrame != frame) {
            minSizeFrame = frame
            minSize = minimumSize()
        }
        return minSize
    }

    open val scaledWidth: Float get() {
        val scale = effectiveScale
        val w = if (staticWidth || renderedW <= 0f) staticW * scale else renderedW
        val (minW, _) = frameMinimumSize()
        return maxOf(w, minW * scale).coerceAtLeast(1f)
    }

    open val scaledHeight: Float get() {
        val scale = effectiveScale
        val h = if (staticWidth || renderedH <= 0f) staticH * scale else renderedH
        val (_, minH) = frameMinimumSize()
        return maxOf(h, minH * scale).coerceAtLeast(1f)
    }

    private fun fracX(anchor: HudAnchor): Float = when (anchor) {
        HudAnchor.Auto -> when (section) {
            Section.TopLeft, Section.CenterLeft, Section.BottomLeft -> 0f
            Section.TopCenter, Section.Center, Section.BottomCenter -> 0.5f
            Section.TopRight, Section.CenterRight, Section.BottomRight -> 1f
        }
        HudAnchor.TopLeft, HudAnchor.Left, HudAnchor.BottomLeft -> 0f
        HudAnchor.Top, HudAnchor.Center, HudAnchor.Bottom -> 0.5f
        HudAnchor.TopRight, HudAnchor.Right, HudAnchor.BottomRight -> 1f
    }

    private fun fracY(anchor: HudAnchor): Float = when (anchor) {
        HudAnchor.Auto -> when (section) {
            Section.TopLeft, Section.TopCenter, Section.TopRight -> 0f
            Section.CenterLeft, Section.Center, Section.CenterRight -> 0.5f
            Section.BottomLeft, Section.BottomCenter, Section.BottomRight -> 1f
        }
        HudAnchor.TopLeft, HudAnchor.Top, HudAnchor.TopRight -> 0f
        HudAnchor.Left, HudAnchor.Center, HudAnchor.Right -> 0.5f
        HudAnchor.BottomLeft, HudAnchor.Bottom, HudAnchor.BottomRight -> 1f
    }

    private val anchorFracX: Float get() = fracX(growthAnchor)

    private val anchorFracY: Float get() = fracY(growthAnchor)

    /** Screen-space X of [point] on this HUD's box, e.g. the middle of its right edge. */
    fun anchorPointX(point: HudAnchor): Float = x + fracX(point) * scaledWidth

    /** Screen-space Y of [point] on this HUD's box. */
    fun anchorPointY(point: HudAnchor): Float = y + fracY(point) * scaledHeight

    private val screenX: Float get() {
        val sw = HudManager.guiScreenWidth
        val secPos = Math.round(sw / GRID_SIZE * relativeX).toFloat()
        return when (section) {
            Section.TopLeft, Section.CenterLeft, Section.BottomLeft -> secPos
            Section.TopCenter, Section.Center, Section.BottomCenter -> sw / 2f + secPos
            Section.TopRight, Section.CenterRight, Section.BottomRight -> sw - secPos
        }
    }

    private val screenY: Float get() {
        val sh = HudManager.guiScreenHeight
        val secPos = Math.round(sh / GRID_SIZE * relativeY).toFloat()
        return when (section) {
            Section.TopLeft, Section.TopCenter, Section.TopRight -> secPos
            Section.CenterLeft, Section.Center, Section.CenterRight -> sh / 2f + secPos
            Section.BottomLeft, Section.BottomCenter, Section.BottomRight -> sh - secPos
        }
    }

    open var x: Float
        get() {
            resolvedAnchorX?.let { a ->
                resolvingAnchor = true
                try {
                    return a.parent.anchorPointX(a.targetPoint) + a.offset - fracX(a.selfPoint) * scaledWidth
                } finally {
                    resolvingAnchor = false
                }
            }
            return screenX - anchorFracX * scaledWidth
        }
        set(v) { updateRelativeX(v) }

    open var y: Float
        get() {
            resolvedAnchorY?.let { a ->
                resolvingAnchor = true
                try {
                    return a.parent.anchorPointY(a.targetPoint) + a.offset - fracY(a.selfPoint) * scaledHeight
                } finally {
                    resolvingAnchor = false
                }
            }
            return screenY - anchorFracY * scaledHeight
        }
        set(v) { updateRelativeY(v) }

    protected open fun updateRelativeX(absX: Float) {
        val sw = HudManager.guiScreenWidth
        val gridW = sw / GRID_SIZE
        resolvedAnchorX?.let { a ->
            val pin = absX + fracX(a.selfPoint) * scaledWidth
            resolvingAnchor = true
            try {
                val offset = pin - a.parent.anchorPointX(a.targetPoint)
                val link = mergeLinkX
                if (a.fromMerge && link != null) mergeLinkX = link.withOffset(offset)
                else anchorOffsetX = offset
            } finally {
                resolvingAnchor = false
            }
        }
        // kept up to date even while anchored, so dropping the anchor (or losing the target HUD)
        // leaves the HUD exactly where it was instead of jumping back to a stale screen position
        val anchor = absX + anchorFracX * scaledWidth
        relativeX = when (section) {
            Section.TopLeft, Section.CenterLeft, Section.BottomLeft -> anchor
            Section.TopCenter, Section.Center, Section.BottomCenter -> anchor - sw / 2f
            else -> sw - anchor
        }.div(gridW).coerceIn(-1f, 2f)
    }

    protected open fun updateRelativeY(absY: Float) {
        val sh = HudManager.guiScreenHeight
        val gridH = sh / GRID_SIZE
        resolvedAnchorY?.let { a ->
            val pin = absY + fracY(a.selfPoint) * scaledHeight
            resolvingAnchor = true
            try {
                val offset = pin - a.parent.anchorPointY(a.targetPoint)
                val link = mergeLinkY
                if (a.fromMerge && link != null) mergeLinkY = link.withOffset(offset)
                else anchorOffsetY = offset
            } finally {
                resolvingAnchor = false
            }
        }
        val anchor = absY + anchorFracY * scaledHeight
        relativeY = when (section) {
            Section.TopLeft, Section.TopCenter, Section.TopRight -> anchor
            Section.CenterLeft, Section.Center, Section.CenterRight -> anchor - sh / 2f
            else -> sh - anchor
        }.div(gridH).coerceIn(-1f, 2f)
    }

    fun setAbsolutePosition(absX: Float, absY: Float) {
        val sw = HudManager.guiScreenWidth
        val sh = HudManager.guiScreenHeight
        val gridW = sw / GRID_SIZE
        val gridH = sh / GRID_SIZE

        val centerX = absX + scaledWidth / 2f
        val centerY = absY + scaledHeight / 2f
        section = when {
            centerX < gridW -> when {
                centerY > 2 * gridH -> Section.BottomLeft
                centerY > gridH -> Section.CenterLeft
                else -> Section.TopLeft
            }
            centerX < 2 * gridW -> when {
                centerY > 2 * gridH -> Section.BottomCenter
                centerY > gridH -> Section.Center
                else -> Section.TopCenter
            }
            else -> when {
                centerY > 2 * gridH -> Section.BottomRight
                centerY > gridH -> Section.CenterRight
                else -> Section.TopRight
            }
        }

        updateRelativeX(absX)
        updateRelativeY(absY)
    }

    open fun onEditorDragStart() {}

    open fun onEditorDragEnd() {}

    private var _hidden: MutableState<Boolean> = mutableStateOf(false)
    open var hidden: Boolean get() = _hidden.value; set(v) { _hidden.value = v }

    private var _locked: MutableState<Boolean> = mutableStateOf(false)
    open var locked: Boolean get() = _locked.value; set(v) { _locked.value = v }

    internal open val persistOwnState: Boolean get() = true

    open val resizeAxes: HudResize get() = HudResize.Both

    @ApiStatus.Internal
    open fun applyEditorWidth(width: Float) {
        staticWidth = true
        staticW = width
    }

    private var _alignment: MutableState<PolyAlign> = mutableStateOf(PolyAlign.Center)
    var alignment: PolyAlign get() = _alignment.value; set(v) { _alignment.value = v }

    private var _growthAnchor: MutableState<HudAnchor> = mutableStateOf(HudAnchor.Auto)
    var growthAnchor: HudAnchor get() = _growthAnchor.value; set(v) { _growthAnchor.value = v }

    val effectiveGrowthAnchor: HudAnchor get() {
        val fx = anchorFracX
        val fy = anchorFracY
        return when {
            fy == 0f -> when (fx) { 0f -> HudAnchor.TopLeft; 0.5f -> HudAnchor.Top; else -> HudAnchor.TopRight }
            fy == 0.5f -> when (fx) { 0f -> HudAnchor.Left; 0.5f -> HudAnchor.Center; else -> HudAnchor.Right }
            else -> when (fx) { 0f -> HudAnchor.BottomLeft; 0.5f -> HudAnchor.Bottom; else -> HudAnchor.BottomRight }
        }
    }

    fun setGrowthAnchorKeepingPosition(anchor: HudAnchor) {
        val absX = x
        val absY = y
        growthAnchor = anchor
        updateRelativeX(absX)
        updateRelativeY(absY)
    }

    private var _anchorTargetId: MutableState<String> = mutableStateOf("")

    /**
     * Config tree id of the HUD this one hangs off, or empty when it is positioned against the
     * screen. Prefer [anchorTo] and [clearAnchor] over setting this directly.
     */
    var anchorTargetId: String get() = _anchorTargetId.value; set(v) { _anchorTargetId.value = v }

    private var _anchorPoint: MutableState<HudAnchor> = mutableStateOf(HudAnchor.TopLeft)

    /** Which of the nine points on the target HUD's box this HUD is pinned to. */
    var anchorPoint: HudAnchor get() = _anchorPoint.value; set(v) { _anchorPoint.value = v }

    private var _selfAnchorPoint: MutableState<HudAnchor> = mutableStateOf(HudAnchor.Auto)

    /**
     * Which of the nine points on *this* HUD's own box sits on the anchor, i.e. the point that is
     * held against [anchorPoint] on the target. [HudAnchor.Auto] uses the growth anchor, which is
     * what anchoring did before the point could be chosen.
     */
    var selfAnchorPoint: HudAnchor get() = _selfAnchorPoint.value; set(v) { _selfAnchorPoint.value = v }

    /** [selfAnchorPoint] with [HudAnchor.Auto] resolved, and the merge link taking priority. */
    val effectiveSelfAnchorPoint: HudAnchor get() = when {
        isAnchored -> selfAnchorPoint.takeUnless { it == HudAnchor.Auto } ?: effectiveGrowthAnchor
        else -> (mergeLinkX ?: mergeLinkY)?.selfPoint
            ?: selfAnchorPoint.takeUnless { it == HudAnchor.Auto } ?: effectiveGrowthAnchor
    }

    /** Moves this HUD's own anchor point to [point] without moving the HUD on screen. */
    fun setSelfAnchorPointKeepingPosition(point: HudAnchor) {
        val absX = x
        val absY = y
        selfAnchorPoint = point
        updateRelativeX(absX)
        updateRelativeY(absY)
    }

    private var _anchorOffsetX: MutableState<Float> = mutableStateOf(0f)

    /** Distance from the target's anchor point to this HUD's own growth anchor, along X. */
    var anchorOffsetX: Float get() = _anchorOffsetX.value; set(v) { _anchorOffsetX.value = v }

    private var _anchorOffsetY: MutableState<Float> = mutableStateOf(0f)

    /** Distance from the target's anchor point to this HUD's own growth anchor, along Y. */
    var anchorOffsetY: Float get() = _anchorOffsetY.value; set(v) { _anchorOffsetY.value = v }

    // set while this HUD is asking its target where it is, so a chain that loops back here falls
    // through to the screen-relative position instead of recursing forever
    @Transient
    private var resolvingAnchor = false

    /** The HUD this one is anchored to, or `null` when it is not anchored or the target is gone. */
    val anchorParent: Hud? get() {
        if (resolvingAnchor) return null
        val id = anchorTargetId
        if (id.isEmpty()) return null
        return HudManager.instanceById(id)?.takeUnless { it === this }
    }

    val isAnchored: Boolean get() = anchorParent != null

    /**
     * A HUD held against a neighbour on one axis because their backgrounds are fused. Rebuilt from
     * the merge groups every time the layout changes and never persisted: unmerging the HUDs drops
     * it again.
     */
    internal class MergeLink(
        val parent: Hud,
        val selfPoint: HudAnchor,
        val targetPoint: HudAnchor,
        val offset: Float,
    ) {
        fun withOffset(offset: Float) = MergeLink(parent, selfPoint, targetPoint, offset)
    }

    @Transient
    private var _mergeLinkX: MutableState<MergeLink?> = mutableStateOf(null)

    internal var mergeLinkX: MergeLink?
        get() = _mergeLinkX.value
        private set(v) { _mergeLinkX.value = v }

    @Transient
    private var _mergeLinkY: MutableState<MergeLink?> = mutableStateOf(null)

    internal var mergeLinkY: MergeLink?
        get() = _mergeLinkY.value
        private set(v) { _mergeLinkY.value = v }

    private fun mergeLink(axis: MergeAxis) = if (axis == MergeAxis.X) mergeLinkX else mergeLinkY

    private val resolvedAnchorX: ResolvedAnchor? get() = resolvedAnchor(MergeAxis.X)

    private val resolvedAnchorY: ResolvedAnchor? get() = resolvedAnchor(MergeAxis.Y)

    private fun resolvedAnchor(axis: MergeAxis): ResolvedAnchor? {
        if (resolvingAnchor) return null
        anchorParent?.let { parent ->
            val self = selfAnchorPoint.takeUnless { it == HudAnchor.Auto } ?: effectiveGrowthAnchor
            val offset = if (axis == MergeAxis.X) anchorOffsetX else anchorOffsetY
            return ResolvedAnchor(parent, anchorPoint, self, offset, false)
        }
        val link = mergeLink(axis) ?: return null
        if (link.parent === this) return null
        return ResolvedAnchor(link.parent, link.targetPoint, link.selfPoint, link.offset, true)
    }

    private class ResolvedAnchor(
        val parent: Hud,
        val targetPoint: HudAnchor,
        val selfPoint: HudAnchor,
        val offset: Float,
        val fromMerge: Boolean,
    )

    /** The HUD this one is held against right now, whether by its own anchor or by a merge. */
    val effectiveAnchorParent: Hud? get() = anchorParent ?: (mergeLinkX ?: mergeLinkY)?.parent

    /** The point on [effectiveAnchorParent] this HUD hangs off. */
    val effectiveAnchorPoint: HudAnchor get() =
        if (isAnchored) anchorPoint else (mergeLinkX ?: mergeLinkY)?.targetPoint ?: anchorPoint

    /** `true` while this HUD is held against a neighbour because their backgrounds are fused. */
    val isMergeAnchored: Boolean get() = mergeLinkX != null || mergeLinkY != null

    /**
     * Holds this HUD against [parent] on [axis], with [selfPoint] on its own box sitting on
     * [targetPoint] of the parent's, for as long as the two stay merged. Keeps the HUD exactly where
     * it is now.
     */
    internal fun applyMergeLink(parent: Hud, selfPoint: HudAnchor, targetPoint: HudAnchor, axis: MergeAxis) {
        if (parent === this) return
        // an anchor the user set by hand outranks a merge, and a merge must never loop back into one
        if (isAnchored || parent.anchorChainContains(this)) return
        val existing = mergeLink(axis)
        if (existing != null && existing.parent === parent &&
            existing.selfPoint == selfPoint && existing.targetPoint == targetPoint
        ) return
        if (axis == MergeAxis.X) {
            val offset = x + fracX(selfPoint) * scaledWidth - parent.anchorPointX(targetPoint)
            mergeLinkX = MergeLink(parent, selfPoint, targetPoint, offset)
        } else {
            val offset = y + fracY(selfPoint) * scaledHeight - parent.anchorPointY(targetPoint)
            mergeLinkY = MergeLink(parent, selfPoint, targetPoint, offset)
        }
    }

    internal fun clearMergeLink() = clearMergeLinks(clearX = true, clearY = true)

    /** Drops the merge link on the given axes, leaving the HUD where it currently sits on screen. */
    internal fun clearMergeLinks(clearX: Boolean, clearY: Boolean) {
        val dropX = clearX && mergeLinkX != null
        val dropY = clearY && mergeLinkY != null
        if (!dropX && !dropY) return
        val absX = x
        val absY = y
        if (dropX) mergeLinkX = null
        if (dropY) mergeLinkY = null
        setAbsolutePosition(absX, absY)
    }

    /**
     * Pins [self] on this HUD to [point] on [target] without moving it: from here on it follows the
     * target when the target moves or resizes, instead of following the screen. Does nothing if that
     * would make a loop of anchors.
     */
    @JvmOverloads
    fun anchorTo(target: Hud, point: HudAnchor, self: HudAnchor = HudAnchor.Auto) {
        if (target === this) return
        val id = target.tree?.id ?: return
        if (target.anchorChainContains(this)) return
        val absX = x
        val absY = y
        // the HUD is the user's to place now: a merge no longer gets to move it around
        mergeLinkX = null
        mergeLinkY = null
        anchorTargetId = id
        anchorPoint = point
        selfAnchorPoint = self
        setAbsolutePosition(absX, absY)
    }

    /** Drops the anchor, leaving the HUD where it currently sits on screen. */
    fun clearAnchor() {
        if (anchorTargetId.isEmpty()) return
        val absX = x
        val absY = y
        anchorTargetId = ""
        anchorOffsetX = 0f
        anchorOffsetY = 0f
        selfAnchorPoint = HudAnchor.Auto
        setAbsolutePosition(absX, absY)
    }

    /** Whether [hud] is this HUD's anchor target, or the target of one of its targets. */
    fun anchorChainContains(hud: Hud): Boolean {
        var current = anchorParent
        var depth = 0
        while (current != null && depth < MAX_ANCHOR_DEPTH) {
            if (current === hud) return true
            current = current.anchorParent
            depth++
        }
        return false
    }

    private var _padTop: MutableState<Float> = mutableStateOf(0f)
    var padTop: Float get() = _padTop.value; set(v) { _padTop.value = v }

    private var _padBottom: MutableState<Float> = mutableStateOf(0f)
    var padBottom: Float get() = _padBottom.value; set(v) { _padBottom.value = v }

    private var _padLeft: MutableState<Float> = mutableStateOf(0f)
    var padLeft: Float get() = _padLeft.value; set(v) { _padLeft.value = v }

    private var _padRight: MutableState<Float> = mutableStateOf(0f)
    var padRight: Float get() = _padRight.value; set(v) { _padRight.value = v }

    private var _staticW: MutableState<Float> = mutableStateOf(200f)
    open var staticW: Float get() = _staticW.value; set(v) { _staticW.value = v }

    private var _staticH: MutableState<Float> = mutableStateOf(48f)
    open var staticH: Float get() = _staticH.value; set(v) { _staticH.value = v }

    private var _font: MutableState<Font> = mutableStateOf(Font.Minecraft)
    var font: Font get() = _font.value; set(v) { _font.value = v }

    private var _caseType: MutableState<Int> = mutableStateOf(0)
    var caseType: Int get() = _caseType.value; set(v) { _caseType.value = v }

    private var _textScale: MutableState<Float> = mutableStateOf(1f)
    var textScale: Float get() = _textScale.value; set(v) { _textScale.value = v }

    private var _textBold: MutableState<Boolean> = mutableStateOf(false)
    var textBold: Boolean get() = _textBold.value; set(v) { _textBold.value = v }

    private var _textItalic: MutableState<Boolean> = mutableStateOf(false)
    var textItalic: Boolean get() = _textItalic.value; set(v) { _textItalic.value = v }

    private var _textUnderline: MutableState<Boolean> = mutableStateOf(false)
    var textUnderline: Boolean get() = _textUnderline.value; set(v) { _textUnderline.value = v }

    private var _textWeight: MutableState<Weight> = mutableStateOf(Weight.Regular)
    var textWeight: Weight get() = _textWeight.value; set(v) { _textWeight.value = v }

    private var _textAlign: MutableState<Int> = mutableStateOf(1)
    var textAlign: Int get() = _textAlign.value; set(v) { _textAlign.value = v }

    private var _useGuiScale: MutableState<Boolean> = mutableStateOf(true)
    var useGuiScale: Boolean get() = _useGuiScale.value; set(v) { _useGuiScale.value = v }

    private var _customScale: MutableState<Float> = mutableStateOf(1f)
    open var customScale: Float get() = _customScale.value; set(v) { _customScale.value = v }

    val effectiveScale: Float
        get() {
            if (useGuiScale) return customScale
            val gui = Platform.compatibility().options().guiScale
            return if (gui > 0f) customScale / gui else customScale
        }

    fun getPoppinsFontName(): String {
        val base = when {
            textBold || textWeight == Weight.Bold -> "poppins-bold"
            textWeight == Weight.Thin -> "poppins-thin"
            textWeight == Weight.Medium -> "poppins-medium"
            textWeight == Weight.Black -> "poppins-black"
            else -> "poppins"
        }
        return if (textItalic) "$base-italic" else base
    }

    private var _showBackground: MutableState<Boolean> = mutableStateOf(true)
    var showBackground: Boolean get() = _showBackground.value; set(v) { _showBackground.value = v }

    private var _bgColor: MutableState<Int> = mutableStateOf(0x80000000.toInt())
    var bgColor: Int get() = _bgColor.value; set(v) { _bgColor.value = v }

    private var _bgChroma: MutableState<Boolean> = mutableStateOf(false)
    var bgChroma: Boolean get() = _bgChroma.value; set(v) { _bgChroma.value = v }

    private var _bgChromaSpeed: MutableState<Float> = mutableStateOf(1f)
    var bgChromaSpeed: Float get() = _bgChromaSpeed.value; set(v) { _bgChromaSpeed.value = v }

    private var _bgRadius: MutableState<Float> = mutableStateOf(4f)
    var bgRadius: Float get() = _bgRadius.value; set(v) { _bgRadius.value = v }

    private var _mergeBackground: MutableState<Boolean> = mutableStateOf(true)

    var mergeBackground: Boolean get() = _mergeBackground.value; set(v) { _mergeBackground.value = v }

    private var _mergeDiagonally: MutableState<Boolean> = mutableStateOf(false)

    var mergeDiagonally: Boolean get() = _mergeDiagonally.value; set(v) { _mergeDiagonally.value = v }

    /**
     * `true` while this HUD's background is being drawn as part of a fused neighbour shape by
     * [HudManager]. A HUD which draws its own background **must** skip it while this is set, or the
     * background ends up drawn twice, once by the fused shape and once by the HUD itself.
     *
     * Prefer [hudBackground], which already accounts for this.
     */
    var bgMerged: Boolean
        get() = _bgMerged.value
        internal set(v) { _bgMerged.value = v }

    @Transient
    private var _bgMerged: MutableState<Boolean> = mutableStateOf(false)

    private var _textColor: MutableState<Int> = mutableStateOf(0xFFFFFFFF.toInt())
    var textColor: Int get() = _textColor.value; set(v) { _textColor.value = v }

    private var _textChroma: MutableState<Boolean> = mutableStateOf(false)
    var textChroma: Boolean get() = _textChroma.value; set(v) { _textChroma.value = v }

    private var _textChromaSpeed: MutableState<Float> = mutableStateOf(1f)
    var textChromaSpeed: Float get() = _textChromaSpeed.value; set(v) { _textChromaSpeed.value = v }

    private var _showShadow: MutableState<Boolean> = mutableStateOf(false)
    var showShadow: Boolean get() = _showShadow.value; set(v) { _showShadow.value = v }

    private var _shadowColor: MutableState<Int> = mutableStateOf(0x40000000)
    var shadowColor: Int get() = _shadowColor.value; set(v) { _shadowColor.value = v }

    private var _shadowChroma: MutableState<Boolean> = mutableStateOf(false)
    var shadowChroma: Boolean get() = _shadowChroma.value; set(v) { _shadowChroma.value = v }

    private var _shadowChromaSpeed: MutableState<Float> = mutableStateOf(1f)
    var shadowChromaSpeed: Float get() = _shadowChromaSpeed.value; set(v) { _shadowChromaSpeed.value = v }

    private var _shadowOffsetX: MutableState<Float> = mutableStateOf(2f)
    var shadowOffsetX: Float get() = _shadowOffsetX.value; set(v) { _shadowOffsetX.value = v }

    private var _shadowOffsetY: MutableState<Float> = mutableStateOf(2f)
    var shadowOffsetY: Float get() = _shadowOffsetY.value; set(v) { _shadowOffsetY.value = v }

    open val alwaysRedraw: Boolean
        get() = bgChroma || textChroma || shadowChroma

    @ApiStatus.Internal
    @JvmField
    var lastLayoutFrame: Long = -1L

    override fun addToInitQueue() {}

    val isReal get() = tree != null

    @Transient
    var configId: String? = null

    @Transient
    internal var _runtime: PolyComposeRuntime? = null

    /** Returns the runtime only if it has already been created; null otherwise. */
    val runtimeOrNull: PolyComposeRuntime? get() = _runtime

    val runtime: PolyComposeRuntime
        get() = _runtime ?: PolyComposeRuntime().also {
            _runtime = it
            it.setContent { Content() }
        }

    @Composable
    abstract fun Content()

    @ApiStatus.Internal
    fun make(with: Tree? = null): Hud {
        if (tree != null) throw IllegalArgumentException("HUD is already made, it cannot be made again")
        val out = if (multipleInstancesAllowed()) clone() else this
        val treeId = with?.id ?: if (multipleInstancesAllowed()) "huds/${(1000..9999).random()}-$id" else "huds/$id"
        val tree = ConfigManager.collect(out, treeId)
        out.apply {
            tree.title = title
            tree.addMetadata("category", category)
            tree.addMetadata("hidden", true)
            if (out.profileLocalTree) tree.addMetadata(ConfigManager.PROFILE_LOCAL_METADATA, true)
            if (!out.persistOwnState) tree.addMetadata(Backend.UI_ONLY_METADATA, true)
            val hideFromConfigUi = { Property.Display.HIDDEN }
            if (out.persistOwnState) {
                tree["hidden"] = ktProperty(out::hidden).apply { addDisplayCondition(hideFromConfigUi) }
                tree["locked"] = ktProperty(out::locked).apply { addDisplayCondition(hideFromConfigUi) }
                tree["section"] = ktProperty(out::section).apply { addDisplayCondition(hideFromConfigUi) }
                tree["relativeX"] = ktProperty(out::relativeX).apply { addDisplayCondition(hideFromConfigUi) }
                tree["relativeY"] = ktProperty(out::relativeY).apply { addDisplayCondition(hideFromConfigUi) }
                tree["anchorTargetId"] = ktProperty(out::anchorTargetId).apply { addDisplayCondition(hideFromConfigUi) }
                tree["anchorPoint"] = ktProperty(out::anchorPoint).apply { addDisplayCondition(hideFromConfigUi) }
                tree["selfAnchorPoint"] = ktProperty(out::selfAnchorPoint).apply { addDisplayCondition(hideFromConfigUi) }
                tree["anchorOffsetX"] = ktProperty(out::anchorOffsetX).apply { addDisplayCondition(hideFromConfigUi) }
                tree["anchorOffsetY"] = ktProperty(out::anchorOffsetY).apply { addDisplayCondition(hideFromConfigUi) }
            }
            tree["toggleKey"] = ktProperty(out::toggleKey).apply { addDisplayCondition(hideFromConfigUi) }
            tree["showKey"] = ktProperty(out::showKey).apply { addDisplayCondition(hideFromConfigUi) }
            tree["alignment"] = ktProperty(out::alignment).apply { addDisplayCondition(hideFromConfigUi) }
            tree["growthAnchor"] = ktProperty(out::growthAnchor).apply {
                description = "The point this HUD stays pinned to when its size changes, so it only ever grows away from that point."
                addDisplayCondition(hideFromConfigUi)
            }
            tree["padTop"] = ktProperty(out::padTop).apply { addDisplayCondition(hideFromConfigUi) }
            tree["padBottom"] = ktProperty(out::padBottom).apply { addDisplayCondition(hideFromConfigUi) }
            tree["padLeft"] = ktProperty(out::padLeft).apply { addDisplayCondition(hideFromConfigUi) }
            tree["padRight"] = ktProperty(out::padRight).apply { addDisplayCondition(hideFromConfigUi) }
            tree["staticWidth"] = ktProperty(out::staticWidth).apply {
                description = "Keeps this HUD at a fixed width and height. This also enables content alignment inside the HUD box."
                addDisplayCondition(hideFromConfigUi)
            }
            if (out.persistOwnState) {
                tree["staticW"] = ktProperty(out::staticW).apply { addDisplayCondition(hideFromConfigUi) }
                tree["staticH"] = ktProperty(out::staticH).apply { addDisplayCondition(hideFromConfigUi) }
            }
            tree["font"] = ktProperty(out::font).apply { addDisplayCondition(hideFromConfigUi) }
            tree["caseType"] = ktProperty(out::caseType).apply { addDisplayCondition(hideFromConfigUi) }
            tree["textScale"] = ktProperty(out::textScale).apply { addDisplayCondition(hideFromConfigUi) }
            tree["textBold"] = ktProperty(out::textBold).apply { addDisplayCondition(hideFromConfigUi) }
            tree["textItalic"] = ktProperty(out::textItalic).apply { addDisplayCondition(hideFromConfigUi) }
            tree["textUnderline"] = ktProperty(out::textUnderline).apply { addDisplayCondition(hideFromConfigUi) }
            tree["textWeight"] = ktProperty(out::textWeight).apply { addDisplayCondition(hideFromConfigUi) }
            tree["textAlign"] = ktProperty(out::textAlign).apply { addDisplayCondition(hideFromConfigUi) }
            tree["useGuiScale"] = ktProperty(out::useGuiScale).apply {
                description = "Uses Minecraft's current GUI scale for this HUD. Turn this off to use a custom HUD scale."
                addDisplayCondition(hideFromConfigUi)
            }
            if (out.persistOwnState) {
                tree["customScale"] = ktProperty(out::customScale).apply { addDisplayCondition(hideFromConfigUi) }
            }
            tree["showBackground"] = ktProperty(out::showBackground).apply { addDisplayCondition(hideFromConfigUi) }
            tree["bgColor"] = ktProperty(out::bgColor).apply { addDisplayCondition(hideFromConfigUi) }
            tree["bgChroma"] = ktProperty(out::bgChroma).apply { addDisplayCondition(hideFromConfigUi) }
            tree["bgChromaSpeed"] = ktProperty(out::bgChromaSpeed).apply { addDisplayCondition(hideFromConfigUi) }
            tree["bgRadius"] = ktProperty(out::bgRadius).apply { addDisplayCondition(hideFromConfigUi) }
            // legacy HUDs draw their own background through the compat bridge, so there is nothing
            // for OneConfig to fuse and no reason to persist the settings for them; the same goes
            // for HUDs which opted out of merging
            if (out !is LegacyHudMarker && out.canMergeBackground()) {
                tree["mergeBackground"] = ktProperty(out::mergeBackground).apply {
                    description = "Fuses this HUD's background with the background of HUDs it touches, so they draw as one shape."
                    addDisplayCondition(hideFromConfigUi)
                }
                tree["mergeDiagonally"] = ktProperty(out::mergeDiagonally).apply {
                    description = "Also fuses backgrounds with HUDs sitting diagonally from this one, which meet it at a corner or only partly along an edge."
                    addDisplayCondition(hideFromConfigUi)
                }
            }
            tree["textColor"] = ktProperty(out::textColor).apply { addDisplayCondition(hideFromConfigUi) }
            tree["textChroma"] = ktProperty(out::textChroma).apply { addDisplayCondition(hideFromConfigUi) }
            tree["textChromaSpeed"] = ktProperty(out::textChromaSpeed).apply { addDisplayCondition(hideFromConfigUi) }
            tree["showShadow"] = ktProperty(out::showShadow).apply { addDisplayCondition(hideFromConfigUi) }
            tree["shadowColor"] = ktProperty(out::shadowColor).apply { addDisplayCondition(hideFromConfigUi) }
            tree["shadowChroma"] = ktProperty(out::shadowChroma).apply { addDisplayCondition(hideFromConfigUi) }
            tree["shadowChromaSpeed"] = ktProperty(out::shadowChromaSpeed).apply { addDisplayCondition(hideFromConfigUi) }
            tree["shadowOffsetX"] = ktProperty(out::shadowOffsetX).apply { addDisplayCondition(hideFromConfigUi) }
            tree["shadowOffsetY"] = ktProperty(out::shadowOffsetY).apply { addDisplayCondition(hideFromConfigUi) }
            (tree["showInF3"] as? Property<*>)?.addDisplayCondition(hideFromConfigUi)
            (tree["showInTab"] as? Property<*>)?.addDisplayCondition(hideFromConfigUi)
            (tree["showInScreens"] as? Property<*>)?.addDisplayCondition(hideFromConfigUi)
            (tree["showInChat"] as? Property<*>)?.addDisplayCondition(hideFromConfigUi)
            addToSerialized(tree)
            tree["hudClass"] = simple(value = out::class.java.name).apply {
                addDisplayCondition { Property.Display.HIDDEN }
            }
            addCallbacks(tree)
            if (with == null) LOGGER.info("generated new HUD config for $title -> ${tree.id}")
            Config.captureDefaults(tree)
            sanitizeHudCapturedDefaults(tree)
            ConfigManager.active().register(tree)
            this.tree = tree
        }
        return out
    }

    protected open fun addCallbacks(tree: Tree) {}
    protected open fun addToSerialized(tree: Tree) {}

    protected fun updateWhenChanged(optionName: String) {
        if (isReal) addCallback(optionName, this::updateAndRecalculate)
        else LOGGER.warn("attempted to add callback to {}'s option '{}', but it is not real.", title, optionName)
    }

    fun updateAndRecalculate() {
        update()
    }

    open fun setup() {}
    abstract fun update(): Boolean
    open fun updateFrequency(): Long = -1L
    open fun defaultPosition(): Pair<Float, Float> = 10f to 10f

    open fun showByDefault(): Boolean = false
    open fun hasBackground(): Boolean = true

    /**
     * Whether this HUD's background may be fused with the backgrounds of the HUDs it touches.
     *
     * Fusing only works when the HUD leaves its own background to OneConfig while [bgMerged] is set:
     * a HUD which unconditionally draws its own background would have it drawn twice, once by itself
     * and once by the fused shape, which reads as a doubled (darker) background. So this defaults to
     * `true` only for [TextHud], which already does that; anything drawing its own content should
     * override it to `true` *and* apply its background through [hudBackground] (or check [bgMerged]
     * by hand), or leave it `false` to opt out of merging entirely.
     *
     * When `false` the merge settings are hidden from the HUD settings panel and not persisted.
     */
    open fun canMergeBackground(): Boolean = this is TextHud

    /**
     * Applies this HUD's background to [modifier], unless the background is turned off or is
     * currently being drawn as part of a fused neighbour shape (see [bgMerged]).
     */
    @JvmOverloads
    fun hudBackground(modifier: PolyModifier = PolyModifier): PolyModifier =
        if (showBackground && hasBackground() && !bgMerged) {
            modifier.background(PolyColor(bgColor, bgChroma, bgChromaSpeed), bgRadius)
        } else {
            modifier
        }

    open fun backgroundColor(): PolyColor? = null
    open fun multipleInstancesAllowed(): Boolean = true
    open fun minimumSize(): Pair<Float, Float> = 0f to 0f

    /**
     * Whether the user is ever allowed to delete this HUD. When `false`, the settings panel and the
     * canvas context menu hide the delete button so the HUD cannot be removed (it can still be
     * hidden/moved). Useful for HUDs that are intrinsic to a mod and cannot be re-created once
     * deleted.

     * Use [canDelete] to ask whether a *particular* HUD can be deleted right now.
     */
    open fun deletable(): Boolean = true

    /**
     * Whether this exact HUD can be deleted right now: it must be a real instance (providers have
     * nothing to delete) of a type the user is allowed to delete ([deletable]).
     */
    fun canDelete(): Boolean = isReal && deletable()

    internal open val profileLocalTree: Boolean get() = true

    /**
     * Drops this instance's config tree reference, turning it back into a provider. Called when the
     * instance is removed, so that a single-instance HUD (whose provider *is* its instance) can be
     * made again instead of throwing "HUD is already made" forever.
     */
    @ApiStatus.Internal
    internal fun detachTree() {
        tree = null
    }

    open fun remove() {}

    @MustBeInvokedByOverriders
    @Suppress("UNCHECKED_CAST")
    override fun clone(): Hud = (super.clone() as Hud).apply {
        _runtime = null
        showKey = -1
        toggleKey = -1
        _staticWidth = mutableStateOf(this@Hud.staticWidth)
        _section = mutableStateOf(this@Hud.section)
        _relativeX = mutableStateOf(this@Hud.relativeX)
        _relativeY = mutableStateOf(this@Hud.relativeY)
        _renderedW = mutableStateOf(0f)
        _renderedH = mutableStateOf(0f)
        _alignment = mutableStateOf(this@Hud.alignment)
        _growthAnchor = mutableStateOf(this@Hud.growthAnchor)
        _anchorTargetId = mutableStateOf(this@Hud.anchorTargetId)
        _anchorPoint = mutableStateOf(this@Hud.anchorPoint)
        _selfAnchorPoint = mutableStateOf(this@Hud.selfAnchorPoint)
        _mergeLinkX = mutableStateOf(null)
        _mergeLinkY = mutableStateOf(null)
        _anchorOffsetX = mutableStateOf(this@Hud.anchorOffsetX)
        _anchorOffsetY = mutableStateOf(this@Hud.anchorOffsetY)
        _padTop = mutableStateOf(this@Hud.padTop)
        _padBottom = mutableStateOf(this@Hud.padBottom)
        _padLeft = mutableStateOf(this@Hud.padLeft)
        _padRight = mutableStateOf(this@Hud.padRight)
        _staticW = mutableStateOf(this@Hud.staticW)
        _staticH = mutableStateOf(this@Hud.staticH)
        _font = mutableStateOf(this@Hud.font)
        _caseType = mutableStateOf(this@Hud.caseType)
        _textScale = mutableStateOf(this@Hud.textScale)
        _textBold = mutableStateOf(this@Hud.textBold)
        _textItalic = mutableStateOf(this@Hud.textItalic)
        _textUnderline = mutableStateOf(this@Hud.textUnderline)
        _textWeight = mutableStateOf(this@Hud.textWeight)
        _textAlign = mutableStateOf(this@Hud.textAlign)
        _useGuiScale = mutableStateOf(this@Hud.useGuiScale)
        _customScale = mutableStateOf(this@Hud.customScale)
        _showBackground = mutableStateOf(this@Hud.showBackground)
        _bgColor = mutableStateOf(this@Hud.bgColor)
        _bgChroma = mutableStateOf(this@Hud.bgChroma)
        _bgChromaSpeed = mutableStateOf(this@Hud.bgChromaSpeed)
        _bgRadius = mutableStateOf(this@Hud.bgRadius)
        _mergeBackground = mutableStateOf(this@Hud.mergeBackground)
        _mergeDiagonally = mutableStateOf(this@Hud.mergeDiagonally)
        _bgMerged = mutableStateOf(false)
        _textColor = mutableStateOf(this@Hud.textColor)
        _textChroma = mutableStateOf(this@Hud.textChroma)
        _textChromaSpeed = mutableStateOf(this@Hud.textChromaSpeed)
        _showShadow = mutableStateOf(this@Hud.showShadow)
        _shadowColor = mutableStateOf(this@Hud.shadowColor)
        _shadowChroma = mutableStateOf(this@Hud.shadowChroma)
        _shadowChromaSpeed = mutableStateOf(this@Hud.shadowChromaSpeed)
        _shadowOffsetX = mutableStateOf(this@Hud.shadowOffsetX)
        _shadowOffsetY = mutableStateOf(this@Hud.shadowOffsetY)
        _hidden = mutableStateOf(this@Hud.hidden)
        _locked = mutableStateOf(this@Hud.locked)
    }

    private companion object {
        private fun sanitizeHudCapturedDefaults(tree: Tree) {
            for (id in listOf("staticW", "staticH")) {
                val prop = tree.getProp(id) ?: continue
                val def = prop.getMetadata<Any?>("default") ?: continue
                val f = (def as? Number)?.toFloat() ?: continue
                if (f <= 0f) prop.removeMetadata("default")
            }
        }
    }

    class Category(val name: String, val id: Byte) {
        companion object {
            @JvmStatic val COMBAT = Category("oneconfig.combat", 1)
            @JvmStatic val INFO = Category("oneconfig.info", 2)
            @JvmStatic val PLAYER = Category("oneconfig.player", 3)
            @JvmStatic val COMPAT = Category("oneconfig.compat", 4)
        }

        override fun toString() = name
        override fun hashCode() = id.toInt()
    }
}
