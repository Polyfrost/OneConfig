package org.polyfrost.oneconfig.api.ui.v1.page

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
class PageGroup internal constructor(val id: String, val title: PageText, val order: Int) {
    override fun equals(other: Any?): Boolean = this === other || (other is PageGroup && other.id == id)

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "PageGroup(id=$id, order=$order)"

    companion object {
        @JvmField
        val MODS_AND_OPTIONS: PageGroup =
            PageGroup("oneconfig:mods_and_options", PageText.translatable("oneconfig.pagegroup.mods_and_options", "Mods & Options"), 100)

        @JvmField
        val PERSONALIZATION: PageGroup =
            PageGroup("oneconfig:personalization", PageText.translatable("oneconfig.pagegroup.personalization", "Personalization"), 200)

        @JvmField
        val ONECONFIG: PageGroup =
            PageGroup("oneconfig:oneconfig", PageText.translatable("oneconfig.pagegroup.oneconfig", "OneConfig"), 300)

        @JvmStatic
        fun create(id: String, title: PageText, order: Int): PageGroup {
            OneConfigPages.requireNamespacedId(id)
            return PageGroup(id, title, order)
        }
    }
}
