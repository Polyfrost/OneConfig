import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.internal.ui.OneConfigInterface
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.components.settings.item.ItemCatalog
import org.polyfrost.oneconfig.internal.ui.components.settings.item.ItemCatalogService
import org.polyfrost.oneconfig.internal.ui.components.settings.item.ItemDescriptor
import org.polyfrost.oneconfig.internal.ui.components.settings.item.ItemIconData
import org.polyfrost.oneconfig.internal.ui.screens.PREFERENCES_ID

private enum class Theme { Dark, Light, System }
private enum class Difficulty { Easy, Normal, Hard, Extreme }

fun main() {
    installPreviewItemCatalog()
    seedRegistry()

    singleWindowApplication(
        title = "One Config UI Test",
        state = WindowState(width = 1280.dp, height = 720.dp)
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource("assets/oneconfig/images/background.png"),
            contentDescription = "Background Image",
            contentScale = ContentScale.FillBounds
        )
        OneConfigInterface(window.width.toFloat(), window.height.toFloat())
    }
}

private fun installPreviewItemCatalog() {
    val items = listOf(
        ItemDescriptor("minecraft:diamond_sword", "Diamond Sword"),
        ItemDescriptor("minecraft:golden_apple", "Golden Apple"),
        ItemDescriptor("minecraft:ender_pearl", "Ender Pearl"),
        ItemDescriptor("minecraft:oak_log", "Oak Log"),
        ItemDescriptor("minecraft:redstone", "Redstone Dust"),
        ItemDescriptor("example:blue_gem", "Azure Crystal"),
    )
    ItemCatalog.installOverride(object : ItemCatalogService {
        override fun items() = items

        override fun icon(id: String): ItemIconData {
            val base = 0xFF000000.toInt() or (id.hashCode() and 0x00FFFFFF)
            return ItemIconData(16, 16, IntArray(16 * 16) { index ->
                if ((index / 16 + index % 16) % 2 == 0) base else base xor 0x00202020
            })
        }
    })
}

private fun seedRegistry() {
    ConfigRegistry.registerTree(testControlsTree(), ConfigSource.OC)
    ConfigRegistry.registerTree(testPrefsTree(), ConfigSource.OC)
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> prop(id: String, title: String, desc: String, value: T, icon: String? = null) =
    Properties.simple(id, title, desc, value, value::class.java as Class<T>).also { p ->
        icon?.let { p.addMetadata("icon", it) }
    }

private fun boolProp(id: String, title: String, desc: String, value: Boolean, icon: String? = null) =
    Properties.simple(id, title, desc, value, Boolean::class.java).also { p ->
        p.addMetadata("visualizer", Visualizer.SwitchVisualizer::class.java)
        icon?.let { p.addMetadata("icon", it) }
    }

private fun checkboxProp(id: String, title: String, desc: String, value: Boolean, icon: String? = null) =
    Properties.simple(id, title, desc, value, Boolean::class.java).also { p ->
        p.addMetadata("visualizer", Visualizer.CheckboxVisualizer::class.java)
        icon?.let { p.addMetadata("icon", it) }
    }

private fun sliderProp(id: String, title: String, desc: String, value: Float, min: Float, max: Float, step: Float = 0f) =
    Properties.simple(id, title, desc, value, Float::class.java).also { p ->
        p.addMetadata("visualizer", Visualizer.SliderVisualizer::class.java)
        p.addMetadata("min", min)
        p.addMetadata("max", max)
        if (step > 0f) p.addMetadata("step", step)
    }

private fun numberProp(id: String, title: String, desc: String, value: Float, unit: String? = null) =
    Properties.simple(id, title, desc, value, Float::class.java).also { p ->
        p.addMetadata("visualizer", Visualizer.NumberVisualizer::class.java)
        unit?.let { p.addMetadata("unit", it) }
    }

private fun textProp(id: String, title: String, desc: String, value: String, placeholder: String? = null) =
    Properties.simple(id, title, desc, value, String::class.java).also { p ->
        p.addMetadata("visualizer", Visualizer.TextVisualizer::class.java)
        placeholder?.let { p.addMetadata("placeholder", it) }
    }

private fun fileProp(
    id: String, title: String, desc: String, value: String,
    types: Array<String> = emptyArray(), filterName: String? = null, directory: Boolean = false,
) =
    Properties.simple(id, title, desc, value, String::class.java).also { p ->
        p.addMetadata("visualizer", Visualizer.FileVisualizer::class.java)
        p.addMetadata("placeholder", if (directory) "Select a folder..." else "Select a file...")
        if (types.isNotEmpty()) p.addMetadata("types", types)
        filterName?.let { p.addMetadata("filterName", it) }
        if (directory) p.addMetadata("directory", true)
    }

private fun <E : Enum<E>> dropdownProp(id: String, title: String, desc: String, value: E) =
    Properties.simple(id, title, desc, value, value::class.java as Class<E>?).also { p ->
        p.addMetadata("visualizer", Visualizer.DropdownVisualizer::class.java)
    }

private fun radioProp(id: String, title: String, desc: String, value: Int, vararg options: String) =
    Properties.simple(id, title, desc, value, Int::class.java).also { p ->
        p.addMetadata("visualizer", Visualizer.RadioVisualizer::class.java)
        p.addMetadata("options", arrayOf(*options))
    }

private fun draggableListProp(id: String, title: String, desc: String, vararg options: String, checkable: Boolean = false) =
    Properties.simple(id, title, desc, options.toTypedArray(), Array<String>::class.java).also { p ->
        p.addMetadata("visualizer", Visualizer.DraggableListVisualizer::class.java)
        p.addMetadata("options", options.toTypedArray())
        if (checkable) p.addMetadata("checkable", true)
    }

private fun textListProp(
    id: String,
    title: String,
    desc: String,
    vararg entries: String,
    placeholder: String? = null,
    regex: String? = null,
    maxEntries: Int = 0,
    reorderable: Boolean = true,
) = Properties.simple(id, title, desc, entries.toList().toTypedArray(), Array<String>::class.java).also { p ->
    p.addMetadata("visualizer", Visualizer.TextListVisualizer::class.java)
    if (placeholder != null) p.addMetadata("placeholder", placeholder)
    if (regex != null) p.addMetadata("regex", regex)
    if (maxEntries > 0) p.addMetadata("maxEntries", maxEntries)
    if (!reorderable) p.addMetadata("reorderable", false)
}

private fun itemListProp(
    id: String,
    title: String,
    desc: String,
    vararg entries: String,
    maxEntries: Int = 0,
) = Properties.simple(id, title, desc, entries.toList().toTypedArray(), Array<String>::class.java).also { p ->
    p.addMetadata("visualizer", Visualizer.ItemListVisualizer::class.java)
    p.addMetadata("addText", "Choose items")
    if (maxEntries > 0) p.addMetadata("maxEntries", maxEntries)
}

private fun fileListProp(
    id: String,
    title: String,
    desc: String,
    vararg entries: String,
    directory: Boolean = false,
    placeholder: String? = null,
) = Properties.simple(id, title, desc, entries.toList().toTypedArray(), Array<String>::class.java).also { p ->
    p.addMetadata("visualizer", Visualizer.FileListVisualizer::class.java)
    if (directory) p.addMetadata("directory", true)
    if (placeholder != null) p.addMetadata("placeholder", placeholder)
}

private fun colorListProp(id: String, title: String, desc: String, vararg entries: Int) =
    Properties.simple(id, title, desc, entries, IntArray::class.java).also { p ->
        p.addMetadata("visualizer", Visualizer.ColorListVisualizer::class.java)
    }

private fun numberListProp(
    id: String,
    title: String,
    desc: String,
    vararg entries: Float,
    min: Float = 0f,
    max: Float = 100f,
    step: Float = 0f,
    slider: Boolean = false,
) = Properties.simple(id, title, desc, entries, FloatArray::class.java).also { p ->
    p.addMetadata(
        "visualizer",
        if (slider) Visualizer.SliderListVisualizer::class.java else Visualizer.NumberListVisualizer::class.java,
    )
    p.addMetadata("min", min)
    p.addMetadata("max", max)
    if (step > 0f) p.addMetadata("step", step)
}

private fun multiSelectProp(id: String, title: String, desc: String, vararg options: String, checkable: Boolean = true) =
    Properties.simple(id, title, desc, BooleanArray(options.size), BooleanArray::class.java).also { p ->
        p.addMetadata("visualizer", Visualizer.MultiSelectDropdownVisualizer::class.java)
        p.addMetadata("options", options.toTypedArray())
        if (!checkable) p.addMetadata("checkable", false)
    }

private fun buttonProp(id: String, title: String, desc: String, text: String, action: () -> Unit) =
    Properties.simple(id, title, desc, Runnable(action), Runnable::class.java).also { p ->
        p.addMetadata("visualizer", Visualizer.ButtonVisualizer::class.java)
        p.addMetadata("text", text)
        p.addMetadata("runnable", Runnable(action))
    }

private fun infoProp(id: String, title: String, desc: String) =
    Properties.simple(id, title, desc, "", String::class.java).also { p ->
        p.addMetadata("visualizer", Visualizer.InfoVisualizer::class.java)
    }

private fun accordion(id: String, title: String, desc: String? = null, icon: String? = null, build: Tree.() -> Unit): Tree {
    val t = Tree(id, title, desc, null)
    icon?.let { t.addMetadata("icon", it) }
    t.build()
    return t
}

private fun testModTree(id: String, title: String, category: Config.Category): Tree {
    val tree = Tree(id, title, null, null)
    tree.addMetadata("category", category)

    val general = Tree("general", "General", null, null).also { it.addMetadata("icon", "settings") }
    general.put(boolProp("enabled", "Enabled", "Enable or disable this mod", true, "star"))
    general.put(prop("opacity", "Opacity", "Rendering opacity", 1.0f))
    general.put(
        accordion("hud-position", "HUD Position", "Adjust where the HUD appears on screen", "cog") {
            put(boolProp("custom-pos", "Custom Position", "Override default HUD position", false))
            put(prop("x", "X", "Horizontal position", 0.5f))
            put(prop("y", "Y", "Vertical position", 0.5f))
        }
    )

    val display = Tree("display", "Display", null, null).also { it.addMetadata("icon", "paintbrush") }
    display.put(prop("scale", "Scale", "UI scale factor", 1.0f))
    display.put(boolProp("shadow", "Shadow", "Render with drop shadow", false, "star"))
    display.put(
        accordion("background", "Background", null, "paintbrush") {
            put(boolProp("show-bg", "Show Background", "Draw a background behind the HUD", true))
            put(prop("bg-opacity", "Opacity", "Background opacity", 0.5f))
            put(prop("padding", "Padding", "Background padding in pixels", 4))
        }
    )

    tree.put(general, display)
    return tree
}

/** One option of every type, for visual testing. */
private fun testControlsTree(): Tree {
    val tree = Tree("controls-demo", "Controls Demo", "A demo of the controls", null)
    tree.addMetadata("category", Config.Category.OTHER)

    val booleans = Tree("booleans", "Booleans", null, null).also { it.addMetadata("icon", "star") }
    booleans.put(boolProp("switch-on", "Switch (on)", "A switch that starts enabled", true))
    booleans.put(boolProp("switch-off", "Switch (off)", "A switch that starts disabled", false))
    booleans.put(checkboxProp("checkbox-on", "Checkbox (on)", "A checkbox that starts checked", true))
    booleans.put(checkboxProp("checkbox-off", "Checkbox (off)", "A checkbox that starts unchecked", false))
    booleans.put(
        accordion("accordion-with-head", "Accordion (with head)", "Toggling the switch collapses the body", "cog") {
            put(boolProp("head", "Master Toggle", "Controls whether this section is active", true))
            put(boolProp("sub-a", "Sub Option A", "A nested boolean", false))
            put(boolProp("sub-b", "Sub Option B", "Another nested boolean", true))
        }
    )
    booleans.put(
        accordion("accordion-no-head", "Accordion (no head)", "No toggle — click header to expand/collapse", "settings") {
            put(boolProp("sub-c", "Sub Option C", "A nested boolean", true))
            put(boolProp("sub-d", "Sub Option D", "Another nested boolean", false))
        }
    )

    val numbers = Tree("numbers", "Numbers & Sliders", null, null).also { it.addMetadata("icon", "settings") }
    numbers.put(sliderProp("volume", "Volume", "Master volume level", 75f, 0f, 100f, 5f))
    numbers.put(sliderProp("brightness", "Brightness", "Display brightness", 0.8f, 0f, 1f))
    numbers.put(numberProp("fov", "Field of View", "Camera field of view in degrees", 90f, "°"))
    numbers.put(numberProp("render-dist", "Render Distance", "Chunk render distance", 12f, "chunks"))

    val text = Tree("text", "Text", null, null).also { it.addMetadata("icon", "paintbrush") }
    text.put(textProp("username", "Username", "Display name override", "Player", "Enter a name..."))
    text.put(textProp("prefix", "Chat Prefix", "Prefix added to chat messages", ""))
    text.put(fileProp("image", "Image", "Pick an image file", "", arrayOf(".png", ".jpg"), "Images"))
    text.put(fileProp("folder", "Directory", "Pick a folder", "", directory = true))

    val selectors = Tree("selectors", "Selectors", null, null).also { it.addMetadata("icon", "profiles") }
    selectors.put(dropdownProp("theme", "Theme", "UI colour theme", Theme.Dark))
    selectors.put(dropdownProp("difficulty", "Difficulty", "Game difficulty", Difficulty.Normal))
    selectors.put(radioProp("quality", "Quality Preset", "Rendering quality level", 1, "Low", "Medium", "High", "Ultra"))
    selectors.put(radioProp("corner-style", "Corner Style", "Shape of UI corners", 0, "Sharp", "Rounded", "Pill"))

    val lists = Tree("lists", "Lists", null, null).also { it.addMetadata("icon", "layers") }
    lists.put(draggableListProp("drag-only", "Drag to Reorder", "No checkboxes — drag only", "Alpha", "Beta", "Gamma", "Delta", "Epsilon"))
    lists.put(draggableListProp("drag-check", "Drag & Select", "Drag to reorder, check to enable", "Alpha", "Beta", "Gamma", "Delta", "Epsilon", checkable = true))
    lists.put(multiSelectProp("multi-check", "Multi-Select", "Checkbox multi-select dropdown", "Apples", "Bananas", "Cherries", "Dates", "Elderberries"))
    lists.put(multiSelectProp("multi-plain", "List Picker", "Single-select list dropdown", "Easy", "Normal", "Hard", "Extreme", checkable = false))
    lists.put(textListProp("text-list", "Ignored Players", "Type entries, add and remove rows", "Notch", "Herobrine", placeholder = "Enter a name..."))
    lists.put(textListProp("text-list-regex", "Allowed Servers", "Validated, max 3 entries", "hypixel.net", placeholder = "example.com", regex = "^[\\w.-]+\\.[a-z]{2,}$", maxEntries = 3, reorderable = false))
    lists.put(itemListProp("item-list", "Tracked Items", "Search and select several Minecraft items", "minecraft:diamond_sword", "minecraft:golden_apple"))
    lists.put(itemListProp("single-item", "Primary Item", "A single-item selector", "minecraft:ender_pearl", maxEntries = 1))
    lists.put(fileListProp("file-list", "Resource Packs", "Pick several files", placeholder = "Select a file..."))
    lists.put(fileListProp("dir-list", "Search Folders", "Pick several folders", directory = true, placeholder = "Select a folder..."))
    lists.put(colorListProp("color-list", "Palette", "One picker per entry", 0xFFFF5555.toInt(), 0xFF55FF55.toInt(), 0x8055AAFF.toInt()))
    lists.put(numberListProp("number-list", "Stack Sizes", "Number inputs", 1f, 16f, 64f, min = 0f, max = 64f))
    lists.put(numberListProp("slider-list", "Layer Opacities", "A slider per entry", 0.25f, 0.5f, 1f, min = 0f, max = 1f, step = 0.05f, slider = true))

    val actions = Tree("actions", "Actions & Info", null, null).also { it.addMetadata("icon", "cog") }
    actions.put(infoProp("info-tip", "Tip", "This is an informational option with no control."))
    actions.put(buttonProp("reset-settings", "Reset Settings", "Restore all defaults", "Reset") {
        println("Reset clicked")
    })
    actions.put(buttonProp("export", "Export Config", "Save config to a file", "Export") {
        println("Export clicked")
    })

    tree.put(booleans, numbers, text, selectors, lists, actions)
    return tree
}

private fun testPrefsTree(): Tree {
    val tree = Tree(PREFERENCES_ID, "Preferences", null, null)
    tree.addMetadata("category", Config.Category.OTHER)

    val general = Tree("general", "General", null, null).also { it.addMetadata("icon", "settings") }
    general.put(textProp("language", "Language", "Interface language", "English"))
    general.put(boolProp("animations", "Animations", "Enable UI animations", true, "star"))
    general.put(boolProp("snapshots", "Snapshot Updates", "Receive snapshot update notifications", false))

    val privacy = Tree("privacy", "Privacy", null, null).also { it.addMetadata("icon", "profiles") }
    privacy.put(
        accordion("telemetry", "Telemetry", null, "cog") {
            put(boolProp("analytics", "Usage Analytics", "Send anonymous usage data to Polyfrost", true))
            put(boolProp("crash-reports", "Crash Reports", "Automatically send crash reports", true))
        }
    )
    privacy.put(boolProp("search-history", "Search History", "Save search history within the session", true))

    tree.put(general, privacy)
    return tree
}
