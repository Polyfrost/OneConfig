package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import org.polyfrost.oneconfig.api.ui.v1.components.Chip as KitChip

@Composable
fun Chip(
    label: String,
    selected: Boolean,
    icon: String? = null,
    onClick: () -> Unit,
) {
    KitChip(label, selected, onClick, icon = icon)
}
