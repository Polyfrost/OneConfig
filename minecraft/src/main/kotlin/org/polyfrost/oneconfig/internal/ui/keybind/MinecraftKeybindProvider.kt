package org.polyfrost.oneconfig.internal.ui.keybind

import com.mojang.blaze3d.platform.InputConstants
import java.util.IdentityHashMap
import java.util.function.Consumer
import java.util.function.Supplier
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeyModifiers
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind

object MinecraftKeybindProvider : KeybindGroupProvider {
    private val properties = IdentityHashMap<KeyMapping, Property<OneConfigKeybind>>()
    private val modIconCache = mutableMapOf<String, String?>()

    private data class MinecraftKeybindGroup(
        val category: String,
        val ownerId: String,
        val title: String,
        val icon: String?,
    )

    private data class DiscoveredKeybind(
        val group: MinecraftKeybindGroup,
        val entry: KeybindEntry,
    )

    override fun groups(): List<KeybindGroup> {
        val minecraft = Minecraft.getInstance()
        val modsById = ModInfo.loadedMods.associateBy { it.id.lowercase() }
        val entriesByGroup = minecraft.options.keyMappings
            .asSequence()
            .filterNot { isVanillaKeyName(it.name) }
            .map { mapping ->
                val owner = ownerMod(mapping, modsById)
                val category = categoryLabel(mapping)
                DiscoveredKeybind(
                    group = MinecraftKeybindGroup(
                        category = category,
                        ownerId = owner?.id ?: "unknown",
                        title = groupTitle(category, owner),
                        icon = modIcon(owner),
                    ),
                    entry = KeybindEntry(
                        path = "minecraft.${mapping.name}",
                        category = "Minecraft",
                        subcategory = "Controls",
                        prop = properties.getOrPut(mapping) { propertyFor(mapping) },
                    ),
                )
            }
            .groupBy { it.group }

        return entriesByGroup
            .toSortedMap(compareBy<MinecraftKeybindGroup> { it.category }.thenBy { it.ownerId })
            .mapNotNull { (group, discovered) ->
                val entries = discovered.map { it.entry }.sortedBy { it.prop.title?.toString() ?: it.path }
                if (entries.isEmpty()) return@mapNotNull null
                KeybindGroup(
                    modId = "minecraft-keybinds.${group.ownerId}.${sanitizeId(group.category)}",
                    modTitle = group.title,
                    modIcon = group.icon,
                    entries = entries,
                )
            }
    }

    private fun propertyFor(mapping: KeyMapping): Property<OneConfigKeybind> {
        val prop = Properties.functional(
            Supplier { mapping.toOneConfigKeybind() },
            Consumer { keybind -> mapping.applyOneConfigKeybind(keybind) },
            "minecraft.${mapping.name}",
            translate(mapping.name),
            null,
            OneConfigKeybind::class.java,
        )
        prop.addMetadata("visualizer", Visualizer.KeybindVisualizer::class.java)
        prop.addMetadata("category", categoryLabel(mapping))
        prop.addMetadata("subcategory", "Minecraft Controls")
        return prop
    }

    private fun ownerMod(mapping: KeyMapping, modsById: Map<String, ModInfo>): ModInfo? {
        return ownerCandidates(mapping.name)
            .map { it.lowercase() }
            .firstNotNullOfOrNull { modsById[it] }
    }

    private fun ownerCandidates(keyName: String): Sequence<String> = sequence {
        if (':' in keyName) {
            val beforeNamespaceSeparator = keyName.substringBefore(':')
            yield(beforeNamespaceSeparator.substringAfterLast('.'))
            yield(beforeNamespaceSeparator)
        }
        if (keyName.startsWith("key.")) {
            yield(keyName.removePrefix("key.").substringBefore('.').substringBefore(':'))
        }
        yield(keyName.substringBefore('.'))
    }

    private fun modIcon(owner: ModInfo?): String? =
        owner?.let { modIconCache.getOrPut(it.id) { it.extractIconFile() } }

    private fun groupTitle(category: String, owner: ModInfo?): String {
        val ownerName = owner?.name?.takeUnless { it.isBlank() } ?: return category
        if (category.contains(ownerName, ignoreCase = true)) return category
        return "$category ($ownerName)"
    }

    private fun sanitizeId(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9._-]"), "_")

    private fun KeyMapping.toOneConfigKeybind(): OneConfigKeybind {
        val key = runCatching { InputConstants.getKey(saveString()) }.getOrDefault(InputConstants.UNKNOWN)
        return when (key.type) {
            InputConstants.Type.KEYSYM -> {
                if (key.value > 0) OneConfigKeybind(intArrayOf(key.value), null, KeyModifiers.NONE, 0L) { true }
                else OneConfigKeybind(null, null, KeyModifiers.NONE, 0L) { true }
            }
            InputConstants.Type.MOUSE -> {
                if (key.value >= 0) OneConfigKeybind(null, intArrayOf(key.value), KeyModifiers.NONE, 0L) { true }
                else OneConfigKeybind(null, null, KeyModifiers.NONE, 0L) { true }
            }
            else -> OneConfigKeybind(null, null, KeyModifiers.NONE, 0L) { true }
        }
    }

    private fun KeyMapping.applyOneConfigKeybind(keybind: OneConfigKeybind?) {
        val mouseButtons = keybind?.mouseBtns
        val keyCodes = keybind?.keyCodes
        val key = when {
            keybind == null || !keybind.isBound -> InputConstants.UNKNOWN
            mouseButtons?.firstOrNull { it >= 0 } != null -> InputConstants.Type.MOUSE.getOrCreate(mouseButtons.first { it >= 0 })
            keyCodes?.firstOrNull { it > 0 } != null -> InputConstants.Type.KEYSYM.getOrCreate(keyCodes.first { it > 0 })
            else -> InputConstants.UNKNOWN
        }
        setKey(key)
        KeyMapping.resetMapping()
        Minecraft.getInstance().options.save()
    }

    private fun categoryLabel(mapping: KeyMapping): String {
        //? if >= 1.21.10 {
        return runCatching { mapping.category.label().string }.getOrNull()
            ?.takeUnless { it.isBlank() }
            ?: mapping.category.toString()
        //?} else {
        /*return translate(mapping.category)
        *///?}
    }

    private fun translate(key: String): String {
        val translated = runCatching { Component.translatable(key).string }.getOrNull()
        return translated?.takeUnless { it == key || it.isBlank() } ?: key
    }

    private fun isVanillaKeyName(name: String): Boolean {
        return name in VANILLA_KEY_NAMES || name.startsWith("key.debug.")
    }

    private val VANILLA_KEY_NAMES = setOf(
        "key.attack",
        "key.use",
        "key.forward",
        "key.left",
        "key.back",
        "key.right",
        "key.jump",
        "key.sneak",
        "key.sprint",
        "key.drop",
        "key.inventory",
        "key.chat",
        "key.playerlist",
        "key.pickItem",
        "key.command",
        "key.socialInteractions",
        "key.screenshot",
        "key.togglePerspective",
        "key.smoothCamera",
        "key.fullscreen",
        "key.spectatorOutlines",
        "key.swapOffhand",
        "key.saveToolbarActivator",
        "key.loadToolbarActivator",
        "key.advancements",
        "key.pause",
        "key.narrator",
        "key.quickActions",
        "key.spectatorHotbar",
        "key.toggleGui",
        "key.toggleSpectatorShaderEffects",
    ) + (1..9).map { "key.hotbar.$it" }
}
