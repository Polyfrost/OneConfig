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

package org.polyfrost.oneconfig.api.config.v1

import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.visualize.Visualizer
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
import org.polyfrost.polyui.utils.mapToArray
import org.polyfrost.polyui.utils.rgba
import kotlin.math.PI

open class ConfigVisualizer {
    private val LOGGER = LogManager.getLogger("OneConfig/Config")
    protected val configCache = HashMap<Tree, Drawable>()
    protected val optBg = rgba(39, 49, 55, 0.2f)
    protected val alignC = Align(cross = Align.Cross.Start)
    protected val alignCV = Align(cross = Align.Cross.Start, mode = Align.Mode.Vertical)
    protected val alignVNoPad = Align(cross = Align.Cross.Start, mode = Align.Mode.Vertical, padding = Vec2.ZERO)
    protected val stdAlign = Align(main = Align.Main.SpaceBetween, padding = Vec2(16f, 8f))
    protected val stdAccord = Align(main = Align.Main.SpaceBetween, padding = Vec2.ZERO)
    protected val padVOnly = Align(padding = Vec2(0f, 12f))
    protected val stdOpt = Align(cross = Align.Cross.Start, mode = Align.Mode.Vertical, padding = Vec2(6f, 6f))
    protected val accordOpt = Align(cross = Align.Cross.Start, padding = Vec2(24f, 12f))

    /**
     * For information, see [create].
     */
    fun get(config: Tree) = create(config) // configCache.getOrPut(config) { create(config) }

    /**
     * Turn the given config tree into a PolyUI representation.
     *
     * This method will skip:
     * - `Property` that do not have `"visualizer"` metadata
     * - any `Tree` that is deeper than 2 levels
     *
     *
     * This method uses the following metadata:
     * - `"icon"`: optional. specifies the icon shown only on full-size options. ignored on accordion properties. must be either a valid [PolyImage] or a `String path` to an image. **Fails** if this is invalid.
     * - `"category"`: optional. specifies the category of the option. defaults to "General".
     * - `"subcategory"`: optional. specifies the subcategory of option. defaults to "General".
     * - `"visualizer"`: required for `Property`. specifies the method to convert a Property to a PolyUI component. must be a class that implements [Visualizer]. **Fails** if this is invalid.
     */
    protected open fun create(
        config: Tree,
        initialPage: String = "General",
    ): Drawable {
        val now = System.nanoTime()
        val options = HashMap<String, HashMap<String, LinkedList<Drawable>>>(4)

        // asm: step 1: sort the tree into a map of:
        // categories
        //   -> subcategories
        //      -> list of options
        for ((_, node) in config.map) {
            processNode(node, options)
        }
        LOGGER.info("creating config page ${config.title} took ${(System.nanoTime() - now) / 1_000_000f}ms")
        return makeFinal(flattenSubcategories(options), initialPage)
    }

    protected open fun makeFinal(categories: Map<String, Drawable>, initialPage: String): Drawable {
        return Group(
            createHeaders(categories),
            categories[initialPage] ?: throw IllegalArgumentException("Initial page $initialPage does not exist"),
            alignment = alignC,
            visibleSize = Vec2(1130f, 635f),
        )
    }

    protected open fun flattenSubcategories(options: Map<String, Map<String, LinkedList<Drawable>>>): Map<String, Drawable> {
        return options.mapValues { (_, subcategories) ->
            Group(
                alignment = alignCV,
                children =
                subcategories.mapToArray { (header, options) ->
                    Group(
                        Text(header, fontSize = 22f),
                        *options.toTypedArray(),
                        alignment = alignCV,
                    )
                },
            )
        }
    }

    protected open /* suspend? */ fun processNode(node: Node, options: HashMap<String, HashMap<String, LinkedList<Drawable>>>) {
        val icon =
            when (val it = node.getMetadata<Any?>("icon")) {
                null -> null
                is PolyImage -> it
                is String -> it.strv()?.image()
                else -> throw IllegalArgumentException(
                    "Property ${node.id} has invalid icon type ${it::class.java.name} (provided by ${node.id}) - must be a PolyImage or String path",
                )
            }
        val category = node.getMetadata<String>("category")?.strv() ?: "General"
        val subcategory = node.getMetadata<String>("subcategory")?.strv() ?: "General"

        val list = options.getOrPut(category) { HashMap(4) }.getOrPut(subcategory) { LinkedList() }
        if (node is Property<*>) {
            val vis = node.getVisualizer() ?: return
            list.add(wrap(vis.visualize(node), node.title, node.description, icon))
        } else {
            node as Tree
            if (node.map.isEmpty()) {
                LOGGER.warn("sub-tree ${node.id} is empty; ignoring")
                return
            }
            list.add(makeAccordion(node, node.title, node.description, icon))
        }
    }

    protected open fun createHeaders(categories: Map<String, Drawable>): Drawable? {
        return Group(
            children = categories.mapToArray { (category, options) ->
                Button(text = category).events {
                    Event.Mouse.Clicked(0) then {
                        parent[0] = options
                    }
                }
            },
        )
    }

    protected open fun makeAccordion(
        tree: Tree,
        title: String,
        desc: String?,
        icon: PolyImage?,
    ): Drawable {
        val options =
            tree.map.mapNotNull map@{ (_, node) ->
                if (node !is Property<*>) return@map null
                val vis = node.getVisualizer() ?: return@map null
                wrapForAccordion(vis.visualize(node), node.title, node.description)
            }
        return Block(
            wrap(Image("chevron-down.svg".image()).also { it.rotation = PI }, title, desc, icon).events {
                self.color = PolyColor.TRANSPARENT.toAnimatable()
                var open = false
                Event.Mouse.Clicked(0) then {
                    open = !open
                    Rotate(this[1], if (!open) PI else 0.0, false, Animations.EaseOutQuad.create(0.2.seconds)).add()
                    val value = parent[1].height
                    val anim = Animations.EaseOutQuad.create(0.4.seconds)
                    val operation = Resize(parent, width = 0f, height = if (open) -value else value, add = true, anim)
                    addOperation(
                        object : DrawableOp.Animatable<Drawable>(parent, anim, onFinish = {
                            this[1].renders = !open
                            this[1].enabled = !open
                        }) {
                            override fun apply(value: Float) {
                                operation.apply()
                                // asm: instruct parent (options list) to replace all its children so that they move with it closing
                                self.parent.repositionChildren()
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
                alignment = accordOpt,
                children = options.toTypedArray(),
            ).namedId("AccordionContent"),
            color = optBg,
            alignment = alignVNoPad,
        ).namedId("AccordionHeader")
    }

    protected open fun wrap(
        drawable: Drawable,
        title: String,
        desc: String?,
        icon: PolyImage?,
    ): Drawable = Block(
        Group(
            if (icon != null) Image(icon) else null,
            Group(
                Text(title, fontSize = 22f).setFont { medium },
                if (desc != null) Text(desc, visibleSize = Vec2(500f, 12f)) else null,
                alignment = stdOpt,
            ),
            alignment = padVOnly,
        ),
        drawable,
        alignment = stdAlign,
        size = Vec2(1078f, 64f),
        color = optBg,
    )

    protected open fun wrapForAccordion(
        drawable: Drawable,
        title: String,
        desc: String?,
    ): Drawable = Group(
        Text(title, fontSize = 16f),
        drawable,
        alignment = stdAccord,
        size = Vec2(503f, 32f),
    ).addHoverInfo(desc)

    fun Property<*>.getVisualizer(): Visualizer? {
        val vis = this.getMetadata<Class<*>>("visualizer") ?: return null
        return visCache.getOrPut(vis) {
            val it = vis.getDeclaredConstructor().newInstance() ?: throw IllegalStateException("Visualizer $vis could not be instantiated; ensure it has a public no-args constructor")
            it as? Visualizer ?: throw IllegalArgumentException("Visualizer $vis does not implement Visualizer")
        }
    }

    companion object {
        @JvmField
        val INSTANCE = ConfigVisualizer()
        protected val visCache = HashMap<Class<*>, Visualizer>()

        fun String?.strv() = this?.trim()?.ifEmpty { null }
    }
}
