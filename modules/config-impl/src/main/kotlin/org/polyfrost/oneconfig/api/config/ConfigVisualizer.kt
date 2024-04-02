/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.api.config

import org.polyfrost.oneconfig.api.config.visualize.Visualizer
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.*
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.operations.DrawableOp
import org.polyfrost.polyui.operations.Resize
import org.polyfrost.polyui.operations.Rotate
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.LinkedList
import org.polyfrost.polyui.utils.image
import org.polyfrost.polyui.utils.rgba
import kotlin.math.PI

object ConfigVisualizer {
    private val visCache = HashMap<Class<*>, Visualizer>()
    private val configCache = HashMap<Tree, Drawable>()
    private val verticalAlign = Align(cross = Align.Cross.Start, mode = Align.Mode.Vertical)
    private val optBg = rgba(39, 49, 55, 0.50f)

    @JvmStatic
    fun get(config: Tree) = configCache.getOrPut(config) { create(config) }

    private fun create(
        config: Tree,
        initialPage: String = "General",
    ): Drawable {
        val now = System.nanoTime()
        val options = LinkedHashMap<String, HashMap<String, LinkedList<Drawable>>>(4)

        // asm: step 1: sort the tree into a map of:
        // categories
        //   -> subcategories
        //      -> list of options
        for ((_, node) in config.map) {
            val title = node.getMetadata<String>("title")?.ifEmpty { null } ?: run {
                Tree.LOGGER.warn("Property ${node.id} is missing required metadata 'title' (provided by ${config.id}); using ID")
                node.id
            }
            val desc = node.getMetadata<String>("description")?.ifEmpty { null }
            val icon =
                when (val it = node.getMetadata<Any?>("icon")) {
                    null -> null
                    is PolyImage -> it
                    is String -> it.image()
                    else -> throw IllegalArgumentException(
                        "Property ${node.id} has invalid icon type ${it::class.java.name} (provided by ${config.id}) - must be a PolyImage or String path",
                    )
                }
            val category = node.getMetadata<String>("category")?.ifEmpty { null } ?: "General"
            val subcategory = node.getMetadata<String>("subcategory")?.ifEmpty { null } ?: "General"

            val list = options.getOrPut(category) { HashMap(4) }.getOrPut(subcategory) { LinkedList() }
            if (node is Property<*>) {
                list.add(make(node).wrap(title, desc, icon))
            } else {
                list.add(makeAccordion(node as Tree, title, desc, icon))
            }
        }

        // asm: step 2: build the actual structure
        val categories =
            options.mapValues { (_, subcategories) ->
                Group(
                    alignment = verticalAlign,
                    children =
                    subcategories.map { (header, options) ->
                        Group(
                            alignment = verticalAlign,
                            children =
                            arrayOf(
                                Text(header, fontSize = 22f),
                                *options.toTypedArray(),
                            ),
                        )
                    }.toTypedArray(),
                )
            }

        PolyUI.LOGGER.info("creating config page ${config.id} took ${(System.nanoTime() - now) / 1_000_000f}ms")
        return Group(
            alignment = Align(cross = Align.Cross.Start),
            visibleSize = Vec2(1130f, 635f),
            children =
            arrayOf(
                createHeaders(categories),
                categories[initialPage] ?: throw IllegalArgumentException("Initial page $initialPage does not exist"),
            ),
        )
    }

    private fun createHeaders(categories: Map<String, Drawable>): Drawable {
        return Group(
            children =
            categories.map { (category, options) ->
                Button(text = category).events {
                    Event.Mouse.Clicked(0) then {
                        parent!![0] = options
                    }
                }
            }.toTypedArray(),
        )
    }

    private fun makeAccordion(
        tree: Tree,
        title: String,
        desc: String?,
        icon: PolyImage?,
    ): Drawable {
        val options =
            tree.map.map { (_, node) ->
                node as? Property<*> ?: throw IllegalArgumentException("Sub-tree ${tree.id} contains sub-tree node ${node.id} - only properties are allowed in sub-trees")
                val optTitle =
                    node.getMetadata<String>("title")?.ifEmpty { null } ?: run {
                        Tree.LOGGER.warn("Property ${node.id} is missing required metadata 'title' (child of sub-tree ${tree.id}); using ID")
                        node.id
                    }
                val optDesc = node.getMetadata<String>("description")?.ifEmpty { null }
                make(node).wrapForAccordion(optTitle, optDesc)
            }
        return Block(
            color = optBg,
            alignment = Align(mode = Align.Mode.Vertical, padding = Vec2.ZERO, cross = Align.Cross.Start),
            children =
            arrayOf(
                Image("chevron-down.svg".image()).also { it.rotation = PI }.wrap(title, desc, icon).events {
                    self.color = PolyColor.TRANSPARENT.toAnimatable()
                    var open = false
                    Event.Mouse.Clicked(0) then {
                        open = !open
                        Rotate(this[1], if (!open) PI else 0.0, false, Animations.EaseOutQuad.create(0.2.seconds)).add()
                        val value = parent!![1].size.y
                        val anim = Animations.EaseOutQuad.create(0.4.seconds)
                        val operation = Resize(parent!!, width = 0f, height = if (open) -value else value, add = true, anim)
                        addOperation(
                            object : DrawableOp.Animatable<Drawable>(parent!!, anim) {
                                override fun apply(value: Float) {
                                    operation.apply()
                                    // asm: instruct parent (options list) to replace all its children so that they move with it closing
                                    self.parent!!.recalculateChildren()
                                    // asm: instruct all children of this accordion to update their visibility based on THIS, NOT its parent
                                    self[1].children!!.fastEach {
                                        it.renders = it.intersects(self.x, self.y, self.width, self.height)
                                    }
                                }
                            },
                        )
                        true
                    }
                },
                Group(
                    size = Vec2(1078f, 0f),
                    alignment = Align(padding = Vec2(24f, 12f), cross = Align.Cross.Start),
                    children = options.toTypedArray(),
                ).namedId("AccordionContent"),
            ),
        ).namedId("AccordionHeader")
    }

    fun make(property: Property<*>): Drawable {
        val cls = property.getMetadata<Class<*>>("visualizer") ?: throw IllegalArgumentException("Property ${property.id} is missing required metadata 'visualizer'")
        return visCache.getOrPut(cls) {
            val it = cls.getDeclaredConstructor().newInstance() ?: throw IllegalArgumentException("Visualizer ${cls.name} could not be instantiated")
            it as? Visualizer ?: throw IllegalArgumentException("Visualizer ${cls.name} does not implement Visualizer")
        }.visualize(property)
    }

    private fun Drawable.wrap(
        title: String,
        desc: String?,
        icon: PolyImage?,
    ): Drawable {
        return Block(
            alignment = Align(main = Align.Main.SpaceBetween, padding = Vec2(16f, 8f)),
            size = Vec2(1078f, 64f),
            color = optBg,
            children =
            arrayOf(
                Group(
                    alignment = Align(padding = Vec2(0f, 12f)),
                    children =
                    arrayOf(
                        if (icon != null) Image(icon).onInit { image.size.max(32f, 32f) } else null,
                        Group(
                            alignment = Align(cross = Align.Cross.Start, mode = Align.Mode.Vertical, padding = Vec2(6f, 6f)),
                            children =
                            arrayOf(
                                Text(title, fontSize = 22f, font = PolyUI.defaultFonts.medium),
                                if (desc != null) Text(desc, visibleSize = Vec2(500f, 12f)) else null,
                            ),
                        ),
                    ),
                ),
                this,
            ),
        )
    }

    private fun Drawable.wrapForAccordion(
        title: String,
        desc: String?,
    ): Drawable {
        return Group(
            alignment = Align(main = Align.Main.SpaceBetween, padding = Vec2.ZERO),
            size = Vec2(503f, 32f),
            children =
            arrayOf(
                Text(title, fontSize = 16f),
                this,
            ),
        ).addHoverInfo(desc)
    }
}

