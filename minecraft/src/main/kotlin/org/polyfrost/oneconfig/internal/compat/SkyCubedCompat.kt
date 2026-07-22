//? skycubed_compat {
/*package org.polyfrost.oneconfig.internal.compat

import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig
import com.teamresourceful.resourcefulconfig.api.types.elements.ResourcefulConfigEntryElement
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigObjectEntry
import me.owdding.lib.overlays.Overlay
import me.owdding.lib.overlays.Overlays
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.hud.v1.OneConfigHudWrapper
import org.polyfrost.oneconfig.api.hud.v1.events.HudEditorToggleEvent
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer
import java.lang.reflect.Field
import java.util.function.Consumer

object SkyCubedCompat {
    private val LOGGER = LogManager.getLogger("OneConfig/SkyCubed-Compat")

    private val OVERLAY_SETTINGS = mapOf(
        "InfoOverlay" to "info",
        "PlayerRpgOverlay" to "rpg",
        "TextOverlay" to "text",
        "SackOverlay" to "sack",
        "AttributeOverlay" to "attribute",
        "PowerOrbOverlay" to "powerOrb",
        "TrophyFishOverlay" to "trophyFish",
        "MinimapOverlay" to "map",
        "DungeonMapOverlay" to "dungeonmap",
        "PickUpLog" to "pickupLog",
        "CommissionsOverlay" to "commissions",
        "DialogueOverlay" to "npc",
        "VanillaBossbarOverlay" to "bossbar",
        "MovableItemText" to "itemtext",
    )

    private var initialized = false
    private var overlaysCategory: ResourcefulConfig? = null
    private var rootConfig: ResourcefulConfig? = null

    @JvmStatic
    fun initialize() {
        if (initialized) return
        initialized = true
        CompatLoader.requireTranslations(skip = true) { register() }
    }

    private fun register() {
        resolveConfig()

        val overlays = collectOverlays()
        var count = 0
        for (overlay in overlays) {
            runCatching {
                SkyCubedHudWrapper(overlay).register()
                count++
            }.onFailure { LOGGER.warn("Failed to register SkyCubed overlay ${overlay.name.string}", it) }
        }
        LOGGER.info("Registered {} SkyCubed overlays into the OneConfig HUD editor", count)

        EventManager.register(HudEditorToggleEvent::class.java, Consumer { event ->
            if (!event.open) runCatching { rootConfig?.save() }
                .onFailure { LOGGER.warn("Failed to save SkyCubed config", it) }
        })

        CompatOverlayRenderer.register(::renderExamples)
    }

    private val screenField: Field? by lazy {
        runCatching {
            Minecraft::class.java.declaredFields.firstOrNull { it.type == Screen::class.java }
                ?.apply { isAccessible = true }
        }.onFailure { LOGGER.warn("Could not resolve Minecraft.screen field for SkyCubed example mode", it) }
            .getOrNull()
    }

    private val editScreen: Any? by lazy {
        runCatching { Class.forName("me.owdding.lib.overlays.EditOverlaysScreen").getDeclaredConstructor().newInstance() }
            .onFailure { LOGGER.warn("Could not build SkyCubed edit screen for example mode", it) }
            .getOrNull()
    }

    private fun renderExamples(ctx: GuiGraphicsExtractor) {
        val overlays = collectOverlays()
        if (overlays.isEmpty()) return

        val mc = Minecraft.getInstance()
        val field = screenField
        val fake = editScreen
        val prev = if (field != null && fake != null) {
            val previous = field.get(mc)
            runCatching { field.set(mc, fake) }
            previous
        } else null
        try {
            for (overlay in overlays) {
                if (!overlay.enabled) continue
                val pos = overlay.position
                val pose = ctx.pose()
                pose.pushMatrix()
                pose.translate(pos.component1().toFloat(), pos.component2().toFloat())
                pose.scale(pos.scale, pos.scale)
                runCatching { overlay.extract(ctx, 0, 0, 0f) }
                pose.popMatrix()
            }
        } finally {
            if (field != null && fake != null) runCatching { field.set(mc, prev) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun collectOverlays(): List<Overlay> = runCatching {
        val field = Overlays::class.java.getDeclaredField("overlays").apply { isAccessible = true }
        (field.get(Overlays) as? List<Overlay>)?.toList() ?: emptyList()
    }.onFailure { LOGGER.warn("Failed to read SkyCubed overlays from the meowdding registry", it) }
        .getOrDefault(emptyList())

    fun configId(): String? = rootConfig?.id()

    private fun resolveConfig() {
        val root = RConfigCompat.registeredConfigs().firstOrNull { it.id().contains("skycubed", ignoreCase = true) }
        rootConfig = root
        overlaysCategory = root?.categories()?.values?.firstOrNull { category ->
            category.id().substringAfterLast('/') == "overlays"
        }
        if (overlaysCategory == null) {
            LOGGER.warn("Could not resolve SkyCubed 'overlays' rconfig category; settings will be geometry-only")
        }
    }

    fun buildSettings(overlay: Overlay): List<Property<*>> {
        val id = OVERLAY_SETTINGS[overlay.javaClass.simpleName] ?: return emptyList()
        val entry = objectEntry(id) ?: return emptyList()
        return runCatching { RConfigCompat.buildProperties(entry) }
            .onFailure { LOGGER.warn("Failed to build SkyCubed settings for '$id'", it) }
            .getOrDefault(emptyList())
    }

    private fun objectEntry(id: String): ResourcefulConfigObjectEntry? {
        val category = overlaysCategory ?: return null
        for (element in category.elements()) {
            if (element is ResourcefulConfigEntryElement && element.id() == id) {
                return element.entry() as? ResourcefulConfigObjectEntry
            }
        }
        return null
    }
}

class SkyCubedHudWrapper(private val overlay: Overlay) : OneConfigHudWrapper {
    private companion object {
        const val FALLBACK_WIDTH = 60f
        const val FALLBACK_HEIGHT = 24f
    }

    override var id: String = "skycubed/" +
        (overlay.javaClass.simpleName + "/" + overlay.name.string).replace(Regex("[^A-Za-z0-9]+"), "_")

    override var name: String = overlay.name.string

    override val modId: String? get() = SkyCubedCompat.configId()

    override var x: Float
        get() = overlay.position.component1().toFloat()
        set(value) { overlay.setX(value.toInt()) }

    override var y: Float
        get() = overlay.position.component2().toFloat()
        set(value) { overlay.setY(value.toInt()) }

    override var scale: Float
        get() = overlay.position.scale
        set(value) { overlay.setScale(value) }

    override var hidden: Boolean
        get() = enabledProperty?.get() == false
        set(value) { enabledProperty?.set(!value) }

    override var scaledWidth: Float
        get() {
            if (!overlay.enabled) return 0f
            val w = overlay.bounds.first
            return (if (w > 0) w.toFloat() else FALLBACK_WIDTH) * overlay.position.scale
        }
        set(_) {}

    override var scaledHeight: Float
        get() {
            if (!overlay.enabled) return 0f
            val h = overlay.bounds.second
            return (if (h > 0) h.toFloat() else FALLBACK_HEIGHT) * overlay.position.scale
        }
        set(_) {}

    private val cachedProperties: List<Property<*>> by lazy { SkyCubedCompat.buildSettings(overlay) }

    private val enabledProperty: Property<Boolean>? by lazy { CompatHudToggle.find(cachedProperties) }

    override fun linkedProperties(): List<Property<*>> = cachedProperties
}
*///? }
