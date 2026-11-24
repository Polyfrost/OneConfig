@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1

import org.polyfrost.oneconfig.api.ui.v1.page.PageText
import org.polyfrost.oneconfig.api.ui.v1.page.translate

public class ModCardType @ExperimentalOneConfigUiApi @JvmOverloads constructor(
    public val id: String,
    title: PageText,
    public val icon: String? = null,
    public val priority: Int = 0,
    public val collapsedByDefault: Boolean = false,
) {
    @JvmOverloads
    public constructor(
        id: String,
        title: String,
        icon: String? = null,
        priority: Int = 0,
        collapsedByDefault: Boolean = false,
    ) : this(id, PageText.literal(title), icon, priority, collapsedByDefault)

    @ExperimentalOneConfigUiApi
    public val titleText: PageText = title

    public val title: String get() = titleText.translate()

    override fun equals(other: Any?): Boolean = this === other || (other is ModCardType && other.id == id)

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        "ModCardType(id=$id, title=$title, priority=$priority, collapsedByDefault=$collapsedByDefault)"
}

public fun interface ModCardTypeResolver {
    public fun typeIdFor(modId: String): String?
}
