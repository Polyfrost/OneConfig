package org.polyfrost.oneconfig.internal.compat.yacl

//#if MC < 26.1
//#if MC != 1.20.4 || FABRIC
object ExtraV1Handlers {
    init {
        extraYaclHandlers.add(AbstractDropdownControllerCompat)
    }
}
//#endif
//#endif