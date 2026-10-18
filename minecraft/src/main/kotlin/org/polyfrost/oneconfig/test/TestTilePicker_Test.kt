package org.polyfrost.oneconfig.test

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.annotations.Option
import org.polyfrost.oneconfig.internal.ui.api.settings.TileListOptionData
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerEntry
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerTileIcon
import org.polyfrost.oneconfig.internal.ui.components.picker.TilePicker
import org.polyfrost.oneconfig.internal.ui.components.picker.toggleEntry
import org.polyfrost.oneconfig.internal.ui.components.settings.LocalOptionWidth
import org.polyfrost.oneconfig.internal.ui.components.settings.TileListOption
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Option(display = TestTilePickerVisualizer_Test::class)
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class TestTilePicker_Test(
    val title: String = "",
    val description: String = "",
    val category: String = "General",
    val subcategory: String = "General",
)

@Option(display = TestTileListVisualizer_Test::class)
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class TestTileList_Test(
    val title: String = "",
    val description: String = "",
    val category: String = "General",
    val subcategory: String = "General",
    val maxEntries: Int = 0,
    val reorderable: Boolean = true,
    val addText: String = "Choose colors",
)

private data class Swatch(override val id: String, override val displayName: String, val color: Color) : PickerEntry

private val Swatches = listOf(
    Swatch("test:red", "Red", Color(0xFFE53935)),
    Swatch("test:orange", "Orange", Color(0xFFFB8C00)),
    Swatch("test:yellow", "Yellow", Color(0xFFFDD835)),
    Swatch("test:green", "Green", Color(0xFF43A047)),
    Swatch("test:cyan", "Cyan", Color(0xFF00ACC1)),
    Swatch("test:blue", "Blue", Color(0xFF1E88E5)),
    Swatch("test:purple", "Purple", Color(0xFF8E24AA)),
    Swatch("test:pink", "Pink", Color(0xFFD81B60)),
    Swatch("test:white", "White", Color(0xFFFAFAFA)),
    Swatch("test:black", "Black", Color(0xFF212121)),
)

private val SwatchShape = RoundedCornerShape(4.dp)
private val PickerShape = RoundedCornerShape(12.dp)
private const val MAX_SWATCHES = 3

private val SwatchIcon: PickerTileIcon<Swatch> = { swatch, modifier, alpha ->
    Box(
        modifier
            .aspectRatio(1f)
            .alpha(alpha)
            .clip(SwatchShape)
            .background(swatch.color, SwatchShape)
            .border(1.dp, LocalTheme.current.borderColor, SwatchShape)
    )
}

class TestTilePickerVisualizer_Test : Visualizer {
    @Composable
    override fun visualize(prop: Property<*>) {
        val theme = LocalTheme.current
        var selected by remember(prop) { mutableStateOf((prop.get() as? Array<*>)?.filterIsInstance<String>().orEmpty()) }

        TilePicker(
            entries = Swatches,
            selected = selected,
            onToggle = { id ->
                selected = toggleEntry(selected, id, MAX_SWATCHES)
                @Suppress("UNCHECKED_CAST")
                (prop as Property<Any>).set(selected.toTypedArray())
            },
            icon = SwatchIcon,
            modifier = Modifier
                .width(LocalOptionWidth.current)
                .clip(PickerShape)
                .background(theme.popupBackground, PickerShape)
                .border(1.dp, theme.borderColor, PickerShape)
                .padding(vertical = 12.dp),
            maxEntries = MAX_SWATCHES,
            autoFocusSearch = false,
            titleKey = "Pick up to $MAX_SWATCHES colors",
            searchPlaceholderKey = "Search colors",
            emptyKey = "No matching colors",
        )
    }
}

class TestTileListVisualizer_Test : Visualizer {
    @Composable
    override fun visualize(prop: Property<*>) {
        TileListOption(
            TileListOptionData(prop),
            Swatches,
            SwatchIcon,
            titleKey = "Select colors",
            searchPlaceholderKey = "Search colors",
            emptyKey = "No matching colors",
            unavailableKey = "Unknown color",
        )
    }
}
