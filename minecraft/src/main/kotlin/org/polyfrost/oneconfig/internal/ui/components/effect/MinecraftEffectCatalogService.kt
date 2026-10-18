package org.polyfrost.oneconfig.internal.ui.components.effect

import org.jetbrains.skia.Image
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading
import org.slf4j.LoggerFactory

//? if > 1.8.9 {
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.effect.MobEffectCategory
//?} else {
/*import kotlin.math.roundToInt
import net.minecraft.client.resource.language.I18n
import net.minecraft.entity.living.effect.StatusEffect
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers
import net.ornithemc.osl.resource.loader.api.resource.manager.ResourceManager
import org.jetbrains.skia.Bitmap
*///?}

class MinecraftEffectCatalogService : EffectCatalogService {
    private val lock = Any()

    @Volatile
    private var catalogCache: List<EffectDescriptor>? = null
    private val iconCache = HashMap<String, Image?>()

    //? if = 1.8.9
    //private var atlasLoaded = false

    init {
        EventManager.register(ResourceFinishedLoading::class.java, Runnable(::clearCaches))
    }

    private fun clearCaches() {
        synchronized(lock) {
            catalogCache = null
            iconCache.clear()
            //? if = 1.8.9
            //atlasLoaded = false
        }
        EffectCatalog.invalidate()
    }

    override fun effects(): List<EffectDescriptor> {
        catalogCache?.let { return it }
        return synchronized(lock) {
            catalogCache ?: buildCatalog().also { catalogCache = it }
        }
    }

    private fun buildCatalog(): List<EffectDescriptor> {
        //? if > 1.8.9 {
        val registry = BuiltInRegistries.MOB_EFFECT
        return registry.mapNotNull { effect ->
            val id = registry.getKey(effect) ?: return@mapNotNull null
            val category = when (effect.category) {
                MobEffectCategory.BENEFICIAL -> EffectCategory.BENEFICIAL
                MobEffectCategory.NEUTRAL -> EffectCategory.NEUTRAL
                MobEffectCategory.HARMFUL -> EffectCategory.HARMFUL
            }
            EffectDescriptor(id.toString(), effect.displayName.string, category)
        }
        //?} else {
        /*return StatusEffect.getKeys().mapNotNull { key ->
            val effect = StatusEffect.get(key.toString()) ?: return@mapNotNull null
            val category = if (effect.isHarmful) EffectCategory.HARMFUL else EffectCategory.BENEFICIAL
            EffectDescriptor(key.toString(), I18n.translate(effect.translationKey), category)
        }
        *///?}
    }

    override fun icon(id: String): Image? = synchronized(lock) {
        //? if > 1.8.9 {
        if (iconCache.containsKey(id)) return iconCache[id]
        loadIcon(id).also { iconCache[id] = it }
        //?} else {
        /*if (!atlasLoaded) {
            atlasLoaded = true
            try {
                loadAtlas()
            } catch (e: Exception) {
                LOG.warn("Failed to load the status effect icons", e)
            }
        }
        iconCache[id]
        *///?}
    }

    //? if > 1.8.9 {
    private fun loadIcon(id: String): Image? {
        val namespace = id.substringBefore(':', "minecraft")
        val path = id.substringAfter(':')
        val texture = Identifier.tryBuild(namespace, "textures/mob_effect/$path.png") ?: return null
        return try {
            Minecraft.getInstance().resourceManager.getResource(texture).orElse(null)
                ?.open()?.use { Image.makeFromEncoded(it.readAllBytes()) }
        } catch (e: Exception) {
            LOG.warn("Failed to load the icon for status effect {}", id, e)
            null
        }
    }
    //?} else {
    /*private fun loadAtlas() {
        val texture = NamespacedIdentifiers.from("minecraft", "textures/gui/container/inventory.png")
        val atlas = ResourceManager.client().getResource(texture).orElse(null)
            ?.open()?.use { Image.makeFromEncoded(it.readBytes()) }
        if (atlas == null) {
            LOG.warn("Failed to read the status effect icon atlas {}", texture)
            return
        }
        atlas.use {
            val scale = atlas.width / 256f
            val size = (ICON_SIZE * scale).roundToInt()
            Bitmap().use { bitmap ->
                bitmap.allocN32Pixels(size, size)
                for (key in StatusEffect.getKeys()) {
                    val effect = StatusEffect.get(key.toString()) ?: continue
                    if (!effect.hasIcon()) continue
                    val x = ((effect.iconIndex % 8 * ICON_SIZE) * scale).roundToInt()
                    val y = ((198 + effect.iconIndex / 8 * ICON_SIZE) * scale).roundToInt()
                    bitmap.erase(0)
                    if (atlas.readPixels(bitmap, x, y)) iconCache[key.toString()] = Image.makeFromBitmap(bitmap)
                }
            }
        }
    }
    *///?}

    private companion object {
        private val LOG = LoggerFactory.getLogger("OneConfig/Effect Catalog")

        //? if = 1.8.9
        //private const val ICON_SIZE = 18
    }
}
