package org.polyfrost.oneconfig.internal.ui.components

import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.platform.v1.Platform

fun localizedText(key: String?, fallback: Any?): Any {
    val translationKey = key?.trim()?.takeIf { it.isNotEmpty() } ?: return fallback ?: ""
    return if (Platform.i18n().hasTranslation(translationKey)) {
        Platform.i18n().translate(translationKey)
    } else {
        fallback ?: translationKey
    }
}

fun localizedString(key: String?, fallback: Any?): String {
    val translationKey = key?.trim()?.takeIf { it.isNotEmpty() }
    return if (translationKey != null && Platform.i18n().hasTranslation(translationKey)) {
        Platform.i18n().translateString(translationKey)
    } else {
        fallback?.asRenderText() ?: translationKey.orEmpty()
    }
}

fun Node.localizedTitle(): Any = localizedText(getMetadata("titleKey"), title ?: id ?: "")

fun Node.localizedDescription(): Any? {
    val key = getMetadata<String>("descriptionKey")
    if (key.isNullOrBlank() && description == null) return null
    return localizedText(key, description)
}

fun Node.localizedGroup(metadataKey: String, keyMetadataKey: String, default: String): String {
    return localizedString(getMetadata(keyMetadataKey), getMetadata<String>(metadataKey) ?: default)
}
