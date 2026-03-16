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

@Suppress("EqualsOrHashCode", "UnstableApiUsage")
abstract class Hud(id: String, title: String, val category: Category) : Cloneable, Config(id, null, title, null) {
    private var _staticWidth: MutableState<Boolean> = mutableStateOf(false)
    var staticWidth: Boolean get() = _staticWidth.value; set(v) { _staticWidth.value = v }

    @Switch(title = "Show in F3")
    var showInF3 = true

    @Switch(title = "Show in Tab")
    var showInTab = true

    @Switch(title = "Show in GUIs")
    var showInScreens = true

    var toggleKey: Int = -1
    var showKey: Int = -1

    private var _x: MutableState<Float> = mutableStateOf(0f)
    var x: Float get() = _x.value; set(v) { _x.value = v }

    private var _y: MutableState<Float> = mutableStateOf(0f)
    var y: Float get() = _y.value; set(v) { _y.value = v }

    private var _renderedW: MutableState<Float> = mutableStateOf(0f)
    var renderedW: Float get() = _renderedW.value; set(v) { _renderedW.value = v }

    private var _renderedH: MutableState<Float> = mutableStateOf(0f)
    var renderedH: Float get() = _renderedH.value; set(v) { _renderedH.value = v }

    var hidden: Boolean = false

    private var _alignment: MutableState<PolyAlign> = mutableStateOf(PolyAlign.TopLeft)
    var alignment: PolyAlign get() = _alignment.value; set(v) { _alignment.value = v }

    private var _padTop: MutableState<Float> = mutableStateOf(0f)
    var padTop: Float get() = _padTop.value; set(v) { _padTop.value = v }

    private var _padBottom: MutableState<Float> = mutableStateOf(0f)
    var padBottom: Float get() = _padBottom.value; set(v) { _padBottom.value = v }

    private var _padLeft: MutableState<Float> = mutableStateOf(0f)
    var padLeft: Float get() = _padLeft.value; set(v) { _padLeft.value = v }

    private var _padRight: MutableState<Float> = mutableStateOf(0f)
    var padRight: Float get() = _padRight.value; set(v) { _padRight.value = v }

    private var _staticW: MutableState<Float> = mutableStateOf(120f)
    var staticW: Float get() = _staticW.value; set(v) { _staticW.value = v }

    private var _staticH: MutableState<Float> = mutableStateOf(32f)
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

    private var _textAlign: MutableState<Int> = mutableStateOf(1)
    var textAlign: Int get() = _textAlign.value; set(v) { _textAlign.value = v }

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
        val treeId = with?.id ?: "huds/${(1000..9999).random()}-$id"
        val tree = ConfigManager.collect(out, treeId)
        out.apply {
            tree.title = title
            tree.addMetadata("category", category)
            tree.addMetadata("hidden", true)
            var hidden = { Property.Display.HIDDEN }
            tree["x"] = ktProperty(out::x).apply { addDisplayCondition(hidden) }
            tree["y"] = ktProperty(out::y).apply { addDisplayCondition(hidden) }
            tree["toggleKey"] = ktProperty(out::toggleKey).apply { addDisplayCondition(hidden) }
            tree["showKey"] = ktProperty(out::showKey).apply { addDisplayCondition(hidden) }
            tree["alignment"] = ktProperty(out::alignment).apply { addDisplayCondition(hidden) }
            tree["padTop"] = ktProperty(out::padTop).apply { addDisplayCondition(hidden) }
            tree["padBottom"] = ktProperty(out::padBottom).apply { addDisplayCondition(hidden) }
            tree["padLeft"] = ktProperty(out::padLeft).apply { addDisplayCondition(hidden) }
            tree["padRight"] = ktProperty(out::padRight).apply { addDisplayCondition(hidden) }
            tree["staticWidth"] = ktProperty(out::staticWidth).apply { addDisplayCondition(hidden) }
            tree["staticW"] = ktProperty(out::staticW).apply { addDisplayCondition(hidden) }
            tree["staticH"] = ktProperty(out::staticH).apply { addDisplayCondition(hidden) }
            tree["font"] = ktProperty(out::font).apply { addDisplayCondition(hidden) }
            tree["caseType"] = ktProperty(out::caseType).apply { addDisplayCondition(hidden) }
            tree["textScale"] = ktProperty(out::textScale).apply { addDisplayCondition(hidden) }
            tree["textBold"] = ktProperty(out::textBold).apply { addDisplayCondition(hidden) }
            tree["textItalic"] = ktProperty(out::textItalic).apply { addDisplayCondition(hidden) }
            tree["textUnderline"] = ktProperty(out::textUnderline).apply { addDisplayCondition(hidden) }
            tree["textAlign"] = ktProperty(out::textAlign).apply { addDisplayCondition(hidden) }
            addToSerialized(tree)
            tree["hudClass"] = simple(value = out::class.java.name).apply {
                addDisplayCondition { Property.Display.HIDDEN }
            }
            addCallbacks(tree)
            if (with == null) LOGGER.info("generated new HUD config for $title -> ${tree.id}")
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

    protected fun updateAndRecalculate() {
        update()
    }

    open fun setup() {}
    abstract fun update(): Boolean
    open fun updateFrequency(): Long = -1L
    open fun defaultPosition(): Pair<Float, Float> = 0f to 0f
    open fun hasBackground(): Boolean = true
    open fun backgroundColor(): PolyColor? = null
    open fun multipleInstancesAllowed(): Boolean = true
    open fun minimumSize(): Pair<Float, Float> = 0f to 0f
    open fun remove() {}

    @MustBeInvokedByOverriders
    @Suppress("UNCHECKED_CAST")
    override fun clone(): Hud = (super.clone() as Hud).apply {
        _runtime = null
        showKey = -1
        toggleKey = -1
        _staticWidth = mutableStateOf(this@Hud.staticWidth)
        _x = mutableStateOf(this@Hud.x)
        _y = mutableStateOf(this@Hud.y)
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
        _textAlign = mutableStateOf(this@Hud.textAlign)
    }

    class Category(val name: String, val id: Byte) {
        companion object {
            @JvmStatic val COMBAT = Category("oneconfig.combat", 1)
            @JvmStatic val INFO = Category("oneconfig.info", 2)
            @JvmStatic val PLAYER = Category("oneconfig.player", 3)
        }

        override fun toString() = name
        override fun hashCode() = id.toInt()
    }
}
