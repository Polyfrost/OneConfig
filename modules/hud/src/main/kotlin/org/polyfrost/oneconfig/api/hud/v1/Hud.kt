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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.MustBeInvokedByOverriders
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
import org.polyfrost.oneconfig.api.hud.v1.HudManager.LOGGER

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

private const val GRID_SIZE = 3

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
                    staticW = maxOf(renderedW, minW)
                    staticH = maxOf(renderedH, minH)
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
                renderedW = maxOf(staticW, minW)
                renderedH = maxOf(staticH, minH)
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
     * Stores the current valid [staticW]/[staticH] as reset defaults (the size after first layout).
     * [TextHud]'s unmeasured {@code -1f} sentinel is never stored.
     */
    fun captureStaticSizeDefaults(force: Boolean = false) {
        val t = tree ?: return
        if (!staticWidth) return
        if (staticW > 0f) {
            t.getProp("staticW")?.let { prop ->
                if (force || prop.getMetadata<Any?>("default") == null) {
                    prop.addMetadata("default", staticW)
                }
            }
        }
        if (staticH > 0f) {
            t.getProp("staticH")?.let { prop ->
                if (force || prop.getMetadata<Any?>("default") == null) {
                    prop.addMetadata("default", staticH)
                }
            }
        }
    }

    /**
     * Stores the reset defaults for [section]/[relativeX]/[relativeY] derived from [defaultPosition].
     */
    fun capturePositionDefaults() {
        val t = tree ?: return
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
    open var renderedH: Float by _renderedH::value

    open val scaledWidth: Float get() {
        val w = if (staticWidth) staticW else renderedW
        val (minW, _) = minimumSize()
        return maxOf(w, minW).coerceAtLeast(1f)
    }

    open val scaledHeight: Float get() {
        val h = if (staticWidth) staticH else renderedH
        val (_, minH) = minimumSize()
        return maxOf(h, minH).coerceAtLeast(1f)
    }

    open var x: Float
        get() {
            val sw = HudManager.guiScreenWidth
            val secPos = Math.round(sw / GRID_SIZE * relativeX).toFloat()
            return when (section) {
                Section.TopLeft, Section.CenterLeft, Section.BottomLeft -> secPos
                Section.TopCenter, Section.Center, Section.BottomCenter -> (sw - scaledWidth) / 2f + secPos
                Section.TopRight, Section.CenterRight, Section.BottomRight -> sw - scaledWidth - secPos
            }
        }
        set(v) { updateRelativeX(v) }

    open var y: Float
        get() {
            val sh = HudManager.guiScreenHeight
            val secPos = Math.round(sh / GRID_SIZE * relativeY).toFloat()
            return when (section) {
                Section.TopLeft, Section.TopCenter, Section.TopRight -> secPos
                Section.CenterLeft, Section.Center, Section.CenterRight -> (sh - scaledHeight) / 2f + secPos
                Section.BottomLeft, Section.BottomCenter, Section.BottomRight -> sh - scaledHeight - secPos
            }
        }
        set(v) { updateRelativeY(v) }

    protected open fun updateRelativeX(absX: Float) {
        val sw = HudManager.guiScreenWidth
        val gridW = sw / GRID_SIZE
        relativeX = when (section) {
            Section.TopLeft, Section.CenterLeft, Section.BottomLeft -> absX / gridW
            Section.TopCenter, Section.Center, Section.BottomCenter -> (absX - (sw - scaledWidth) / 2f) / gridW
            else -> (sw - scaledWidth - absX) / gridW
        }.coerceIn(-1f, 2f)
    }

    protected open fun updateRelativeY(absY: Float) {
        val sh = HudManager.guiScreenHeight
        val gridH = sh / GRID_SIZE
        relativeY = when (section) {
            Section.TopLeft, Section.TopCenter, Section.TopRight -> absY / gridH
            Section.CenterLeft, Section.Center, Section.CenterRight -> (absY - (sh - scaledHeight) / 2f) / gridH
            else -> (sh - scaledHeight - absY) / gridH
        }.coerceIn(-1f, 2f)
    }

    fun setAbsolutePosition(absX: Float, absY: Float) {
        val sw = HudManager.guiScreenWidth
        val sh = HudManager.guiScreenHeight
        val gridW = sw / GRID_SIZE
        val gridH = sh / GRID_SIZE

        section = when {
            absX < gridW -> when {
                absY > 2 * gridH -> Section.BottomLeft
                absY > gridH -> Section.CenterLeft
                else -> Section.TopLeft
            }
            absX < 2 * gridW -> when {
                absY > 2 * gridH -> Section.BottomCenter
                absY > gridH -> Section.Center
                else -> Section.TopCenter
            }
            else -> when {
                absY > 2 * gridH -> Section.BottomRight
                absY > gridH -> Section.CenterRight
                else -> Section.TopRight
            }
        }

        updateRelativeX(absX)
        updateRelativeY(absY)
    }

    var hidden: Boolean = false

    private var _alignment: MutableState<PolyAlign> = mutableStateOf(PolyAlign.Center)
    var alignment: PolyAlign get() = _alignment.value; set(v) { _alignment.value = v }

    private var _padTop: MutableState<Float> = mutableStateOf(0f)
    var padTop: Float get() = _padTop.value; set(v) { _padTop.value = v }

    private var _padBottom: MutableState<Float> = mutableStateOf(0f)
    var padBottom: Float get() = _padBottom.value; set(v) { _padBottom.value = v }

    private var _padLeft: MutableState<Float> = mutableStateOf(0f)
    var padLeft: Float get() = _padLeft.value; set(v) { _padLeft.value = v }

    private var _padRight: MutableState<Float> = mutableStateOf(0f)
    var padRight: Float get() = _padRight.value; set(v) { _padRight.value = v }

    private var _staticW: MutableState<Float> = mutableStateOf(200f)
    var staticW: Float get() = _staticW.value; set(v) { _staticW.value = v }

    private var _staticH: MutableState<Float> = mutableStateOf(48f)
    var staticH: Float get() = _staticH.value; set(v) { _staticH.value = v }

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
    var customScale: Float get() = _customScale.value; set(v) { _customScale.value = v }

    val effectiveScale: Float get() = if (useGuiScale) 1f else customScale

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
            val hidden = { Property.Display.HIDDEN }
            tree["section"] = ktProperty(out::section).apply { addDisplayCondition(hidden) }
            tree["relativeX"] = ktProperty(out::relativeX).apply { addDisplayCondition(hidden) }
            tree["relativeY"] = ktProperty(out::relativeY).apply { addDisplayCondition(hidden) }
            tree["toggleKey"] = ktProperty(out::toggleKey).apply { addDisplayCondition(hidden) }
            tree["showKey"] = ktProperty(out::showKey).apply { addDisplayCondition(hidden) }
            tree["alignment"] = ktProperty(out::alignment).apply { addDisplayCondition(hidden) }
            tree["padTop"] = ktProperty(out::padTop).apply { addDisplayCondition(hidden) }
            tree["padBottom"] = ktProperty(out::padBottom).apply { addDisplayCondition(hidden) }
            tree["padLeft"] = ktProperty(out::padLeft).apply { addDisplayCondition(hidden) }
            tree["padRight"] = ktProperty(out::padRight).apply { addDisplayCondition(hidden) }
            tree["staticWidth"] = ktProperty(out::staticWidth).apply {
                description = "Keeps this HUD at a fixed width and height. This also enables content alignment inside the HUD box."
                addDisplayCondition(hidden)
            }
            tree["staticW"] = ktProperty(out::staticW).apply { addDisplayCondition(hidden) }
            tree["staticH"] = ktProperty(out::staticH).apply { addDisplayCondition(hidden) }
            tree["font"] = ktProperty(out::font).apply { addDisplayCondition(hidden) }
            tree["caseType"] = ktProperty(out::caseType).apply { addDisplayCondition(hidden) }
            tree["textScale"] = ktProperty(out::textScale).apply { addDisplayCondition(hidden) }
            tree["textBold"] = ktProperty(out::textBold).apply { addDisplayCondition(hidden) }
            tree["textItalic"] = ktProperty(out::textItalic).apply { addDisplayCondition(hidden) }
            tree["textUnderline"] = ktProperty(out::textUnderline).apply { addDisplayCondition(hidden) }
            tree["textWeight"] = ktProperty(out::textWeight).apply { addDisplayCondition(hidden) }
            tree["textAlign"] = ktProperty(out::textAlign).apply { addDisplayCondition(hidden) }
            tree["useGuiScale"] = ktProperty(out::useGuiScale).apply {
                description = "Uses Minecraft's current GUI scale for this HUD. Turn this off to use a custom HUD scale."
                addDisplayCondition(hidden)
            }
            tree["customScale"] = ktProperty(out::customScale).apply { addDisplayCondition(hidden) }
            tree["showBackground"] = ktProperty(out::showBackground).apply { addDisplayCondition(hidden) }
            tree["bgColor"] = ktProperty(out::bgColor).apply { addDisplayCondition(hidden) }
            tree["bgChroma"] = ktProperty(out::bgChroma).apply { addDisplayCondition(hidden) }
            tree["bgChromaSpeed"] = ktProperty(out::bgChromaSpeed).apply { addDisplayCondition(hidden) }
            tree["bgRadius"] = ktProperty(out::bgRadius).apply { addDisplayCondition(hidden) }
            tree["textColor"] = ktProperty(out::textColor).apply { addDisplayCondition(hidden) }
            tree["textChroma"] = ktProperty(out::textChroma).apply { addDisplayCondition(hidden) }
            tree["textChromaSpeed"] = ktProperty(out::textChromaSpeed).apply { addDisplayCondition(hidden) }
            tree["showShadow"] = ktProperty(out::showShadow).apply { addDisplayCondition(hidden) }
            tree["shadowColor"] = ktProperty(out::shadowColor).apply { addDisplayCondition(hidden) }
            tree["shadowChroma"] = ktProperty(out::shadowChroma).apply { addDisplayCondition(hidden) }
            tree["shadowChromaSpeed"] = ktProperty(out::shadowChromaSpeed).apply { addDisplayCondition(hidden) }
            tree["shadowOffsetX"] = ktProperty(out::shadowOffsetX).apply { addDisplayCondition(hidden) }
            tree["shadowOffsetY"] = ktProperty(out::shadowOffsetY).apply { addDisplayCondition(hidden) }
            (tree["showInF3"] as? Property<*>)?.addDisplayCondition(hidden)
            (tree["showInTab"] as? Property<*>)?.addDisplayCondition(hidden)
            (tree["showInScreens"] as? Property<*>)?.addDisplayCondition(hidden)
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
    open fun hasBackground(): Boolean = true
    open fun backgroundColor(): PolyColor? = null
    open fun multipleInstancesAllowed(): Boolean = true
    open fun minimumSize(): Pair<Float, Float> = 0f to 0f

    /**
     * Whether this HUD may be deleted from the HUD design studio. When `false`, the editor hides
     * the trash button so the HUD cannot be removed (it can still be hidden/moved). Useful for HUDs
     * that are intrinsic to a mod and cannot be re-created once deleted.
     */
    open fun deletable(): Boolean = isReal

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
        _textColor = mutableStateOf(this@Hud.textColor)
        _textChroma = mutableStateOf(this@Hud.textChroma)
        _textChromaSpeed = mutableStateOf(this@Hud.textChromaSpeed)
        _showShadow = mutableStateOf(this@Hud.showShadow)
        _shadowColor = mutableStateOf(this@Hud.shadowColor)
        _shadowChroma = mutableStateOf(this@Hud.shadowChroma)
        _shadowChromaSpeed = mutableStateOf(this@Hud.shadowChromaSpeed)
        _shadowOffsetX = mutableStateOf(this@Hud.shadowOffsetX)
        _shadowOffsetY = mutableStateOf(this@Hud.shadowOffsetY)
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
