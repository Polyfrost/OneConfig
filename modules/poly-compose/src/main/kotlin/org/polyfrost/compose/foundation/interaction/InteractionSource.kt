package org.polyfrost.compose.foundation.interaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

abstract class InteractionSource internal constructor() {
    internal val hovered: MutableState<Boolean> = mutableStateOf(false)
    internal val pressed: MutableState<Boolean> = mutableStateOf(false)
    internal val focused: MutableState<Boolean> = mutableStateOf(false)
}

class MutableInteractionSource : InteractionSource()

@Composable
fun InteractionSource.collectIsHoveredAsState(): State<Boolean> = hovered

@Composable
fun InteractionSource.collectIsPressedAsState(): State<Boolean> = pressed

@Composable
fun InteractionSource.collectIsFocusedAsState(): State<Boolean> = focused
