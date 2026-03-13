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
import org.polyfrost.oneconfig.internal.ui.screens.PREFERENCES_ID

private enum class Theme { Dark, Light, System }
private enum class Difficulty { Easy, Normal, Hard, Extreme }

fun main() {
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

private fun <E : Enum<E>> dropdownProp(id: String, title: String, desc: String, value: E) =
    Properties.simple(id, title, desc, value, value::class.java as Class<E>?).also { p ->
        p.addMetadata("visualizer", Visualizer.DropdownVisualizer::class.java)
    }

private fun radioProp(id: String, title: String, desc: String, value: Int, vararg options: String) =
    Properties.simple(id, title, desc, value, Int::class.java).also { p ->
        p.addMetadata("visualizer", Visualizer.RadioVisualizer::class.java)
        p.addMetadata("options", arrayOf(*options))
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

    val selectors = Tree("selectors", "Selectors", null, null).also { it.addMetadata("icon", "profiles") }
    selectors.put(dropdownProp("theme", "Theme", "UI colour theme", Theme.Dark))
    selectors.put(dropdownProp("difficulty", "Difficulty", "Game difficulty", Difficulty.Normal))
    selectors.put(radioProp("quality", "Quality Preset", "Rendering quality level", 1, "Low", "Medium", "High", "Ultra"))
    selectors.put(radioProp("corner-style", "Corner Style", "Shape of UI corners", 0, "Sharp", "Rounded", "Pill"))

    val actions = Tree("actions", "Actions & Info", null, null).also { it.addMetadata("icon", "cog") }
    actions.put(infoProp("info-tip", "Tip", "This is an informational option with no control."))
    actions.put(buttonProp("reset-settings", "Reset Settings", "Restore all defaults", "Reset") {
        println("Reset clicked")
    })
    actions.put(buttonProp("export", "Export Config", "Save config to a file", "Export") {
        println("Export clicked")
    })

    tree.put(booleans, numbers, text, selectors, actions)
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
