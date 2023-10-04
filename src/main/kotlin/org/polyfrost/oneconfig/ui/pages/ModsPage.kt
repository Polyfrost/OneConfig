package org.polyfrost.oneconfig.ui.pages

import org.polyfrost.oneconfig.api.config.Config
import org.polyfrost.oneconfig.api.config.ConfigVisualizer
import org.polyfrost.oneconfig.api.config.ConfigVisualizer.addEventHandler
import org.polyfrost.oneconfig.api.config.Tree
import org.polyfrost.oneconfig.ui.elements.Card
import org.polyfrost.polyui.component.impl.Button
import org.polyfrost.polyui.event.MouseClicked
import org.polyfrost.polyui.input.Translator.Companion.localised
import org.polyfrost.polyui.layout.Layout
import org.polyfrost.polyui.layout.Layout.Companion.drawables
import org.polyfrost.polyui.layout.impl.FlexLayout
import org.polyfrost.polyui.layout.impl.PixelLayout
import org.polyfrost.polyui.layout.impl.SwitchingLayout
import org.polyfrost.polyui.property.impl.ButtonProperties
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.utils.fastEach

class ModsPage(trees: Collection<Tree>, private val owner: SwitchingLayout) {
    private val cache = HashMap<Tree, Layout>()
    private val noIconProps = object : ButtonProperties() {
        override val verticalPadding: Float
            get() = 8.5f
    }
    val buttonProps = object : ButtonProperties() {
        override val iconTextSpacing: Float
            get() = 10f
    }

    val content: Layout
    val header = FlexLayout(
        at = origin,
        size = 1126.px * 40.px,
        resizesChildren = true,
        drawables = drawables(
            Button(
                properties = noIconProps,
                at = flex(),
                text = "oneconfig.mods".localised(),
                fontSize = 16.px,
            ),
            *Config.Category.values().map {
                val icon = if (it.iconPath != null) PolyImage(it.iconPath, 17f, 17f) else null
                Button(
                    properties = if (icon == null) noIconProps else null,
                    fontSize = 16.px,
                    at = flex(),
                    text = it.name.localised(),
                    left = icon,
                )
            }.toTypedArray(),
        ),
    )

    init {
        content = FlexLayout(
            resizesChildren = true,
            at = 0.px * 40.px,
            size = 1136.px * 0.px,
            gap = Gap(12.px, 12.px),
            drawables = trees.mapNotNull { tree ->
                val meta: Config = tree.getMetadata("meta") ?: return@mapNotNull null
                val c = Card(
                    at = flex(),
                    configData = meta,
                )
                c.children[0].addEventHandler(MouseClicked(0)) {
                    if (!c.enabled) return@addEventHandler false
                    owner.switch(cache.getOrPut(tree) { ConfigVisualizer.create(owner.layout, tree) })
                    true
                }
                c.children[1].addEventHandler(MouseClicked(0)) {
                    c.enabled = !c.enabled
                    true
                }
                return@mapNotNull c
            }.toTypedArray(),
        ).scrolling(1126.px * 600.px)
    }

    val self: Layout = object :
        PixelLayout(
            at = origin,
            size = 1126.px * 600.px,
            drawables = drawables(
                header,
                content,
            ),
        ),
        Page {
        override fun filter(query: String, search: Any.(String) -> Boolean) {
            children[1].components.fastEach {
                if (it !is Card) return@fastEach
                it.exists = it.title.string.run { search(query) }
            }
            children[1].calculateBounds()
        }
    }

    fun open() {
        owner.switch(self)
    }
}
