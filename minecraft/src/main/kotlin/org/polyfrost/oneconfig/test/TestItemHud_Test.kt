package org.polyfrost.oneconfig.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.polyfrost.compose.composables.PolyBox
import org.polyfrost.compose.composables.PolyMcText
import org.polyfrost.compose.composables.PolyRow
import org.polyfrost.compose.composables.padding
import org.polyfrost.compose.layout.PolyInsets
import org.polyfrost.oneconfig.api.config.v1.annotations.ItemList
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.internal.ui.components.item.ItemCatalog
import org.polyfrost.oneconfig.internal.ui.components.item.PolyItemIcon

class TestItemHud_Test : Hud("test-item-hud", "Item List Hud", Category.INFO) {

    @ItemList(title = "Items", description = "Items drawn by this HUD")
    var items: MutableList<String> = mutableListOf("minecraft:diamond_sword", "minecraft:golden_apple")

    @Slider(title = "Icon Size", min = 8f, max = 32f, step = 1f)
    var iconSize: Int = 16

    @Switch(title = "Show Names")
    var showNames: Boolean = false

    private var displayItems: MutableState<List<String>> = mutableStateOf(emptyList())

    init {
        padLeft = 4f
        padRight = 4f
        padTop = 4f
        padBottom = 4f
    }

    @Composable
    override fun Content() {
        val ids = displayItems.value
        PolyBox(modifier = hudBackground().padding(PolyInsets(padLeft, padTop, padRight, padBottom))) {
            PolyRow(gap = 3f) {
                if (ids.isEmpty()) {
                    PolyMcText(text = "§7No items", scale = 1f)
                } else {
                    ids.forEach { id ->
                        PolyRow(gap = 2f) {
                            PolyItemIcon(id, iconSize.toFloat())
                            if (showNames) {
                                PolyMcText(text = displayName(id), scale = 1f)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun displayName(id: String): String =
        ItemCatalog.items().firstOrNull { it.id == id }?.displayName ?: id.substringAfter(':')

    override fun update(): Boolean {
        val ids = items.toList()
        if (ids == displayItems.value) return false
        displayItems.value = ids
        return true
    }

    override fun setup() {
        super.setup()
        if (isReal) {
            updateWhenChanged("items")
            updateWhenChanged("iconSize")
            updateWhenChanged("showNames")
        }
        update()
    }

    override fun defaultPosition(): Pair<Float, Float> = 10f to 150f

    override fun showByDefault(): Boolean = true

    override fun minimumSize(): Pair<Float, Float> =
        (padLeft + padRight + 8f) to (padTop + padBottom + 8f)

    override fun clone(): Hud = (super.clone() as TestItemHud_Test).also {
        it.items = this.items.toMutableList()
        it.displayItems = mutableStateOf(emptyList())
    }
}
