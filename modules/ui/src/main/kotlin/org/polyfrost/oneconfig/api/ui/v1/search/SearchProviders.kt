package org.polyfrost.oneconfig.api.ui.v1.search

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.requireSearchBridge
import org.polyfrost.oneconfig.api.ui.v1.internal.searchBridge

@ExperimentalOneConfigUiApi
object SearchProviders {
    @JvmStatic
    fun register(provider: SearchProvider) {
        requireSearchBridge().register(provider)
    }

    @JvmStatic
    fun unregister(provider: SearchProvider): Boolean = searchBridge()?.unregister(provider) ?: false

    @JvmStatic
    fun documentIds(): Set<String> = searchBridge()?.documentIds() ?: emptySet()

    @JvmStatic
    fun document(id: String): SearchDocument? = searchBridge()?.document(id)
}
