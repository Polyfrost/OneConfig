package org.polyfrost.oneconfig.internal.ui.components

import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.platform.v1.Platform

internal fun translationKeyOrNull(value: Any?): String? {
    val text = (value as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (text.indexOf('.') < 0 || text.any { it.isWhitespace() }) return null
    return if (Platform.i18n().hasTranslation(text)) text else null
}

fun localizedText(key: String?, fallback: Any?): Any {
    val translationKey = key?.trim()?.takeIf { it.isNotEmpty() }
        ?: return translationKeyOrNull(fallback)?.let { Platform.i18n().translate(it) } ?: fallback ?: ""
    return Platform.i18n().translate(translationKey, fallback ?: translationKey)
}

fun localizedString(key: String?, fallback: Any?): String {
    val translationKey = key?.trim()?.takeIf { it.isNotEmpty() }
    if (translationKey != null && Platform.i18n().hasTranslation(translationKey)) {
        return Platform.i18n().translateString(translationKey)
    }
    translationKeyOrNull(fallback)?.let { return Platform.i18n().translateString(it) }
    return fallback?.asRenderText() ?: translationKey.orEmpty()
}

/**
 * Translates [value] when it is a bare translation key such as `some.mod.option` and otherwise returns it
 * unchanged
 *
 * Use at any display site reading a raw string that mods may have filled with a translation key
 */
fun localizedValue(value: Any?): Any? =
    translationKeyOrNull(value)?.let { Platform.i18n().translate(it) } ?: value

/** [localizedValue] for sites which need a plain string */
fun localizedLabel(text: String?): String? =
    translationKeyOrNull(text)?.let { Platform.i18n().translateString(it) } ?: text

fun Node.localizedTitle(): Any = localizedText(getMetadata("titleKey"), title ?: id ?: "")

fun Node.localizedDescription(): Any? {
    val key = getMetadata<String>("descriptionKey")
    if (key.isNullOrBlank() && description == null) return null
    return localizedText(key, description)
}

fun Node.localizedGroup(metadataKey: String, keyMetadataKey: String, default: String): String {
    return localizedString(getMetadata(keyMetadataKey), getMetadata<String>(metadataKey) ?: default)
}
