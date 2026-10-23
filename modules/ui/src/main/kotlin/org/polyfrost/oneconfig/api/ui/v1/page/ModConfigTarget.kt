package org.polyfrost.oneconfig.api.ui.v1.page

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
class ModConfigTarget internal constructor(val modId: String, val category: String?) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is ModConfigTarget && other.modId == modId && other.category == category)

    override fun hashCode(): Int = 31 * modId.hashCode() + (category?.hashCode() ?: 0)

    override fun toString(): String = "ModConfigTarget(modId=$modId, category=$category)"
}
