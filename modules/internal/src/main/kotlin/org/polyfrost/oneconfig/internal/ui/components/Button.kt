package org.polyfrost.oneconfig.internal.ui.components

internal fun iconButtonLabel(iconName: String): String = when (iconName) {
    "left-arrow" -> translated("oneconfig.button.back", "Back")
    "right-arrow" -> translated("oneconfig.button.forward", "Forward")
    "close" -> translated("oneconfig.button.close", "Close")
    "folder" -> translated("oneconfig.button.browse", "Browse")
    "trash" -> translated("oneconfig.list.remove", "Remove")
    else -> iconName.replace('-', ' ').replaceFirstChar { it.uppercase() }
}
