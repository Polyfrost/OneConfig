package org.polyfrost.oneconfig.api.ui.v1.search

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.requireSearchBridge
import org.polyfrost.oneconfig.api.ui.v1.internal.searchBridge

@ExperimentalOneConfigUiApi
public object SearchProviders {
    @JvmStatic
    public fun register(provider: SearchProvider) {
        requireSearchBridge().register(provider)
    }

    @JvmStatic
    public fun unregister(provider: SearchProvider): Boolean = searchBridge()?.unregister(provider) ?: false

    @JvmStatic
    public fun documentIds(): Set<String> = searchBridge()?.documentIds() ?: emptySet()

    @JvmStatic
    public fun document(id: String): SearchDocument? = searchBridge()?.document(id)
}
