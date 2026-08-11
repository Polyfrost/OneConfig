package org.polyfrost.oneconfig.internal.compat

//? stella_compat {
import co.stellarskys.stella.Stella
import co.stellarskys.stella.api.config.core.*
import co.stellarskys.stella.hud.HUDElement
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.hud.HUDManager.customRenderers
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.height
import co.stellarskys.stella.utils.render.Render2D.width
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.backend.Backend
import org.polyfrost.oneconfig.api.config.v1.dsl.category
import org.polyfrost.oneconfig.api.config.v1.dsl.saveFunction
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.api.config.v1.dsl.visualizer
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.hud.v1.OneConfigHudWrapper
import org.polyfrost.oneconfig.api.hud.v1.events.HudEditorToggleEvent
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeyModifiers
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer
import java.awt.Color
import java.util.function.Consumer

object StellaCompat {
    private val LOGGER = LogManager.getLogger("OneConfig/Stella-Compat")
    private val hudProperties = mutableMapOf<String, Property<*>>()
    private val tree = Tree.tree()
    private var initialized = false

    @JvmStatic
    fun initialize() {
        CompatLoader.requireTranslations(skip = true) {
            ensureRegistered()
        }
    }

    fun ensureRegistered() {
        if (initialized) return
        initialized = true

        CompatLoader.nativeLoadedConfigs.add(Stella.NAMESPACE)

        try {
            addConfigTree()
            addHuds()
        } catch (_: Throwable) {
            fallbackTree()
            return
        }

        config.registerListener { _, _ -> revaluateDisplays() }

        EventManager.register(HudEditorToggleEvent::class.java, Consumer { event ->
            HUDManager.shouldRenderHuds = !event.open
            if (!event.open) {
                runCatching { HUDManager.saveAllLayouts() }
                    .onFailure { LOGGER.warn("Failed to save Stella HUD layouts", it) }
                runCatching { config.save() }
                    .onFailure { LOGGER.warn("Failed to save Stella config", it) }
            }
        })
    }

    private fun revaluateDisplays() {
        for (node in tree.map.values) {
            (node as? Property<*>)?.revaluateDisplay()
        }
        for (property in hudProperties.values) {
            property.revaluateDisplay()
        }
    }

    private fun addConfigTree() {
        val info = ModInfo.loadedMods.firstOrNull { it.id == "stella" }

        tree.id = Stella.NAMESPACE
        tree.title = "Stella"
        tree.saveFunction = { config.save() }
        info?.extractIconFile()?.let { tree.addMetadata("icon_path", it) }

        for ((catName, category) in config.categories) {
            for (subcategory in category.subcategories.values) {
                buildProperty(subcategory.configName, subcategory)?.let { property ->
                    property.category = catName
                    property.subcategory = subcategory.subName
                    hookShouldShow(property, subcategory)
                    tree.put(property)
                }

                for ((key, element) in subcategory.elements) {
                    val property = buildProperty(key, element) ?: continue
                    property.category = catName
                    property.subcategory = subcategory.subName
                    hookShouldShow(property, element)
                    tree.put(property)
                }
            }
        }

        CompatSnapshots.register(tree)
    }

    fun fallbackTree() {
        val fallback = Tree.tree()
        fallback.id = Stella.NAMESPACE
        fallback.title = "Stella"
        ModInfo.loadedMods.firstOrNull { it.id == "stella" }?.extractIconFile()?.let { fallback.addMetadata("icon_path", it) }
        fallback.addMetadata("on_click") { runCatching { config.open() } }
        fallback.addMetadata(Backend.UI_ONLY_METADATA, true)
        fallback.addMetadata(Backend.UI_PLACEHOLDER_METADATA, true)
        ConfigManager.active().register(fallback)
    }

    private fun buildProperty(key: String, element: ConfigElement): Property<*>? {
        if (element.configName.isBlank()) return null

        val name = element.name
        val desc = element.description

        return when (element) {
            is Toggle -> Properties.functional<Boolean>(
                getter = { element.value as Boolean },
                setter = { element.value = it },
                id = key, name = name, description = desc, type = Boolean::class.javaPrimitiveType
            ).apply { visualizer = Visualizer.SwitchVisualizer::class.java }

            is ConfigSubcategory -> Properties.functional<Boolean>(
                getter = { element.value as Boolean },
                setter = { element.value = it },
                id = key, name = "Enable", description = desc, type = Boolean::class.javaPrimitiveType
            ).apply { visualizer = Visualizer.SwitchVisualizer::class.java }

            is StepSlider -> Properties.functional(
                getter = { element.value as Int },
                setter = { element.value = it },
                id = key, name = name, description = desc, type = Int::class.javaPrimitiveType,
            ).apply {
                visualizer = Visualizer.SliderVisualizer::class.java
                addMetadata("min", element.min.toFloat())
                addMetadata("max", element.max.toFloat())
                addMetadata("step", element.step.toFloat())
            }

            is Slider -> Properties.functional(
                getter = { element.value as Float },
                setter = { element.value = it },
                id = key, name = name, description = desc, type = Float::class.javaPrimitiveType,
            ).apply {
                visualizer = Visualizer.SliderVisualizer::class.java
                addMetadata("min", element.min)
                addMetadata("max", element.max)
            }

            is Dropdown -> Properties.functional(
                getter = { element.value as Int },
                setter = { element.value = it },
                id = key, name = name, description = desc, type = Int::class.javaPrimitiveType,
            ).apply {
                visualizer = Visualizer.DropdownVisualizer::class.java
                addMetadata("options", element.options)
            }

            is ColorPicker -> Properties.functional(
                getter = { element.value as Color },
                setter = { element.value = it },
                id = key, name = name, description = desc, type = Color::class.java,
            ).apply { visualizer = Visualizer.ColorVisualizer::class.java }

            is TextInput -> Properties.functional(
                getter = { element.value as String },
                setter = { element.value = it },
                id = key, name = name, description = desc, type = String::class.java,
            ).apply { visualizer = Visualizer.TextVisualizer::class.java }

            is Button -> Properties.dummy(id = key, name = name, description = desc).apply {
                visualizer = Visualizer.ButtonVisualizer::class.java
                addMetadata("runnable", Runnable { element.onClick?.invoke() })
            }

            is TextParagraph -> Properties.dummy(id = key, name = name, description = desc)
                .apply { visualizer = Visualizer.InfoVisualizer::class.java }

            is Keybind -> {
                val handler = element.value as Keybind.Handler
                Properties.functional(
                    getter = {
                        val code = handler.keyCode()
                        OneConfigKeybind(if (code > 0) intArrayOf(code) else null, null, KeyModifiers.NONE, 0L) { false }
                    },
                    setter = { kb ->
                        element.value = kb.keyCodes?.firstOrNull() ?: 0
                        revaluateDisplays()
                    },
                    id = key, name = name, description = desc, type = OneConfigKeybind::class.java,
                ).apply { visualizer = Visualizer.KeybindVisualizer::class.java }
            }
            else -> null
        }
    }

    private fun hookShouldShow (property: Property<*>, element: ConfigElement) {
        val shouldShow = element.showIf ?: return
        property.addDisplayCondition {
            if (shouldShow(config)) Property.Display.SHOWN else Property.Display.HIDDEN
        }
    }

    private class StellaHudWrapper(private val element: HUDElement): OneConfigHudWrapper {
        private val confElement: ConfigElement? get() =  config.elementMap[element.configKey]

        override var id: String = "stella/${element.id}"
        override var name: String = confElement?.name ?: element.id
        override var modId: String = Stella.NAMESPACE

        override var x: Float
            get() = element.x
            set(value) { element.x = value }

        override var y: Float
            get() = element.y
            set(value) { element.y = value }

        override var scale: Float
            get() = element.scale
            set(value) { element.scale = value }

        override var hidden: Boolean
            get() = !element.isEnabled()
            set(value) { confElement?.value = !value }

        override var scaledWidth: Float
            get() = element.width * element.scale
            set(_) {}

        override var scaledHeight: Float
            get() = element.height * element.scale
            set(_) {}

        override fun linkedProperties(): List<Property<*>> = buildHudProperties(element)
    }

    private fun addHuds() {
        for (element in HUDManager.elements.values) {
            runCatching { StellaHudWrapper(element).register() }
                .onFailure { LOGGER.error("Failed to register Stella HUD '${element.id}'", it) }
        }

        CompatOverlayRenderer.register(::renderExamples)
    }

    private fun renderExamples(ctx: GuiGraphicsExtractor) {
        HUDManager.elements.values.forEach { element ->
            if (!element.isEnabled()) return@forEach

            ctx.pose().pushMatrix()
            ctx.pose().translate(element.x, element.y)
            ctx.pose().scale(element.scale, element.scale)

            val custom = customRenderers[element.id]
            if (custom != null) custom(ctx)
            else {
                if (element.width == 0 && element.height == 0) {
                    element.width = element.text.width() + 4
                    element.height = element.text.height() + 6
                }

                Render2D.drawString(ctx, element.text, 2, 3, shadow = false)
            }

            ctx.pose().popMatrix()
        }
    }

    private fun buildHudProperties(element: HUDElement): List<Property<*>> {
        val dynamic = element.dynamicVisibility
        val toggle = element.configKey?.let { buildOrGetProp(it, dynamic) }
        val related = element.related.mapNotNull { buildOrGetProp(it, dynamic) }
        return listOfNotNull(toggle) + related
    }

    private fun buildOrGetProp(key: String, dynamic: Boolean): Property<*>? {
        hudProperties[key]?.let { return it }
        val element = config.elementMap[key] ?: return null
        val property = buildProperty(key, element) ?: return null
        if (dynamic) hookShouldShow(property, element)
        hudProperties[key] = property
        return property
    }
}
//? }
