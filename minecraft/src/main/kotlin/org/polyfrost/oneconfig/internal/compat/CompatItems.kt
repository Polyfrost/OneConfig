package org.polyfrost.oneconfig.internal.compat

//? if >= 1.20.1 {
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item

object CompatItems {
    @JvmStatic
    fun id(item: Any?): String? = (item as? Item)?.let { BuiltInRegistries.ITEM.getKey(it).toString() }

    @JvmStatic
    fun byId(id: Any?): Item? = id?.toString()?.let { s -> BuiltInRegistries.ITEM.firstOrNull { BuiltInRegistries.ITEM.getKey(it).toString() == s } }
}
//?}
