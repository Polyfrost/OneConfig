package org.polyfrost.oneconfig.compat

import net.kyori.adventure.text.Component

interface VersionUtils {
}
private lateinit var utils: VersionUtils

object OneConfigVersionUtils : VersionUtils by utils {
    init {
        utils = Class.forName("org.polyfrost.oneconfig.compat.OneConfigVersionUtilsImpl")
            .getField("INSTANCE")
            .get(null) as VersionUtils
    }
}