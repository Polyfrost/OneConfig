package org.polyfrost.oneconfig.api.ui.v1.internal

internal fun normaliseModId(modId: String) = modId.trim().removeSuffix(".json").lowercase()

internal fun modIdKeys(modId: String) = sequenceOf(
    modId,
    modId.removeSuffix(".json"),
    modId.substringBefore('/'),
    modId.substringBefore('/').removeSuffix(".json"),
).map { it.trim().lowercase() }.distinct()
