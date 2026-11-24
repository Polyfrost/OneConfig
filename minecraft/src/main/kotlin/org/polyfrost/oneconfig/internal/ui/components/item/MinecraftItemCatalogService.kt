package org.polyfrost.oneconfig.internal.ui.components.item

import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.polyfrost.compose.minecraft.mc.McItemIconHandle
import org.polyfrost.compose.minecraft.mc.McItemIcons
import org.polyfrost.compose.render.RenderContext

//? if > 1.8.9 {
import net.minecraft.core.registries.BuiltInRegistries
//?}

class MinecraftItemCatalogService : ItemCatalogService {
    private data class RegistryEntry(val item: Item, val id: String)

    private class Icon(private val handle: McItemIconHandle) : ItemIconHandle {
        override fun draw(ctx: RenderContext, x: Float, y: Float, w: Float, h: Float, alpha: Float): Boolean =
            handle.draw(ctx, x, y, w, h, alpha)

        override fun close() = handle.close()
    }

    private val descriptors: List<ItemDescriptor> by lazy {
        entries.map { entry ->
            //? if > 1.8.9 {
            ItemDescriptor(entry.id, Component.translatable(entry.item.descriptionId).string)
            //?} else
            //ItemDescriptor(entry.id, entry.item.getName(ItemStack(entry.item)))
        }
    }

    override val iconsAvailable: Boolean get() = McItemIcons.available

    override fun items(): List<ItemDescriptor> = descriptors

    override fun requestIcons() = McItemIcons.requestAvailability()

    override fun openIcon(id: String): ItemIconHandle? {
        val item = item(id) ?: return null
        return Icon(McItemIcons.open(ItemStack(item)))
    }

    companion object {
        private val entries: List<RegistryEntry> by lazy {
            //? if > 1.8.9 {
            BuiltInRegistries.ITEM.mapNotNull { item ->
                if (item === Items.AIR) return@mapNotNull null
                RegistryEntry(item, BuiltInRegistries.ITEM.getKey(item).toString())
            }
            //?} else {
            /*Item.REGISTRY.mapNotNull { item ->
                val id = Item.REGISTRY.getKey(item)?.toString() ?: return@mapNotNull null
                RegistryEntry(item, id)
            }
            *///?}
        }

        private val entriesById: Map<String, RegistryEntry> by lazy { entries.associateBy(RegistryEntry::id) }

        @JvmStatic
        fun item(id: String): Item? = entriesById[id]?.item
    }
}
