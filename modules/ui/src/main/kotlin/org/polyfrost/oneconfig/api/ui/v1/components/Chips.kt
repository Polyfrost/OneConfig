package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@ExperimentalOneConfigUiApi
@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    componentsBridge().Chip(label, selected, null, onClick)
}

@ExperimentalOneConfigUiApi
@Composable
fun Chip(label: String, selected: Boolean, icon: String?, onClick: () -> Unit) {
    componentsBridge().Chip(label, selected, icon, onClick)
}
