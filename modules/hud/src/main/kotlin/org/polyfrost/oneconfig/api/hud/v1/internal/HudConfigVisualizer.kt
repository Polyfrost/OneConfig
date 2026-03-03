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

package org.polyfrost.oneconfig.api.hud.v1.internal

import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Properties.ktProperty
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.getProp
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import org.polyfrost.oneconfig.api.event.v1.events.HudEvent
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.ui.v1.keybind.OCKeybindHelper
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.extensions.setFont
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Group
import org.polyfrost.polyui.component.impl.Image
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.fastEachIndexed
import java.util.function.Consumer
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.io.path.exists
import kotlin.random.Random

object HudConfigVisualizer : ConfigVisualizer() {

    override fun createHeaders(categories: Map<String, Drawable>) = null

    override fun flattenSubcategories(options: Map<String, Map<String, ArrayList<Drawable>>>): Map<String, Drawable> {
        // one category with one subcategory, so skip the headers
        return if (options.values.size == 1 && options.values.first().size == 1) {
            mapOf(
                options.keys.first() to Group(
                    *options.values.first().values.first().toTypedArray(),
                    alignment = alignCStartNoPad,
                )
            )
        } else super.flattenSubcategories(options)
    }

    override fun wrap(drawable: Drawable, title: String?, desc: String?, icon: PolyImage?): Drawable {
        return Group(
            if (icon != null) Image(icon) else null,
            Group(
                Text(title ?: "Null", fontSize = 16f).setFont { medium },
                if (desc != null) Text(desc, visibleSize = Vec2(240f, 12f)) else null,
                alignment = stdOpt,
            ),
            drawable,
            alignment = stdAlign,
            size = Vec2(480f, 48f),
        )
    }

    fun makeIDForHudTree(hud: Hud<*>): String {
        val folder = ConfigManager.active().folder
        val init = "huds/${hud.id}"
        if (!folder.resolve(init).exists()) return init
        var p = "huds/${Random.Default.nextInt(0, 100)}-${hud.id}"
        var i = 0
        while (folder.resolve(p).exists()) {
            p = "huds/${Random.Default.nextInt(0, 999)}-${hud.id}"
            when (i++) {
                100 -> println("they all say that it gets better")
                500 -> println("yeah they all say that it gets better;; it gets better the more you grow")
                999 -> throw IllegalStateException("... but what if i dont?")
            }
        }
        return p
    }

    fun addHudConfigEntries(cmp: Component, tree: Tree) {
        if (cmp is Drawable) {
            tree["color"] = ktProperty(cmp::_color)
        }
        when (cmp) {
            is Block -> {
                tree["boarderColor"] = ktProperty(cmp::borderColor)
                tree["boarderWidth"] = ktProperty(cmp::borderWidth)
            }

            is Text -> {
                tree["font"] = ktProperty(cmp::_font)
                tree["fontSize"] = ktProperty(cmp::uFontSize)
            }
        }
        cmp.children?.fastEachIndexed { i, it ->
            val child = Tree.tree("$i")
            addHudConfigEntries(it, child)
            tree.put(child)
        }
    }

    @Suppress("UnstableApiUsage")
    fun addDefaultCallbacks(hud: Hud<*>, tree: Tree) {
        tree.getProp<Boolean>("staticWidth")?.addCallback { value ->
            hud.getBackground()?.let {
                if (value) it.layoutFlags = it.layoutFlags or 0b00000010
                else it.layoutFlags = it.layoutFlags and 0b11111101.toByte()
            }
            false
        }

        val hideHandler = Consumer<HudEvent> { (opened): HudEvent ->
            hud.hidden = opened
        }
        val hideF3Handler = EventHandler.of(HudEvent.Debug::class.java, hideHandler)
        val hideTabHandler = EventHandler.of(HudEvent.Tab::class.java, hideHandler)
        val hideScreenHandler = EventHandler.of(ScreenOpenEvent::class.java) { (screen): ScreenOpenEvent ->
            hud.hidden = screen != null
            // asm: don't hide when we open the hud editor
            // because otherwise you couldn't edit it (which sucks)
            if (HudManager.isEditing) hud.hidden = false
        }
        tree.getProp<Boolean>("showInF3")?.addCallback {
            if (!it) hideF3Handler.register()
            else hideF3Handler?.unregister()
            false
        }
        tree.getProp<Boolean>("showInTab")?.addCallback {
            if (!it) hideTabHandler.register()
            else hideTabHandler?.unregister()
            false
        }
        tree.getProp<Boolean>("showInScreens")?.addCallback {
            if (!it) hideScreenHandler.register()
            else hideScreenHandler?.unregister()
            false
        }

        hud.showKey = OCKeybindHelper.builder().does { hud.hidden = !it }.register()
        hud.toggleKey = OCKeybindHelper.builder().does { if (it) hud.hidden = !hud.hidden }.register()
    }
}
