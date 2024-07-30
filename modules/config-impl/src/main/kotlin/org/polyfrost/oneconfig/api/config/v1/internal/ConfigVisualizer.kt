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

package org.polyfrost.oneconfig.api.config.v1.internal

import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.*
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.operations.ComponentOp
import org.polyfrost.polyui.operations.Resize
import org.polyfrost.polyui.operations.Rotate
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.*
import kotlin.math.PI

open class ConfigVisualizer {
    private val LOGGER = LogManager.getLogger("OneConfig/Config")
    protected val cache = HashMap<Tree, Map<String, Map<String, ArrayList<Triple<String, String?, Drawable>>>>>()
    protected val optBg = rgba(39, 49, 55, 0.2f)
    protected val alignC = Align(cross = Align.Cross.Start)
    protected val alignCV = Align(cross = Align.Cross.Start, mode = Align.Mode.Vertical)
    protected val alignVNoPad = Align(cross = Align.Cross.Start, mode = Align.Mode.Vertical, pad = Vec2.ZERO)
    protected val stdAlign = Align(main = Align.Main.SpaceBetween, pad = Vec2(16f, 8f))
    protected val stdAccord = Align(main = Align.Main.SpaceBetween, pad = Vec2.ZERO)
    protected val ic2text = Align(pad = Vec2(8f, 0f))
    protected val stdOpt = Align(cross = Align.Cross.Start, mode = Align.Mode.Vertical, pad = Vec2(0f, 8f))
    protected val accordOpt = Align(cross = Align.Cross.Start, pad = Vec2(24f, 12f))

    /**
     * For information, see [create].
     */
    fun get(config: Tree) = create(config)

    fun getMatching(str: String): List<Drawable> {
        if (str.length < 2) return emptyList()
        val out = ArrayList<Drawable>()
        for ((tree, opts) in cache) {
            for ((category, sub) in opts) {
                for ((header, options) in sub) {
                    for ((title, desc, drawable) in options) {
                        if (title.contains(str, ignoreCase = true) || title.levenshteinDistance(str) <= 2) out.add(drawable)
                        else if (desc != null && (desc.contains(str, ignoreCase = true) || desc.levenshteinDistance(str) <= 2)) out.add(drawable)
                    }
                }
            }
        }
        return out
    }

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
        val options = cache.getOrPut(config) {
            val now = System.nanoTime()
            val options = HashMap<String, HashMap<String, ArrayList<Triple<String, String?, Drawable>>>>(4)

            // asm: step 1: sort the tree into a map of:
            // categories
            //   -> subcategories
            //      -> list of options
            for ((_, node) in config.map) {
                processNode(node, options)
            }
            LOGGER.info("creating config page ${config.title} took ${(System.nanoTime() - now) / 1_000_000f}ms")
            options
        }
        return makeFinal(flattenSubcategories(options), initialPage)
    }

    protected open fun makeFinal(categories: Map<String, Drawable>, initialPage: String): Drawable {
        return Group(
            createHeaders(categories),
            categories[initialPage] ?: categories.values.first(),
            alignment = alignC,
            visibleSize = Vec2(1130f, 635f),
        )
    }

    protected open fun flattenSubcategories(options: Map<String, Map<String, ArrayList<Triple<String, String?, Drawable>>>>): Map<String, Drawable> {
        return options.mapValues { (_, subcategories) ->
            Group(
                children = subcategories.mapToArray { (header, opts) ->
                    Group(
                        Text(header, fontSize = 22f),
                        *opts.mapToArray { it.third },
                        alignment = alignCV,
                    )
                },
                alignment = alignCV,
            )
        }
    }

    protected open /* suspend? */ fun processNode(node: Node, options: HashMap<String, HashMap<String, ArrayList<Triple<String, String?, Drawable>>>>) {
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

        val list = options.getOrPut(category) { HashMap(4) }.getOrPut(subcategory) { ArrayList(8) }
        if (node is Property<*>) {
            val vis = node.getVisualizer() ?: return
            list.add(Triple(node.title, node.description, wrap(vis.visualize(node), node.title ?: return, node.description, icon)))
        } else {
            node as Tree
            if (node.map.isEmpty()) {
                LOGGER.warn("sub-tree ${node.id} is empty; ignoring")
                return
            }
            list.add(Triple(node.title, node.description, makeAccordion(node, node.title ?: return, node.description, icon)))
        }
    }

    protected open fun createHeaders(categories: Map<String, Drawable>): Drawable? {
        if (categories.size <= 1) return null
        return Group(
            children = categories.mapToArray { (category, options) ->
                Button(text = category).onClick {
                    parent.parent[1] = options
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
        val index = ArrayList<Pair<String, String?>>(tree.map.size)
        val options =
            tree.map.mapNotNull map@{ (_, node) ->
                if (node !is Property<*>) return@map null
                val vis = node.getVisualizer() ?: return@map null
                index.add(node.title to node.description)
                wrapForAccordion(vis.visualize(node), node.title ?: return@map null, node.description)
            }
        var open = false
        val out = Block(
            wrap(Image("polyui/chevron-down.svg".image()).also { it.rotation = PI }, title, desc, icon).events {
                self.color = PolyColor.TRANSPARENT
                Event.Mouse.Companion.Clicked then {
                    open = !open
                    Rotate(this[1], if (!open) PI else 0.0, false, Animations.Default.create(0.2.seconds)).add()
                    val value = parent[1].height
                    val anim = Animations.Default.create(0.4.seconds)
                    val operation = Resize(parent, width = 0f, height = if (open) -value else value, add = true, anim)
                    addOperation(
                        object : ComponentOp.Animatable<Component>(parent, anim, onFinish = {
                            this[1].clipped = !open
                            this[1].isEnabled = !open
                        }) {
                            override fun apply(value: Float) {
                                operation.apply()
                                // asm: instruct parent (options list) to replace all its children so that they move with it closing
                                self.parent.position()
                                // asm: instruct all children of this accordion to update their visibility based on THIS, NOT its parent
                                self[1].children!!.fastEach {
                                    it.clipped = it.intersects(self.x, self.y, self.width, self.height)
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
        //index.fastEach { this.index[out] = it }
        return out
    }

    protected open fun wrap(
        drawable: Drawable,
        title: String,
        desc: String?,
        icon: PolyImage?,
    ): Drawable = Block(
        Group(
            if (icon != null) Image(icon).onInit { ensureLargerThan(32f by 32f) } else null,
            Group(
                Text(title, fontSize = 22f).setFont { medium },
                if (desc != null) Text(desc, visibleSize = Vec2(500f, 0f)).secondary() else null,
                alignment = stdOpt,
            ),
            alignment = ic2text,
        ),
        drawable,
        alignment = stdAlign,
        size = Vec2(1078f, 0f),
        color = optBg,
    ).minimumSize(Vec2(1078f, 64f))//.also { index[it] = title to desc }

    protected open fun wrapForAccordion(
        drawable: Drawable,
        title: String,
        desc: String?,
    ): Drawable = Group(
        Text(title, fontSize = 16f),
        drawable,
        alignment = stdAccord,
        size = Vec2(503f, 32f),
    ).apply { if (desc != null) addHoverInfo(Text(desc)) }

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
