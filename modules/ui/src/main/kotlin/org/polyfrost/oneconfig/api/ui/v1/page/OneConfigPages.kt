package org.polyfrost.oneconfig.api.ui.v1.page

import androidx.compose.runtime.mutableStateListOf
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
object OneConfigPages {
    private val ID = Regex("[a-z0-9_.-]+:[a-z0-9_./-]+")

    private val lock = Any()

    private val pages = mutableStateListOf<OneConfigPage>()

    @JvmStatic
    fun register(page: OneConfigPage) {
        synchronized(lock) {
            require(pages.none { it.id == page.id }) { "A page with id ${page.id} is already registered" }
            pages.add(page)
        }
    }

    @JvmStatic
    fun unregister(id: String): Boolean = synchronized(lock) { pages.removeAll { it.id == id } }

    @JvmStatic
    fun all(): List<OneConfigPage> =
        pages.toList().sortedWith(compareBy<OneConfigPage>({ it.group.order }, { it.group.id }, { it.order }))

    @JvmStatic
    fun byId(id: String): OneConfigPage? = pages.toList().firstOrNull { it.id == id }

    @JvmStatic
    fun modConfig(modId: String): ModConfigTarget = ModConfigTarget(modId, null)

    @JvmStatic
    fun modConfig(modId: String, category: String?): ModConfigTarget = ModConfigTarget(modId, category)

    internal fun requireNamespacedId(id: String) {
        require(ID.matches(id)) { "Id $id must be namespaced like \"mymod:page\" using lowercase letters, digits and _ . - /" }
    }
}
