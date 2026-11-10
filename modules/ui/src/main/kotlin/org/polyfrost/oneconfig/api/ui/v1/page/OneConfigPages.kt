package org.polyfrost.oneconfig.api.ui.v1.page

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public object OneConfigPages {
    private val ID = Regex("[a-z0-9_.-]+:[a-z0-9_./-]+")

    private val lock = Any()

    private val pages = mutableStateListOf<OneConfigPage>()

    @JvmStatic
    public fun register(page: OneConfigPage) {
        synchronized(lock) {
            require(pages.none { it.id == page.id }) { "A page with id ${page.id} is already registered" }
            Snapshot.withMutableSnapshot { pages.add(page) }
        }
    }

    @JvmStatic
    public fun unregister(id: String): Boolean = synchronized(lock) { Snapshot.withMutableSnapshot { pages.removeAll { it.id == id } } }

    @JvmStatic
    public fun all(): List<OneConfigPage> =
        pages.toList().sortedWith(compareBy<OneConfigPage>({ it.group.order }, { it.group.id }, { it.order }))

    @JvmStatic
    public fun byId(id: String): OneConfigPage? = pages.toList().firstOrNull { it.id == id }

    @JvmStatic
    public fun modConfig(modId: String): ModConfigTarget = ModConfigTarget(modId, null)

    @JvmStatic
    public fun modConfig(modId: String, category: String?): ModConfigTarget = ModConfigTarget(modId, category)

    internal fun requireNamespacedId(id: String) {
        require(ID.matches(id)) { "Id $id must be namespaced like \"mymod:page\" using lowercase letters, digits and _ . - /" }
    }
}
