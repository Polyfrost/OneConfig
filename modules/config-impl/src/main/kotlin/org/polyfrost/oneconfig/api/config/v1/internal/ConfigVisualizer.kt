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
import org.polyfrost.oneconfig.api.config.v1.*
import org.polyfrost.oneconfig.api.config.v1.dsl.ConfigDSL.Companion.config
import org.polyfrost.oneconfig.utils.v1.MHUtils.setAccessible
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.color.rgba
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.Scrollable
import org.polyfrost.polyui.component.extensions.*
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.operations.Resize
import org.polyfrost.polyui.operations.Rotate
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Point
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.*
import java.lang.ref.WeakReference
import kotlin.math.PI

open class ConfigVisualizer {
    private val LOGGER = LogManager.getLogger("OneConfig/Config")
    protected val configs = HashMap<Tree, Drawable>()
    protected val optBg = rgba(39, 49, 55, 0.2f)
    protected val alignCStart = Align(line = Align.Line.Start, mode = Align.Mode.Vertical, wrap = Align.Wrap.NEVER, padBetween = Vec2(6f, 10f))
    protected val alignCStartNoPad = Align(line = Align.Line.Start, mode = Align.Mode.Vertical, wrap = Align.Wrap.NEVER, pad = Vec2.ZERO)
    protected val stdAlign = Align(main = Align.Content.SpaceBetween, cross = Align.Content.Center, pad = Vec2(16f, 8f))
    protected val stdAccord = Align(main = Align.Content.SpaceBetween, pad = Vec2.ZERO)
    protected val ic2text = Align(pad = Vec2(8f, 0f))
    protected val stdOpt = Align(line = Align.Line.Start, pad = Vec2(0f, 8f), mode = Align.Mode.Vertical, wrap = Align.Wrap.NEVER)
    protected val accordOpt = Align(line = Align.Line.Start, pad = Vec2(22f, 12f))

    /**
     * For information, see [create].
     */
    fun get(config: Tree): Drawable {
        if (config.getOrPutMetadata("no_cache") { false }) return create(config)
        val it = configs[config]
        if (it != null) {
            // asm: might've been searched, so we need to reposition our option lists
            val listToReposition = it.children?.last()
            listToReposition?.children?.fastEach {
                it.position()
            }
            return it
        } else {
            val configPage = create(config)
            configs[config] = configPage
            return configPage
        }
    }

    /**
     * Clears the cache of all created config screens.
     */
    fun clearCache() {
        configs.clear()
    }

    fun getMatching(str: String): List<Drawable> {
        val it = str.trim()
        if (it.length < 2) return emptyList()
        val out = ArrayList<Drawable>()
        for (config in configs.keys) {
            getMatching(it, config, out)
        }
        return out
    }

    private fun getMatching(it: String, config: Tree, out: ArrayList<Drawable>) {
        config.onAll { _, node ->
            if (node is Tree) {
                getMatching(it, node, out)
                return@onAll
            }
            val visualized = node.getMetadata<WeakReference<Drawable>>("drawable")?.get() ?: return@onAll
            if (node.title.matchesSearch(it) || node.description.matchesSearch(it)) {
                out.add(visualized)
            } else {
                node.getMetadata<ArrayList<String>>("aliases")?.fastEach { alias ->
                    if (alias.matchesSearch(it)) {
                        out.add(visualized)
                        return@onAll
                    }
                }
            }
        }
    }

    private fun String?.matchesSearch(search: String) = this != null && (this.contains(search, ignoreCase = true) || this.levenshteinDistance(search) <= 2)

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
        initialCategory: String = "General",
    ): Drawable {
        val now = System.nanoTime()
        val options = LinkedHashMap<String, HashMap<String, ArrayList<Drawable>>>(4)

        // asm: step 1: sort the tree into a map of:
        // categories
        //   -> subcategories
        //      -> list of options
        for ((_, node) in config.map) {
            // first ignore empty tree nodes
            if(node is Tree) {
                if (node.map.isEmpty()) {
                    LOGGER.warn("sub-tree ${node.id} is empty; ignoring")
                    continue
                }
            } else {
                node as Property<*>
                if (node.getMetadata<Any?>("visualizer") == null) {
                    // LOGGER.warn("Property ${node.id} does not have a visualizer; ignoring")
                    continue
                }
            }

            processNode(config, node, options)
        }
        LOGGER.info("creating config page ${config.title} took ${(System.nanoTime() - now) / 1_000_000f}ms")
        return makeFinal(flattenSubcategories(options), initialCategory).addRethemingListeners().addRescalingListeners()
    }

    protected open fun makeFinal(categories: Map<String, Drawable>, initialCategory: String): Drawable {
        return Group(
            createHeaders(categories),
            categories[initialCategory] ?: categories.values.first(),
            alignment = Align(line = Align.Line.Start, mode = Align.Mode.Vertical, wrap = Align.Wrap.NEVER),
            visibleSize = Vec2(1130f, 635f),
        )
    }

    protected open fun flattenSubcategories(options: Map<String, Map<String, ArrayList<Drawable>>>): Map<String, Drawable> {
        return options.mapValues { (_, subcategories) ->
            Group(
                children = subcategories.mapToArray { (header, opts) ->
                    Group(
                        Text(header, fontSize = 22f).padded(0f, 0f, 0f, 2f),
                        *opts.toTypedArray(),
                        alignment = alignCStart,
                    )
                },
                alignment = alignCStart,
            )
        }
    }

    protected open /* suspend? */ fun processNode(root: Tree, node: Node, options: HashMap<String, HashMap<String, ArrayList<Drawable>>>) {
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

        val list = options.getOrPut(category) { LinkedHashMap(4) }.getOrPut(subcategory) { ArrayList(8) }
        if (node is Property<*>) {
            val vis = node.getVisualizer() ?: return
            list.add(wrap(vis.visualize(node), node.title, node.description, icon).addHideHandler(node).addResetMenu(root, node).linkTo(node))
        } else {
            node as Tree
            list.add(makeAccordion(root, node, node.title, node.description, icon).linkTo(node))
        }
    }

    protected open fun createHeaders(categories: Map<String, Drawable>): Drawable? {
        if (categories.size <= 1) return null
        return Group(
            children = categories.mapToArray { (category, options) ->
                Button(text = category).onClick {
                    parent.parent[1] = options
                    (parent.parent[1] as Scrollable).visibleSize = Vec2(1130f, 635f) // asm: reset size to default
                }
            },
        )
    }

    protected open fun makeAccordion(
        root: Tree,
        tree: Tree,
        title: String,
        desc: String?,
        icon: PolyImage?,
    ): Drawable {
        val options =
            tree.map.mapNotNull map@{ (_, node) ->
                if (node !is Property<*>) return@map null
                val vis = node.getVisualizer() ?: return@map null
                wrapForAccordion(vis.visualize(node), node.title ?: return@map null, node.description).addHideHandler(node).addResetMenu(root, node).linkTo(node)
            }

        var open = true
        val e: Property<*>? = tree.getProp("enabled")
        val toWrap: Drawable
        var enabled: Property<Boolean>? = null
        var contentHeight = -1f
        // asm: signature as it prevents re-wrapping of function
        val openInsn: Drawable.(Any?) -> Unit = {
            open = !open
            val arrow = if (enabled != null) this[1][1] else this[1]
            val anim = Animations.Default.create(0.6.seconds)
            Rotate(arrow, if (!open) PI else 0.0, false, anim).add()
            val content = parent[1]
            if (contentHeight == -1f) contentHeight = content.height
            Resize(parent, width = 0f, height = if (open) -contentHeight else contentHeight, add = true, animation = anim).add()
            Resize(content, width = 0f, height = if (open) -contentHeight else contentHeight, add = true, animation = anim).add()
            // won't ever open properly unless it renders at least once (tee hee) :)
            if (!open) {
                content.height = 1f
                content.renders = true
            }
        }

        if (e != null && e.type == Boolean::class.java && e.getVisualizer() == null) {
            open = e.getAs()
            toWrap = Group(
                Switch(
                    lateralStretch = 2f,
                    size = 21f,
                    state = open
                ).onToggle {
                    enabled?.setAs(it)
                    if (open != it) (parent.parent as Drawable).openInsn(null)
                },
                Image("polyui/chevron-down.svg").also { it.rotation = PI }
            )
            @Suppress("UNCHECKED_CAST") // reason: #already-type-checked
            enabled = e as Property<Boolean>
        } else {
            toWrap = Image("polyui/chevron-down.svg").also { it.rotation = PI }
        }
        val out = Block(
            wrap(toWrap, title, desc, icon).also {
                it.color = PolyColor.TRANSPARENT
                it.onClick(openInsn)
            }.namedId("AccordionHeader"),
            Group(
                size = Vec2(1078f, 0f),
                alignment = accordOpt,
                children = options.toTypedArray(),
            ).namedId("AccordionContent"),
            color = optBg,
            alignment = alignCStartNoPad,
        ).namedId("AccordionContainer")
        return out
    }

    protected open fun wrap(
        drawable: Drawable,
        title: String?,
        desc: String?,
        icon: PolyImage?,
    ): Drawable = Block(
        if (title != null)
            Group(
                if (icon != null) Image(icon).onInit { ensureLargerThan(32f by 32f) } else null,
                Group(
                    Text(title, fontSize = 22f).setFont { medium },
                    if (desc != null) Text(desc, visibleSize = Vec2(500f, 0f)).secondary() else null,
                    alignment = stdOpt,
                ),
                alignment = ic2text,
            ) else null,
        drawable,
        alignment = stdAlign,
        size = Vec2(1078f, 0f),
        color = optBg,
    ).minimumSize(Vec2(1078f, 64f)).radius(12f)

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
            val it = vis.getDeclaredConstructor().setAccessible().newInstance() ?: throw IllegalStateException("Visualizer $vis could not be instantiated; ensure it has a public no-args constructor")
            it as? Visualizer ?: throw IllegalArgumentException("Visualizer $vis does not implement Visualizer")
        }
    }

    private fun Drawable.addResetMenu(root: Tree, prop: Property<*>) = this.events {
        Event.Mouse.Clicked(1) then {
            PopupMenu(Group(Image("assets/oneconfig/ico/trash.svg"), Text("Restore Default")).setPalette { state.danger }.withHoverStates().onClick {
                val backup = ConfigManager.backup().get(root.id)
                if (backup == null) {
                    LOGGER.error("Failed to locate backup source for ${root.id}, the config needs to be deleted manually to create a backup source")
                    this@addResetMenu.shake()
                    return@onClick false
                }
                prop.overwrite(backup.get(Tree.evaluatePath(root, prop).split('.')), false)
                polyUI.unfocus()
                false
            }, polyUI = polyUI, position = Point.Above)
            false
        }
    }

    private fun Drawable.addHideHandler(prop: Property<*>): Drawable {
        this.on(Event.Lifetime.Disabled) {
            this.alpha = 0.8f
        }
        this.on(Event.Lifetime.Enabled) {
            this.alpha = 1f
        }
        var hidden = false
        prop.onDisplayChange { s ->
            if (s == Property.Display.HIDDEN) {
                hidden = true
                if (!initialized) {
                    this.afterParentInit(Int.MAX_VALUE) {
                        layoutIgnored = true
                        parent.position()
                        renders = false
                    }
                } else {
                    layoutIgnored = true
                    parent.position()
                    renders = false
                }
            } else if (hidden) {
                hidden = false
                layoutIgnored = false
                parent.position()
                renders = true
            }
            this.isEnabled = s == Property.Display.SHOWN
        }
        return this
    }

    private fun Drawable.linkTo(node: Node): Drawable {
        // asm: stored in a weak reference to avoid potential memory leaks
        node.addMetadata("drawable", WeakReference(this))
        return this
    }

    companion object {
        @JvmField
        val INSTANCE = ConfigVisualizer()
        protected val visCache = HashMap<Class<*>, Visualizer>()

        fun String?.strv() = this?.trim()?.ifEmpty { null }
    }
}
