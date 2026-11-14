@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Announce
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.page.PageText
import org.polyfrost.oneconfig.api.ui.v1.page.resolve
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
public sealed class LoadState {
    public data object Idle : LoadState()

    public data object Loading : LoadState()

    public data class Error(public val message: String) : LoadState()
}

private val ICON_SIZE = 32.dp
private val PROGRESS_WIDTH = 160.dp
private val TEXT_MAX_WIDTH = 420.dp
private const val RESIZE_MILLIS = 150

@ExperimentalOneConfigUiApi
@Composable
public fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    text: String? = null,
    state: LoadState = LoadState.Idle,
    onRetry: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val glyph: (@Composable (Modifier, Color) -> Unit)? = if (icon == null) null else { { iconModifier, tint -> Icon(icon, null, iconModifier, tint = tint) } }
    EmptyStateFrame(title, modifier, glyph, text, state, onRetry, action)
}

@ExperimentalOneConfigUiApi
@Composable
public fun EmptyState(
    title: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    text: String? = null,
    state: LoadState = LoadState.Idle,
    onRetry: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    EmptyStateFrame(title, modifier, { iconModifier, tint -> Image(icon, null, iconModifier, colorFilter = ColorFilter.tint(tint)) }, text, state, onRetry, action)
}

@Composable
private fun EmptyStateFrame(
    title: String,
    modifier: Modifier,
    icon: (@Composable (Modifier, Color) -> Unit)?,
    text: String?,
    state: LoadState,
    onRetry: (() -> Unit)?,
    action: (@Composable () -> Unit)?,
) {
    val theme = OneConfigTheme.current
    val spacing = theme.spacing
    val typography = theme.typography
    Announce((state as? LoadState.Error)?.message)
    Column(
        modifier
            .animateContentSize(Motion.finiteSpec(tween(RESIZE_MILLIS)))
            .padding(spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.m, Alignment.CenterVertically),
    ) {
        when (state) {
            LoadState.Loading -> ProgressBar(null, Modifier.widthIn(max = PROGRESS_WIDTH).padding(bottom = spacing.xs))
            is LoadState.Error -> Icon("alert-circle", null, Modifier.size(ICON_SIZE), tint = theme.error)
            LoadState.Idle -> icon?.invoke(Modifier.size(ICON_SIZE), theme.mutedText)
        }
        LayoutText(
            title,
            typography.heading,
            theme.textColor,
            Modifier.widthIn(max = TEXT_MAX_WIDTH).semantics { heading() },
            align = TextAlign.Center,
        )
        val body = (state as? LoadState.Error)?.message?.takeIf { it.isNotBlank() } ?: text
        if (body != null) {
            LayoutText(body, typography.small, theme.mutedText, Modifier.widthIn(max = TEXT_MAX_WIDTH), align = TextAlign.Center)
        }
        when {
            state is LoadState.Error && onRetry != null -> TextButton(
                layoutString("oneconfig.ui.retry", "Try again"),
                "refresh",
                onRetry,
                Modifier.padding(top = spacing.xs),
            )
            state != LoadState.Loading && action != null -> Column(Modifier.padding(top = spacing.xs)) { action() }
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun EmptyState(
    title: PageText,
    modifier: Modifier = Modifier,
    icon: String? = null,
    text: PageText? = null,
    state: LoadState = LoadState.Idle,
    onRetry: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    EmptyState(title.resolve(), modifier, icon, text?.resolve(), state, onRetry, action)
}

@ExperimentalOneConfigUiApi
@Composable
public fun EmptyState(
    title: PageText,
    icon: Painter,
    modifier: Modifier = Modifier,
    text: PageText? = null,
    state: LoadState = LoadState.Idle,
    onRetry: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    EmptyState(title.resolve(), icon, modifier, text?.resolve(), state, onRetry, action)
}
