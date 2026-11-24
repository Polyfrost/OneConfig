@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@ExperimentalOneConfigUiApi
@Composable
public fun DropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    entries: List<MenuEntry>,
    modifier: Modifier = Modifier,
    placement: Placement = Placement.BelowStart,
    matchAnchorWidth: Boolean = false,
    maxHeight: Dp = popupMaxHeight(),
    gap: Dp = MenuDefaults.anchorGap,
    flip: Boolean = true,
    tabNavigation: Boolean = false,
    sound: UiSound? = UiSound.Click,
    popupSounds: Boolean = LocalPopupSounds.current,
) {
    CompositionLocalProvider(LocalMenuSound provides sound, LocalPopupSounds provides popupSounds) {
        TriggeredDropdownMenu(expanded, onDismiss, entries, null, modifier, placement, matchAnchorWidth, maxHeight, gap, flip, tabNavigation)
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun DropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    placement: Placement = Placement.BelowStart,
    matchAnchorWidth: Boolean = false,
    gap: Dp = MenuDefaults.anchorGap,
    flip: Boolean = true,
    tabNavigation: Boolean = false,
    sound: UiSound? = UiSound.Click,
    popupSounds: Boolean = LocalPopupSounds.current,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalMenuSound provides sound, LocalPopupSounds provides popupSounds) {
        TriggeredDropdownMenu(expanded, onDismiss, null, modifier, placement, matchAnchorWidth, gap, flip, tabNavigation, content)
    }
}

@Composable
internal fun TriggeredDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    entries: List<MenuEntry>,
    trigger: PopupTrigger?,
    modifier: Modifier = Modifier,
    placement: Placement = Placement.BelowStart,
    matchAnchorWidth: Boolean = false,
    maxHeight: Dp = popupMaxHeight(),
    gap: Dp = MenuDefaults.anchorGap,
    flip: Boolean = true,
    tabNavigation: Boolean = false,
) {
    AnchoredPopup(expanded, onDismiss, placement, gap, matchAnchorWidth, trigger, flip) { anchorWidth ->
        MenuList(entries, onDismiss, anchorWidth.then(modifier).heightIn(max = maxHeight), tabNavigation = tabNavigation)
    }
}

@Composable
internal fun TriggeredDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    trigger: PopupTrigger?,
    modifier: Modifier = Modifier,
    placement: Placement = Placement.BelowStart,
    matchAnchorWidth: Boolean = false,
    gap: Dp = MenuDefaults.anchorGap,
    flip: Boolean = true,
    tabNavigation: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnchoredPopup(expanded, onDismiss, placement, gap, matchAnchorWidth, trigger, flip) { anchorWidth ->
        MenuSurface(anchorWidth.then(modifier), popupMaxHeight(), tabNavigation = tabNavigation, content = content)
    }
}

internal class PopupTrigger {
    val focusRequester = FocusRequester()
    var focused = false

    fun restoreFocus() {
        runCatching { focusRequester.requestFocus() }
    }
}

@Composable
internal fun rememberPopupTrigger(): PopupTrigger = remember { PopupTrigger() }

internal fun Modifier.popupTrigger(trigger: PopupTrigger): Modifier =
    focusRequester(trigger.focusRequester).onFocusChanged { trigger.focused = it.hasFocus }

@Composable
internal fun AnchoredPopup(
    expanded: Boolean,
    onDismiss: () -> Unit,
    placement: Placement,
    gap: Dp = 6.dp,
    matchAnchorWidth: Boolean = false,
    trigger: PopupTrigger? = null,
    flip: Boolean = true,
    content: @Composable (anchorWidth: Modifier) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = expanded
    if (!expanded && visibility.isIdle && !visibility.currentState) return
    var anchorWidth by remember { mutableIntStateOf(0) }
    if (matchAnchorWidth) {
        Layout(
            content = {},
            modifier = Modifier.onPlaced { anchorWidth = it.parentCoordinates?.size?.width ?: 0 },
        ) { _, _ -> layout(0, 0) {} }
    }
    if (expanded) {
        if (trigger != null) {
            val hadFocus = remember(trigger) { trigger.focused }
            DisposableEffect(trigger) {
                onDispose { if (hadFocus) scope.launch { trigger.restoreFocus() } }
            }
        }
        EscapeCloses(onDismiss)
        PopupSounds()
    }
    val latestDismiss by rememberUpdatedState(onDismiss)
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val origin = remember(placement, rtl) { placement.transformOrigin(rtl) }
    componentsBridge().Popup(rememberAnchoredPosition(placement, gap, flip), { if (visibility.targetState) latestDismiss() }, true) {
        val width = if (matchAnchorWidth && anchorWidth > 0) Modifier.widthIn(min = with(density) { anchorWidth.toDp() }) else Modifier
        PopupMotion(visibility, origin) { content(width) }
    }
}

@Composable
internal fun PopupSounds(enabled: Boolean = LocalPopupSounds.current) {
    if (!enabled) return
    DisposableEffect(Unit) {
        playUiSound(UiSound.Open)
        onDispose { playUiSound(UiSound.Close) }
    }
}

@Composable
internal fun PopupMotion(visibility: MutableTransitionState<Boolean>, origin: TransformOrigin, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visibleState = visibility,
        enter = Motion.enter(fadeIn(tween(Motion.standard)) + scaleIn(tween(Motion.standard), POPUP_HIDDEN_SCALE, origin)),
        exit = Motion.exit(fadeOut(tween(Motion.standard)) + scaleOut(tween(Motion.standard), POPUP_HIDDEN_SCALE, origin)),
    ) {
        Box(Modifier.blockInputWhen { !visibility.targetState }) { content() }
    }
}

private const val POPUP_HIDDEN_SCALE = 0.95f

internal fun Modifier.blockInputWhen(blocked: () -> Boolean): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (blocked()) event.changes.forEach { it.consume() }
        }
    }
}

private fun Placement.transformOrigin(rtl: Boolean): TransformOrigin {
    val start = if (rtl) 1f else 0f
    val end = 1f - start
    return when (this) {
        Placement.BelowStart -> TransformOrigin(start, 0f)
        Placement.BelowCenter -> TransformOrigin(0.5f, 0f)
        Placement.BelowEnd -> TransformOrigin(end, 0f)
        Placement.AboveStart -> TransformOrigin(start, 1f)
        Placement.AboveCenter -> TransformOrigin(0.5f, 1f)
        Placement.AboveEnd -> TransformOrigin(end, 1f)
        Placement.Start -> TransformOrigin(end, 0f)
        Placement.End -> TransformOrigin(start, 0f)
    }
}
