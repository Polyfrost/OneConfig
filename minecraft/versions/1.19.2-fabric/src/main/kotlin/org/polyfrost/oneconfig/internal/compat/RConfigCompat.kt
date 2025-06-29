package org.polyfrost.oneconfig.internal.compat

import com.teamresourceful.resourcefulconfig.common.config.ResourcefulConfig

internal object RConfigCompat {

    @JvmStatic
    fun enable() {}

    @JvmStatic
    fun addConfig(config: ResourcefulConfig) {
        // according to gravy, no client side config used it in 1.19.2-1.20.1, so the compat isn't really needed since it won't show up in oneconfig anyway.
    }
}