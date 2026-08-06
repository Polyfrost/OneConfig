package org.polyfrost.oneconfig.api.ui.v1

class ModCardType @JvmOverloads constructor(
    val id: String,
    val title: String,
    val icon: String? = null,
    val priority: Int = 0,
) {
    override fun equals(other: Any?) = this === other || (other is ModCardType && other.id == id)

    override fun hashCode() = id.hashCode()

    override fun toString() = "ModCardType(id=$id, title=$title, priority=$priority)"
}

fun interface ModCardTypeResolver {
    fun typeIdFor(modId: String): String?
}
