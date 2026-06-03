package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentIteratorType
import net.kyori.adventure.text.KeybindComponent
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource

@Composable
@Suppress("FunctionName")
fun TextComponent(component: Component) = buildAnnotatedString {
    component.iterable(ComponentIteratorType.DEPTH_FIRST).forEach {
        withStyle(
            SpanStyle(
            color = it.color()?.let { Color(it.value()).copy(alpha = 1f) } ?: Color.Unspecified
        )) {
            append(Platform.compatibility().resolveComponent(it))
        }
    }

}