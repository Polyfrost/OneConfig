/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     PolyUI is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation,
 * AND the simple request that you adequately accredit us if you use PolyUI.
 * See details here <https://github.com/Polyfrost/polyui-jvm/ACCREDITATION.md>.
 *     This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.polyfrost.oneconfig.internal.config.impl

import org.polyfrost.oneconfig.api.config.Property
import org.polyfrost.oneconfig.api.config.Tree
import org.polyfrost.oneconfig.api.config.annotations.Accordion
import org.polyfrost.oneconfig.api.config.elements.AccordionOption
import org.polyfrost.oneconfig.api.config.elements.Option
import org.polyfrost.oneconfig.ui.Page
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.impl.Button
import org.polyfrost.polyui.component.impl.Dropdown
import org.polyfrost.polyui.component.impl.Image
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.event.MouseClicked
import org.polyfrost.polyui.input.Translator.Companion.localised
import org.polyfrost.polyui.layout.Layout
import org.polyfrost.polyui.layout.impl.FlexLayout
import org.polyfrost.polyui.layout.impl.PixelLayout
import org.polyfrost.polyui.layout.impl.SwitchingLayout
import org.polyfrost.polyui.property.impl.ImageProperties
import org.polyfrost.polyui.renderer.data.FontFamily
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.fastEachIndexed
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import kotlin.math.PI

object ConfigVisualizer {
    private const val WIDTH = 1126f
    private const val OPT_OPT_GAP = 8f
    private const val HEADER_HEIGHT = 40f
    private const val HEIGHT = 600f
    private val cache = HashMap<Class<*>, MethodHandle>(10)

    @JvmStatic
    fun create(owner: Layout, config: Tree, initialPage: String = "General"): Layout {
        val now = System.nanoTime()
        owner.polyUI.translator.dontWarn = true
        val map = HashMap<String, Layout>()
        createInternal(config, map, owner)
        val s = SwitchingLayout(
            at = 0.px * HEADER_HEIGHT.px,
            size = WIDTH.px * HEIGHT.px,
        )
        val fl = FlexLayout(
            at = origin,
            gap = Gap(5.px, 12.px),
            wrapDirection = FlexLayout.Wrap.NoWrap,
            drawables = createHeaders(map, s),
        )
        val out = object :
            PixelLayout(
                size = WIDTH.px * (HEIGHT + HEADER_HEIGHT).px,
                drawables = drawables(fl, s),
            ),
            Page {
            override var simpleName = "OptionPage@${config.id}"

            override fun filter(query: String, search: Any.(String) -> Boolean) {
                children[0].components.fastEach {
                    if (it !is Button) return@fastEach
                    it.exists = it.text!!.string.run { search(query) }
                }
                children[0].calculateBounds()
                children[1].components.fastEach self@{
                    if (it is Text) {
                        it.exists = query.isEmpty()
                    }
                    if (it is Option) {
                        it.exists = it.title.string.run { search(query) } ||
                                (it.description != null && it.description.string.run { search(query) })
                        if (!it.exists) {
                            if (it.children.size > 4) {
                                it.children.fastEach(3) { opt ->
                                    opt as AccordionOption
                                    it.exists = opt.title.string.run { search(query) } ||
                                            (opt.description != null && opt.description.string.run { search(query) })
                                    if (it.exists) return@self
                                    it.exists = false
                                    if (check(opt.option, query, search)) {
                                        it.exists = true
                                        return@self
                                    }
                                }
                            } else {
                                if (check(it.option, query, search)) {
                                    it.exists = true
                                }
                            }
                        }
                    }
                }
                children[1].calculateBounds()
            }

            override fun onColorsChanged(colors: Colors) {
                super.onColorsChanged(colors)
                map.forEach { (_, v) ->
                    v.onColorsChanged(colors)
                }
            }

            override fun onFontsChanged(fonts: FontFamily) {
                super.onFontsChanged(fonts)
                map.forEach { (_, v) ->
                    v.onFontsChanged(fonts)
                }
            }
        }
        owner.add(out)
        // assert width is correct
        for (layout in map.values) {
            layout.width = WIDTH
        }
        s.switch(map[initialPage] ?: throw IllegalArgumentException("Tried to switch to non-existent page $initialPage"))
        owner.polyUI.translator.dontWarn = false
        println("Creating config page took ${(System.nanoTime() - now) / 1_000_000f}ms")
        return out
    }

    private fun check(cmp: Component, q: String, f: Any.(String) -> Boolean): Boolean {
        if (cmp is Dropdown) {
            cmp.dropdown.components.fastEach {
                if (it !is Dropdown.Entry) return@fastEach
                println(it.text.string)
                if (it.text.string.run { f(q) }) return true
            }
        }
        return false
    }

    private fun put(map: HashMap<String, Layout>, category: String, subcategory: String, icon: PolyImage?, title: String, desc: String?, component: Component, index: Int = -1): Option {
        val opt = Option(
            icon = icon,
            title = title.localised(),
            desc = desc?.localised(),
            option = component,
        )
        val sub = map[category] ?: FlexLayout(
            at = origin,
            resizesChildren = true,
            drawables = arrayOf(
                Text(
                    // 22x0
                    at = flex(endRowAfter = true),
                    fontSize = 22.px,
                    initialText = subcategory.localised(),
                    acceptInput = false,
                ),
            ),
        ).scrolling(WIDTH.px * HEIGHT.px)
        val i = addSubcategoryTitle(sub, subcategory)
        sub.add(opt, if (index != -1) index + i else -1)
        map[category] = sub
        return opt
    }

    fun addSubcategoryTitle(layout: Layout, subcategory: String): Int {
        layout.components.fastEachIndexed { i, it ->
            if (it is Text && it.initialText.string == subcategory) return i
        }
        layout.add(
            Text(
                // 22x0
                at = flex(endRowAfter = true),
                fontSize = 22.px,
                initialText = subcategory.localised(),
                acceptInput = false,
            ),
        )
        return layout.components.size - 1
    }

    fun visualize(prop: Property<*>): Component {
        val visualizer = prop.getMetadata<Class<*>>("visualizer") ?: throw IllegalArgumentException("Property ${prop.name} is missing required metadata 'visualizer'")
        val m = cache[visualizer] ?: run {
            val m = MethodHandles.lookup().unreflect(visualizer.declaredMethods[0]).bindTo(visualizer.getDeclaredConstructor().newInstance())
            cache[visualizer] = m
            m
        }
        return m.invoke(prop) as Component
    }

    private fun createInternal(config: Tree, map: HashMap<String, Layout>, owner: Layout) {
        for (prop in config.values) {
            val cmp = visualize(prop)
            val title = prop.getMetadata<String>("title") ?: throw IllegalArgumentException("Property ${prop.name} is missing required metadata 'title'")
            val desc = prop.getMetadata<String>("description")
            val iconPath = prop.getMetadata<String>("icon") ?: ""
            val category = prop.getMetadata<String>("category") ?: "General"
            val subcategory = prop.getMetadata<String>("subcategory") ?: "General"
            val icon = if (iconPath.isEmpty()) null else PolyImage(iconPath, 32f, 32f)
            put(map, category, subcategory, icon, title, desc, cmp)
        }
        for (c in config.children) {
            val a = c.getMetadata<Accordion>("annotation")
            if (a != null) {
                var open = true
                var oldSize: Vec2<Unit>? = null
                val title = a.title.ifEmpty { c.id }
                val icon = if (a.icon.isEmpty()) null else PolyImage(a.icon, 32f, 32f)
                val option = put(
                    map,
                    a.category,
                    a.subcategory,
                    icon,
                    title,
                    a.description.ifEmpty { null },
                    Image(
                        properties = ImageProperties(true),
                        image = PolyImage("chevron-down.svg", 16f, 16f),
                        at = origin,
                    ),
                    a.index,
                )
                option.addEventHandler(MouseClicked(0)) self@{
                    var b = false
                    // allow clicking anywhere on top bar
                    if (polyUI.mouseX !in trueX..(trueX + width) || polyUI.mouseY !in trueY..(trueY + 64f)) return@self false
                    if (oldSize == null) oldSize = option.size!!.clone()
                    option.option.rotateTo(if (open) 0.0 else 180.0, Animations.EaseOutExpo, 0.4.seconds)
                    option.resize(if (!open) oldSize!! else oldSize!!.a * 64.px, Animations.EaseOutExpo, 0.4.seconds) {
                        b = true
                    }
                    polyUI.addHook {
                        layout.calculateBounds()
                        this@self.x = x
                        this@self.y = y
                        b
                    }
                    open = !open
                    true
                }
                option.simpleName = "AccordionHeader" + option.simpleName
                if (open) option.option.rotation = PI
                createAccordion(c, option)
            } else {
                createInternal(c, map, owner)
            }
        }
    }

    // todo wierd kotlin bug????? it can't resolve it for some reason
    @Suppress("UNCHECKED_CAST")
    fun <S : Drawable, E : Event> S.addEventHandler(event: E, function: S.(E) -> Boolean) {
        this.addEventHandler(event, function as Drawable.(Event) -> Boolean)
    }

    private fun createAccordion(config: Tree, o: Option) {
        require(config.children.size == 0) { "Configs cannot have accordions inside accordions!" }
        var s = false
        var yy = 66f
        config.values.fastEach {
            val cmp = visualize(it)
            val optTitle = it.getMetadata<String>("title") ?: throw IllegalArgumentException("Property ${it.name} is missing required metadata 'title'")
            val opt = AccordionOption(
                at = (if (s) 534f else 22f).px * yy.px,
                title = optTitle.localised(),
                desc = it.getMetadata<String>("description")?.localised(),
                option = cmp,
            )
            if (s) yy += opt.height + OPT_OPT_GAP
            s = !s
            o.addComponents(opt)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createHeaders(map: HashMap<String, Layout>, s: SwitchingLayout): Array<Button> {
        val a = arrayOfNulls<Button>(map.size)
        var i = map.size - 1
        for (e in map.entries) {
            a[i] = Button(
                at = flex(),
                text = e.key.localised(),
                events = {
                    MouseClicked(0) to {
                        s.switch(e.value)
                    }
                },
            )
            i--
        }
        return a as Array<Button>
    }
}
