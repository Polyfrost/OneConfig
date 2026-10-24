package org.polyfrost.compose.ui.text.style

enum class TextAlign(internal val factor: Float) { Start(0f), Center(0.5f), End(1f) }

enum class TextOverflow { Visible, Clip, Ellipsis }
