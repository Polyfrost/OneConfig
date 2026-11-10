package org.polyfrost.oneconfig.api.ui.v1

public class ModCardType @JvmOverloads constructor(
    public val id: String,
    public val title: String,
    public val icon: String? = null,
    public val priority: Int = 0,
    public val collapsedByDefault: Boolean = false,
) {
    override fun equals(other: Any?): Boolean = this === other || (other is ModCardType && other.id == id)

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        "ModCardType(id=$id, title=$title, priority=$priority, collapsedByDefault=$collapsedByDefault)"
}

public fun interface ModCardTypeResolver {
    public fun typeIdFor(modId: String): String?
}
